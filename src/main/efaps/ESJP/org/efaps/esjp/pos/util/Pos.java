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

package org.efaps.esjp.pos.util;

import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.IBitEnum;
import org.efaps.admin.datamodel.attributetype.BitEnumType;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.api.annotation.EFapsSysConfAttribute;
import org.efaps.api.annotation.EFapsSystemConfiguration;
import org.efaps.esjp.admin.common.systemconfiguration.BooleanSysConfAttribute;
import org.efaps.util.cache.CacheReloadException;

@EFapsUUID("77a6fc1e-a406-419f-a1aa-8207255a2522")
@EFapsApplication("eFapsApp-POS")
@EFapsSystemConfiguration("b038bf69-b588-431d-8c02-f53d4aac46c9")
public class Pos
{
    /** The base. */
    public static final String BASE = "org.efaps.pos.";
    /** POS-Configuration. */
    public static final UUID SYSCONFUUID = UUID.fromString("b038bf69-b588-431d-8c02-f53d4aac46c9");

    /** See description. */
    @EFapsSysConfAttribute
    public static final BooleanSysConfAttribute ACTIVATE = new BooleanSysConfAttribute()
                    .sysConfUUID(SYSCONFUUID)
                    .key(BASE + "Activate")
                    .description("Activate the POS implementation");

    public enum Role implements IBitEnum
    {
        ADMIN, USER;

        @Override
        public int getInt()
        {
            return BitEnumType.getInt4Index(ordinal());
        }

        @Override
        public int getBitIndex()
        {
            return ordinal();
        }
    }

    public enum DocType implements IBitEnum
    {
        RECEIPT, INVOICE, TICKET;

        @Override
        public int getInt()
        {
            return BitEnumType.getInt4Index(ordinal());
        }

        @Override
        public int getBitIndex()
        {
            return ordinal();
        }
    }

    /**
     * @return the SystemConfigruation for Payroll
     * @throws CacheReloadException on error
     */
    public static SystemConfiguration getSysConfig()
        throws CacheReloadException
    {
        // POS-Configuration
        return SystemConfiguration.get(SYSCONFUUID);
    }
}