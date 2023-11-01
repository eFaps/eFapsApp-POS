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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.promotions.PromotionService;
import org.efaps.util.EFapsException;

@EFapsUUID("a3abf58f-11b0-4b2f-a5c8-cf69bf897b41")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Promotion
    extends AbstractRest
{
    @Path("/{identifier}/promotions")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response getPromotions(@PathParam("identifier") final String identifier)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE);
        final var promotions = new PromotionService().getPromotions();
        final Response ret = Response.ok()
                        .entity(promotions)
                        .build();
        return ret;
    }
}
