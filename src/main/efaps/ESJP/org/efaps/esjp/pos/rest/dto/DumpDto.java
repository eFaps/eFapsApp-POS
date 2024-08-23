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
package org.efaps.esjp.pos.rest.dto;

import java.time.OffsetDateTime;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = DumpDto.Builder.class)
@EFapsUUID("aa9e8037-4bbb-4085-9a92-6b35bda6db85")
@EFapsApplication("eFapsApp-POS")
public class DumpDto
{

    private final String oid;
    private final OffsetDateTime updateAt;

    private DumpDto(Builder builder)
    {
        this.oid = builder.oid;
        this.updateAt = builder.updateAt;
    }

    public String getOid()
    {
        return oid;
    }

    public OffsetDateTime getUpdateAt()
    {
        return updateAt;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {

        private String oid;
        private OffsetDateTime updateAt;

        private Builder()
        {
        }

        public Builder withOid(String oid)
        {
            this.oid = oid;
            return this;
        }

        public Builder withUpdateAt(OffsetDateTime updateAt)
        {
            this.updateAt = updateAt;
            return this;
        }

        public DumpDto build()
        {
            return new DumpDto(this);
        }
    }
}
