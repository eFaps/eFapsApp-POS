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
import java.util.Collections;
import java.util.List;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

@JsonDeserialize(builder = StatusReportDto.Builder.class)
@EFapsUUID("2c22be00-b5b4-44a1-8fc4-0a40781f1cd0")
@EFapsApplication("eFapsApp-POS")
public class StatusReportDto
{

    private final OffsetDateTime dateTime;
    private final List<BackendStatusDto> backendStatus;
    private final int reloadInterval;

    private StatusReportDto(Builder builder)
    {
        this.dateTime = builder.dateTime;
        this.backendStatus = builder.backendStatus;
        this.reloadInterval = builder.reloadInterval;
    }

    public OffsetDateTime getDateTime()
    {
        return dateTime;
    }

    public List<BackendStatusDto> getBackendStatus()
    {
        return backendStatus;
    }

    public int getReloadInterval()
    {
        return reloadInterval;
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static final class Builder
    {

        private OffsetDateTime dateTime;
        private List<BackendStatusDto> backendStatus = Collections.emptyList();
        private int reloadInterval = 5;

        private Builder()
        {
        }

        public Builder withDateTime(OffsetDateTime dateTime)
        {
            this.dateTime = dateTime;
            return this;
        }

        public Builder withBackendStatus(List<BackendStatusDto> backendStatus)
        {
            this.backendStatus = backendStatus;
            return this;
        }

        public Builder withReloadInterval(int reloadInterval)
        {
            this.reloadInterval = reloadInterval;
            return this;
        }

        public StatusReportDto build()
        {
            return new StatusReportDto(this);
        }
    }
}
