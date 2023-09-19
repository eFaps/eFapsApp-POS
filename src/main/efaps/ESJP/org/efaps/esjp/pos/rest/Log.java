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

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.ci.CIType;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Insert;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.pos.listener.IOnLog;
import org.efaps.pos.dto.LogEntryDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("3059a31c-8c71-49d3-bee3-ca5866c04b31")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Log
    extends AbstractRest
{

    public static final String ORDEROID = "orderOid";

    private static final Logger LOG = LoggerFactory.getLogger(Stocktaking.class);

    @Path("/{identifier}/log-entries")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addEntry(@PathParam("identifier") final String identifier,
                             final LogEntryDto dto)
        throws EFapsException
    {
        checkAccess(identifier);
        LOG.debug("LogEntry: {}", dto);

        final var eval = EQL.builder()
                        .print()
                        .query(CIPOS.Backend)
                        .where()
                        .attribute(CIPOS.Backend.Identifier).eq(identifier)
                        .select()
                        .attribute(CIPOS.Backend.Identifier)
                        .evaluate();
        eval.next();
        final var backendInst = eval.inst();

        final var inst = new org.efaps.esjp.erp.Log()
        {

            @Override
            protected CIType getType()
            {
                return CIPOS.Log;
            }

            @Override
            protected void addToInsert(final Insert insert)
                throws EFapsException
            {
                insert.set(CIPOS.Log.BackendLink, backendInst)
                                .set(CIPOS.Log.Ident, dto.getIdent());
            }
        }.withKey(dto.getKey())
                .withLevel(dto.getLevel().name())
                .withLogDateTime(dto.getCreatedAt())
                .withMessage(identifier)
                .withValue(dto.getValue())
                .register();

        if (dto.getInfo() != null && dto.getInfo().containsKey(ORDEROID)) {
            final var orderInst = Instance.get(dto.getInfo().get(ORDEROID));
            if (InstanceUtils.isType(orderInst, CIPOS.Order)) {
                EQL.builder().insert(CIPOS.Log2Order)
                                .set(CIPOS.Log2Order.FromLink, inst)
                                .set(CIPOS.Log2Order.ToLink, orderInst)
                                .execute();
            }
        }
        for (final IOnLog listener : Listener.get().<IOnLog>invoke(IOnLog.class)) {
            listener.afterCreate(inst);
        }
        return Response.ok(inst.getOid())
                        .build();
    }
}
