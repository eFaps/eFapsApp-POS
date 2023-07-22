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

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.EFapsException;

@EFapsUUID("af40729e-51f2-4b78-8caf-51fd7095dad0")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Warehouse
    extends Warehouse_Base
{
    @Override
    @Path("/{identifier}/warehouses")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getWarehouses(@PathParam("identifier") final String _identifier)
        throws EFapsException
    {
        return super.getWarehouses(_identifier);
    }
}
