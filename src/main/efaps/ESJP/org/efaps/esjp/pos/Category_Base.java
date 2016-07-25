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

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
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
        final Instance catInst = Instance.get(catOid);
        enque4SendMsg(_parameter, catInst, CIPOS.MessageCategoryConnect);
        return ret;
    }

    protected void enque4SendMsg(final Parameter _parameter,
                                 final Instance _catInst,
                                 final CIType _messageType)
        throws EFapsException
    {

        final PrintQuery catPrint = new PrintQuery(_catInst);
        final SelectBuilder sel = new SelectBuilder().linkto(CIPOS.Category.ParentLink).attribute(CIPOS.Category.UUID);
        catPrint.addAttribute(CIPOS.Category.UUID, CIPOS.Category.Name);
        catPrint.addSelect(sel);
        catPrint.execute();

        final CategoryInfo cat = new CategoryInfo();
        cat.setName(catPrint.<String>getAttribute(CIPOS.Category.Name));
        cat.setUuid(catPrint.<String>getAttribute(CIPOS.Category.UUID));
        if (catPrint.getSelect(sel) != null) {
            cat.setParentUUID(catPrint.<String>getSelect(sel));
        }
        try {
            final JAXBContext jc = JAXBContext.newInstance(CategoryInfo.class);
            final Marshaller marschaller = jc.createMarshaller();
            final StringWriter writer = new StringWriter();
            marschaller.marshal(cat, writer);

            final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Category2SubscriptionCategory);
            queryBldr.addWhereAttrEqValue(CIPOS.Category2SubscriptionCategory.FromLink, getParent(_catInst).getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder jmsOidSel = new SelectBuilder().linkto(CIPOS.Category2SubscriptionCategory.ToLink)
                            .linkto(CIPOS.SubscriptionCategory.JmsLink).oid();
            multi.addSelect(jmsOidSel);
            multi.execute();
            while (multi.next()) {
                final String jmsOid = multi.<String>getSelect(jmsOidSel);
                final Instance jmsInst = Instance.get(jmsOid);
                if (jmsInst.isValid()) {
                    final Insert insert = new Insert(_messageType);
                    insert.add(CIPOS.MessageAbstract.Content, writer.toString());
                    insert.add(CIPOS.MessageAbstract.JmsLink, jmsInst.getId());
                    insert.execute();
                }
            }

        } catch (final JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
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

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return Return
     * @throws EFapsException on error
     */
    public Return updateTrigger(final Parameter _parameter)
        throws EFapsException
    {
        enque4SendMsg(_parameter, _parameter.getInstance(), CIPOS.MessageCategoryUpdate);
        return new Return();
    }

    public Return insertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        enque4SendMsg(_parameter, _parameter.getInstance(), CIPOS.MessageCategoryInsert);
        return new Return();
    }

}
