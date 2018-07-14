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
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.pos.dto.BalanceDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("61004751-c8c1-44ba-baa6-05f7604ec549")
@EFapsApplication("eFapsApp-POS")
public abstract class Balance_Base
{
    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Balance.class);


    public Response addBalance(final BalanceDto _balanceDto)
        throws EFapsException
    {
        LOG.debug("Recieved: {}", _balanceDto);
        final BalanceDto dto;
        if (_balanceDto.getOid() == null) {
            final Insert insert = new Insert(CIPOS.Balance);
            insert.add(CIPOS.Balance.Status, Status.find(CIPOS.BalanceStatus.Open));
            insert.add(CIPOS.Balance.Name, _balanceDto.getNumber());
            insert.add(CIPOS.Balance.UserLink, Instance.get(_balanceDto.getUserOid()));
            insert.add(CIPOS.Balance.StartAt, _balanceDto.getStartAt());
            insert.execute();

            dto = BalanceDto.builder()
                            .withId(_balanceDto.getId())
                            .withOID(insert.getInstance().getOid())
                            .build();
        } else {
            dto = BalanceDto.builder().build();
        }
        final Response ret = Response.ok()
                        .entity(dto)
                        .build();
        return ret;
    }
}