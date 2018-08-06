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

import javax.ws.rs.core.Response;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CISales;
import org.efaps.pos.dto.AbstractDocItemDto;
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
    extends AbstractDocument
{

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Receipt.class);

    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    public Response addReceipt(final String _identifier, final ReceiptDto _receiptDto)
        throws EFapsException
    {
        checkAccess(_identifier);
        LOG.debug("Recieved: {}", _receiptDto);
        final ReceiptDto dto;
        if (_receiptDto.getOid() == null) {
            final Instance docInst = createDocument(CISales.Receipt, Status.find(CISales.ReceiptStatus.Paid),
                            _receiptDto);
            for (final AbstractDocItemDto item : _receiptDto.getItems()) {
                createPosition(docInst, CISales.ReceiptPosition, item);
            }
            addPayments(docInst, _receiptDto);
            dto = ReceiptDto.builder()
                            .withId(_receiptDto.getId())
                            .withOID(docInst.getOid())
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
