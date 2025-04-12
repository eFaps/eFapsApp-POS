/*
 * Copyright © 2003 - 2024 The eFaps Team (-)
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
 */
package org.efaps.esjp.pos.report;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.comparators.ComparatorChain;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.jasperreport.AbstractDynamicReport;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.FilteredReport;
import org.efaps.esjp.erp.rest.modules.IFilteredReportProvider;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.esjp.ui.rest.dto.OptionDto;
import org.efaps.esjp.ui.rest.dto.ValueDto;
import org.efaps.esjp.ui.rest.dto.ValueType;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.dynamicreports.jasper.builder.JasperReportBuilder;
import net.sf.dynamicreports.report.builder.DynamicReports;
import net.sf.dynamicreports.report.builder.column.TextColumnBuilder;
import net.sf.dynamicreports.report.builder.group.ColumnGroupBuilder;
import net.sf.dynamicreports.report.builder.subtotal.AggregationSubtotalBuilder;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;

@EFapsUUID("8b2e901e-9b65-4670-b6cf-ecb23baec42f")
@EFapsApplication("eFapsApp-POS")
public abstract class BalanceReport_Base
    extends FilteredReport
    implements IFilteredReportProvider
{

    private static final Logger LOG = LoggerFactory.getLogger(BalanceReport.class);

    public enum Grouping
    {
        BACKEND, PAYMENTTYPE, USER, BALANCE;
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
    @Override
    public AbstractDynamicReport getReport(final Parameter _parameter)
        throws EFapsException
    {
        return new DynBalanceReport(this);
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<ValueDto> getFilters()
    {
        ZoneId zoneId = ZoneId.systemDefault();
        try {
            clearCache(ParameterUtil.instance());
            zoneId = Context.getThreadContext().getZoneId();
        } catch (final EFapsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        final var filterMap = getFilterMap();
        String dateFromValue = null;
        String dateToValue = null;
        List<String> groupByValue=  null;
        if (filterMap != null && filterMap.containsKey("dateFrom")) {
            dateFromValue = ((DateTime) filterMap.get("dateFrom")).toLocalDate().toString();
        } else {
            dateFromValue = LocalDate.now(zoneId).toString();
        }

        if (filterMap != null && filterMap.containsKey("dateTo")) {
            dateToValue = ((DateTime) filterMap.get("dateTo")).toLocalDate().toString();
        } else {
            dateToValue = LocalDate.now(zoneId).toString();
        }

        if (filterMap != null && filterMap.containsKey("groupBy")) {
            groupByValue = ((List<Grouping>) filterMap.get("groupBy")).stream().map(Grouping::name).toList();
        }

        final var dateFrom = ValueDto.builder()
                        .withName("dateFrom")
                        .withLabel(DBProperties.getProperty("org.efaps.esjp.pos.report.BalanceReport.dateFrom"))
                        .withType(ValueType.DATE)
                        .withRequired(true)
                        .withValue(dateFromValue)
                        .build();
        final var dateTo = ValueDto.builder()
                        .withName("dateTo")
                        .withLabel(DBProperties.getProperty("org.efaps.esjp.pos.report.BalanceReport.dateTo"))
                        .withType(ValueType.DATE)
                        .withRequired(true)
                        .withValue(dateToValue)
                        .build();

        final var groupBy = ValueDto.builder()
                        .withName("groupBy")
                        .withLabel(DBProperties.getProperty("org.efaps.esjp.pos.report.BalanceReport.groupBy"))
                        .withType(ValueType.PICKLIST)
                        .withValue(groupByValue)
                        .withOptions(Arrays.asList(OptionDto.builder()
                                        .withLabel(DBProperties.getProperty(
                                                        "org.efaps.esjp.pos.report.BalanceReport_Base$Grouping.BALANCE"))
                                        .withValue(Grouping.BALANCE.name())
                                        .build(),
                                        OptionDto.builder()
                                                        .withLabel(DBProperties.getProperty(
                                                                        "org.efaps.esjp.pos.report.BalanceReport_Base$Grouping.BACKEND"))
                                                        .withValue(Grouping.BACKEND.name())
                                                        .build(),
                                        OptionDto.builder()
                                                        .withLabel(DBProperties.getProperty(
                                                                        "org.efaps.esjp.pos.report.BalanceReport_Base$Grouping.PAYMENTTYPE"))
                                                        .withValue(Grouping.PAYMENTTYPE.name())
                                                        .build(),
                                        OptionDto.builder()
                                                        .withLabel(DBProperties.getProperty(
                                                                        "org.efaps.esjp.pos.report.BalanceReport_Base$Grouping.USER"))
                                                        .withValue(Grouping.USER.name())
                                                        .build()))
                        .build();
        return Arrays.asList(dateFrom, dateTo, groupBy);
    }

    @Override
    public Object evalFilterValue4Key(final String key,
                                      final List<String> values)
    {
        return values.stream().map(value -> EnumUtils.getEnum(Grouping.class, value)).toList();
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
                final QueryBuilder balanceAttrQueryBldr = getQueryBldrFromProperties(_parameter,
                                Pos.BALANCE_REPORT.get());
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
                final SelectBuilder selBackendName = new SelectBuilder(selBalance).linkto(CIPOS.Balance.BackendLink)
                                .attribute(CIPOS.Backend.Name);
                final SelectBuilder selDoc = SelectBuilder.get().linkto(CIPOS.Balance2Document.ToLink);
                final SelectBuilder selDocInst = new SelectBuilder(selDoc).instance();
                final SelectBuilder selDocName = new SelectBuilder(selDoc).attribute(CISales.DocumentAbstract.Name);
                multi.addSelect(selBalanceName, selDocName, selDocInst, selUserFirstName, selUserLastName,
                                selBackendName);
                multi.execute();
                final Map<Instance, DataBean> docBeans = new HashMap<>();
                while (multi.next()) {
                    final Instance docInst = multi.getSelect(selDocInst);
                    final DataBean bean = getDataBean()
                                    .setBackendName(multi.getSelect(selBackendName))
                                    .setBalanceName(multi.getSelect(selBalanceName))
                                    .setUserFirstName(multi.getSelect(selUserFirstName))
                                    .setUserLastName(multi.getSelect(selUserLastName))
                                    .setDocName(multi.getSelect(selDocName));
                    if (InstanceUtils.isValid(docInst)) {
                        bean.setDocType(docInst.getType().getLabel());
                        LOG.debug("Bean: {}", bean);
                        docBeans.put(docInst, bean);
                    }
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
                    paymentMulti.addSelect(selCreateDocInst, selTargetDocInst, selTargetDocName, selTargetDocCode,
                                    selTargetDocAmount);
                    paymentMulti.execute();
                    final Map<Instance, DataBean> cardPayments = new HashMap<>();
                    final Map<Instance, DataBean> electronicPayments = new HashMap<>();
                    while (paymentMulti.next()) {
                        final Instance docInst = paymentMulti.getSelect(selCreateDocInst);
                        final Instance paymentInst = paymentMulti.getSelect(selTargetDocInst);
                        BigDecimal amount = paymentMulti.getSelect(selTargetDocAmount);
                        if (InstanceUtils.isKindOf(paymentInst, CISales.PaymentDocumentOutAbstract)) {
                            amount = amount.negate();
                        }
                        final DataBean docBean = docBeans.get(docInst);
                        final DataBean bean = getDataBean()
                                        .setBackendName(docBean.getBackendName())
                                        .setBalanceName(docBean.getBalanceName())
                                        .setUserFirstName(docBean.getUserFirstName())
                                        .setUserLastName(docBean.getUserLastName())
                                        .setDocName(docBean.getDocName())
                                        .setDocType(docBean.getDocType())
                                        .setPaymentName(paymentMulti.getSelect(selTargetDocName))
                                        .setPaymentCode(paymentMulti.getSelect(selTargetDocCode))
                                        .setPaymentType(paymentInst.getType().getLabel())
                                        .setAmount(amount);
                        if (InstanceUtils.isType(paymentInst, CISales.PaymentCard)) {
                            cardPayments.put(paymentInst, bean);
                        }
                        if (InstanceUtils.isType(paymentInst, CISales.PaymentElectronic)) {
                            electronicPayments.put(paymentInst, bean);
                        }
                        values.add(bean);
                    }
                    cardPayments(cardPayments);
                    electronicPayments(electronicPayments);
                }

                final ComparatorChain<DataBean> chain = new ComparatorChain<>();
                final Map<String, Object> filters = getFilteredReport().getFilterMap(_parameter);
                final var rawGroupBy = filters.get("groupBy");
                if (rawGroupBy != null) {
                    final var selected = evalGroupBy(rawGroupBy);
                    for (final var sel : selected) {
                        switch (sel) {
                            case BACKEND:
                                chain.addComparator(Comparator.comparing(DataBean::getBackendName));
                                break;
                            case PAYMENTTYPE:
                                chain.addComparator(Comparator.comparing(DataBean::getPaymentType));
                                break;
                            case USER:
                                chain.addComparator(
                                                Comparator.comparing(DataBean::getUserName));
                                break;
                            case BALANCE:
                                chain.addComparator(Comparator.comparing(DataBean::getBalanceName));
                                break;
                            default:
                                break;
                        }
                    }
                }
                chain.addComparator(Comparator.comparing(DataBean::getPaymentCode));
                Collections.sort(values, chain);
                ret = new JRBeanCollectionDataSource(values);
                getFilteredReport().cache(_parameter, ret);
            }
            return ret;
        }

        protected void cardPayments(final Map<Instance, DataBean> _cardPayments)
            throws EFapsException
        {
            if (!_cardPayments.isEmpty()) {
                final MultiPrintQuery paymentMulti = new MultiPrintQuery(new ArrayList<>(_cardPayments.keySet()));
                final SelectBuilder selCardPayment = SelectBuilder.get().linkto(CISales.PaymentCard.CardType)
                                .attribute(CISales.AttributeDefinitionPaymentCardType.Value);
                paymentMulti.addSelect(selCardPayment);
                paymentMulti.executeWithoutAccessCheck();
                while (paymentMulti.next()) {
                    if (_cardPayments.containsKey(paymentMulti.getCurrentInstance())) {
                        final String paymentType = paymentMulti.getSelect(selCardPayment);
                        if (StringUtils.isNotEmpty(paymentType)) {
                            _cardPayments.get(paymentMulti.getCurrentInstance())
                                            .setPaymentType(paymentType);
                        }
                    }
                }
            }
        }

        protected void electronicPayments(final Map<Instance, DataBean> _electronicPayments)
            throws EFapsException
        {
            if (!_electronicPayments.isEmpty()) {
                final MultiPrintQuery paymentMulti = new MultiPrintQuery(new ArrayList<>(_electronicPayments.keySet()));
                final SelectBuilder selCardPayment = SelectBuilder.get()
                                .linkto(CISales.PaymentElectronic.ElectronicPaymentType)
                                .attribute(CISales.AttributeDefinitionPaymentElectronicType.Value);
                paymentMulti.addSelect(selCardPayment);
                paymentMulti.executeWithoutAccessCheck();
                while (paymentMulti.next()) {
                    if (_electronicPayments.containsKey(paymentMulti.getCurrentInstance())) {
                        final String paymentType = paymentMulti.getSelect(selCardPayment);
                        if (StringUtils.isNotEmpty(paymentType)) {
                            _electronicPayments.get(paymentMulti.getCurrentInstance())
                                            .setPaymentType(paymentType);
                        }
                    }
                }
            }
        }

        protected void add2QueryBldr(final Parameter _parameter,
                                     final QueryBuilder _queryBldr)
            throws EFapsException
        {
            final Map<String, Object> filterMap = filteredReport.getFilterMap(_parameter);
            final DateTime dateFrom;
            if (filterMap.containsKey("dateFrom")) {
                dateFrom = (DateTime) filterMap.get("dateFrom");
            } else {
                dateFrom = new DateTime();
            }
            final DateTime dateTo;
            if (filterMap.containsKey("dateTo")) {
                dateTo = (DateTime) filterMap.get("dateTo");
            } else {
                dateTo = new DateTime();
            }
            _queryBldr.addWhereAttrGreaterValue(CIPOS.Balance.StartAt, dateFrom.withTimeAtStartOfDay().minusMinutes(1));
            _queryBldr.addWhereAttrLessValue(CIPOS.Balance.StartAt, dateTo.plusDays(1).withTimeAtStartOfDay());
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
            final TextColumnBuilder<String> backendName = DynamicReports.col.column(label("backendName"), "backendName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> balanceName = DynamicReports.col.column(label("balanceName"), "balanceName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> userName = DynamicReports.col.column(label("userName"), "userName",
                            DynamicReports.type.stringType());
            final TextColumnBuilder<String> docType = DynamicReports.col.column(label("docType"), "docType",
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

            final var rawGroupBy = filters.get("groupBy");
            if (rawGroupBy != null) {
                final var selected = evalGroupBy(rawGroupBy);
                for (final var sel : selected) {
                    switch (sel) {
                        case BACKEND:
                            final ColumnGroupBuilder backendGroup = DynamicReports.grp.group(backendName)
                                            .groupByDataType();
                            final AggregationSubtotalBuilder<BigDecimal> backendGroupSum = DynamicReports.sbt
                                            .sum(amount);
                            _builder.groupBy(backendGroup);
                            _builder.addSubtotalAtGroupFooter(backendGroup, backendGroupSum);
                            break;
                        case PAYMENTTYPE:
                            final ColumnGroupBuilder paymentTypeGroup = DynamicReports.grp.group(paymentType)
                                            .groupByDataType();
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
                            final ColumnGroupBuilder balanceGroup = DynamicReports.grp.group(balanceName)
                                            .groupByDataType();
                            final AggregationSubtotalBuilder<BigDecimal> balanceGroupSum = DynamicReports.sbt
                                            .sum(amount);
                            _builder.groupBy(balanceGroup);
                            _builder.addSubtotalAtGroupFooter(balanceGroup, balanceGroupSum);
                            break;
                        default:
                            break;
                    }
                }

            }
            _builder.addColumn(backendName, balanceName, userName, docType, docName, paymentName, paymentCode,
                            paymentType, amount)
                            .subtotalsAtSummary(DynamicReports.sbt.sum(amount));
        }

        protected String label(final String _key)
        {
            return getFilteredReport().getDBProperty("Column." + _key);
        }

        @SuppressWarnings("unchecked")
        protected List<Grouping> evalGroupBy(final Object rawGroupBy) {
            List<Grouping> entries = null;
            if (rawGroupBy != null) {
                if (rawGroupBy instanceof final GroupByFilterValue groupBy) {
                    entries = groupBy.getObject().stream().map(val -> ((Grouping) val)).toList();
                } else {
                    entries = (List<Grouping>) rawGroupBy;
                }
            }
            return entries;
        }
    }

    public static class DataBean
    {

        private String backendName;
        private String balanceName;
        private String docType;
        private String docName;
        private String userFirstName;
        private String userLastName;
        private String paymentName;
        private String paymentCode;
        private String paymentType;

        private BigDecimal amount;

        public String getBackendName()
        {
            return backendName;
        }

        public DataBean setBackendName(String backendName)
        {
            this.backendName = backendName;
            return this;
        }

        public String getBalanceName()
        {
            return balanceName;
        }

        public DataBean setBalanceName(final String _balanceName)
        {
            balanceName = _balanceName;
            return this;
        }

        public String getDocType()
        {
            return docType;
        }

        public DataBean setDocType(final String docType)
        {
            this.docType = docType;
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

        public String getUserName()
        {
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
