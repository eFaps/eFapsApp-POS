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

package org.efaps.esjp.pos.rest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

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
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.util.ERP;
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
import org.efaps.pos.dto.InvoiceDto;
import org.efaps.pos.dto.PaymentDto;
import org.efaps.pos.dto.PaymentType;
import org.efaps.pos.dto.ReceiptDto;
import org.efaps.pos.dto.TaxEntryDto;
import org.efaps.pos.dto.TicketDto;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.joda.time.DateTime;

@EFapsUUID("2c0b2e38-14cb-474a-8b49-0859f38784c5")
@EFapsApplication("eFapsApp-POS")
public abstract class AbstractDocument_Base
    extends AbstractRest
{

    protected Instance createDocument(final CIType _ciType, final Status _status, final AbstractDocumentDto _dto)
        throws EFapsException
    {
        final Insert insert = new Insert(_ciType);
        insert.add(CISales.DocumentSumAbstract.Name, _dto.getNumber());
        insert.add(CISales.DocumentSumAbstract.Date, _dto.getDate());
        insert.add(CISales.DocumentSumAbstract.StatusAbstract, _status);
        insert.add(CISales.DocumentSumAbstract.Note, _dto.getNote());

        final Instance contactInst = Instance.get(_dto.getContactOid());
        if (InstanceUtils.isValid(contactInst)) {
            insert.add(CISales.DocumentSumAbstract.Contact, contactInst);
        }
        final BigDecimal netTotal = _dto.getNetTotal() == null ? BigDecimal.ZERO : _dto.getNetTotal();
        final BigDecimal crossTotal = _dto.getCrossTotal() == null ? BigDecimal.ZERO : _dto.getCrossTotal();

        insert.add(CISales.DocumentSumAbstract.NetTotal, netTotal);
        insert.add(CISales.DocumentSumAbstract.CrossTotal, crossTotal);
        insert.add(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.DocumentSumAbstract.RateNetTotal, netTotal);
        insert.add(CISales.DocumentSumAbstract.RateCrossTotal, crossTotal);
        insert.add(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.DocumentSumAbstract.CurrencyId, ERP.CURRENCYBASE.get());
        insert.add(CISales.DocumentSumAbstract.RateCurrencyId, ERP.CURRENCYBASE.get());
        insert.add(CISales.DocumentSumAbstract.Rate, new Object[] { 1, 1 });
        final Taxes taxes = getTaxes(_dto.getDate(), _dto.getTaxes());
        insert.add(CISales.DocumentSumAbstract.Taxes, taxes);
        insert.add(CISales.DocumentSumAbstract.RateTaxes, taxes);
        insert.execute();

        final Instance ret = insert.getInstance();

        final Instance workspacInst = Instance.get(_dto.getWorkspaceOid());
        if (InstanceUtils.isValid(workspacInst)) {
            final PrintQuery print = new PrintQuery(workspacInst);
            print.addAttribute(CIPOS.Workspace.POSLink);
            print.executeWithoutAccessCheck();

            final Insert relInsert = new Insert(CIPOS.POS2Document);
            relInsert.add(CIPOS.POS2Document.FromLink, print.<Long>getAttribute(CIPOS.Workspace.POSLink));
            relInsert.add(CIPOS.POS2Document.ToLink, ret);
            relInsert.execute();
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
                    return CISales.TransactionDocumentShadowOut.getType();
                }

                @Override
                protected void addStatus2DocCreate(final Parameter _parameter,
                                                   final Insert _insert,
                                                   final CreatedDoc _createdDoc)
                    throws EFapsException
                {
                    _insert.add(CISales.DocumentAbstract.StatusAbstract,
                                    Status.find(CISales.TransactionDocumentShadowOutStatus.Closed));
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
                    return CISales.TransactionDocumentShadowOutPosition.getType();
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
            }
            if (relType != null) {
                final Insert insert = new Insert(relType);
                insert.add(CISales.Document2TransactionDocumentShadowAbstract.FromAbstractLink, _docInstance);
                insert.add(CISales.Document2TransactionDocumentShadowAbstract.ToAbstractLink, createdDoc.getInstance());
                insert.execute();
            }
        }
    }

    protected Instance createPosition(final Instance _docInstance, final CIType _ciType, final AbstractDocItemDto _dto,
                                      final LocalDate _date)
        throws EFapsException
    {
        final Insert insert = new Insert(_ciType);
        insert.add(CISales.PositionAbstract.PositionNumber, _dto.getIndex());
        insert.add(CISales.PositionAbstract.DocumentAbstractLink, _docInstance);

        final Instance prodInst = Instance.get(_dto.getProductOid());
        insert.add(CISales.PositionAbstract.Product, prodInst);

        final PrintQuery print = new PrintQuery(prodInst);
        print.addAttribute(CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.DefaultUoM);
        print.execute();
        insert.add(CISales.PositionAbstract.ProductDesc, print.<String>getAttribute(
                        CIProducts.ProductAbstract.Description));
        insert.add(CISales.PositionAbstract.UoM, print.<Long>getAttribute(CIProducts.ProductAbstract.DefaultUoM));
        insert.add(CISales.PositionSumAbstract.Quantity, _dto.getQuantity());
        insert.add(CISales.PositionSumAbstract.Discount, BigDecimal.ZERO);
        insert.add(CISales.PositionSumAbstract.DiscountNetUnitPrice, _dto.getNetUnitPrice());
        insert.add(CISales.PositionSumAbstract.CrossUnitPrice, _dto.getCrossUnitPrice());
        insert.add(CISales.PositionSumAbstract.NetUnitPrice, _dto.getNetUnitPrice());
        insert.add(CISales.PositionSumAbstract.CrossPrice, _dto.getCrossPrice());
        insert.add(CISales.PositionSumAbstract.NetPrice, _dto.getNetPrice());
        insert.add(CISales.PositionSumAbstract.CurrencyId, ERP.CURRENCYBASE.get());
        insert.add(CISales.PositionSumAbstract.Rate, new Object[] { 1, 1 });
        insert.add(CISales.PositionSumAbstract.RateCurrencyId, ERP.CURRENCYBASE.get());
        insert.add(CISales.PositionSumAbstract.RateNetUnitPrice, _dto.getNetUnitPrice());
        insert.add(CISales.PositionSumAbstract.RateCrossUnitPrice, _dto.getCrossUnitPrice());
        insert.add(CISales.PositionSumAbstract.RateDiscountNetUnitPrice, _dto.getCrossUnitPrice());
        insert.add(CISales.PositionSumAbstract.RateNetPrice, _dto.getNetPrice());
        insert.add(CISales.PositionSumAbstract.RateCrossPrice, _dto.getCrossPrice());
        insert.add(CISales.PositionSumAbstract.Remark, _dto.getRemark());
        insert.add(CISales.PositionSumAbstract.Tax, getTaxCat(_dto.getTaxes().iterator().next()).getInstance());
        final Taxes taxes = getTaxes(_date, _dto.getTaxes());
        insert.add(CISales.PositionSumAbstract.Taxes, taxes);
        insert.add(CISales.PositionSumAbstract.RateTaxes, taxes);
        insert.execute();
        return insert.getInstance();
    }

    protected Taxes getTaxes(final LocalDate _date, final Set<TaxEntryDto> _taxes)
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
                final Parameter parameter = ParameterUtil.instance();
                final CIType docType = getPaymentDocType(paymentDto.getType());
                final Insert insert = new Insert(docType);
                final PosPayment posPayment = new PosPayment(docType);
                insert.add(CISales.PaymentDocumentAbstract.Name,
                                NumberGenerator.get(UUID.fromString(Pos.PAYMENTDOCUMENT_SEQ.get())).getNextVal());
                if (PaymentType.CARD.equals(paymentDto.getType())) {
                    insert.add(CISales.PaymentCard.CardType, paymentDto.getCardTypeId());
                }

                if (PaymentType.ELECTRONIC.equals(paymentDto.getType())) {
                    final Instance epayInst = evalEletronicPaymentType(paymentDto.getMappingKey());
                    if (InstanceUtils.isValid(epayInst)) {
                        insert.add(CISales.PaymentElectronic.ElectronicPaymentType, epayInst);
                    }
                }

                final String code = posPayment.getCode4CreateDoc(parameter);
                if (code != null) {
                    insert.add(CISales.PaymentDocumentAbstract.Code, code);
                }
                insert.add(CISales.PaymentDocumentAbstract.Amount, paymentDto.getAmount());
                insert.add(CISales.PaymentDocumentAbstract.Date, _dto.getDate());
                final Instance baseCurrInst = Currency.getBaseCurrency();
                insert.add(CISales.PaymentDocumentAbstract.RateCurrencyLink, baseCurrInst);
                insert.add(CISales.PaymentDocumentAbstract.CurrencyLink, baseCurrInst);
                insert.add(CISales.PaymentDocumentAbstract.RateCurrencyLink, baseCurrInst);
                insert.add(CISales.PaymentDocumentAbstract.CurrencyLink, baseCurrInst);

                final Instance contactInst = Instance.get(_dto.getContactOid());
                if (InstanceUtils.isValid(contactInst)) {
                    insert.add(CISales.PaymentDocumentAbstract.Contact, contactInst);
                }
                insert.add(CISales.PaymentDocumentAbstract.Rate, new Object[] { 1, 1 });
                insert.add(docType.getType().getStatusAttribute(), getPaymentDocStatus(paymentDto.getType()));
                insert.execute();

                final Insert payInsert = new Insert(CISales.Payment);
                payInsert.add(CISales.Payment.Status, Status.find(CISales.PaymentStatus.Executed));
                payInsert.add(CISales.Payment.CreateDocument, _docInst);
                payInsert.add(CISales.Payment.RateCurrencyLink, baseCurrInst);
                payInsert.add(CISales.Payment.Amount, paymentDto.getAmount());
                payInsert.add(CISales.Payment.TargetDocument, insert.getInstance());
                payInsert.add(CISales.Payment.CurrencyLink, baseCurrInst);
                payInsert.add(CISales.Payment.Date, new DateTime());
                payInsert.add(CISales.Payment.Rate, new Object[] { 1, 1 });
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

    protected Status getPaymentDocStatus(final PaymentType _paymentType)
        throws CacheReloadException
    {
        Status ret;
        switch (_paymentType) {
            case ELECTRONIC:
                ret = Status.find(CISales.PaymentElectronicStatus.Closed);
                break;
            case CARD:
                ret = Status.find(CISales.PaymentCardStatus.Closed);
                break;
            case CASH:
                ret = Status.find(CISales.PaymentCashStatus.Closed);
                break;
            case CHANGE:
                ret = Status.find(CISales.PaymentCashOutStatus.Closed);
                break;
            case FREE:
            default:
                ret = Status.find(CISales.PaymentInternalStatus.Closed);
                break;
        }
        return ret;
    }

    protected CIType getPaymentDocType(final PaymentType _paymentType)
    {
        CIType ret;
        switch (_paymentType) {
            case ELECTRONIC:
                ret = CISales.PaymentElectronic;
                break;
            case CARD:
                ret = CISales.PaymentCard;
                break;
            case CASH:
                ret = CISales.PaymentCash;
                break;
            case CHANGE:
                ret = CISales.PaymentCashOut;
                break;
            case FREE:
            default:
                ret = CISales.PaymentInternal;
                break;
        }
        return ret;
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
