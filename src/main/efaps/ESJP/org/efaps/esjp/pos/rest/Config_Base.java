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

import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.admin.common.systemconfiguration.AbstractSysConfAttribute;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("7f5c44d3-ea2e-4d49-a1ba-b69715db111d")
@EFapsApplication("eFapsApp-POS")
public abstract class Config_Base
    extends AbstractRest
{
    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Config.class);

    public Response getConfig(final String _identifier)
        throws EFapsException
    {
        checkAccess(_identifier);
        final Map<String, String> config = Pos.CONFIG.get().entrySet().stream()
                        .collect(Collectors.toMap(e -> e.getKey().toString(), e -> e.getValue().toString()));
        LOG.debug("Request for Configs: {}", config);

        FieldUtils.getAllFieldsList(Pos.class).stream().filter(field -> {
            return Modifier.isStatic(field.getModifiers());
        }).filter(field -> {
            return !StringUtils.equalsAny(field.getName(), "CONFIG", "BASE", "SYSCONFUUID");
        }).forEach(field -> {
            try {
                final var fieldObject = FieldUtils.readStaticField(field);
                if (fieldObject instanceof AbstractSysConfAttribute) {
                    final var sysConfAttr = (AbstractSysConfAttribute<?,?>) fieldObject;
                    config.put( sysConfAttr.getKey(), String.valueOf(sysConfAttr.get()));
                }
            } catch (IllegalAccessException | EFapsException e1) {
                LOG.error("Catched error on read static field: {}", e1);
            }
        });

        config.put(ERP.COMPANY_NAME.getKey(), ERP.COMPANY_NAME.get());
        config.put(ERP.COMPANY_TAX.getKey(), ERP.COMPANY_TAX.get());
        config.put(ERP.COMPANY_ACTIVITY.getKey(), ERP.COMPANY_ACTIVITY.get());
        config.put(ERP.COMPANY_CITY.getKey(), ERP.COMPANY_CITY.get());
        config.put(ERP.COMPANY_COUNTRY.getKey(), ERP.COMPANY_COUNTRY.get());
        config.put(ERP.COMPANY_UBIGEO.getKey(), ERP.COMPANY_UBIGEO.get());
        config.put(ERP.COMPANY_STREET.getKey(), ERP.COMPANY_STREET.get());
        config.put(ERP.COMPANY_REGION.getKey(), ERP.COMPANY_REGION.get());
        config.put(ERP.COMPANY_CITY.getKey(), ERP.COMPANY_CITY.get());
        config.put(ERP.COMPANY_DISTRICT.getKey(), ERP.COMPANY_DISTRICT.get());
        config.put(ERP.COMPANY_ESTABLECIMIENTO.getKey(), ERP.COMPANY_ESTABLECIMIENTO.get());

        final Response ret = Response.ok()
                        .entity(config)
                        .build();
        return ret;
    }
}
