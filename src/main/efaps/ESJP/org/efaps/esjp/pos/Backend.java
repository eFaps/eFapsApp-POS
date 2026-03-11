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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.eql.EQL;
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

    public Return createBackend(final Parameter parameter)
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
                insert.add(CIPOS.BackendAbstract.Identifier, RandomUtil.randomAlphanumeric(16));
            }
        }.execute(parameter);
    }

    public Return autoComplete4Backend(final Parameter parameter)
        throws EFapsException
    {
        final String input = (String) parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<>();
        if (input.length() > 0) {
            final Map<String, Map<String, String>> tmpMap = new TreeMap<>();

            final var eval = EQL.builder().print().query(CIPOS.Backend)
                            .where()
                            .attribute(CIPOS.Backend.Name).ilike(input)
                            .select().attribute(CIPOS.Backend.Name, CIPOS.Backend.Description)
                            .evaluate();

            while (eval.next()) {
                final long id = eval.inst().getId();
                final String name = eval.get(CIPOS.Backend.Name);
                final String description = eval.get(CIPOS.Backend.Description);
                final String choice = name + " - " + description;
                final Map<String, String> map = new HashMap<>();
                map.put("eFapsAutoCompleteKEY", String.valueOf(id));
                map.put("eFapsAutoCompleteCHOICE", choice);
                map.put("eFapsAutoCompleteVALUE", name);
                tmpMap.put(choice, map);
            }
            list.addAll(tmpMap.values());
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }
}
