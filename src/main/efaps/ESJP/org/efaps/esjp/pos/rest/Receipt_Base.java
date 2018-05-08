/*
 * Copyright 2003 - 2018 The eFaps Team
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

import javax.ws.rs.core.Response;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.pos.dto.ReceiptDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("a4b4a8af-2349-497b-af84-32d3b4d2dd57")
@EFapsApplication("eFapsApp-POS")
public abstract class Receipt_Base
{

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Receipt.class);

    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    public Response addReceipt(final ReceiptDto _receiptDto)
        throws EFapsException
    {
        LOG.debug("Recieved: {}", _receiptDto);
        final ReceiptDto dto;
        if (_receiptDto.getOid() == null) {
            final Insert insert = new Insert(CISales.Receipt);
            insert.add(CISales.Receipt.Name, _receiptDto.getNumber());
            insert.add(CISales.Receipt.Date, _receiptDto.getDate());
            insert.add(CISales.Receipt.Status, Status.find(CISales.ReceiptStatus.Paid));

            final BigDecimal netTotal = _receiptDto.getNetTotal() == null ? BigDecimal.ZERO : _receiptDto.getNetTotal();
            final BigDecimal crossTotal = _receiptDto.getCrossTotal() == null ? BigDecimal.ZERO
                            : _receiptDto.getCrossTotal();

            insert.add(CISales.Receipt.NetTotal, netTotal);
            insert.add(CISales.Receipt.CrossTotal, crossTotal);
            insert.add(CISales.Receipt.DiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.Receipt.RateNetTotal, netTotal);
            insert.add(CISales.Receipt.RateCrossTotal, crossTotal);
            insert.add(CISales.Receipt.RateDiscountTotal, BigDecimal.ZERO);
            insert.add(CISales.Receipt.CurrencyId, ERP.CURRENCYBASE.get());
            insert.add(CISales.Receipt.RateCurrencyId, ERP.CURRENCYBASE.get());
            insert.add(CISales.Receipt.Rate, new Object[] { 1, 1 });
            insert.execute();

            dto = ReceiptDto.builder()
                            .withId(_receiptDto.getId())
                            .withOID(insert.getInstance().getOid())
                            .build();
        } else {
            dto = ReceiptDto.builder().build();
        }

        final Response ret = Response.ok()
                        .entity(dto)
                        .build();
        return ret;
    }
}
