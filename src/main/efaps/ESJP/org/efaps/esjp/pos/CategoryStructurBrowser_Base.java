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
import org.efaps.eql.builder.Where;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ui.structurbrowser.StandartStructurBrowser;
import org.efaps.util.EFapsException;
/**
 * TODO description!
 *
 * @author The eFasp Team
 */
@EFapsUUID("cc86a55a-93c3-4cfa-a008-e37ecaa9c3c4")
@EFapsApplication("eFapsApp-POS")
public abstract class CategoryStructurBrowser_Base
    extends StandartStructurBrowser
{

    @Override
    protected Return checkHideColumn4Row(final Parameter _parameter)
        throws EFapsException
    {
        return new Return();
    }

    @Override
    protected void add2MainQuery(final Parameter _parameter, final Where _where)
        throws EFapsException
    {
       _where.attribute(CIPOS.Category.ParentLink).isNull();
    }

}
