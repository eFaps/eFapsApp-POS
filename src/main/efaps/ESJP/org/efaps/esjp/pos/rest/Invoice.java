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
import org.efaps.pos.dto.InvoiceDto;
import org.efaps.util.EFapsException;

@EFapsUUID("966b71f0-fd1d-4fc6-ad27-354d8affa46a")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Invoice
    extends Invoice_Base
{

    @Override
    @Path("/{identifier}/invoices")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addInvoice(@PathParam("identifier") final String identifier,
                               final InvoiceDto receipt)
        throws EFapsException
    {
        return super.addInvoice(identifier, receipt);
    }

    @Override
    @Path("/{identifier}/invoices/{oid}")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getInvoice(@PathParam("identifier") final String identifier,
                               @PathParam("oid") final String oid)
        throws EFapsException
    {
        return super.getInvoice(identifier, oid);
    }

    @Override
    @Path("/{identifier}/invoices")
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response retrieveInvoices(@PathParam("identifier") final String identifier,
                                 @QueryParam("number") final String number)
        throws EFapsException
    {
        return super.retrieveInvoices(identifier, number);
    }
}
