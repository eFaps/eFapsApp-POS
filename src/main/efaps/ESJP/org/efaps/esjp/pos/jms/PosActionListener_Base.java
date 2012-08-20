/*
 * Copyright 2003 - 2011 The eFaps Team
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
 * Revision:        $Rev: 7442 $
 * Last Changed:    $Date: 2012-03-29 00:45:04 -0500 (jue, 29 mar 2012) $
 * Last Changed By: $Author: jan@moxter.net $
 */

package org.efaps.esjp.pos.jms;

import java.io.StringReader;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.TextMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.jms.actions.IAction;
import org.efaps.esjp.jms.msg.listener.AbstractSecuredListener;
import org.efaps.esjp.jms.msg.listener.AbstractSecuredListener_Base;
import org.efaps.esjp.pos.documents.PosAccount;
import org.efaps.esjp.pos.documents.PosReceipt;
import org.efaps.esjp.pos.jaxb.JmsCloseCash;
import org.efaps.esjp.pos.jaxb.TicketInfo;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id: SecuredActionListener_Base.java 7442 2012-03-29 05:45:04Z jan@moxter.net $
 */
@EFapsUUID("67f30b76-cfb0-4481-bd07-13ba19f0c203")
@EFapsRevision("$Rev: 7442 $")
public abstract class PosActionListener_Base
    extends AbstractSecuredListener
{

    /**
     * {@inheritDoc}
     */
    @Override
    public String getUserName()
    {
        return "Administrator";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object onContextMessage(final Message _msg)
    {
        Object object = null;
        try {
            if (_msg instanceof TextMessage) {
                final TextMessage msg = (TextMessage) _msg;
                final String xml = msg.getText();
                AbstractSecuredListener_Base.LOG.debug("unmarshalling: {} ", xml);
                final JAXBContext jc = JAXBContext.newInstance(getClasses());
                final Unmarshaller unmarschaller = jc.createUnmarshaller();
                final Source source = new StreamSource(new StringReader(xml));
                object = unmarschaller.unmarshal(source);
            }
            if (object instanceof IAction) {
                final IAction action = (IAction) object;
                object = action.execute();
            }else if(object instanceof TicketInfo){
                new PosReceipt().createTicketInfo((TicketInfo) object);
            }else if(object instanceof JmsCloseCash){
                new PosAccount().cashDeskBalance((JmsCloseCash) object);
            }
        } catch (final JMSException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (final EFapsException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return object;
    }
}