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
import org.efaps.db.Update;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.pos.util.DocumentUtils;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.pos.dto.CreateDocumentDto;
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
    public Response addOrder(final String _identifier, final OrderDto _orderDto)
        throws EFapsException
    {
        checkAccess(_identifier);
        LOG.debug("Recieved: {}", _orderDto);
        final OrderDto dto;
        if (_orderDto.getOid() == null) {
            Status status;
            if (DocStatus.CANCELED.equals(_orderDto.getStatus())) {
                status = Status.find(CIPOS.OrderStatus.Canceled);
            } else {
                status = Status.find(CIPOS.OrderStatus.Closed);
            }

            final Instance docInst = createDocument(status, _orderDto);
            createPositions(docInst, _orderDto);
            final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Backend);
            queryBldr.addWhereAttrEqValue(CIPOS.Backend.Status, Status.find(CIPOS.BackendStatus.Active));
            queryBldr.addWhereAttrEqValue(CIPOS.Backend.Identifier, _identifier);
            final InstanceQuery query = queryBldr.getQuery();
            final Instance backendInst = query.execute().get(0);

            final Update update = new Update(docInst);
            update.add(CIPOS.Order.BackendLink, backendInst);
            update.execute();

            if (_orderDto.getPayableOid() != null) {
                final Instance payableInst = Instance.get(_orderDto.getPayableOid());
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
            dto = OrderDto.builder()
                            .withId(_orderDto.getId())
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
        LOG.debug("Create Order from : {}", dto);

        final var evalBackend = EQL.builder()
                        .print().query(CIPOS.BackendAbstract)
                        .where().attribute(CIPOS.BackendAbstract.Identifier).eq(identifier)
                        .select().instance()
                        .evaluate();
        evalBackend.next();

        final var currencyInst =  DocumentUtils.getCurrencyInst(dto.getCurrency());
        final var rateObj = DocumentUtils.getRate(dto.getCurrency(), BigDecimal.ONE);

        final var name = NumberGenerator.get(UUID.fromString(Pos.ORDER_NUMGEN.get())).getNextVal();

        EQL.builder().insert(CIPOS.Order)
            .set(CIPOS.Order.Status, CIPOS.OrderStatus.Open)
            .set(CIPOS.Order.Name, name)
            .set(CIPOS.Order.Date, LocalDate.now(Context.getThreadContext().getZoneId()))
            .set(CIPOS.Order.BackendLink, evalBackend.inst())
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
        return null;
    }
}
