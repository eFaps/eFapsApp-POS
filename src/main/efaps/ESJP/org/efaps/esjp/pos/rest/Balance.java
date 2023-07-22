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

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.pos.dto.BalanceDto;
import org.efaps.util.EFapsException;

@EFapsUUID("9b34adda-5341-4ed5-99b4-295842e43ee2")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Balance
    extends Balance_Base
{
    @Override
    @Path("/{identifier}/balance")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addBalance(@PathParam("identifier") final String _identifier, final BalanceDto _balanceDto)
        throws EFapsException
    {
        return super.addBalance(_identifier, _balanceDto);
    }

    @Override
    @Path("/{identifier}/balance/{oid}")
    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateBalance(@PathParam("identifier") final String _identifier,
                                  @PathParam("oid") final String _balanceOid, final BalanceDto _balanceDto)
        throws EFapsException
    {
        return super.updateBalance(_identifier, _balanceOid, _balanceDto);
    }
}
