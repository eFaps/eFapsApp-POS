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
package org.efaps.esjp.pos.rest.module;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Context;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.pos.MonitoringService;
import org.efaps.util.EFapsException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@EFapsUUID("18834375-0363-405b-bdb8-2b581e4ea49e")
@EFapsApplication("eFapsApp-POS")
@Path("/ui/modules/pos-status")
public class StatusReportController
{

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatusReport()
        throws EFapsException
    {
        return Response.ok(getReport()).build();
    }

    public StatusReportDto getReport()
        throws EFapsException
    {
        final var eval = EQL.builder().print()
                        .query(CIPOS.BackendAbstract)
                        .where()
                        .attribute(CIPOS.BackendAbstract.StatusAbstract).eq(CIPOS.BackendStatus.Active)
                        .select()
                        .attribute(CIPOS.BackendAbstract.Identifier, CIPOS.BackendAbstract.Name)
                        .evaluate();
        final List<BackendStatusDto> backendStatus = new ArrayList<>();
        while (eval.next()) {
            final String identifier = eval.get(CIPOS.BackendAbstract.Identifier);
            OffsetDateTime lastSeenAt = MonitoringService.getLastRequestCache().get(identifier);
            if (lastSeenAt != null) {
                final var offset = Context.getThreadContext().getZoneId().getRules().getOffset(LocalDateTime.now());
                lastSeenAt = lastSeenAt.withOffsetSameInstant(offset);
            }
            backendStatus.add(BackendStatusDto.builder()
                            .withOid(eval.inst().getOid())
                            .withName(eval.get(CIPOS.BackendAbstract.Name))
                            .withLastSeenAt(lastSeenAt)
                            .build());
        }
        return StatusReportDto.builder()
                        .withDateTime(OffsetDateTime.now(Context.getThreadContext().getZoneId()).withNano(0))
                        .withBackendStatus(backendStatus)
                        .build();
    }
}
