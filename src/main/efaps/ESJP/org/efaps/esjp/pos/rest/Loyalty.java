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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.loyalty.LoyaltyService;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("0312c52c-65d1-42bb-ac63-1dab4aa2a59e")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Loyalty
    extends AbstractRest
{
    private static final Logger LOG = LoggerFactory.getLogger(Loyalty.class);

    @Path("/{identifier}/loyalty-programs")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getPrograms(@PathParam("identifier") final String identifier,
                                @QueryParam("contact-identifier") final String contactIdentifier)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE, ACCESSROLE.MOBILE);
        LOG.info("Request for loyalty-programs witdth identifier {} and contactIdentifier: {}", identifier, contactIdentifier);
        if (contactIdentifier != null) {
            new LoyaltyService().queryPrograms4Contact(contactIdentifier);
        }
        final Response ret = Response.ok()
                        .build();
        return ret;
    }
}
