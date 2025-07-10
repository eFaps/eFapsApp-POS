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
package org.efaps.esjp.pos;

import java.time.OffsetDateTime;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.util.cache.InfinispanCache;
import org.infinispan.Cache;

@EFapsUUID("3aff9ebc-f86f-4695-8bc7-0f2a4e3ba14e")
@EFapsApplication("eFapsApp-POS")
public class MonitoringService
{
    private static String REQCACHENAME = MonitoringService.class.getName() + ".ReqCache";

    public static Cache<String, OffsetDateTime> getLastRequestCache() {
        if (!InfinispanCache.get().exists(REQCACHENAME)) {
            InfinispanCache.get().initCache(REQCACHENAME);
        }
        return InfinispanCache.get().getCache(REQCACHENAME);
    }
}
