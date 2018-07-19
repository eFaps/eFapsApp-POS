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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.EFapsException;

@EFapsUUID("5bb09116-f83d-4ae9-9471-c261372a3202")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Config
    extends Config_Base
{

    @Override
    @GET
    @Path("/{identifier}/config")
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getConfig(@PathParam("identifier") final String _identifier)
        throws EFapsException
    {
        return super.getConfig(_identifier);
    }
}
