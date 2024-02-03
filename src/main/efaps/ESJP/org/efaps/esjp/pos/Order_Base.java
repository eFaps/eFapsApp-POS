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

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Delete;
import org.efaps.db.Instance;
import org.efaps.db.InstanceQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.util.EFapsException;

@EFapsUUID("041aac04-f455-4d6c-a84c-a27fbcd3bb9c")
@EFapsApplication("eFapsApp-POS")
public abstract class Order_Base
{
    /**
     * Pre delete trigger. Delete positions.
     *
     * @param _parameter the parameter
     * @return the return
     * @throws EFapsException the e faps exception
     */
    public Return preDeleteTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Instance orderInst = _parameter.getInstance();
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.OrderPosition);
        queryBldr.addWhereAttrEqValue(CIPOS.OrderPosition.OrderLink, orderInst);
        final InstanceQuery query = queryBldr.getQuery();
        query.execute();
        while (query.next()) {
            final Delete delete = new Delete(query.getCurrentValue());
            delete.execute();
        }
        return new Return();
    }
}
