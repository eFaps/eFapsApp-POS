/*
 * Copyright 2003 - 2009 The eFaps Team
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
 * Revision:        $Rev: 7179 $
 * Last Changed:    $Date: 2011-10-07 13:34:24 -0500 (vie, 07 oct 2011) $
 * Last Changed By: $Author: jorge.cueva@moxter.net $
 */

package org.efaps.esjp.pos.documents;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
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
import org.efaps.db.Context;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.uitable.MultiPrint;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
import org.efaps.esjp.pos.jaxb.TicketInfo;
import org.efaps.esjp.pos.jaxb.TicketLineInfo;
import org.efaps.esjp.sales.Calculator_Base;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.ui.wicket.util.EFapsKey;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: Payment_Base.java 7179 2011-10-07 18:34:24Z jorge.cueva@moxter.net $
 */
@EFapsUUID("dda77565-8a2b-4a69-b100-b723f062ffcc")
@EFapsRevision("$Rev: 7179 $")
public  abstract class PosPayment_Base
    implements Serializable
{

    /**
     * String used as key to store the Date int the Session.
     */
    public static final String OPENAMOUNT_SESSIONKEY = "eFaps_SalesPayment_OpenAmount_SessionKey";

    /**
     * Needed for serialization.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Metho used to create a Payment.
     *
     * @param _parameter Parameter as passed from the eFpas API
     * @return new Return
     * @throws EFapsException on error
     */
    public Return create(final TicketInfo _ticket)
        throws EFapsException
    {
            final CreatedDoc docInst = new PosReceipt().createTicket(_ticket);

            final String dateStr = _ticket.getDate().toString();

            final Insert insert = new Insert(CISales.Payment);
            insert.add(CISales.Payment.Date, dateStr);
            insert.add(CISales.Payment.CreateDocument, ((Long) docInst.getInstance().getId()));
            insert.execute();

            final Instance paymentInst = insert.getInstance();

            final QueryBuilder queryBldr = new QueryBuilder(CIPOS.POS);
            queryBldr.addWhereAttrEqValue(CIPOS.POS.Name, _ticket.getHost());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIPOS.POS.Account);
            multi.execute();
            long idAccount = 0;
            while (multi.next()) {
                idAccount = multi.getAttribute(CIPOS.POS.Account);
            }

            for (final TicketLineInfo t : _ticket.getTicketLines()) {
                final Insert transIns = new Insert(CISales.TransactionInbound);
                transIns.add(CISales.TransactionInbound.Amount, t.getTotal());
                transIns.add(CISales.TransactionInbound.CurrencyId, 1);
                transIns.add(CISales.TransactionInbound.Payment, ((Long) paymentInst.getId()).toString());
                transIns.add(CISales.TransactionInbound.PaymentType, _ticket.getM_paymentName());
                transIns.add(CISales.TransactionInbound.Description, t.getProductName());
                transIns.add(CISales.TransactionInbound.Date, _ticket.getDate());
                transIns.add(CISales.TransactionInbound.Account, idAccount);
                transIns.execute();
            }
        return new Return();
    }



    /**
     * Method to calculate the open amount for an instance.
     *
     * @param _parameter Parameter as passed from the eFaps API
     * @param _instance instance the open amount is wanted for
     * @return open amount
     * @throws EFapsException on error
     */

    /**
     * Represents on open amount.
     */
    public class OpenAmount
        implements Serializable
    {
        /**
         * Needed for serialization.
         */
        private static final long serialVersionUID = 1L;

        /**
         * CurrencyInst of this OpenAmount.
         */
        private final CurrencyInst currencyInstance;

        /**
         * Cross Total.
         */
        private final BigDecimal crossTotal;

        /**
         * Date of this OpenAmount.
         */
        private final DateTime date;

        /**
         * @param _currencyInstance Currency Instance
         * @param _crossTotal       Cross Total
         * @param _rateDate         date of the rate
         */
        public OpenAmount(final CurrencyInst _currencyInstance,
                          final BigDecimal _crossTotal,
                          final DateTime _rateDate)
        {
            this.currencyInstance = _currencyInstance;
            this.crossTotal = _crossTotal;
            this.date = _rateDate;
        }

        /**
         * Getter method for the instance variable {@link #symbol}.
         *
         * @return value of instance variable {@link #symbol}
         * @throws EFapsException on error
         */
        public String getSymbol()
            throws EFapsException
        {
            return this.currencyInstance.getSymbol();
        }

        /**
         * Getter method for the instance variable {@link #currencyInstance}.
         *
         * @return value of instance variable {@link #currencyInstance}
         */
        public Instance getCurrencyInstance()
        {
            return this.currencyInstance.getInstance();
        }

        /**
         * Getter method for the instance variable {@link #crossTotal}.
         *
         * @return value of instance variable {@link #crossTotal}
         */
        public BigDecimal getCrossTotal()
        {
            return this.crossTotal;
        }

        /**
         * Getter method for the instance variable {@link #date}.
         *
         * @return value of instance variable {@link #date}
         */
        public DateTime getDate()
        {
            return this.date;
        }

    }
}
