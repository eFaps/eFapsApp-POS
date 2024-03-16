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

import javax.ws.rs.core.Response;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.pos.dto.BalanceDto;
import org.efaps.pos.dto.BalanceStatus;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("61004751-c8c1-44ba-baa6-05f7604ec549")
@EFapsApplication("eFapsApp-POS")
public abstract class Balance_Base
    extends AbstractRest
{
    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Balance.class);

    public Response addBalance(final String identifier, final BalanceDto _balanceDto)
        throws EFapsException
    {
        checkAccess(identifier);
        LOG.debug("Recieved: {}", _balanceDto);
        final BalanceDto dto;
        if (_balanceDto.getOid() == null) {
            final Instance backendInst = getBackendInstance(identifier);

            final Insert insert = new Insert(CIPOS.Balance);
            insert.add(CIPOS.Balance.Status, Status.find(CIPOS.BalanceStatus.Open));
            insert.add(CIPOS.Balance.BackendLink, backendInst);
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

    public Response updateBalance(final String _identifier, final String _balanceOid, final BalanceDto _balanceDto)
        throws EFapsException
    {
        checkAccess(_identifier);
        LOG.debug("Recieved: {} - {}", _balanceOid, _balanceDto);
        final Response ret;
        final Instance balanceInst = Instance.get(_balanceOid);
        if (InstanceUtils.isKindOf(balanceInst, CIPOS.Balance) && _balanceDto.getOid().equals(_balanceOid)) {
            if ( _balanceDto.getEndAt() != null && _balanceDto.getStatus().equals(BalanceStatus.CLOSED)) {
                final Update update = new Update(Instance.get(_balanceOid));
                update.add(CIPOS.Balance.Status, Status.find(CIPOS.BalanceStatus.Closed));
                update.add(CIPOS.Balance.EndAt, _balanceDto.getEndAt());
                update.execute();
                ret = Response.ok()
                                .build();
            } else {
                ret = Response.status(Response.Status.BAD_REQUEST)
                            .build();
            }
        } else {
            ret = Response.status(Response.Status.NOT_FOUND)
                            .build();
        }
        return ret;
    }
}
