/*
 * Copyright 2003 - 2023 The eFaps Team
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
package org.efaps.esjp.pos;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.common.uiform.Create;
import org.efaps.util.EFapsException;
import org.efaps.util.RandomUtil;

@EFapsUUID("4ad4d7ae-bb0b-48a9-9af5-cf3055b5101c")
@EFapsApplication("eFapsApp-POS")
public class Backend
{

    public Return createMobile(final Parameter parameter)
        throws EFapsException
    {
        return new Create()
        {

            @Override
            protected void add2basicInsert(final Parameter parameter,
                                           final Insert insert)
                throws EFapsException
            {
                super.add2basicInsert(parameter, insert);
                insert.add(CIPOS.BackendAbstract.Identifier, RandomUtil.randomAlphabetic(8).toUpperCase());
            }
        }.execute(parameter);
    }
}
