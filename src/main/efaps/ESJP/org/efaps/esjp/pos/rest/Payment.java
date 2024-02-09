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

import java.time.LocalDate;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIStatus;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.SerialNumbers;
import org.efaps.pos.dto.PaymentDto;
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
                               final PaymentDto dto)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE, ACCESSROLE.MOBILE);
        LOG.debug("Create Payable for Order: {} and {}", orderOid, dto);

        final var orderInst = Instance.get(orderOid);
        if (InstanceUtils.isType(orderInst, CIPOS.Order)) {
            final var orderEval = EQL.builder().print(orderInst)
                            .attribute(CIPOS.Order.Contact)
                            .evaluate();
            if (orderEval.next()) {
                final var documentType = CISales.Receipt;
                final var positionType = CISales.ReceiptPosition;
                final var targetDocInst = cloneDoc(identifier, orderInst, documentType);
                clonePositions(orderInst, targetDocInst, positionType);
            }
        }

        return null;
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
            status = CISales.InvoiceStatus.Open;
        } else {
            status = CISales.ReceiptStatus.Open;
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
                        .evaluate();
        docEval.next();

        return EQL.builder().insert(documentType)
                        .set(CISales.DocumentSumAbstract.Name, evaluateDocName(identifier, documentType, true))
                        .set(CISales.DocumentSumAbstract.Date, LocalDate.now(Context.getThreadContext().getZoneId()))
                        .set(CISales.DocumentSumAbstract.StatusAbstract, status)
                        .set(CISales.DocumentSumAbstract.Contact, docEval.get(CISales.DocumentSumAbstract.Contact))
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

    protected String evaluateDocName(final String identifier,
                                     final CIType documentType,
                                     final Boolean isCreate)
        throws EFapsException
    {
        String serialNumber = null;
        CIAttribute attr;
        if (documentType.equals(CISales.Invoice)) {
            attr = null;
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
}
