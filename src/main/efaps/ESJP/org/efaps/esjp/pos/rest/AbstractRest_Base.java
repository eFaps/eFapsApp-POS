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
import java.util.UUID;

import org.apache.commons.lang3.ArrayUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.user.Role;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.eql2.StmtFlag;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.pos.MonitoringService;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@EFapsUUID("4f3f9a28-2cb4-440c-bbe0-98dac596c3b8")
@EFapsApplication("eFapsApp-POS")
public abstract class AbstractRest_Base
{

    private static final Logger LOG = LoggerFactory.getLogger(AbstractRest.class);

    /**
     * Check access on Assigned Role.
     *
     * @throws EFapsException the eFaps exception
     */
    protected void checkAccess(final ACCESSROLE... roles)
        throws EFapsException
    {
        boolean ret = false;
        LOG.debug("Checking access by role for the pos REST endpoints:");
        for (final ACCESSROLE role : roles) {
            LOG.debug("  --> {}", role);
            ret = Context.getThreadContext().getPerson().isAssigned(Role.get(UUID.fromString(role.uuid)));
            if (ret) {
                break;
            }
        }
        if (!ret) {
            LOG.error("Access denied due to missing Roles");
            throw new ForbiddenException("User does not have correct Roles assigned");
        }
    }

    /**
     * Check access on Role and Backend Identifier.
     *
     * @param _identifier the identifier
     * @throws EFapsException the eFaps exception
     */
    protected void checkAccess(final String identifier,
                               final ACCESSROLE... roles)
        throws EFapsException
    {
        if (ArrayUtils.isEmpty(roles)) {
            checkAccess(ACCESSROLE.BE);
        } else {
            checkAccess(roles);
        }
        final var backendEval = EQL.builder()
                        .with(StmtFlag.REQCACHED)
                        .print().query(CIPOS.BackendAbstract)
                        .where()
                        .attribute(CIPOS.BackendAbstract.StatusAbstract).eq(CIPOS.BackendStatus.Active)
                        .and()
                        .attribute(CIPOS.BackendAbstract.Identifier).eq(identifier)
                        .select()
                        .instance()
                        .evaluate();
        final var backendInstsCount = backendEval.count();

        if (backendInstsCount == 0) {
            LOG.error("Access denied due to no Backend registration for: {}", identifier);
            throw new ForbiddenException("No valid Backend registered.");
        }
        if (backendInstsCount > 1) {
            LOG.error("Access denied due to having more than one Backend with the same identifier: {}", identifier);
            throw new ForbiddenException("Duplicated identifier");
        }
        backendEval.next();
        final var backendInst = backendEval.inst();
        if (InstanceUtils.isType(backendInst, CIPOS.Backend) && ArrayUtils.isNotEmpty(roles)
                        && !ArrayUtils.contains(roles, ACCESSROLE.BE)) {
            LOG.error("Access denied due to being a BE only endpoint");
            throw new ForbiddenException("BE only endpoint");
        }
        if (InstanceUtils.isType(backendInst, CIPOS.BackendMobile) && ArrayUtils.isNotEmpty(roles)
                        && !ArrayUtils.contains(roles, ACCESSROLE.MOBILE)) {
            LOG.error("Access denied due to being a Mobile only endpoint");
            throw new ForbiddenException("Mobile only endpoint");
        }
        MonitoringService.getLastRequestCache().put(identifier, OffsetDateTime.now());
    }

    protected Instance getBackendInstance(final String identifier)
        throws EFapsException
    {
        final var backendEval = EQL.builder()
                        .with(StmtFlag.REQCACHED)
                        .print().query(CIPOS.BackendAbstract)
                        .where()
                        .attribute(CIPOS.BackendAbstract.StatusAbstract).eq(CIPOS.BackendStatus.Active)
                        .and()
                        .attribute(CIPOS.BackendAbstract.Identifier).eq(identifier)
                        .select()
                        .instance()
                        .evaluate();
        backendEval.next();
        return backendEval.inst();
    }

    protected ObjectMapper getObjectMapper()
    {
        final var mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    public enum ACCESSROLE
    {

        BE("b1fcb12e-b4e0-4c84-8382-c557d61fdb51"), MOBILE("a1305af0-1d2a-477e-96fd-2debda9f95d8");

        String uuid;

        ACCESSROLE(final String uuid)
        {
            this.uuid = uuid;
        }
    }
}
