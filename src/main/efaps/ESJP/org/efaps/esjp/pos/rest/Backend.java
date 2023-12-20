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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.pos.dto.ReportToBaseDto;
import org.efaps.util.EFapsException;

@EFapsUUID("488b32a8-c2c1-4bcf-aca4-4093b9dfb9fc")
@EFapsApplication("eFapsApp-POS")
@Path("/pos/backend")
public class Backend
    extends Backend_Base
{

    @Override
    @Path("/identifier")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getIdentifier()
        throws EFapsException
    {
        return super.getIdentifier();
    }

    @Override
    @Path("/{identifier}/report-to-base")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response reportToBase(@PathParam("identifier") final String _identifier,
                                 final ReportToBaseDto dto)
        throws EFapsException
    {
        return super.reportToBase(_identifier, dto);
    }
}
