/*
 * Copyright 2003 - 2023 The eFaps Team
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

import java.time.OffsetDateTime;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.pos.dto.ContactDto;
import org.efaps.util.EFapsException;

@EFapsUUID("c36628f2-d679-4b4e-ab45-a0bedb83468f")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Contact
    extends Contact_Base
{
    @Override
    @Path("/{identifier}/contacts")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response getContacts(@PathParam("identifier") final String _identifier,
                                @QueryParam("limit") final int limit,
                                @QueryParam("offset") final int offset,
                                @QueryParam("after") final OffsetDateTime after)
        throws EFapsException
    {
        return super.getContacts(_identifier, limit, offset, after);
    }

    @Override
    @Path("/{identifier}/contacts/{oid}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response getContact(@PathParam("identifier") final String _identifier,
                               @PathParam("oid") final String oid)
        throws EFapsException
    {
        return super.getContact(_identifier, oid);
    }

    @Override
    @Path("/{identifier}/contacts")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addContact(@PathParam("identifier") final String _identifier, final ContactDto _contact)
        throws EFapsException
    {
        return super.addContact(_identifier, _contact);
    }
}
