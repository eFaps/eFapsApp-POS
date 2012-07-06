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

package org.efaps.esjp.pos.jms;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.TextMessage;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAdminCommon;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.jms.JmsHandler;
import org.efaps.jms.JmsHandler.JmsDefinition;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("b052d33e-4bc2-4ed8-92ec-fe7bbdfce2f7")
@EFapsRevision("$Rev$")
public abstract class Publisher_Base
{

    public Return publish(final Parameter _parameter)
        throws EFapsException
    {

        try {
            final Instance subInst = _parameter.getInstance();
            final PrintQuery print = new PrintQuery(subInst);
            final SelectBuilder sel = new SelectBuilder().linkto(CIPOS.SubscriptionAbstract.JmsLink).oid();
            final SelectBuilder selJmsName = new SelectBuilder().linkto(CIPOS.SubscriptionAbstract.JmsLink)
                            .attribute(CIAdminCommon.JmsAbstract.Name);

            print.addSelect(sel, selJmsName);
            print.execute();
            final String jmsOid = print.<String>getSelect(sel);
            final String jmsName = print.<String>getSelect(selJmsName);
            final Instance jmsInst = Instance.get(jmsOid);

            final JmsDefinition def = JmsHandler.getJmsDefinition(jmsName);
            final MessageProducer producer = def.getMessageProducer();

            final QueryBuilder queryBldr = new QueryBuilder(CIPOS.MessageAbstract);
            queryBldr.addWhereAttrEqValue(CIPOS.MessageAbstract.JmsLink, jmsInst.getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.addAttribute(CIPOS.MessageAbstract.Content);
            multi.execute();
            while (multi.next()) {
                final TextMessage msg = def.getSession().createTextMessage();
                msg.setText(multi.<String>getAttribute(CIPOS.MessageAbstract.Content));
                producer.send(msg);
            }
            def.getSession().commit();
        } catch (final JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return new Return();
    }
}
