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

import java.io.StringWriter;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.efaps.admin.datamodel.Attribute;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.pos.jaxb.CategoryInfo;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("f1c816e2-1543-4975-b69a-799b4809802b")
@EFapsRevision("$Rev$")
public abstract class Category_Base
{

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return Return
     * @throws EFapsException on error
     */
    public Return insertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        final Return ret = new Return();
        final Map<?,?> values = (Map<?, ?>) _parameter.get(ParameterValues.NEW_VALUES);

        final Attribute uuidAttr = CIPOS.Category.getType().getAttribute(CIPOS.Category.UUID.name);
        final Attribute nameAttr = CIPOS.Category.getType().getAttribute(CIPOS.Category.Name.name);
        final Attribute parentAttr = CIPOS.Category.getType().getAttribute(CIPOS.Category.ParentLink.name);
        final CategoryInfo cat = new CategoryInfo();
        cat.setName((String) ((Object[]) values.get(nameAttr))[0]);
        cat.setUuid((String) ((Object[]) values.get(uuidAttr))[0]);
        if (values.containsKey(parentAttr)) {

        }

        try {
            final JAXBContext jc = JAXBContext.newInstance(CategoryInfo.class);
            final Marshaller marschaller = jc.createMarshaller();
            final StringWriter writer = new StringWriter();
            marschaller.marshal(cat, writer);

            final Insert insert = new Insert(CIPOS.MessageCategoryInsert);
            insert.add(CIPOS.MessageCategoryInsert.Content, writer.toString());
            insert.execute();

        } catch (final JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return ret;
    }
}
