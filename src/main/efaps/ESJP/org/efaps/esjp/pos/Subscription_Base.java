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
import java.math.BigDecimal;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.pos.jaxb.TaxCategoryInfo;
import org.efaps.esjp.pos.jaxb.TaxInfo;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("6e42ce3b-eb4f-4ad1-b2e3-aeb4fa2b0f1c")
@EFapsRevision("$Rev$")
public abstract class Subscription_Base
{

    public Return enqueueAll(final Parameter _parameter)
        throws EFapsException
    {
        enqueueTaxcategories(_parameter);
        enqueueTax(_parameter);
        return new Return();
    }

    protected void enqueueTaxcategories(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.TaxCategory);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CISales.TaxCategory.Name, CISales.TaxCategory.UUID);
        multi.execute();
        while (multi.next()) {
            final TaxCategoryInfo taxcat = new TaxCategoryInfo();
            taxcat.setName(multi.<String>getAttribute(CISales.TaxCategory.Name));
            taxcat.setUuid(multi.<String>getAttribute(CISales.TaxCategory.UUID));
            enqueObject(_parameter, _parameter.getInstance(), taxcat, CIPOS.MessageEnqueueAll);
        }
    }

    protected void enqueueTax(final Parameter _parameter)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CISales.Tax);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder sel = new SelectBuilder().linkto(CISales.Tax.TaxCategory).attribute(
                        CISales.TaxCategory.UUID);
        multi.addAttribute(CISales.Tax.Name, CISales.Tax.UUID, CISales.Tax.Numerator, CISales.Tax.Denominator,
                        CISales.Tax.ValidFrom);
        multi.addSelect(sel);
        multi.execute();
        while (multi.next()) {
            final TaxInfo tax = new TaxInfo();
            tax.setName(multi.<String>getAttribute(CISales.Tax.Name));
            tax.setUuid(multi.<String>getAttribute(CISales.Tax.UUID));
            tax.setTaxCategoryUUID(multi.<String>getSelect(sel));
            final Integer numerator = multi.<Integer>getAttribute(CISales.Tax.Numerator);
            final Integer denominator = multi.<Integer>getAttribute(CISales.Tax.Denominator);
            tax.setRate(new BigDecimal(numerator).setScale(8)
                            .divide(new BigDecimal(denominator), BigDecimal.ROUND_HALF_UP)
                            .doubleValue());
            tax.setValidFrom(multi.<DateTime>getAttribute(CISales.Tax.ValidFrom).toDate());
            enqueObject(_parameter, _parameter.getInstance(), tax, CIPOS.MessageEnqueueAll);
        }
    }

    protected void enqueObject(final Parameter _parameter,
                               final Instance _jmsInst,
                               final Object _object,
                               final CIType _messageType)
        throws EFapsException
    {
        try {
            final JAXBContext jc = JAXBContext.newInstance(_object.getClass());
            final Marshaller marschaller = jc.createMarshaller();
            final StringWriter writer = new StringWriter();
            marschaller.marshal(_object, writer);
            final Insert insert = new Insert(_messageType);
            insert.add(CIPOS.MessageAbstract.Content, writer.toString());
            insert.add(CIPOS.MessageAbstract.JmsLink, _jmsInst.getId());
            insert.execute();

        } catch (final JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}
