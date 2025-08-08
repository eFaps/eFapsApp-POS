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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.pos.dto.CreditNoteDto;
import org.efaps.util.EFapsException;

@EFapsUUID("cc98857d-0670-4f2b-84aa-3506c3b45ca9")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class CreditNote
    extends CreditNote_Base
{

    @Override
    @Path("/{identifier}/creditnotes")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addCreditNote(@PathParam("identifier") final String _identifier, final CreditNoteDto _creditNote)
        throws EFapsException
    {
        return super.addCreditNote(_identifier, _creditNote);
    }

    @Override
    @Path("/{identifier}/creditnotes/{oid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getCreditNote(@PathParam("identifier") final String identifier,
                                  @PathParam("oid") final String oid)
        throws EFapsException
    {
        return super.getCreditNote(identifier, oid);
    }


    @Override
    @Path("/{identifier}/creditnotes")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveCreditNotes(@PathParam("identifier") final String identifier,
                                        @QueryParam("number") final String number)
        throws EFapsException
    {
        return super.retrieveCreditNotes(identifier, number);
    }
}

