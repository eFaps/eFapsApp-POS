/*
 * Copyright 2003 - 2019 The eFaps Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.efaps.esjp.pos.report;

import java.io.File;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

@EFapsUUID("8b2e901e-9b65-4670-b6cf-ecb23baec42f")
@EFapsApplication("eFapsApp-POS")
public abstract class BalanceReport_Base
    extends FilteredReport
{

    public enum Grouping
    {
        PAYMENTTYPE,
        USER,
        BALANCE;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing html snipplet
     * @throws EFapsException on error
     */
    public Return generateReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final AbstractDynamicReport dyRp = getReport(_parameter);
        final String html = dyRp.getHtmlSnipplet(_parameter);
        ret.put(ReturnValues.SNIPLETT, html);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return Return containing the file
     * @throws EFapsException on error
     */
    public Return exportReport(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final String mime = getProperty(_parameter, "Mime");
        final AbstractDynamicReport dyRp = getReport(_parameter);
        dyRp.setFileName(getDBProperty("FileName"));
        File file = null;
        if ("xls".equalsIgnoreCase(mime)) {
            file = dyRp.getExcel(_parameter);
        } else if ("pdf".equalsIgnoreCase(mime)) {
            file = dyRp.getPDF(_parameter);
        }
        ret.put(ReturnValues.VALUES, file);
        ret.put(ReturnValues.TRUE, true);
        return ret;
    }

    /**
     * @param _parameter Parameter as passed by the eFasp API
     * @return the report class
     * @throws EFapsException on error
     */
    protected AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynBalanceReport(this);
    }

    /**
     * The Class DynDocBalanceReport.
     */
    public static class DynBalanceReport
        extends AbstractDynamicReport
    {

        /** The filtered report. */
        private final BalanceReport_Base filteredReport;

        /**
         * Instantiates a new dyn doc balance report.
         *
         * @param _filteredReport the filtered report
         */
        public DynBalanceReport(final BalanceReport_Base _filteredReport)
        {
            filteredReport = _filteredReport;
        }

        @Override
        protected JRDataSource createDataSource(final Parameter _parameter)
            throws EFapsException
        {
            final JRRewindableDataSource ret;
            if (getFilteredReport().isCached(_parameter)) {
                ret = getFilteredReport().getDataSourceFromCache(_parameter);
                try {
                    ret.moveFirst();
                } catch (final JRException e) {
                    throw new EFapsException("JRException", e);
                }
            } else {
                final List<DataBean> values = new ArrayList<>();
                final QueryBuilder balanceAttrQueryBldr = getQueryBldrFromProperties(_parameter, Pos.BALANCE_REPORT.get());
                add2QueryBldr(_parameter, balanceAttrQueryBldr);

                final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Balance2Document);
                queryBldr.addWhereAttrInQuery(CIPOS.Balance2Document.FromLink, balanceAttrQueryBldr.getAttributeQuery(
                                CIPOS.Balance.ID));

                final MultiPrintQuery multi = queryBldr.getPrint();
                final SelectBuilder selBalance = SelectBuilder.get().linkto(CIPOS.Balance2Document.FromLink);
                final SelectBuilder selBalanceName = new SelectBuilder(selBalance).attribute(CIPOS.Balance.Name);
                final SelectBuilder selUserFirstName = new SelectBuilder(selBalance).linkto(CIPOS.Balance.UserLink)
                                .linkto(CIPOS.User.EmployeeLink)
                                .attribute(CIHumanResource.Employee.FirstName);
                final SelectBuilder selUserLastName = new SelectBuilder(selBalance).linkto(CIPOS.Balance.UserLink)
                                .linkto(CIPOS.User.EmployeeLink)
                                .attribute(CIHumanResource.Employee.LastName);
                final SelectBuilder selDoc = SelectBuilder.get().linkto(CIPOS.Balance2Document.ToLink);
                final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
                final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Name);
                multi.addSelect(selBalanceName, selDocName, selDocInst, selUserFirstName, selUserLastName);
                multi.execute();
                final Map<Instance, DataBean> docBeans = new HashMap<>();
                while (multi.next()) {
                    final DataBean bean = getDataBean()
                                    .setBalanceName(multi.getSelect(selBalanceName))
                                    .setUserFirstName(multi.getSelect(selUserFirstName))
                                    .setUserLastName(multi.getSelect(selUserLastName))
                                    .setDocName(multi.getSelect(selDocName));
                    final Instance docInst = multi.getSelect(selDocInst);
                    docBeans.put(docInst, bean);
                }
                if (!docBeans.isEmpty()) {
                    final QueryBuilder paymentQueryBldr = new QueryBuilder(CISales.Payment);
                    paymentQueryBldr.addWhereAttrEqValue(CISales.Payment.CreateDocument, docBeans.keySet().toArray());

                    final SelectBuilder selCreateDoc = SelectBuilder.get().linkto(CISales.Payment.CreateDocument);
                    final SelectBuilder selCreateDocInst = new SelectBuilder(selCreateDoc).instance();
                    final SelectBuilder selTargetDoc = SelectBuilder.get().linkto(CISales.Payment.TargetDocument);
                    final SelectBuilder selTargetDocInst = new SelectBuilder(selTargetDoc).instance();
                    final SelectBuilder selTargetDocName = new SelectBuilder(selTargetDoc).attribute(
                                    CISales.PaymentDocumentIOAbstract.Name);
                    final SelectBuilder selTargetDocCode = new SelectBuilder(selTargetDoc).attribute(
                                    CISales.PaymentDocumentIOAbstract.Code);
                    final SelectBuilder selTargetDocAmount = new SelectBuilder(selTargetDoc).attribute(
                                    CISales.PaymentDocumentIOAbstract.Amount);

                    final MultiPrintQuery paymentMulti = paymentQueryBldr.getPrint();
                    paymentMulti.addSelect(selCreateDocInst, selTargetDocInst, selTargetDocName, selTargetDocCode, selTargetDocAmount);
                    paymentMulti.execute();
                    final Map<Instance, DataBean> cardPayments = new HashMap<>();
                    while (paymentMulti.next()) {
                        final Instance docInst = paymentMulti.getSelect(selCreateDocInst);
                        final Instance paymentInst = paymentMulti.getSelect(selTargetDocInst);
                        String paymentType = null;
                        if (!InstanceUtils.isType(paymentInst, CISales.PaymentCard)) {
                            paymentType = paymentInst.getType().getLabel();
                        }
                        BigDecimal amount = paymentMulti.getSelect(selTargetDocAmount);
                        if (InstanceUtils.isKindOf(paymentInst, CISales.PaymentDocumentOutAbstract)) {
                            amount = amount.negate();
                        }
                        final DataBean docBean = docBeans.get(docInst);
                        final DataBean bean = getDataBean()
                                        .setBalanceName(docBean.getBalanceName())
                                        .setUserFirstName(docBean.getUserFirstName())
                                        .setUserLastName(docBean.getUserLastName())
                                        .setDocName(docBean.getDocName())
                                        .setPaymentName(paymentMulti.getSelect(selTargetDocName))
                                        .setPaymentCode(paymentMulti.getSelect(selTargetDocCode))
                                        .setPaymentType(paymentType)
                                        .setAmount(amount);
                        if (paymentType == null) {
                            cardPayments.put(paymentInst, bean);
                        }
                        values.add(bean);
                    }

                    if (!cardPayments.isEmpty()) {
                        final MultiPrintQuery cardPaymentMulti = new MultiPrintQuery(new ArrayList<>(cardPayments.keySet()));
                        final SelectBuilder selCardPayment = SelectBuilder.get().linkto(CISales.PaymentCard.CardType)
                                        .attribute(CISales.AttributeDefinitionPaymentCardType.Value);
                        cardPaymentMulti.addSelect(selCardPayment);
                        cardPaymentMulti.executeWithoutAccessCheck();
                        while (cardPaymentMulti.next()) {
                            if (cardPayments.containsKey(cardPaymentMulti.getCurrentInstance())) {
                                cardPayments.get(cardPaymentMulti.getCurrentInstance())
                                    .setPaymentType(cardPaymentMulti.getSelect(selCardPayment));
                            }
                        }
                    }
                }

                final ComparatorChain<DataBean> chain = new ComparatorChain<>();
                final Map<String, Object> filters = getFilteredReport().getFilterMap(_parameter);
                final GroupByFilterValue groupBy = (GroupByFilterValue) filters.get("groupBy");
                if (groupBy != null) {
                    final List<Enum<?>> selected = groupBy.getObject();
                    for (final Enum<?> sel : selected) {
                        switch ((Grouping) sel) {
                            case PAYMENTTYPE:
                                chain.addComparator((_arg0, _arg1) -> _arg0.getPaymentType().compareTo(_arg1.getPaymentType()));
                                break;
                            case USER:
                                chain.addComparator((_arg0, _arg1) -> _arg0.getUserName().compareTo(_arg1.getUserName()));
                                break;
                            case BALANCE:
                                chain.addComparator((_arg0, _arg1) -> _arg0.getBalanceName().compareTo(_arg1.getBalanceName()));
                                break;
                            default:
                                break;
                        }
                    }
                }
                chain.addComparator((_arg0, _arg1) -> _arg0.getPaymentCode().compareTo(_arg1.getPaymentCode()));
                Collections.sort(values, chain);
                ret = new JRBeanCollectionDataSource(values);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        protected void add2QueryBldr(final Parameter _parameter,
                                     final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filterMap = filteredReport.getFilterMap(_parameter);
            final DateTime date;
            if (filterMap.containsKey("date")) {
                date = (DateTime) filterMap.get("date");
            } else {
                date = new DateTime();
            }
            _queryBldr.addWhereAttrGreaterValue(CIPOS.Balance.StartAt, date.withTimeAtStartOfDay().minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(CIPOS.Balance.StartAt, date.plusDays(1).withTimeAtStartOfDay());
        }

        /**
         * Gets the filtered report.
         *
         * @return the filtered report
         */
        public BalanceReport_Base getFilteredReport()
        {
            return filteredReport;
        }

        public DataBean getDataBean()
        {
            return new DataBean();
        }

        @Override
        protected void addColumnDefinition(final Parameter _parameter,
                                           final JasperReportBuilder _builder)
            throws EFapsException
        {
            final TextColumnBuilder<String> balanceName = DynamicReports.col.column(label("balanceName"), "balanceName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> userName = DynamicReports.col.column(label("userName"), "userName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> docName = DynamicReports.col.column(label("docName"), "docName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> paymentName = DynamicReports.col.column(label("paymentName"), "paymentName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> paymentCode = DynamicReports.col.column(label("paymentCode"), "paymentCode",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> paymentType = DynamicReports.col.column(label("paymentType"), "paymentType",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<BigDecimal> amount = DynamicReports.col.column(label("amount"), "amount",
                            DynamicReports.type.bigDecimalType());

            final Map<String, Object> filters = getFilteredReport().getFilterMap(_parameter);
            final GroupByFilterValue groupBy = (GroupByFilterValue) filters.get("groupBy");
            if (groupBy != null) {
                final List<Enum<?>> selected = groupBy.getObject();
                for (final Enum<?> sel : selected) {
                    switch ((Grouping) sel) {
                        case PAYMENTTYPE:
                            final ColumnGroupBuilder paymentTypeGroup = DynamicReports.grp.group(paymentType).groupByDataType();
                            final AggregationSubtotalBuilder<BigDecimal> groupSum = DynamicReports.sbt.sum(amount);
                            _builder.groupBy(paymentTypeGroup);
                            _builder.addSubtotalAtGroupFooter(paymentTypeGroup, groupSum);
                            break;
                        case USER:
                            final ColumnGroupBuilder userGroup = DynamicReports.grp.group(userName).groupByDataType();
                            final AggregationSubtotalBuilder<BigDecimal> userGroupSum = DynamicReports.sbt.sum(amount);
                            _builder.groupBy(userGroup);
                            _builder.addSubtotalAtGroupFooter(userGroup, userGroupSum);
                            break;
                        case BALANCE:
                            final ColumnGroupBuilder balanceGroup = DynamicReports.grp.group(balanceName).groupByDataType();
                            final AggregationSubtotalBuilder<BigDecimal> balanceGroupSum = DynamicReports.sbt.sum(amount);
                            _builder.groupBy(balanceGroup);
                            _builder.addSubtotalAtGroupFooter(balanceGroup, balanceGroupSum);
                            break;
                        default:
                            break;
                    }
                }
            }
            _builder
                .addColumn(balanceName, userName, docName, paymentName, paymentCode, paymentType, amount)
                .subtotalsAtSummary(DynamicReports.sbt.sum(amount));
        }

        protected String label(final String _key) {
            return getFilteredReport().getDBProperty("Column." + _key);
        }
    }

    public static class DataBean
    {

        private String balanceName;
        private String docName;
        private String userFirstName;
        private String userLastName;
        private String paymentName;
        private String paymentCode;
        private String paymentType;

        private BigDecimal amount;

        public String getBalanceName()
        {
            return balanceName;
        }

        public DataBean setBalanceName(final String _balanceName)
        {
            balanceName = _balanceName;
            return this;
        }

        public String getDocName()
        {
            return docName;
        }

        public DataBean setDocName(final String _docName)
        {
            docName = _docName;
            return this;
        }

        public String getUserFirstName()
        {
            return userFirstName;
        }

        public DataBean setUserFirstName(final String _userFirstName)
        {
            userFirstName = _userFirstName;
            return this;
        }

        public String getUserLastName()
        {
            return userLastName;
        }

        public DataBean setUserLastName(final String _userLastName)
        {
            userLastName = _userLastName;
            return this;
        }

        public String getUserName() {
            return getUserFirstName() + " " + getUserLastName();
        }

        public String getPaymentName()
        {
            return paymentName;
        }

        public DataBean setPaymentName(final String _paymentName)
        {
            paymentName = _paymentName;
            return this;
        }

        public String getPaymentCode()
        {
            return paymentCode;
        }

        public DataBean setPaymentCode(final String _paymentCode)
        {
            paymentCode = _paymentCode;
            return this;
        }

        public String getPaymentType()
        {
            return paymentType;
        }

        public DataBean setPaymentType(final String _paymentType)
        {
            paymentType = _paymentType;
            return this;
        }

        public BigDecimal getAmount()
        {
            return amount;
        }

        public DataBean setAmount(final BigDecimal _amount)
        {
            amount = _amount;
            return this;
        }
    }
}