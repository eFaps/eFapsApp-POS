/*
 * Copyright 2003 - 2010 The eFaps Team
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
 * Revision:        $Rev: 7479 $
 * Last Changed:    $Date: 2012-04-30 10:51:42 -0500 (lun, 30 abr 2012) $
 * Last Changed By: $Author: jorge.cueva@moxter.net $
 */

package org.efaps.esjp.pos.documents;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.datamodel.Type;
import org.efaps.admin.datamodel.ui.FieldValue;
import org.efaps.admin.dbproperty.DBProperties;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.ui.AbstractCommand;
import org.efaps.admin.ui.AbstractUserInterfaceObject.TargetMode;
import org.efaps.ci.CIType;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.db.transaction.ConnectionResource;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
import org.efaps.esjp.pos.jaxb.TicketInfo;
import org.efaps.esjp.pos.jaxb.TicketLineInfo;
import org.efaps.ui.wicket.util.DateUtil;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Account_Base.java 7479 2012-04-30 15:51:42Z
 *          jorge.cueva@moxter.net $
 */
@EFapsUUID("6a1e1062-3abb-4698-b4e3-44201bf6c7d3")
@EFapsRevision("$Rev: 7479 $")
public abstract class PosAccount_Base
{

    /**
     * method to get a formater.
     *
     * @return formater with value.
     * @throws EFapsException on error.
     */
    protected DecimalFormat getTwoDigitsformater()
        throws EFapsException
    {
        final DecimalFormat formater = (DecimalFormat) NumberFormat.getInstance(Context.getThreadContext().getLocale());
        formater.setMaximumFractionDigits(2);
        formater.setMinimumFractionDigits(2);
        formater.setRoundingMode(RoundingMode.HALF_UP);
        formater.setParseBigDecimal(true);
        return formater;
    }

    /**
     * Method for create a new Cash Desk Balance.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return cashDeskBalance(final TicketInfo _ticket)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.POS);
        queryBldr.addWhereAttrEqValue(CIPOS.POS.Name, _ticket.getHost());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPOS.POS.Account);
        multi.execute();
        long idAccount = 0;
        while (multi.next()) {
            idAccount = multi.getAttribute(CIPOS.POS.Account);
        }

        // Sales-Configuration
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final DateTime date = new DateTime(_ticket.getDate());
        final Insert insert = new Insert(CIPOS.CashDeskBalance);
        insert.add(CISales.CashDeskBalance.Name, new DateTime().toLocalTime());
        insert.add(CISales.CashDeskBalance.Date, date);
        insert.add(CISales.CashDeskBalance.Status, Status.find("Sales_CashDeskBalanceStatus", "Closed").getId());
        insert.add(CISales.CashDeskBalance.CrossTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.NetTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.RateNetTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.CashDeskBalance.Rate, new Object[] { 1, 1 });
        insert.add(CISales.CashDeskBalance.RateCurrencyId, baseCurrInst.getId());
        insert.add(CISales.CashDeskBalance.CurrencyId, baseCurrInst.getId());
        insert.execute();

        final Instance balanceInst = insert.getInstance();

        for (final TicketLineInfo t : _ticket.getTicketLines()) {

            QueryBuilder attrQueryBldr = new QueryBuilder(CISales.TransactionAbstract);
            attrQueryBldr.addWhereAttrEqValue(CISales.TransactionAbstract.Date, new Date());
            attrQueryBldr.addWhereAttrEqValue(CISales.TransactionAbstract.Account, idAccount);
            AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CISales.TransactionAbstract.Payment);

            QueryBuilder queryBldr2 = new QueryBuilder(CISales.Payment);
            queryBldr2.addWhereAttrInQuery(CISales.Payment.ID, attrQuery);
            InstanceQuery query = queryBldr2.getQuery();
            query.execute();
            while (query.next()) {
                Instance instPay = query.getCurrentValue();
                final Update update = new Update(instPay);
                update.add("TargetDocument", balanceInst.getId());
                update.execute();
            }
        }

        final Insert payInsert = new Insert(CIPOS.Payment);
        payInsert.add(CIPOS.Payment.Date, new DateTime());
        payInsert.add(CIPOS.Payment.CreateDocument, balanceInst.getId());
        payInsert.execute();

        final Instance payInst = payInsert.getInstance();

        for (final TicketLineInfo t : _ticket.getTicketLines()) {

            Integer payIds = t.getLineId();
            Long currId = baseCurrInst.getId();
            Double amounts = t.getTotal();

            BigDecimal crossTotal = BigDecimal.ZERO;
            final Map<Long, BigDecimal> curr2Rate = new HashMap<Long, BigDecimal>();
            final BigDecimal amount = new BigDecimal(amounts);
            final BigDecimal rate;
            if (curr2Rate.containsKey(currId)) {
                rate = curr2Rate.get(currId);
            } else {
                rate = getRate(date, currId);
                curr2Rate.put(currId, rate);
            }

            crossTotal = crossTotal.add(amount.divide(rate, BigDecimal.ROUND_HALF_UP));
            final Insert transInsert = new Insert(CISales.TransactionOutbound);
            transInsert.add(CISales.TransactionOutbound.Amount, amount);
            transInsert.add(CISales.TransactionOutbound.CurrencyId, currId);
            transInsert.add(CISales.TransactionOutbound.PaymentType, payIds);
            transInsert.add(CISales.TransactionOutbound.Payment, payInst.getId());
            transInsert.add(CISales.TransactionOutbound.Account, idAccount);
            transInsert.add(CISales.TransactionOutbound.Description, "CashDeskBalance");
            transInsert.add(CISales.TransactionOutbound.Date, new DateTime());
            transInsert.execute();
        }

        BigDecimal crossTotal = BigDecimal.ZERO;
        final Map<Long, BigDecimal> curr2Rate = new HashMap<Long, BigDecimal>();
        final Update update = new Update(balanceInst);
        update.add("CrossTotal", crossTotal);
        update.execute();

        return new Return();
    }

    protected BigDecimal getRate(final DateTime _date,
                                 final Long _currId)
        throws EFapsException
    {
        final BigDecimal ret;
        final QueryBuilder queryBldr = new QueryBuilder(CIERP.CurrencyRateClient);
        queryBldr.addWhereAttrEqValue(CIERP.CurrencyRateClient.CurrencyLink, _currId);
        queryBldr.addWhereAttrLessValue(CIERP.CurrencyRateClient.ValidFrom, _date.plusMinutes(1));
        queryBldr.addWhereAttrGreaterValue(CIERP.CurrencyRateClient.ValidUntil, _date);

        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIERP.CurrencyRateClient.Rate);
        multi.execute();
        if (multi.next()) {
            ret = multi.<BigDecimal>getAttribute(CIERP.CurrencyRateClient.Rate);
        } else {
            ret = BigDecimal.ONE;
        }
        return ret;
    }
}