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
import java.util.ArrayList;
import java.util.UUID;

import javax.ws.rs.core.Response;

import org.efaps.admin.common.NumberGenerator;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.pos.util.DocumentUtils;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.esjp.sales.CalculatorService;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.pos.dto.CreateDocumentDto;
import org.efaps.pos.dto.DocItemDto;
import org.efaps.pos.dto.DocStatus;
import org.efaps.pos.dto.OrderDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("4fbc39c0-2b8a-4872-ad91-80d01658a38f")
@EFapsApplication("eFapsApp-POS")
public abstract class Order_Base
    extends AbstractDocument
{

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Order.class);

    @Override
    protected CIType getDocumentType()
    {
        return CIPOS.Order;
    }

    @Override
    protected CIType getPositionType()
    {
        return CIPOS.OrderPosition;
    }

    @Override
    protected CIType getEmployee2DocumentType()
    {
        return CIPOS.Employee2Order;
    }

    @Override
    protected CIType getDepartment2DocumentType()
    {
        return null;
    }

    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    public Response addOrder(final String identifier,
                             final OrderDto orderDto)
        throws EFapsException
    {
        checkAccess(identifier);
        LOG.debug("Recieved: {}", orderDto);
        final OrderDto dto;
        if (orderDto.getOid() == null) {
            Status status;
            if (DocStatus.CANCELED.equals(orderDto.getStatus())) {
                status = Status.find(CIPOS.OrderStatus.Canceled);
            } else {
                status = Status.find(CIPOS.OrderStatus.Closed);
            }

            final Instance docInst = createDocument(status, orderDto);
            createPositions(docInst, orderDto);
            final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Backend);
            queryBldr.addWhereAttrEqValue(CIPOS.Backend.Status, Status.find(CIPOS.BackendStatus.Active));
            queryBldr.addWhereAttrEqValue(CIPOS.Backend.Identifier, identifier);
            final InstanceQuery query = queryBldr.getQuery();
            final Instance backendInst = query.execute().get(0);

            Instance orderOptionInst = null;
            if (orderDto.getOrderOptionKey() != null) {
                final var eval = EQL.builder().print().query(CIPOS.AttributeDefinitionOrderOption)
                                .where()
                                .attribute(CIPOS.AttributeDefinitionOrderOption.MappingKey)
                                .eq(orderDto.getOrderOptionKey())
                                .select()
                                .oid()
                                .evaluate();
                if (eval.next()) {
                    orderOptionInst = eval.inst();
                }
            }
            EQL.builder().update(docInst)
                .set(CIPOS.Order.BackendLink, backendInst)
                .set(CIPOS.Order.OrderOptionLink, orderOptionInst)
                .set(CIPOS.Order.Shoutout, orderDto.getShoutout())
                .execute();

            if (orderDto.getPayableOid() != null) {
                final Instance payableInst = Instance.get(orderDto.getPayableOid());
                Insert insert = null;
                if (InstanceUtils.isType(payableInst, CISales.Invoice)) {
                    insert = new Insert(CIPOS.Order2Invoice);
                } else if (InstanceUtils.isType(payableInst, CISales.Receipt)) {
                    insert = new Insert(CIPOS.Order2Receipt);
                } else if (InstanceUtils.isType(payableInst, CIPOS.Ticket)) {
                    insert = new Insert(CIPOS.Order2Ticket);
                }
                if (insert != null) {
                    insert.add(CIERP.Document2DocumentAbstract.FromAbstractLink, docInst);
                    insert.add(CIERP.Document2DocumentAbstract.ToAbstractLink, payableInst);
                    insert.execute();
                }
            }

            afterCreate(docInst);
            dto = OrderDto.builder()
                            .withId(orderDto.getId())
                            .withOID(docInst.getOid())
                            .build();
        } else {
            dto = OrderDto.builder().build();
        }
        final Response ret = Response.ok()
                        .entity(dto)
                        .build();
        return ret;
    }

    public Response createOrder(final String identifier,
                                final CreateDocumentDto dto)
        throws EFapsException
    {
        checkAccess(ACCESSROLE.MOBILE);
        LOG.info("Create Order from : {}", dto);

        final var beInst = getBackendInstance(identifier);

        final var currencyInst = DocumentUtils.getCurrencyInst(dto.getCurrency());
        final var rateObj = DocumentUtils.getRate(dto.getCurrency(), BigDecimal.ONE);

        final var name = NumberGenerator.get(UUID.fromString(Pos.ORDER_NUMGEN.get())).getNextVal();

        final var orderInst = EQL.builder().insert(CIPOS.Order)
                        .set(CIPOS.Order.Status, CIPOS.OrderStatus.Open)
                        .set(CIPOS.Order.Name, name)
                        .set(CIPOS.Order.Date, LocalDate.now(Context.getThreadContext().getZoneId()))
                        .set(CIPOS.Order.BackendLink, beInst)
                        .set(CIPOS.Order.CurrencyId, currencyInst.getInstance())
                        .set(CIPOS.Order.RateCurrencyId, currencyInst.getInstance())
                        .set(CIPOS.Order.NetTotal, BigDecimal.ZERO)
                        .set(CIPOS.Order.RateNetTotal, BigDecimal.ZERO)
                        .set(CIPOS.Order.CrossTotal, BigDecimal.ZERO)
                        .set(CIPOS.Order.RateCrossTotal, BigDecimal.ZERO)
                        .set(CIPOS.Order.DiscountTotal, BigDecimal.ZERO)
                        .set(CIPOS.Order.RateDiscountTotal, BigDecimal.ZERO)
                        .set(CIPOS.Order.Rate, rateObj)
                        .set(CIPOS.Order.Taxes, new Taxes())
                        .set(CIPOS.Order.RateTaxes, new Taxes())
                        .execute();

        upsertItems(dto, orderInst);
        new CalculatorService().recalculate(orderInst);
        return Response.ok(getOrder(orderInst)).build();
    }

    protected void upsertItems(final CreateDocumentDto dto,
                               final Instance orderInst)
        throws EFapsException
    {
        final var posEval = EQL.builder().print().query(CIPOS.OrderPosition)
                        .where().attribute(CIPOS.OrderPosition.OrderLink).eq(orderInst)
                        .select().instance()
                        .evaluate();
        while (posEval.next()) {
            EQL.builder().delete(posEval.inst()).stmt().execute();
        }

        final var currencyInst = DocumentUtils.getCurrencyInst(dto.getCurrency());
        final var rateObj = DocumentUtils.getRate(dto.getCurrency(), BigDecimal.ONE);

        int idx = 0;
        for (final var item : dto.getItems()) {
            final var prodEval = EQL.builder().print(item.getProductOid())
                            .attribute(CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.TaxCategory,
                                            CIProducts.ProductAbstract.DefaultUoM)
                            .evaluate();
            if (prodEval.next()) {
                EQL.builder().insert(CIPOS.OrderPosition)
                                .set(CIPOS.OrderPosition.OrderLink, orderInst)
                                .set(CIPOS.OrderPosition.CrossPrice, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.CrossUnitPrice, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.CurrencyId, currencyInst.getInstance())
                                .set(CIPOS.OrderPosition.Discount, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.DiscountNetUnitPrice, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.NetPrice, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.NetUnitPrice, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.PositionNumber, idx++)
                                .set(CIPOS.OrderPosition.Product, prodEval.inst())
                                .set(CIPOS.OrderPosition.ProductDesc,
                                                prodEval.get(CIProducts.ProductAbstract.Description))
                                .set(CIPOS.OrderPosition.Quantity, item.getQuantity())
                                .set(CIPOS.OrderPosition.Rate, rateObj)
                                .set(CIPOS.OrderPosition.RateCrossPrice, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.RateCrossUnitPrice, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.RateCurrencyId, currencyInst.getInstance())
                                .set(CIPOS.OrderPosition.RateDiscountNetUnitPrice, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.RateNetPrice, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.RateTaxes, new Taxes())
                                .set(CIPOS.OrderPosition.RateNetUnitPrice, BigDecimal.ZERO)
                                .set(CIPOS.OrderPosition.Remark, "")
                                .set(CIPOS.OrderPosition.Tax, prodEval.get(CIProducts.ProductAbstract.TaxCategory))
                                .set(CIPOS.OrderPosition.Taxes, new Taxes())
                                .set(CIPOS.OrderPosition.UoM, prodEval.get(CIProducts.ProductAbstract.DefaultUoM))
                                .execute();
            }
        }
    }

    public Response updateOrder(final String identifier,
                                final String oid,
                                final CreateDocumentDto dto)
        throws EFapsException
    {
        checkAccess(ACCESSROLE.MOBILE);
        LOG.info("Update Order from : {}", dto);

        // TODO check correct identifier, check status

        final var orderInst = Instance.get(oid);
        upsertItems(dto, orderInst);

        new CalculatorService().recalculate(orderInst);
        return Response.ok(getOrder(orderInst)).build();
    }

    public Response updateOrderWithContact(final String identifier,
                                           final String oid,
                                           final String contactOid)
        throws EFapsException
    {
        checkAccess(ACCESSROLE.MOBILE);
        LOG.info("Update Order oid: {} with contactOid: {}", oid, contactOid);

        // TODO check correct identifier, check status

        final var orderInst = Instance.get(oid);
        final var contactInst = Instance.get(contactOid);

        if (InstanceUtils.isType(orderInst, CIPOS.Order)) {
            EQL.builder().update(orderInst)
                            .set(CIPOS.Order.Contact,
                                            InstanceUtils.isType(contactInst, CIContacts.Contact) ? contactInst : null)
                            .execute();
        }
        return Response.ok(getOrder(orderInst)).build();
    }

    public Response getOrder(final String identifier,
                             final String oid)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.MOBILE, ACCESSROLE.BE);
        LOG.info("GET Order for : {}", oid);
        final var orderInst = Instance.get(oid);
        Response response = null;
        if (InstanceUtils.isType(orderInst, CIPOS.Order)) {
            response = Response.ok(getOrder(Instance.get(oid))).build();
        } else {
            response = Response.status(Response.Status.BAD_REQUEST).build();
        }
        return response;
    }

    protected OrderDto getOrder(final Instance instance)
        throws EFapsException
    {
        final var docEval = EQL.builder().print(instance)
                        .attribute(CISales.DocumentAbstract.Name, CISales.DocumentSumAbstract.RateNetTotal,
                                        CISales.DocumentSumAbstract.RateCrossTotal,
                                        CISales.DocumentSumAbstract.RateCurrencyId)
                        .evaluate();
        docEval.next();

        final var posEval = EQL.builder().print().query(CISales.PositionSumAbstract)
                        .where()
                        .attribute(CISales.PositionSumAbstract.DocumentAbstractLink).eq(instance)
                        .select()
                        .attribute(CISales.PositionSumAbstract.PositionNumber, CISales.PositionSumAbstract.Quantity)
                        .linkto(CISales.PositionSumAbstract.Product).oid().as("productOid")
                        .orderBy(CISales.PositionSumAbstract.PositionNumber)
                        .evaluate();
        final var items = new ArrayList<DocItemDto>();
        while (posEval.next()) {
            items.add(DocItemDto.builder()
                            .withProductOid(posEval.get("productOid"))
                            .withQuantity(posEval.get(CISales.PositionSumAbstract.Quantity))
                            .build());
        }

        return OrderDto.builder()
                        .withOID(instance.getOid())
                        .withId(instance.getOid())
                        .withNumber(docEval.get(CISales.DocumentAbstract.Name))
                        .withNetTotal(docEval.get(CISales.DocumentSumAbstract.RateNetTotal))
                        .withCrossTotal(docEval.get(CISales.DocumentSumAbstract.RateCrossTotal))
                        .withCurrency(DocumentUtils
                                        .getCurrency(docEval.<Long>get(CISales.DocumentSumAbstract.RateCurrencyId)))
                        .withStatus(DocStatus.OPEN)
                        .withItems(items)
                        .build();
    }
}
