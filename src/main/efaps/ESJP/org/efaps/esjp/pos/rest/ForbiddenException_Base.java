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

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

@EFapsUUID("e0f2c0cc-f479-4345-b750-b433e7b1f51f")
@EFapsApplication("eFapsApp-POS")
public abstract class ForbiddenException_Base
    extends WebApplicationException
{

    /** */
    private static final long serialVersionUID = 1L;

    public ForbiddenException_Base(final String _message) {
        super(Response.status(Response.Status.FORBIDDEN)
                    .entity(_message)
                    .type(MediaType.TEXT_PLAIN)
                    .build());
    }
}
