/*
 * Copyright 2003 - 2018 The eFaps Team
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
package org.efaps.esjp.pos.rest;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.pos.dto.ContactDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("d74ba98c-e90c-4704-8d59-0e78d58a9bdb")
@EFapsApplication("eFapsApp-POS")
public abstract class Contact_Base
    extends AbstractRest
{
    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Contact.class);

    public Response getContacts(final String _identifier)
        throws EFapsException
    {
        final List<ContactDto> sequences = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIContacts.Contact);
        queryBldr.addWhereClassification((Classification) CIContacts.ClassClient.getType());
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selTaxNumber = SelectBuilder.get()
                        .clazz(CIContacts.ClassOrganisation)
                        .attribute(CIContacts.ClassOrganisation.TaxNumber);
        multi.addAttribute(CIContacts.Contact.Name);
        multi.addSelect(selTaxNumber);
        multi.execute();
        while(multi.next()) {
            sequences.add(ContactDto.builder()
                            .withOID(multi.getCurrentInstance().getOid())
                            .withName(multi.getAttribute(CIContacts.Contact.Name))
                            .withTaxNumber(multi.getSelect(selTaxNumber))
                            .build());
        }
        final Response ret = Response.ok()
                        .entity(sequences)
                        .build();
        return ret;
    }

    public Response addContact(final String _identifier, final ContactDto _contactDto)
        throws EFapsException
    {
        LOG.debug("Recieved: {}", _contactDto);
        final ContactDto dto;
        if (_contactDto.getOid() == null) {
            final Insert insert = new Insert(CIContacts.Contact);
            insert.add(CIContacts.Contact.Name, _contactDto.getName());
            insert.add(CIContacts.Contact.Status, Status.find(CIContacts.ContactStatus.Active));
            insert.execute();

            final Instance contactInst = insert.getInstance();

            final Classification classification = (Classification) CIContacts.ClassOrganisation.getType();
            final Insert relInsert = new Insert(classification.getClassifyRelationType());
            relInsert.add(classification.getRelLinkAttributeName(), contactInst);
            relInsert.add(classification.getRelTypeAttributeName(), classification.getId());
            relInsert.execute();

            final Insert classInsert = new Insert(classification);
            classInsert.add(classification.getLinkAttributeName(), contactInst);
            classInsert.add(CIContacts.ClassOrganisation.TaxNumber, _contactDto.getTaxNumber());
            classInsert.execute();

            final Classification clientClass = (Classification) CIContacts.ClassClient.getType();
            final Insert relClientInsert = new Insert(clientClass.getClassifyRelationType());
            relClientInsert.add(clientClass.getRelLinkAttributeName(), contactInst);
            relClientInsert.add(clientClass.getRelTypeAttributeName(), clientClass.getId());
            relClientInsert.execute();

            final Insert clientClassInsert = new Insert(clientClass);
            clientClassInsert.add(clientClass.getLinkAttributeName(), contactInst);
            clientClassInsert.execute();

            dto = ContactDto.builder()
                            .withOID(insert.getInstance().getOid())
                            .build();
        } else {
            dto = ContactDto.builder().build();
        }
        final Response ret = Response.ok()
                        .entity(dto)
                        .build();
        return ret;
    }
}
