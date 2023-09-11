/*
 * Copyright 2003 - 2019 The eFaps Team
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

import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.user.Role;
import org.efaps.db.Context;
import org.efaps.db.InstanceQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.util.EFapsException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@EFapsUUID("4f3f9a28-2cb4-440c-bbe0-98dac596c3b8")
@EFapsApplication("eFapsApp-POS")
public abstract class AbstractRest_Base
{

    /**
     * Check access on Assigned Role.
     *
     * @throws EFapsException the eFaps exception
     */
    protected void checkAccess()
        throws EFapsException
    {
        // POS_BE
        if (!Context.getThreadContext().getPerson().isAssigned(Role.get(UUID.fromString(
                        "b1fcb12e-b4e0-4c84-8382-c557d61fdb51")))) {
            throw new ForbiddenException("User does not have correct Roles assigned");
        }
    }

    /**
     * Check access on Role and Backend Identifier.
     *
     * @param _identifier the identifier
     * @throws EFapsException the eFaps exception
     */
    protected void checkAccess(final String _identifier)
        throws EFapsException
    {
        checkAccess();
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Backend);
        queryBldr.addWhereAttrEqValue(CIPOS.Backend.Status, Status.find(CIPOS.BackendStatus.Active));
        queryBldr.addWhereAttrEqValue(CIPOS.Backend.Identifier, _identifier);
        final InstanceQuery query = queryBldr.getQuery();
        if (CollectionUtils.isEmpty(query.execute())) {
            throw new ForbiddenException("No valid Backend registered.");
        }
    }

    protected ObjectMapper getObjectMapper()
    {
        final var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}
