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
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.AttributeQuery;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.pos.jaxb.JmsCloseCash;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 Copyright 2003 - 2009 The eFaps Team

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 Author:          The eFaps Team
 Revision:        $Rev: 7910 $
 Last Changed:    $Date: 2012-08-14 18:19:11 -0500 (mar, 14 ago 2012) $
 Last Changed By: $Author: diana.uriol@efaps.org $
 */
@EFapsUUID("6a1e1062-3abb-4698-b4e3-44201bf6c7d3")
@EFapsRevision("$Rev: 7479 $")
public abstract class PosAccount_Base
{
    /**
     * Method for create a new Cash Desk Balance.
     *
     * @param _parameter Parameter as passed from the eFaps API.
     * @return new Return.
     * @throws EFapsException on error.
     */
    public Return cashDeskBalance(final JmsCloseCash _close)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.POS);
        queryBldr.addWhereAttrEqValue(CIPOS.POS.Name, _close.getM_sHost());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPOS.POS.AccountLink);
        multi.addAttribute(CIPOS.POS.ID);
        multi.execute();
        Long idAccount = null;
        Long idPos = null;
        while (multi.next()) {
            idAccount = multi.<Long>getAttribute(CIPOS.POS.AccountLink);
            idPos = multi.<Long>getAttribute(CIPOS.POS.ID);
        }

        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
        final DateTime date = new DateTime(_close.getM_dDate());

        final Insert insert = new Insert(CIPOS.CashDeskBalance);
        insert.add(CIPOS.CashDeskBalance.Name, new DateTime().toLocalTime());
        insert.add(CIPOS.CashDeskBalance.Date, date);
        insert.add(CIPOS.CashDeskBalance.Status, Status.find("Sales_CashDeskBalanceStatus", "Closed").getId());
        insert.add(CIPOS.CashDeskBalance.CrossTotal, BigDecimal.ZERO);
        insert.add(CIPOS.CashDeskBalance.NetTotal, BigDecimal.ZERO);
        insert.add(CIPOS.CashDeskBalance.DiscountTotal, BigDecimal.ZERO);
        insert.add(CIPOS.CashDeskBalance.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CIPOS.CashDeskBalance.RateNetTotal, BigDecimal.ZERO);
        insert.add(CIPOS.CashDeskBalance.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CIPOS.CashDeskBalance.Rate, new Object[] { 1, 1 });
        insert.add(CIPOS.CashDeskBalance.RateCurrencyId, baseCurrInst.getId());
        insert.add(CIPOS.CashDeskBalance.CurrencyId, baseCurrInst.getId());
        insert.execute();

        final Instance balanceInst = insert.getInstance();

        final Insert payInsert = new Insert(CIPOS.Payment);
        payInsert.add(CIPOS.Payment.Date, new DateTime());
        payInsert.add(CIPOS.Payment.CreateDocument, balanceInst.getId());
        payInsert.execute();

        final Instance payInst = payInsert.getInstance();
        final Long currId = baseCurrInst.getId();
        BigDecimal amount = BigDecimal.ZERO;

        final Insert transInsert = new Insert(CIPOS.TransactionOutbound);
        transInsert.add(CIPOS.TransactionOutbound.Amount, amount);
        transInsert.add(CIPOS.TransactionOutbound.CurrencyId, currId);
        transInsert.add(CIPOS.TransactionOutbound.Payment, payInst.getId());
        transInsert.add(CIPOS.TransactionOutbound.Account, idAccount);
        transInsert.add(CIPOS.TransactionOutbound.Description, "CashDeskBalance");
        transInsert.add(CIPOS.TransactionOutbound.Date, new DateTime());
        transInsert.add(CIPOS.TransactionOutbound.POSLink, idPos);
        transInsert.execute();
        final Instance transInst = transInsert.getInstance();

        final QueryBuilder attrQueryBldr = new QueryBuilder(CIPOS.TransactionAbstract);
        attrQueryBldr.addWhereAttrLessValue(CIPOS.TransactionAbstract.Date, new DateTime());
        attrQueryBldr.addWhereAttrEqValue(CIPOS.TransactionAbstract.POSLink, idPos); // .Account,idAccount);
        final AttributeQuery attrQuery = attrQueryBldr.getAttributeQuery(CIPOS.TransactionAbstract.Payment);

        final QueryBuilder queryBldr2 = new QueryBuilder(CIPOS.Payment);
        queryBldr2.addWhereAttrInQuery(CIPOS.Payment.ID, attrQuery);
        queryBldr2.addWhereAttrIsNull(CIPOS.Payment.TargetDocument);
        final InstanceQuery query = queryBldr2.getQuery();
        query.execute();
        while (query.next()) {

            final Instance instPay = query.getCurrentValue();

            final QueryBuilder queryBldr4 = new QueryBuilder(CIPOS.TransactionInbound);
            queryBldr4.addWhereAttrEqValue(CIPOS.TransactionInbound.Payment, instPay.getId());
            final MultiPrintQuery multi4 = queryBldr4.getPrint();
            multi4.addAttribute(CIPOS.TransactionInbound.Amount);
            multi4.execute();
            while (multi4.next()) {
                amount = amount.add(multi4.<BigDecimal>getAttribute(CIPOS.TransactionInbound.Amount));
            }

            final Update update = new Update(instPay);
            update.add("TargetDocument", balanceInst.getId());
            update.execute();
        }

        final Update update = new Update(balanceInst);
        update.add(CISales.CashDeskBalance.CrossTotal, amount);
        update.execute();

        final Update updateTrans = new Update(transInst);
        updateTrans.add(CIPOS.TransactionOutbound.Amount, amount);
        updateTrans.execute();

        final Insert posToBalanceIns = new Insert(CIPOS.POS2CashDeskBalance);
        posToBalanceIns.add(CIPOS.POS2CashDeskBalance.FromLink, idPos);
        posToBalanceIns.add(CIPOS.POS2CashDeskBalance.ToLink, balanceInst.getId());
        posToBalanceIns.execute();

        return new Return();
    }

}
