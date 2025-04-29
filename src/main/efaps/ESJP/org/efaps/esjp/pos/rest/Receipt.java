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
import org.efaps.pos.dto.ReceiptDto;
import org.efaps.util.EFapsException;

/**
 * The Class Product.
 */
@EFapsUUID("4e545365-e0e7-4837-a187-50fc09f6e553")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Receipt
    extends Receipt_Base
{

    @Override
    @Path("/{identifier}/receipts")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addReceipt(@PathParam("identifier") final String _identifier, final ReceiptDto _receipt)
        throws EFapsException
    {
        return super.addReceipt(_identifier, _receipt);
    }

    @Override
    @Path("/{identifier}/receipts/{oid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getReceipt(@PathParam("identifier") final String identifier,
                               @PathParam("oid") final String oid)
        throws EFapsException
    {
        return super.getReceipt(identifier, oid);
    }

    @Override
    @Path("/{identifier}/receipts")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveReceipts(@PathParam("identifier") final String identifier,
                                     @QueryParam("number") final String number)
        throws EFapsException
    {
        return super.retrieveReceipts(identifier, number);
    }
}
