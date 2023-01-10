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
import org.efaps.ci.CIType;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.pos.dto.AbstractDocItemDto;
import org.efaps.pos.dto.TicketDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("dc0a081c-f070-456b-a36e-c28e3cc825e5")
@EFapsApplication("eFapsApp-POS")
public abstract class Ticket_Base
    extends AbstractDocument
{

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Receipt.class);

    @Override
    protected CIType getDocumentType()
    {
        return CIPOS.Ticket;
    }

    @Override
    protected CIType getEmployee2DocumentType()
    {
        return CIPOS.Employee2Ticket;
    }

    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    public Response addTicket(final String _identifier, final TicketDto _ticketDto)
        throws EFapsException
    {
        checkAccess(_identifier);
        checkAccess(_identifier);
        LOG.debug("Recieved: {}", _ticketDto);
        final TicketDto dto;
        if (_ticketDto.getOid() == null) {
            final Instance docInst = createDocument(Status.find(CIPOS.TicketStatus.Closed), _ticketDto);
            for (final AbstractDocItemDto item : _ticketDto.getItems()) {
                createPosition(docInst, CIPOS.TicketPosition, item, _ticketDto.getDate());
            }
            addPayments(docInst, _ticketDto);
            createTransactionDocument(_ticketDto, docInst);

            dto = TicketDto.builder()
                            .withId(_ticketDto.getId())
                            .withOID(docInst.getOid())
                            .build();
        } else {
            dto = TicketDto.builder().build();
        }
        final Response ret = Response.ok()
                        .entity(dto)
                        .build();
        return ret;
    }
}
