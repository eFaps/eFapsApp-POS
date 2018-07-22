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

import java.util.UUID;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.user.Role;
import org.efaps.db.Context;
import org.efaps.util.EFapsException;

@EFapsUUID("4f3f9a28-2cb4-440c-bbe0-98dac596c3b8")
@EFapsApplication("eFapsApp-POS")
public abstract class AbstractRest_Base
{

    protected void checkAccess() throws EFapsException
    {
        // POS_BE
        if (!Context.getThreadContext().getPerson().isAssigned(Role.get(UUID.fromString(
                        "b1fcb12e-b4e0-4c84-8382-c557d61fdb51")))) {
            throw new ForbiddenException("User does not have correct Roles assigned");
        }
    }
}
