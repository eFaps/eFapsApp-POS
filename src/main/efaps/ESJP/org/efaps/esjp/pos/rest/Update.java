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

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.pos.UpdateDefinition;
import org.efaps.pos.dto.UpdateConfirmationDto;
import org.efaps.util.EFapsException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@EFapsUUID("3e77f8ab-f87d-41b7-986b-504357dbd956")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Update
    extends AbstractRest
{
    @Path("/{identifier}/update")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response getUpdateDefinition(@PathParam("identifier") final String identifier)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE);
        final var backendInst = getBackendInstance(identifier);
        final var updateDto = new UpdateDefinition().getUpdate(backendInst);
        final Response ret = Response.ok()
                        .entity(updateDto)
                        .build();
        return ret;
    }

    @Path("/{identifier}/update/confirm")
    @POST
    public Response confirmUpdateDefinition(@PathParam("identifier") final String identifier,
                                            final UpdateConfirmationDto dto)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE);
        final var backendInst = getBackendInstance(identifier);

        new UpdateDefinition().confirm(backendInst, dto);
        final Response ret = Response.ok()
                        .build();
        return ret;
    }
}
