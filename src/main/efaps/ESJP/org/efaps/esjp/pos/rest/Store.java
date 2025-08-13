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

import java.util.ArrayList;
import java.util.List;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.store.Resource;
import org.efaps.pos.dto.StoreStatus;
import org.efaps.pos.dto.StoreStatusRequestDto;
import org.efaps.pos.dto.StoreStatusResponseDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@EFapsUUID("8c0c65a9-429f-4740-975e-06473f5c905e")
@EFapsApplication("eFapsApp-POS")
@Path("/pos/{identifier}/store")
public class Store
    extends AbstractRest
{

    private static final Logger LOG = LoggerFactory.getLogger(Stocktaking.class);

    @POST
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getObjectStatus(@PathParam("identifier") final String identifier,
                                    final StoreStatusRequestDto dto)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE);
        LOG.info("Store status request with: {}", dto);
        final List<StoreStatus> status = new ArrayList<>();
        for (final var oid : dto.getOids()) {
            final var instance = Instance.get(oid);
            if (instance.isValid() && instance.getType().hasStore()) {
                final Resource resource = org.efaps.db.store.Store.get(instance.getType().getStoreId())
                                .getResource(instance);
                status.add(StoreStatus.builder()
                                .withOid(oid)
                                .withExisting(resource.exists())
                                .withModifiedAt(resource.exists() ? resource.getModified() : null)
                                .build());
            }
        }
        final Response ret = Response.ok()
                        .entity(StoreStatusResponseDto.builder()
                                        .withStatus(status)
                                        .build())
                        .build();
        return ret;
    }
}
