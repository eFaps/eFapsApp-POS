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

import org.apache.commons.lang.builder.ToStringBuilder;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.pos.dto.AbstractPayableDocumentDto;
import org.efaps.pos.dto.DocType;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = PayAndEmitResponseDto.Builder.class)
@EFapsUUID("abf31062-ecbe-43d2-91e3-652669d53d6f")
@EFapsApplication("eFapsApp-POS")
public class PayAndEmitResponseDto
{

    private final DocType payableType;

    private final AbstractPayableDocumentDto payable;

    private final String ublHash;

    private PayAndEmitResponseDto(Builder builder)
    {
        this.payableType = builder.payableType;
        this.payable = builder.payable;
        this.ublHash = builder.ublHash;
    }

    public DocType getPayableType()
    {
        return payableType;
    }

    public AbstractPayableDocumentDto getPayable()
    {
        return payable;
    }

    public String getUblHash()
    {
        return ublHash;
    }

    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString(this);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {

        private DocType payableType;
        private AbstractPayableDocumentDto payable;
        private String ublHash;

        private Builder()
        {
        }

        public Builder withPayableType(DocType payableType)
        {
            this.payableType = payableType;
            return this;
        }

        public Builder withPayable(AbstractPayableDocumentDto payable)
        {
            this.payable = payable;
            return this;
        }

        public Builder withUblHash(String ublHash)
        {
            this.ublHash = ublHash;
            return this;
        }

        public PayAndEmitResponseDto build()
        {
            return new PayAndEmitResponseDto(this);
        }
    }
}
