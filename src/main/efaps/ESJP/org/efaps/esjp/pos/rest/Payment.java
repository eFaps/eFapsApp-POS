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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIStatus;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.stmt.selection.Evaluator;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Insert;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.contacts.util.Contacts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.electronicbilling.EBillingDocument;
import org.efaps.esjp.electronicbilling.util.ElectronicBilling;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.SerialNumbers;
import org.efaps.esjp.pos.rest.AbstractDocument_Base.PosPayment;
import org.efaps.esjp.pos.rest.dto.PayAndEmitResponseDto;
import org.efaps.esjp.pos.util.DocumentUtils;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.esjp.sales.tax.Tax_Base;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.pos.dto.AbstractPayableDocumentDto;
import org.efaps.pos.dto.DocItemDto;
import org.efaps.pos.dto.DocStatus;
import org.efaps.pos.dto.DocType;
import org.efaps.pos.dto.IPaymentDto;
import org.efaps.pos.dto.PaymentAbstractDto;
import org.efaps.pos.dto.PaymentCardDto;
import org.efaps.pos.dto.PaymentElectronicDto;
import org.efaps.pos.dto.PaymentType;
import org.efaps.pos.dto.ReceiptDto;
import org.efaps.pos.dto.TaxEntryDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("a91ad587-dd3b-41c8-8d39-2140ba9eafab")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Payment
    extends AbstractRest
{

    private static final Logger LOG = LoggerFactory.getLogger(Payment.class);

    @Path("/{identifier}/payments")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response payAndEmit(@PathParam("identifier") final String identifier,
                               @QueryParam("order-oid") final String orderOid,
                               @QueryParam("doc-type") final DocType docTypePara,
                               final List<IPaymentDto> paymentDtos)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE, ACCESSROLE.MOBILE);
        LOG.debug("Create Payable for Order: {} with: {} and {}", orderOid, docTypePara, paymentDtos);
        AbstractPayableDocumentDto payableDto = null;

        final var orderInst = Instance.get(orderOid);
        String ublHhash = null;
        DocType docType = null;
        if (InstanceUtils.isType(orderInst, CIPOS.Order)) {
            final var orderEval = EQL.builder().print(orderInst)
                            .linkto(CIPOS.Order.Contact)
                            .clazz(CIContacts.ClassOrganisation)
                            .attribute(CIContacts.ClassOrganisation.TaxNumber)
                            .as("TaxNumber")
                            .evaluate();
            if (orderEval.next()) {
                if (docTypePara == null) {
                    if (StringUtils.isNotEmpty(orderEval.get("TaxNumber"))) {
                        docType = DocType.INVOICE;
                    } else {
                        docType = DocType.RECEIPT;
                    }
                } else {
                    docType = docTypePara;
                }
                final CIType documentType;
                final CIType positionType;
                boolean ebilling = false;
                final CIType connectType = switch (docType) {
                    case INVOICE -> {
                        documentType = CISales.Invoice;
                        positionType = CISales.InvoicePosition;
                        ebilling = ElectronicBilling.INVOICE_ACTIVE.get();
                        yield CIPOS.Order2Invoice;
                    }
                    case TICKET -> {
                        documentType = CIPOS.Ticket;
                        positionType = CIPOS.TicketPosition;
                        yield CIPOS.Order2Ticket;
                    }
                    default -> {
                        documentType = CISales.Receipt;
                        positionType = CISales.ReceiptPosition;
                        ebilling = ElectronicBilling.RECEIPT_ACTIVE.get();
                        yield CIPOS.Order2Receipt;
                    }
                };

                final var targetDocInst = cloneDoc(identifier, orderInst, documentType);
                clonePositions(orderInst, targetDocInst, positionType);
                connect(orderInst, targetDocInst, connectType);
                addPayments(identifier, targetDocInst, paymentDtos);
                payableDto = toPayableDto(targetDocInst);

                EQL.builder().update(orderInst).set(CIPOS.Order.Status, CIPOS.OrderStatus.Closed).execute();

                if (ebilling) {
                    final var parameter = ParameterUtil.instance();
                    parameter.put(ParameterValues.CALL_INSTANCE, targetDocInst);
                    final var edoc = new EBillingDocument();
                    edoc.createDocument(parameter);
                    ublHhash = edoc.getHash(targetDocInst);
                }
            }
        }
        return payableDto == null
                        ? Response.noContent().build()
                        : Response.ok(PayAndEmitResponseDto.builder()
                                        .withPayable(payableDto)
                                        .withPayableType(docType)
                                        .withUblHash(ublHhash)
                                        .build()).build();
    }

    protected void addPayments(final String identifier,
                               final Instance docInst,
                               final List<IPaymentDto> paymentDtos)
        throws EFapsException
    {
        final var docEval = EQL.builder().print(docInst)
                        .attribute(CISales.DocumentSumAbstract.Date, CISales.DocumentSumAbstract.Contact)
                        .evaluate();
        docEval.next();

        for (final IPaymentDto payment : paymentDtos) {
            LOG.debug("adding PaymentDto: {}", payment);
            final var paymentDto = (PaymentAbstractDto) payment;
            final Parameter parameter = ParameterUtil.instance();

            final var rateCurrencyInst = DocumentUtils.getCurrencyInst(paymentDto.getCurrency());
            LOG.debug("using rateCurrencyInst: {}", rateCurrencyInst);
            final boolean negate = paymentDto.getAmount().compareTo(BigDecimal.ZERO) < 0;
            final CIType docType = DocumentUtils.getPaymentDocType(payment.getType(), negate);
            final var insert = EQL.builder().insert(docType);
            final PosPayment posPayment = new PosPayment(docType);
            insert.set(CISales.PaymentDocumentAbstract.Name,
                            NumberGenerator.get(UUID.fromString(Pos.PAYMENTDOCUMENT_SEQ.get())).getNextVal());
            if (PaymentType.CARD.equals(payment.getType())) {
                final var cardPayment = (PaymentCardDto) paymentDto;
                final Instance epayInst = evalCardPaymentType(cardPayment.getCardLabel());
                if (InstanceUtils.isValid(epayInst)) {
                    insert.set(CISales.PaymentCard.CardType, epayInst);
                }
                insert.set(CISales.PaymentCard.CardNumber, cardPayment.getCardNumber());
                insert.set(CISales.PaymentCard.ServiceProvider, cardPayment.getServiceProvider());
                insert.set(CISales.PaymentCard.Authorization, cardPayment.getAuthorization());
                insert.set(CISales.PaymentCard.OperationId, cardPayment.getOperationId());
                insert.set(CISales.PaymentCard.OperationDateTime, paymentDto.getOperationDateTime());
                insert.set(CISales.PaymentCard.Info, paymentDto.getInfo());
            }

            if (PaymentType.ELECTRONIC.equals(payment.getType())) {
                final var electronicPayment = (PaymentElectronicDto) paymentDto;

                final Instance epayInst = evalEletronicPaymentType(electronicPayment.getMappingKey());
                if (InstanceUtils.isValid(epayInst)) {
                    insert.set(CISales.PaymentElectronic.ElectronicPaymentType, epayInst);
                }
                insert.set(CISales.PaymentElectronic.ServiceProvider, electronicPayment.getServiceProvider());
                insert.set(CISales.PaymentElectronic.EquipmentIdent, electronicPayment.getEquipmentIdent());
                insert.set(CISales.PaymentElectronic.Authorization, electronicPayment.getAuthorization());
                insert.set(CISales.PaymentElectronic.OperationId, electronicPayment.getOperationId());
                insert.set(CISales.PaymentElectronic.OperationDateTime, paymentDto.getOperationDateTime());
                insert.set(CISales.PaymentElectronic.Info, paymentDto.getInfo());
                insert.set(CISales.PaymentElectronic.CardLabel, electronicPayment.getCardLabel());
                insert.set(CISales.PaymentElectronic.CardNumber, electronicPayment.getCardNumber());
            }

            final String code = posPayment.getCode4CreateDoc(parameter);
            if (code != null) {
                insert.set(CISales.PaymentDocumentAbstract.Code, code);
            }
            insert.set(CISales.PaymentDocumentAbstract.Amount, paymentDto.getAmount());
            insert.set(CISales.PaymentDocumentAbstract.Date, docEval.get(CISales.DocumentSumAbstract.Date));
            final Instance baseCurrInst = Currency.getBaseCurrency();
            insert.set(CISales.PaymentDocumentAbstract.RateCurrencyLink, rateCurrencyInst.getInstance());
            insert.set(CISales.PaymentDocumentAbstract.CurrencyLink, baseCurrInst);
            insert.set(CISales.PaymentDocumentAbstract.Contact, docEval.get(CISales.DocumentSumAbstract.Contact));
            final var rate = DocumentUtils.getRate(paymentDto.getCurrency(), paymentDto.getExchangeRate());
            insert.set(CISales.PaymentDocumentAbstract.Rate, rate);
            insert.set(docType.getType().getStatusAttribute().getName(), String.valueOf(
                            DocumentUtils.getPaymentDocStatus(payment.getType(), negate).getId()));
            final var payDocInst = insert.execute();

            final var payInsert = EQL.builder().insert(CISales.Payment);
            payInsert.set(CISales.Payment.Status, CISales.PaymentStatus.Executed);
            payInsert.set(CISales.Payment.CreateDocument, docInst);
            payInsert.set(CISales.Payment.RateCurrencyLink, rateCurrencyInst.getInstance());
            payInsert.set(CISales.Payment.Amount, paymentDto.getAmount());
            payInsert.set(CISales.Payment.TargetDocument, payDocInst);
            payInsert.set(CISales.Payment.CurrencyLink, baseCurrInst);
            payInsert.set(CISales.Payment.Date, OffsetDateTime.now(Context.getThreadContext().getZoneId()));
            payInsert.set(CISales.Payment.Rate, rate);
            final var payInst = payInsert.execute();

            Insert transIns;
            if (InstanceUtils.isKindOf(payDocInst, CISales.PaymentDocumentAbstract)) {
                transIns = EQL.builder().insert(CISales.TransactionInbound);
            } else {
                transIns = EQL.builder().insert(CISales.TransactionOutbound);
            }
            transIns.set(CISales.TransactionAbstract.CurrencyId, baseCurrInst);
            transIns.set(CISales.TransactionAbstract.Payment, payInst);
            transIns.set(CISales.TransactionAbstract.Amount, paymentDto.getAmount());
            transIns.set(CISales.TransactionAbstract.Date, docEval.get(CISales.DocumentSumAbstract.Date));
            transIns.set(CISales.TransactionAbstract.Account, getAccountInst(identifier));
            transIns.execute();
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

    protected Instance evalCardPaymentType(final String mappingKey)
        throws EFapsException
    {
        Instance ret = null;
        if (mappingKey != null) {
            final QueryBuilder queryBldr = new QueryBuilder(CISales.AttributeDefinitionPaymentCardType);
            queryBldr.addWhereAttrEqValue(CISales.AttributeDefinitionPaymentCardType.MappingKey, mappingKey);
            final InstanceQuery query = queryBldr.getQuery();
            query.executeWithoutAccessCheck();
            if (query.next()) {
                ret = query.getCurrentValue();
            }
        }
        return ret;
    }

    protected Instance getAccountInst(final String identifier)
        throws EFapsException
    {
        Instance accountInst = null;
        final var eval = EQL.builder().print().query(CIPOS.BackendMobile)
                        .where()
                        .attribute(CIPOS.BackendMobile.Identifier).eq(identifier)
                        .select()
                        .linkto(CIPOS.BackendMobile.AccountLink).instance().as("accountInst")
                        .evaluate();
        if (eval.next()) {
            accountInst = eval.get("accountInst");
        }
        return accountInst;
    }

    protected void connect(final Instance sourceDocInst,
                           final Instance targetDocInst,
                           final CIType connectType)
        throws EFapsException
    {
        EQL.builder().insert(connectType)
                        .set(CIPOS.Order2Document.FromAbstractLink, sourceDocInst)
                        .set(CIPOS.Order2Document.ToAbstractLink, targetDocInst)
                        .execute();
    }

    protected void clonePositions(final Instance sourceDocInst,
                                  final Instance targetDocInst,
                                  final CIType positionType)
        throws EFapsException
    {
        final var posEval = EQL.builder().print().query(CISales.PositionSumAbstract)
                        .where()
                        .attribute(CISales.PositionSumAbstract.DocumentAbstractLink).eq(sourceDocInst)
                        .select()
                        .attribute(CISales.PositionSumAbstract.PositionNumber,
                                        CISales.PositionSumAbstract.Product,
                                        CISales.PositionSumAbstract.ProductDesc,
                                        CISales.PositionSumAbstract.UoM,
                                        CISales.PositionSumAbstract.Quantity,
                                        CISales.PositionSumAbstract.CrossUnitPrice,
                                        CISales.PositionSumAbstract.NetUnitPrice,
                                        CISales.PositionSumAbstract.CrossPrice,
                                        CISales.PositionSumAbstract.NetPrice,
                                        CISales.PositionSumAbstract.Tax,
                                        CISales.PositionSumAbstract.Taxes,
                                        CISales.PositionSumAbstract.Discount,
                                        CISales.PositionSumAbstract.DiscountNetUnitPrice,
                                        CISales.PositionSumAbstract.CurrencyId,
                                        CISales.PositionSumAbstract.Rate,
                                        CISales.PositionSumAbstract.RateCurrencyId,
                                        CISales.PositionSumAbstract.RateNetUnitPrice,
                                        CISales.PositionSumAbstract.RateCrossUnitPrice,
                                        CISales.PositionSumAbstract.RateDiscountNetUnitPrice,
                                        CISales.PositionSumAbstract.RateNetPrice,
                                        CISales.PositionSumAbstract.RateCrossPrice,
                                        CISales.PositionSumAbstract.RateTaxes)
                        .evaluate();
        while (posEval.next()) {
            EQL.builder().insert(positionType)
                            .set(CISales.PositionAbstract.DocumentAbstractLink, targetDocInst)
                            .set(CISales.PositionAbstract.PositionNumber,
                                            posEval.get(CISales.PositionSumAbstract.PositionNumber))
                            .set(CISales.PositionAbstract.Product, posEval.get(CISales.PositionSumAbstract.Product))
                            .set(CISales.PositionAbstract.ProductDesc,
                                            posEval.get(CISales.PositionSumAbstract.ProductDesc))
                            .set(CISales.PositionAbstract.UoM, posEval.get(CISales.PositionSumAbstract.UoM))
                            .set(CISales.PositionSumAbstract.Quantity,
                                            posEval.get(CISales.PositionSumAbstract.Quantity))
                            .set(CISales.PositionSumAbstract.CrossUnitPrice,
                                            posEval.get(CISales.PositionSumAbstract.CrossUnitPrice))
                            .set(CISales.PositionSumAbstract.NetUnitPrice,
                                            posEval.get(CISales.PositionSumAbstract.NetUnitPrice))
                            .set(CISales.PositionSumAbstract.CrossPrice,
                                            posEval.get(CISales.PositionSumAbstract.CrossPrice))
                            .set(CISales.PositionSumAbstract.NetPrice,
                                            posEval.get(CISales.PositionSumAbstract.NetPrice))
                            .set(CISales.PositionSumAbstract.Tax,
                                            posEval.get(CISales.PositionSumAbstract.Tax))
                            .set(CISales.PositionSumAbstract.Taxes,
                                            posEval.get(CISales.PositionSumAbstract.Taxes))
                            .set(CISales.PositionSumAbstract.Discount,
                                            posEval.get(CISales.PositionSumAbstract.Discount))
                            .set(CISales.PositionSumAbstract.DiscountNetUnitPrice,
                                            posEval.get(CISales.PositionSumAbstract.Discount))
                            .set(CISales.PositionSumAbstract.CurrencyId,
                                            posEval.get(CISales.PositionSumAbstract.CurrencyId))
                            .set(CISales.PositionSumAbstract.Rate,
                                            posEval.get(CISales.PositionSumAbstract.Rate))
                            .set(CISales.PositionSumAbstract.RateCurrencyId,
                                            posEval.get(CISales.PositionSumAbstract.RateCurrencyId))
                            .set(CISales.PositionSumAbstract.RateNetUnitPrice,
                                            posEval.get(CISales.PositionSumAbstract.RateNetUnitPrice))
                            .set(CISales.PositionSumAbstract.RateCrossUnitPrice,
                                            posEval.get(CISales.PositionSumAbstract.RateCrossUnitPrice))
                            .set(CISales.PositionSumAbstract.RateDiscountNetUnitPrice,
                                            posEval.get(CISales.PositionSumAbstract.RateDiscountNetUnitPrice))
                            .set(CISales.PositionSumAbstract.RateNetPrice,
                                            posEval.get(CISales.PositionSumAbstract.RateNetPrice))
                            .set(CISales.PositionSumAbstract.RateCrossPrice,
                                            posEval.get(CISales.PositionSumAbstract.RateCrossPrice))
                            .set(CISales.PositionSumAbstract.RateTaxes,
                                            posEval.get(CISales.PositionSumAbstract.RateTaxes))
                            .execute();
        }
    }

    protected Instance cloneDoc(final String identifier,
                                final Instance docInst,
                                final CIType documentType)
        throws EFapsException
    {
        CIStatus status;
        if (documentType.equals(CISales.Invoice)) {
            status = CISales.InvoiceStatus.Paid;
        } else if (documentType.equals(CIPOS.Ticket)) {
            status = CIPOS.TicketStatus.Closed;
        } else {
            status = CISales.ReceiptStatus.Paid;
        }
        final var docEval = EQL.builder().print(docInst)
                        .attribute(CISales.DocumentSumAbstract.Contact,
                                        CISales.DocumentSumAbstract.RateCrossTotal,
                                        CISales.DocumentSumAbstract.CrossTotal,
                                        CISales.DocumentSumAbstract.RateNetTotal,
                                        CISales.DocumentSumAbstract.NetTotal,
                                        CISales.DocumentSumAbstract.RateDiscountTotal,
                                        CISales.DocumentSumAbstract.RateTaxes,
                                        CISales.DocumentSumAbstract.Taxes,
                                        CISales.DocumentSumAbstract.DiscountTotal,
                                        CISales.DocumentSumAbstract.CurrencyId,
                                        CISales.DocumentSumAbstract.Rate,
                                        CISales.DocumentSumAbstract.RateCurrencyId)
                        .linkto(CISales.DocumentSumAbstract.Contact).instance().as("contactInst")
                        .evaluate();
        docEval.next();
        final var contactInst = evalContactInstance(docEval);
        return EQL.builder().insert(documentType)
                        .set(CISales.DocumentSumAbstract.Name, evaluateDocName(identifier, documentType, false))
                        .set(CISales.DocumentSumAbstract.Date, LocalDate.now(Context.getThreadContext().getZoneId()))
                        .set(CISales.DocumentSumAbstract.StatusAbstract, status)
                        .set(CISales.DocumentSumAbstract.Contact, contactInst)
                        .set(CISales.DocumentSumAbstract.RateCrossTotal,
                                        docEval.get(CISales.DocumentSumAbstract.RateCrossTotal))
                        .set(CISales.DocumentSumAbstract.CrossTotal,
                                        docEval.get(CISales.DocumentSumAbstract.CrossTotal))
                        .set(CISales.DocumentSumAbstract.RateNetTotal,
                                        docEval.get(CISales.DocumentSumAbstract.RateNetTotal))
                        .set(CISales.DocumentSumAbstract.NetTotal, docEval.get(CISales.DocumentSumAbstract.NetTotal))
                        .set(CISales.DocumentSumAbstract.RateDiscountTotal,
                                        docEval.get(CISales.DocumentSumAbstract.RateDiscountTotal))
                        .set(CISales.DocumentSumAbstract.RateTaxes, docEval.get(CISales.DocumentSumAbstract.RateTaxes))
                        .set(CISales.DocumentSumAbstract.Taxes, docEval.get(CISales.DocumentSumAbstract.Taxes))
                        .set(CISales.DocumentSumAbstract.DiscountTotal,
                                        docEval.get(CISales.DocumentSumAbstract.DiscountTotal))
                        .set(CISales.DocumentSumAbstract.CurrencyId,
                                        docEval.get(CISales.DocumentSumAbstract.CurrencyId))
                        .set(CISales.DocumentSumAbstract.Rate, docEval.get(CISales.DocumentSumAbstract.Rate))
                        .set(CISales.DocumentSumAbstract.RateCurrencyId,
                                        docEval.get(CISales.DocumentSumAbstract.RateCurrencyId))
                        .execute();
    }

    protected Instance evalContactInstance(final Evaluator docEval)
        throws EFapsException
    {
        Instance ret = docEval.get("contactInst");
        if (!InstanceUtils.isValid(ret)) {
            final BigDecimal crossTotal = docEval.get(CISales.DocumentSumAbstract.CrossTotal);
            if (crossTotal.compareTo(new BigDecimal(700)) > 0) {
                throw new EFapsException(this.getClass(), "invalid");
            }
            ret = Contacts.STRAYCOSTUMER.get();
        }
        return ret;
    }

    protected String evaluateDocName(final String identifier,
                                     final CIType documentType,
                                     final Boolean isCreate)
        throws EFapsException
    {
        String serialNumber = null;
        CIAttribute attr;
        if (documentType.equals(CISales.Invoice)) {
            attr = CIPOS.BackendMobile.InvoiceSerial;
        } else if (documentType.equals(CIPOS.Ticket)) {
            attr = CIPOS.BackendMobile.TicketSerial;
        } else {
            attr = CIPOS.BackendMobile.ReceiptSerial;
        }

        final var eval = EQL.builder().print()
                        .query(CIPOS.BackendMobile)
                        .where()
                        .attribute(CIPOS.BackendMobile.Identifier).eq(identifier)
                        .select()
                        .attribute(attr)
                        .evaluate();
        if (eval.next()) {
            serialNumber = eval.get(attr);
        }
        if (serialNumber == null) {
            LOG.error("Missing configuration for SerialNumbers for identifier: {}", identifier);
        }
        return isCreate ? SerialNumbers.getPlaceholder(documentType, serialNumber)
                        : SerialNumbers.getNext(documentType, serialNumber);
    }

    protected AbstractPayableDocumentDto toPayableDto(final Instance instance)
        throws EFapsException
    {
        final var docEval = EQL.builder().print(instance)
                        .attribute(CISales.DocumentAbstract.Name, CISales.DocumentSumAbstract.RateNetTotal,
                                        CISales.DocumentSumAbstract.RateCrossTotal,
                                        CISales.DocumentSumAbstract.RateCurrencyId,
                                        CISales.DocumentSumAbstract.RateTaxes)
                        .linkto(CISales.DocumentAbstract.Contact).oid().as("contactOid")
                        .evaluate();
        docEval.next();

        final var posEval = EQL.builder().print().query(CISales.PositionSumAbstract)
                        .where()
                        .attribute(CISales.PositionSumAbstract.DocumentAbstractLink).eq(instance)
                        .select()
                        .attribute(CISales.PositionSumAbstract.PositionNumber, CISales.PositionSumAbstract.Quantity,
                                        CISales.PositionSumAbstract.CrossPrice)
                        .linkto(CISales.PositionSumAbstract.Product).oid().as("productOid")
                        .orderBy(CISales.PositionSumAbstract.PositionNumber)
                        .evaluate();
        final var items = new ArrayList<DocItemDto>();
        while (posEval.next()) {
            items.add(DocItemDto.builder()
                            .withProductOid(posEval.get("productOid"))
                            .withQuantity(posEval.get(CISales.PositionSumAbstract.Quantity))
                            .withCrossPrice(posEval.get(CISales.PositionSumAbstract.CrossPrice))
                            .build());
        }
        final var payableAmount = docEval.<BigDecimal>get(CISales.DocumentSumAbstract.RateCrossTotal);
        return ReceiptDto.builder()
                        .withOID(instance.getOid())
                        .withId(instance.getOid())
                        .withNumber(docEval.get(CISales.DocumentAbstract.Name))
                        .withContactOid(docEval.get("contactOid"))
                        .withNetTotal(docEval.get(CISales.DocumentSumAbstract.RateNetTotal))
                        .withCrossTotal(docEval.get(CISales.DocumentSumAbstract.RateCrossTotal))
                        .withPayableAmount(payableAmount)
                        .withTaxes(getTaxes(docEval.get(CISales.DocumentSumAbstract.RateTaxes)))
                        .withCurrency(DocumentUtils
                                        .getCurrency(docEval.<Long>get(CISales.DocumentSumAbstract.RateCurrencyId)))
                        .withStatus(DocStatus.OPEN)
                        .withItems(items)
                        .build();
    }

    public Collection<TaxEntryDto> getTaxes(final Taxes docTaxes)
        throws EFapsException
    {
        final var taxes = new HashSet<TaxEntryDto>();
        for (final var taxEntry : docTaxes.getEntries()) {
            taxes.add(TaxEntryDto.builder()
                            .withAmount(taxEntry.getAmount())
                            .withBase(taxEntry.getBase())
                            .withCurrency(DocumentUtils.getCurrency(taxEntry.getCurrencyUUID()))
                            .withTax(Calculator.toDto(Tax_Base.get(taxEntry.getCatUUID(), taxEntry.getUUID())))
                            .build());
        }
        return taxes;
    }

}
