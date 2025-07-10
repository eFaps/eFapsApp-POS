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
package org.efaps.esjp.pos.rest.module;

import java.time.OffsetDateTime;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = BackendStatusDto.Builder.class)
@EFapsUUID("2cc5876a-74e3-4cff-b1b6-c16202942da0")
@EFapsApplication("eFapsApp-POS")
public class BackendStatusDto
{

    private final String name;
    private final OffsetDateTime lastSeenAt;

    private BackendStatusDto(Builder builder)
    {
        this.name = builder.name;
        this.lastSeenAt = builder.lastSeenAt;
    }

    public String getName()
    {
        return name;
    }

    public OffsetDateTime getLastSeenAt()
    {
        return lastSeenAt;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {

        private String name;
        private OffsetDateTime lastSeenAt;

        private Builder()
        {
        }

        public Builder withName(String name)
        {
            this.name = name;
            return this;
        }

        public Builder withLastSeenAt(OffsetDateTime lastSeenAt)
        {
            this.lastSeenAt = lastSeenAt;
            return this;
        }

        public BackendStatusDto build()
        {
            return new BackendStatusDto(this);
        }
    }

}
