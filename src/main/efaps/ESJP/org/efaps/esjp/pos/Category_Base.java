/*
 * Copyright 2003 - 2012 The eFaps Team
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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.pos;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f1c816e2-1543-4975-b69a-799b4809802b")
@EFapsApplication("eFapsApp-POS")
public abstract class Category_Base
{

    public Return connectTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Instance cat2subInst = _parameter.getInstance();
        final PrintQuery print = new PrintQuery(cat2subInst);
        final SelectBuilder catOidSel = new SelectBuilder().linkto(CIPOS.Category2SubscriptionCategory.FromLink).oid();
        print.addSelect(catOidSel);
        print.execute();
        final String catOid = print.<String>getSelect(catOidSel);
        return ret;
    }

    public Instance getParent(final Instance _instance)
        throws EFapsException
    {
        Instance ret = _instance;
        final PrintQuery catPrint = new PrintQuery(_instance);
        final SelectBuilder sel = new SelectBuilder().linkto(CIPOS.Category.ParentLink).oid();
        catPrint.addSelect(sel);
        catPrint.execute();
        if (catPrint.getSelect(sel) != null) {
            final Instance inst = Instance.get(catPrint.<String>getSelect(sel));
            if (inst.isValid()) {
                ret = getParent(inst);
            }
        }
        return ret;
    }
}
