/*
 * Copyright 2003 - 2019 The eFaps Team
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
import org.efaps.esjp.ci.CIPOS;
import org.efaps.pos.dto.AbstractDocItemDto;
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

            final Instance docInst = createDocument(CIPOS.Order, status, _orderDto);
            for (final AbstractDocItemDto item : _orderDto.getItems()) {
                createPosition(docInst, CIPOS.OrderPosition, item);
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
}
