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
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.pos.dto.CreateDocumentDto;
import org.efaps.pos.dto.OrderDto;
import org.efaps.util.EFapsException;

@EFapsUUID("658ee4d2-25db-4146-b928-873e9e70f77f")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Order
    extends Order_Base
{

    @Override
    @Path("/{identifier}/orders")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addOrder(@PathParam("identifier") final String _identifier,
                             final OrderDto _order)
        throws EFapsException
    {
        return super.addOrder(_identifier, _order);
    }

    @Override
    @Path("/{identifier}/documents/orders")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createOrder(@PathParam("identifier") final String identifier,
                                final CreateDocumentDto dto)
        throws EFapsException
    {
        return super.createOrder(identifier, dto);
    }

    @Override
    @Path("/{identifier}/documents/orders/{oid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrder(@PathParam("identifier") final String identifier,
                                @PathParam("oid") final String oid,
                                final CreateDocumentDto dto)
        throws EFapsException
    {
        return super.updateOrder(identifier, oid, dto);
    }

    @Override
    @Path("/{identifier}/documents/orders/{oid}/contact/{contactOid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateOrderWithContact(@PathParam("identifier") final String identifier,
                                           @PathParam("oid") final String oid,
                                           @PathParam("contactOid") final String contactOid)
        throws EFapsException
    {
        return super.updateOrderWithContact(identifier, oid, contactOid);
    }

    @Override
    @Path("/{identifier}/documents/orders/{oid}")
    @GET
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOrder(@PathParam("identifier") final String identifier,
                             @PathParam("oid") final String oid)
        throws EFapsException
    {
        return super.getOrder(identifier, oid);
    }
}
