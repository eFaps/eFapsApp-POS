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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.pos.dto.ContactDto;
import org.efaps.util.EFapsException;

@EFapsUUID("d74ba98c-e90c-4704-8d59-0e78d58a9bdb")
@EFapsApplication("eFapsApp-POS")
public abstract class Contact_Base
{

    @Path("contacts")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response getContacts()
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
}
