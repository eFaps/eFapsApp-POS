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
package org.efaps.esjp.pos.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.UUID;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.stmt.selection.Evaluator;
import org.efaps.eql.EQL;
import org.efaps.eql2.StmtFlag;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.pos.util.DocumentUtils;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.esjp.sales.document.TransactionDocument;
import org.efaps.esjp.sales.payment.AbstractPaymentDocument;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.TaxCat;
import org.efaps.esjp.sales.tax.TaxCat_Base;
import org.efaps.esjp.sales.tax.Tax_Base;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.pos.dto.AbstractDocItemDto;
import org.efaps.pos.dto.AbstractDocumentDto;
import org.efaps.pos.dto.AbstractPayableDocumentDto;
import org.efaps.pos.dto.CreditNoteDto;
import org.efaps.pos.dto.InvoiceDto;
import org.efaps.pos.dto.PaymentDto;
import org.efaps.pos.dto.PaymentType;
import org.efaps.pos.dto.ReceiptDto;
import org.efaps.pos.dto.TaxEntryDto;
import org.efaps.pos.dto.TicketDto;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("2c0b2e38-14cb-474a-8b49-0859f38784c5")
@EFapsApplication("eFapsApp-POS")
public abstract class AbstractDocument_Base
    extends AbstractRest
{
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDocument.class);

    protected abstract CIType getDocumentType();
    protected abstract CIType getPositionType();
    protected abstract CIType getEmployee2DocumentType();
    protected abstract CIType getDepartment2DocumentType();

    protected Instance createDocument(final Status _status, final AbstractDocumentDto _dto)
        throws EFapsException
    {
        final Insert insert = new Insert(getDocumentType());
        insert.add(CISales.DocumentSumAbstract.Name, _dto.getNumber());
        insert.add(CISales.DocumentSumAbstract.Date, _dto.getDate());
        insert.add(CISales.DocumentSumAbstract.StatusAbstract, _status);
        insert.add(CISales.DocumentSumAbstract.Note, _dto.getNote());

        final Instance contactInst = Instance.get(_dto.getContactOid());
        if (InstanceUtils.isValid(contactInst)) {
            insert.add(CISales.DocumentSumAbstract.Contact, contactInst);
        }

        final BigDecimal rateNetTotal = _dto.getNetTotal() == null ? BigDecimal.ZERO : _dto.getNetTotal();
        final BigDecimal rateCrossTotal = _dto.getCrossTotal() == null ? BigDecimal.ZERO : _dto.getCrossTotal();

        insert.add(CISales.DocumentSumAbstract.NetTotal,
                        DocumentUtils.exchange(rateNetTotal, _dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.DocumentSumAbstract.CrossTotal,
                        DocumentUtils.exchange(rateCrossTotal, _dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.DocumentSumAbstract.RateNetTotal, rateNetTotal);
        insert.add(CISales.DocumentSumAbstract.RateCrossTotal, rateCrossTotal);
        insert.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.DocumentSumAbstract.CurrencyId, ERP.CURRENCYBASE.get());
        insert.add(CISales.DocumentSumAbstract.RateCurrencyId, DocumentUtils.getCurrencyInst(_dto.getCurrency()).getInstance());
        insert.add(CISales.DocumentSumAbstract.Rate, DocumentUtils.getRate(_dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.DocumentSumAbstract.Taxes,
                        getTaxes(_dto.getDate(), _dto.getTaxes(), _dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.DocumentSumAbstract.RateTaxes, getRateTaxes(_dto.getDate(), _dto.getTaxes()));
        insert.execute();

        final Instance ret = insert.getInstance();

        final Instance workspaceInst = Instance.get(_dto.getWorkspaceOid());
        if (InstanceUtils.isValid(workspaceInst)) {
            final var eval = EQL.builder().with(StmtFlag.TRIGGEROFF)
                .print(workspaceInst)
                .linkto(CIPOS.Workspace.POSLink).instance().as("POSInst")
                .linkto(CIPOS.Workspace.POSLink).linkto(CIPOS.POS.DepartmentLink).instance().as("depInst")
                .evaluate();
            eval.next();

            EQL.builder().insert(CIPOS.POS2Document)
                .set(CIPOS.POS2Document.FromLink, eval.get("POSInst"))
                .set(CIPOS.POS2Document.ToLink, ret)
                .execute();

            if (Pos.POS_ASSIGNDEPARTMENT.get()) {
                final Instance depInst = eval.get("depInst");
                if (InstanceUtils.isValid(depInst) && getDepartment2DocumentType() != null) {
                    EQL.builder().insert(getDepartment2DocumentType())
                                    .set(CIHumanResource.Department2DocumentAbstract.FromAbstractLink, depInst)
                                    .set(CIHumanResource.Department2DocumentAbstract.ToAbstractLink, ret)
                                    .execute();
                }
            }
        }

        if (_dto instanceof AbstractPayableDocumentDto) {
            final Instance balanceInst = Instance.get(((AbstractPayableDocumentDto) _dto).getBalanceOid());
            if (InstanceUtils.isKindOf(balanceInst, CIPOS.Balance)) {
                final Insert relInsert = new Insert(CIPOS.Balance2Document);
                relInsert.add(CIPOS.Balance2Document.FromLink, balanceInst);
                relInsert.add(CIPOS.Balance2Document.ToLink, ret);
                relInsert.execute();
            }
        }

        if (getEmployee2DocumentType() != null && CollectionUtils.isNotEmpty(_dto.getEmployeeRelations())) {
            for (final var relation : _dto.getEmployeeRelations()) {
                final var employeeInst = Instance.get(relation.getEmployeeOid());
                if (InstanceUtils.isKindOf(employeeInst, CIHumanResource.EmployeeAbstract)) {
                    switch (relation.getType()) {
                        case SELLER:
                        default:
                            final Insert relInsert = new Insert(getEmployee2DocumentType());
                            relInsert.add(CIHumanResource.Employee2DocumentAbstract.FromAbstractLink, employeeInst);
                            relInsert.add(CIHumanResource.Employee2DocumentAbstract.ToAbstractLink, ret);
                            relInsert.execute();
                    }
                }
            }
        }
        return ret;
    }




    protected void createTransactionDocument(final AbstractDocumentDto _dto,
                                             final Instance _docInstance)
        throws EFapsException
    {
        Instance warehouseInst = null;
        final Instance workspaceInst = Instance.get(_dto.getWorkspaceOid());
        if (InstanceUtils.isValid(workspaceInst)) {
            final Evaluator evaluator = EQL.builder()
                            .print(workspaceInst)
                            .linkto(CIPOS.Workspace.WarehouseLink).instance()
                            .evaluate();
            if (evaluator.next()) {
                warehouseInst = evaluator.get(1);
            }
        }
        if (InstanceUtils.isKindOf(warehouseInst, CIProducts.Warehouse)) {
            final TransactionDocument transactionDocument = new TransactionDocument()
            {

                @Override
                protected Type getType4DocCreate(final Parameter _parameter)
                    throws EFapsException
                {
                    return _dto instanceof CreditNoteDto ? CISales.TransactionDocumentShadowIn.getType()
                                    : CISales.TransactionDocumentShadowOut.getType();
                }

                @Override
                protected void addStatus2DocCreate(final Parameter _parameter,
                                                   final Insert _insert,
                                                   final CreatedDoc _createdDoc)
                    throws EFapsException
                {
                    _insert.add(CISales.DocumentAbstract.StatusAbstract,
                                    _dto instanceof CreditNoteDto
                                                    ? Status.find(CISales.TransactionDocumentShadowInStatus.Closed)
                                                    : Status.find(CISales.TransactionDocumentShadowOutStatus.Closed));
                }

                @Override
                protected void connect2ProductDocumentType(final Parameter _parameter,
                                                           final CreatedDoc _createdDoc)
                    throws EFapsException
                {
                    final Instance instDocType = Pos.PRODDOCTYPE4DOC.get();
                    if (instDocType.isValid() && _createdDoc.getInstance().isValid()) {
                        insert2DocumentTypeAbstract(CISales.Document2ProductDocumentType, _createdDoc, instDocType);
                    }
                }

                @Override
                protected Type getType4PositionCreate(final Parameter _parameter)
                    throws EFapsException
                {
                    return _dto instanceof CreditNoteDto ? CISales.TransactionDocumentShadowInPosition.getType()
                                    : CISales.TransactionDocumentShadowOutPosition.getType();
                }
            };
            final Parameter parameter = ParameterUtil.instance();
            ParameterUtil.setParameterValues(parameter, "storage", warehouseInst.getOid());
            final CreatedDoc createdDoc = transactionDocument.createDocumentShadow(parameter, _docInstance);

            CIType relType = null;
            if (_dto instanceof InvoiceDto) {
                relType = CISales.Invoice2TransactionDocumentShadowOut;
            } else if (_dto instanceof ReceiptDto) {
                relType = CISales.Receipt2TransactionDocumentShadowOut;
            } else if (_dto instanceof TicketDto) {
                relType = CIPOS.Ticket2TransactionDocumentShadowOut;
            } else if (_dto instanceof CreditNoteDto) {
                relType = CISales.CreditNote2TransactionDocumentShadowIn;
            }
            if (relType != null) {
                final Insert insert = new Insert(relType);
                insert.add(CISales.Document2TransactionDocumentShadowAbstract.FromAbstractLink, _docInstance);
                insert.add(CISales.Document2TransactionDocumentShadowAbstract.ToAbstractLink, createdDoc.getInstance());
                insert.execute();
            }
        }
    }

    protected void createPositions(final Instance docInstance,
                                   final AbstractDocumentDto documentDto)
        throws EFapsException
    {
        final var sortedItems = documentDto.getItems().stream().sorted((item0,
                                                       item1) -> item0.getIndex().compareTo(item1.getIndex()))
                        .collect(Collectors.toList());
        Instance parentInstance = null;
        for (final var item : sortedItems) {
            if (item.getParentIdx() == null) {
                parentInstance = createPosition(docInstance, item, documentDto.getDate());
            } else {
                createChildPosition(docInstance, item, parentInstance);
            }
        }
    }

    protected Instance createChildPosition(final Instance docInstance,
                                           final AbstractDocItemDto itemDto,
                                           final Instance parentPositionInstance)
        throws EFapsException
    {
        final Insert insert = new Insert(CISales.ConfigurationPosition);
        insert.add(CISales.ConfigurationPosition.PositionAbstractLink, parentPositionInstance);
        insert.add(CISales.PositionAbstract.PositionNumber, itemDto.getIndex());
        insert.add(CISales.PositionAbstract.DocumentAbstractLink, docInstance);
        insert.add(CISales.PositionAbstract.Product,  Instance.get(itemDto.getProductOid()));
        final var productInfo = getProductInfo(itemDto.getProductOid());
        insert.add(CISales.PositionAbstract.ProductDesc, productInfo[0]);
        insert.add(CISales.PositionAbstract.UoM, productInfo[1]);
        insert.add(CISales.PositionAbstract.Quantity, itemDto.getQuantity());
        insert.execute();
        return insert.getInstance();
    }

    protected Object[] getProductInfo(String productOid)
        throws EFapsException
    {
        final var eval = EQL.builder().print(productOid)
                        .attribute(CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.DefaultUoM)
                        .evaluate();
        return new Object[] { eval.get(CIProducts.ProductAbstract.Description),
                        eval.get(CIProducts.ProductAbstract.DefaultUoM) };
    }


    protected Instance createPosition(final Instance _docInstance,
                                      final AbstractDocItemDto _dto,
                                      final LocalDate _date)
        throws EFapsException
    {
        final Insert insert = new Insert(getPositionType());
        insert.add(CISales.PositionAbstract.PositionNumber, _dto.getIndex());
        insert.add(CISales.PositionAbstract.DocumentAbstractLink, _docInstance);
        insert.add(CISales.PositionAbstract.Product,  Instance.get(_dto.getProductOid()));
        final var productInfo = getProductInfo(_dto.getProductOid());
        insert.add(CISales.PositionAbstract.ProductDesc, productInfo[0]);
        insert.add(CISales.PositionAbstract.UoM, productInfo[1]);
        insert.add(CISales.PositionAbstract.Quantity, _dto.getQuantity());
        insert.add(CISales.PositionSumAbstract.Discount, BigDecimal.ZERO);
        insert.add(CISales.PositionSumAbstract.DiscountNetUnitPrice,
                        DocumentUtils.exchange(_dto.getNetUnitPrice(), _dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.PositionSumAbstract.CrossUnitPrice,
                        DocumentUtils.exchange(_dto.getCrossUnitPrice(), _dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.PositionSumAbstract.NetUnitPrice,
                        DocumentUtils.exchange(_dto.getNetUnitPrice(), _dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.PositionSumAbstract.CrossPrice,
                        DocumentUtils.exchange(_dto.getCrossPrice(), _dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.PositionSumAbstract.NetPrice,
                        DocumentUtils.exchange(_dto.getNetPrice(), _dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.PositionSumAbstract.CurrencyId, ERP.CURRENCYBASE.get());
        insert.add(CISales.PositionSumAbstract.Rate, DocumentUtils.getRate(_dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.PositionSumAbstract.RateCurrencyId, DocumentUtils.getCurrencyInst(_dto.getCurrency()).getInstance());
        insert.add(CISales.PositionSumAbstract.RateNetUnitPrice, _dto.getNetUnitPrice());
        insert.add(CISales.PositionSumAbstract.RateCrossUnitPrice, _dto.getCrossUnitPrice());
        insert.add(CISales.PositionSumAbstract.RateDiscountNetUnitPrice, _dto.getCrossUnitPrice());
        insert.add(CISales.PositionSumAbstract.RateNetPrice, _dto.getNetPrice());
        insert.add(CISales.PositionSumAbstract.RateCrossPrice, _dto.getCrossPrice());
        insert.add(CISales.PositionSumAbstract.Remark, _dto.getRemark());
        insert.add(CISales.PositionSumAbstract.Tax, getTaxCat(_dto.getTaxes().iterator().next()).getInstance());
        insert.add(CISales.PositionSumAbstract.Taxes,
                        getTaxes(_date, _dto.getTaxes(), _dto.getCurrency(), _dto.getExchangeRate()));
        insert.add(CISales.PositionSumAbstract.RateTaxes, getRateTaxes(_date, _dto.getTaxes()));
        insert.execute();
        return insert.getInstance();
    }

    protected Taxes getTaxes(final LocalDate _date, final Collection<TaxEntryDto> _taxes,
                             final org.efaps.pos.dto.Currency currency,
                             final BigDecimal exchangeRate)
        throws EFapsException
    {
        final Taxes ret = new Taxes();
        for (final TaxEntryDto dto : _taxes) {
            final TaxEntry taxentry = new TaxEntry();
            taxentry.setBase(DocumentUtils.exchange(dto.getBase(), currency, exchangeRate));
            taxentry.setAmount(DocumentUtils.exchange(dto.getAmount(), currency, exchangeRate));
            taxentry.setUUID(getTax(dto).getUUID());
            taxentry.setCatUUID(getTax(dto).getTaxCat().getUuid());
            taxentry.setCurrencyUUID(CurrencyInst.get(ERP.CURRENCYBASE.get()).getUUID());
            taxentry.setDate(new org.joda.time.LocalDate(_date.getYear(), _date.getMonthValue(), _date.getDayOfMonth())
                            .toDateTimeAtCurrentTime().withTimeAtStartOfDay());
            ret.getEntries().add(taxentry);
        }
        return ret;
    }

    protected Taxes getRateTaxes(final LocalDate _date, final Collection<TaxEntryDto> _taxes)
        throws EFapsException
    {
        final Taxes ret = new Taxes();
        for (final TaxEntryDto dto : _taxes) {
            final TaxEntry taxentry = new TaxEntry();
            taxentry.setBase(dto.getBase());
            taxentry.setAmount(dto.getAmount());
            taxentry.setUUID(getTax(dto).getUUID());
            taxentry.setCatUUID(getTax(dto).getTaxCat().getUuid());
            taxentry.setCurrencyUUID(CurrencyInst.get(ERP.CURRENCYBASE.get()).getUUID());
            taxentry.setDate(new org.joda.time.LocalDate(_date.getYear(), _date.getMonthValue(), _date.getDayOfMonth())
                            .toDateTimeAtCurrentTime().withTimeAtStartOfDay());
            ret.getEntries().add(taxentry);
        }
        return ret;
    }

    protected Tax getTax(final TaxEntryDto _taxEntry)
        throws EFapsException
    {
        return Tax_Base.get(UUID.fromString(_taxEntry.getTax().getCatKey()),
                        UUID.fromString(_taxEntry.getTax().getKey()));
    }

    protected TaxCat getTaxCat(final TaxEntryDto _taxEntry)
        throws EFapsException
    {
        return TaxCat_Base.get(UUID.fromString(_taxEntry.getTax().getCatKey()));
    }

    protected void addPayments(final Instance _docInst, final AbstractPayableDocumentDto _dto)
        throws EFapsException
    {
        if (CollectionUtils.isNotEmpty(_dto.getPayments())) {
            for (final PaymentDto paymentDto : _dto.getPayments()) {
                LOG.debug("adding PaymentDto: {}", paymentDto);
                final Parameter parameter = ParameterUtil.instance();

                final var rateCurrencyInst = DocumentUtils.getCurrencyInst(paymentDto.getCurrency());
                LOG.debug("using rateCurrencyInst: {}", rateCurrencyInst);
                final boolean negate = paymentDto.getAmount().compareTo(BigDecimal.ZERO) < 0;
                final CIType docType = DocumentUtils.getPaymentDocType(paymentDto.getType(), negate);
                final Insert insert = new Insert(docType);
                final PosPayment posPayment = new PosPayment(docType);
                insert.add(CISales.PaymentDocumentAbstract.Name,
                                NumberGenerator.get(UUID.fromString(Pos.PAYMENTDOCUMENT_SEQ.get())).getNextVal());
                if (PaymentType.CARD.equals(paymentDto.getType())) {
                    insert.add(CISales.PaymentCard.CardType, paymentDto.getCardTypeId());
                    insert.add(CISales.PaymentCard.CardNumber, paymentDto.getCardNumber());
                    insert.add(CISales.PaymentCard.ServiceProvider, paymentDto.getServiceProvider());
                    insert.add(CISales.PaymentCard.Authorization, paymentDto.getAuthorization());
                    insert.add(CISales.PaymentCard.OperationId, paymentDto.getOperationId());
                    insert.add(CISales.PaymentCard.OperationDateTime, paymentDto.getOperationDateTime());
                    insert.add(CISales.PaymentCard.Info, paymentDto.getInfo());
                }

                if (PaymentType.ELECTRONIC.equals(paymentDto.getType())) {
                    final Instance epayInst = evalEletronicPaymentType(paymentDto.getMappingKey());
                    if (InstanceUtils.isValid(epayInst)) {
                        insert.add(CISales.PaymentElectronic.ElectronicPaymentType, epayInst);
                    }
                    insert.add(CISales.PaymentElectronic.ServiceProvider, paymentDto.getServiceProvider());
                    insert.add(CISales.PaymentElectronic.EquipmentIdent, paymentDto.getEquipmentIdent());
                    insert.add(CISales.PaymentElectronic.Authorization, paymentDto.getAuthorization());
                    insert.add(CISales.PaymentElectronic.OperationId, paymentDto.getOperationId());
                    insert.add(CISales.PaymentElectronic.OperationDateTime, paymentDto.getOperationDateTime());
                    insert.add(CISales.PaymentElectronic.Info, paymentDto.getInfo());
                    insert.add(CISales.PaymentElectronic.CardLabel, paymentDto.getCardLabel());
                    insert.add(CISales.PaymentElectronic.CardNumber, paymentDto.getCardNumber());
                }

                final String code = posPayment.getCode4CreateDoc(parameter);
                if (code != null) {
                    insert.add(CISales.PaymentDocumentAbstract.Code, code);
                }
                insert.add(CISales.PaymentDocumentAbstract.Amount, paymentDto.getAmount());
                insert.add(CISales.PaymentDocumentAbstract.Date, _dto.getDate());
                final Instance baseCurrInst = Currency.getBaseCurrency();
                insert.add(CISales.PaymentDocumentAbstract.RateCurrencyLink, rateCurrencyInst.getInstance());
                insert.add(CISales.PaymentDocumentAbstract.CurrencyLink, baseCurrInst);

                final Instance contactInst = Instance.get(_dto.getContactOid());
                if (InstanceUtils.isValid(contactInst)) {
                    insert.add(CISales.PaymentDocumentAbstract.Contact, contactInst);
                }
                final var rate = DocumentUtils.getRate(paymentDto.getCurrency(), paymentDto.getExchangeRate());
                insert.add(CISales.PaymentDocumentAbstract.Rate, rate);

                insert.add(docType.getType().getStatusAttribute(),
                                DocumentUtils.getPaymentDocStatus(paymentDto.getType(), negate));
                insert.execute();

                final Insert payInsert = new Insert(CISales.Payment);
                payInsert.add(CISales.Payment.Status, Status.find(CISales.PaymentStatus.Executed));
                payInsert.add(CISales.Payment.CreateDocument, _docInst);
                payInsert.add(CISales.Payment.RateCurrencyLink, rateCurrencyInst.getInstance());
                payInsert.add(CISales.Payment.Amount, paymentDto.getAmount());
                payInsert.add(CISales.Payment.TargetDocument, insert.getInstance());
                payInsert.add(CISales.Payment.CurrencyLink, baseCurrInst);
                payInsert.add(CISales.Payment.Date, new DateTime());
                payInsert.add(CISales.Payment.Rate, rate);
                payInsert.execute();

                final Insert transIns;
                if (InstanceUtils.isKindOf(insert.getInstance(), CISales.PaymentDocumentAbstract)) {
                    transIns = new Insert(CISales.TransactionInbound);
                } else {
                    transIns = new Insert(CISales.TransactionOutbound);
                }
                transIns.add(CISales.TransactionAbstract.CurrencyId, baseCurrInst);
                transIns.add(CISales.TransactionAbstract.Payment, payInsert.getInstance());
                transIns.add(CISales.TransactionAbstract.Amount, paymentDto.getAmount());
                transIns.add(CISales.TransactionAbstract.Date, _dto.getDate());
                transIns.add(CISales.TransactionAbstract.Account, getAccountInst(_dto));
                transIns.execute();
            }
        }
    }

    protected Instance evalEletronicPaymentType(final String _mappingKey)
        throws EFapsException
    {
        Instance ret = null;
        final QueryBuilder queryBldr = new QueryBuilder(CISales.AttributeDefinitionPaymentElectronicType);
        queryBldr.addWhereAttrEqValue(CISales.AttributeDefinitionPaymentElectronicType.MappingKey, _mappingKey);
        final InstanceQuery query = queryBldr.getQuery();
        query.executeWithoutAccessCheck();
        if (query.next()) {
            ret = query.getCurrentValue();
        }
        return ret;
    }

    protected Instance getAccountInst(final AbstractDocumentDto _documentDto)
        throws EFapsException
    {
        final Instance workspacInst = Instance.get(_documentDto.getWorkspaceOid());
        final PrintQuery print = new PrintQuery(workspacInst);
        final SelectBuilder selAccountInst = SelectBuilder.get()
                        .linkto(CIPOS.Workspace.POSLink)
                        .linkto(CIPOS.POS.AccountLink).instance();
        print.addSelect(selAccountInst);
        print.execute();
        return print.getSelect(selAccountInst);
    }

    public static class PosPayment
        extends AbstractPaymentDocument
    {

        private final CIType docType;

        public PosPayment(final CIType _docType)
        {
            docType = _docType;
        }

        @Override
        protected Type getType4DocCreate(final Parameter _parameter)
            throws EFapsException
        {
            return docType.getType();
        }

        @Override
        protected String getCode4CreateDoc(final Parameter _parameter)
            throws EFapsException
        {
            return super.getCode4CreateDoc(_parameter);
        }
    }
}
