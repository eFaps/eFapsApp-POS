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

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.user.Company;
import org.efaps.db.Context;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.pos.rest.Contact;
import org.efaps.util.EFapsException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("2ff003b8-4879-41b5-b0ee-34d4434585a9")
@EFapsApplication("eFapsApp-POS")
public class ContactBatchJob
    implements Job
{

    private static final Logger LOG = LoggerFactory.getLogger(ContactBatchJob.class);

    @Override
    public void execute(JobExecutionContext context)
        throws JobExecutionException
    {
        try {

            for (final Long companyId : Context.getThreadContext().getPerson().getCompanies()) {
                final Company company = Company.get(companyId);
                Context.getThreadContext().setCompany(company);
                final var parameter = ParameterUtil.instance();
                new Contact().prepareContactsDump(parameter);
            }
            // remove the company to be sure
            Context.getThreadContext().setCompany(null);
        } catch (final EFapsException e) {
            LOG.error("Catched", e);
        }
    }
}
