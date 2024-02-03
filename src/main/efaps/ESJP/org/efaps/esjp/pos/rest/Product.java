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

import java.time.OffsetDateTime;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.EFapsException;

/**
 * The Class Product.
 */
@EFapsUUID("f1c816e2-1543-4975-b69a-799b4809802b")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Product
    extends Product_Base
{

    @Override
    @Path("/{identifier}/products")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response getProducts(@PathParam("identifier") final String _identifier,
                                @QueryParam("limit") final int limit,
                                @QueryParam("offset") final int offset,
                                @QueryParam("after") final OffsetDateTime after,
                                @QueryParam("term") final String term,
                                @QueryParam("barcode") final String barcode)
        throws EFapsException
    {
        return super.getProducts(_identifier, limit, offset, after, term, barcode);
    }

    @Override
    @Path("/{identifier}/products/{oid}")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response getProduct(@PathParam("identifier") final String _identifier,
                               @PathParam("oid") final String oid)
        throws EFapsException
    {
        return super.getProduct(_identifier, oid);
    }
}
