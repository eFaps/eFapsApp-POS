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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.pos.UpdateDefinition;
import org.efaps.util.EFapsException;

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
}
