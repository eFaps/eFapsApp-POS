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
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.pos.dto.ReportToBaseDto;
import org.efaps.util.EFapsException;
import org.efaps.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

@EFapsUUID("bed0d26f-3dd6-40dc-a75c-dbf0a075e353")
@EFapsApplication("eFapsApp-POS")
public abstract class Backend_Base
    extends AbstractRest
{

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Backend.class);

    public Response getIdentifier()
        throws EFapsException
    {
        checkAccess(ACCESSROLE.BE);
        LOG.debug("Recieved request for Identifier");
        final String ident;
        if (Pos.ALLOWAUTOIDENT.get()) {
            ident = RandomUtil.randomAlphanumeric(16);
            final Insert insert = new Insert(CIPOS.Backend);
            insert.add(CIPOS.Backend.Name, "New backend");
            insert.add(CIPOS.Backend.Identifier, ident);
            insert.add(CIPOS.Backend.Status, Status.find(CIPOS.BackendStatus.Active));
            insert.execute();
        } else {
            ident = "Auto ident is deactivated.";
        }
        final Response ret = Response.ok()
                        .entity(ident)
                        .build();
        return ret;
    }

    public Response reportToBase(final String identifier,
                                 final ReportToBaseDto dto)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE);
        LOG.debug("Recieved request for report to base");

        String payload = null;
        try {
            payload = getObjectMapper().writeValueAsString(dto.getDetails());
        } catch (final JsonProcessingException e) {
            LOG.error("Catched", e);
        }

        final var beInst = getBackendInstance(identifier);
        EQL.builder()
                        .insert(CIPOS.MonitoringReportToBase)
                        .set(CIPOS.MonitoringReportToBase.BackendLink, beInst)
                        .set(CIPOS.MonitoringReportToBase.Version, dto.getVersion())
                        .set(CIPOS.MonitoringReportToBase.InstalationId, dto.getInstalationId())
                        .set(CIPOS.MonitoringReportToBase.RegisteredAt, dto.getCreatedAt())
                        .set(CIPOS.MonitoringReportToBase.Payload, payload)
                        .execute();
        return Response.ok().build();
    }
}
