/*
 * Copyright 2003 - 2023 The eFaps Team
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

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.datamodel.AttributeSet;
import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.eql.EQL;
import org.efaps.eql2.StmtFlag;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.pos.dto.ContactDto;
import org.efaps.pos.dto.IdentificationType;
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

    @SuppressWarnings("unchecked")
    public Response getContacts(final String _identifier,
                                final int limit,
                                final int offset,
                                final OffsetDateTime after)
        throws EFapsException
    {
        checkAccess(_identifier);
        final List<ContactDto> contacts = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIContacts.Contact);
        queryBldr.addWhereClassification((Classification) CIContacts.ClassClient.getType());
        queryBldr.addWhereAttrEqValue(CIContacts.Contact.Status, Status.find(CIContacts.ContactStatus.Active));
        queryBldr.addOrderByAttributeAsc(CIContacts.Contact.ID);
        if (limit > 0) {
            queryBldr.setLimit(limit);
        }
        if (offset > 0) {
            queryBldr.setOffset(offset);
        }
        if (after != null) {
            queryBldr.addWhereAttrGreaterValue(CIContacts.Contact.Modified, after);
        }
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selTaxNumber = SelectBuilder.get()
                        .clazz(CIContacts.ClassOrganisation)
                        .attribute(CIContacts.ClassOrganisation.TaxNumber);
        final SelectBuilder selIdentityCard = SelectBuilder.get()
                        .clazz(CIContacts.ClassPerson)
                        .attribute(CIContacts.ClassPerson.IdentityCard);
        final SelectBuilder selDOIType = SelectBuilder.get()
                        .clazz(CIContacts.ClassPerson)
                        .linkto(CIContacts.ClassPerson.DOITypeLink)
                        .attribute(CIContacts.AttributeDefinitionDOIType.Value);

        final SelectBuilder selEmails = SelectBuilder.get().clazz(CIContacts.Class)
                        .attributeset(CIContacts.Class.EmailSet, "attribute[ElectronicBilling]==true")
                        .attribute("Email");
        if (Pos.CONTACT_ACIVATEEMAIL.get()) {
            multi.addSelect(selEmails);
        }
        multi.addAttribute(CIContacts.Contact.Name);
        multi.addSelect(selTaxNumber, selIdentityCard, selDOIType);
        multi.execute();
        while (multi.next()) {
            String idNumber = multi.getSelect(selTaxNumber);
            IdentificationType idType;
            if (StringUtils.isNotBlank(idNumber)) {
                idType = IdentificationType.RUC;
            } else {
                idNumber = multi.getSelect(selIdentityCard);
                final String doiType = multi.getSelect(selDOIType);
                if (StringUtils.isBlank(doiType)) {
                    idType = IdentificationType.OTHER;
                } else {
                    switch (doiType) {
                        case "01":
                            idType = IdentificationType.DNI;
                            break;
                        case "02":
                            idType = IdentificationType.PASSPORT;
                            break;
                        case "04":
                            idType = IdentificationType.CE;
                            break;
                        default:
                            idType = IdentificationType.OTHER;
                            break;
                    }
                }
            }
            String email = null;
            if (Pos.CONTACT_ACIVATEEMAIL.get()) {
                final Object obj = multi.getSelect(selEmails);
                if (obj instanceof List) {
                    email = ((List<String>) obj).get(0);
                } else if (obj != null) {
                    email = (String) obj;
                }
            }

            contacts.add(ContactDto.builder()
                            .withOID(multi.getCurrentInstance().getOid())
                            .withName(multi.getAttribute(CIContacts.Contact.Name))
                            .withIdType(idType)
                            .withIdNumber(idNumber)
                            .withEmail(email)
                            .build());
        }
        final Response ret = Response.ok()
                        .entity(contacts)
                        .build();
        return ret;
    }

    public Response getContact(final String _identifier,
                               final String oid)
        throws EFapsException
    {
        checkAccess(_identifier);
        Response ret = null;
        final var contactInstance = Instance.get(oid);
        if (InstanceUtils.isType(contactInstance, CIContacts.Contact)) {
            final var eval = EQL.builder()
                            .with(StmtFlag.TRIGGEROFF)
                            .print(contactInstance)
                            .attribute(CIContacts.Contact.Name)
                            .clazz(CIContacts.ClassOrganisation).attribute(CIContacts.ClassOrganisation.TaxNumber)
                                .as("taxNumber")
                            .clazz(CIContacts.ClassPerson).attribute(CIContacts.ClassPerson.IdentityCard)
                                .as("identityCard")
                            .clazz(CIContacts.ClassPerson).linkto(CIContacts.ClassPerson.DOITypeLink)
                                .attribute(CIContacts.AttributeDefinitionDOIType.Value).as("doiType")
                            .evaluate();
            if (eval.next()) {
                String idNumber = eval.get("taxNumber");
                IdentificationType idType;
                if (StringUtils.isNotBlank(idNumber)) {
                    idType = IdentificationType.RUC;
                } else {
                    idNumber = eval.get("identityCard");
                    final String doiType = eval.get("doiType");
                    if (StringUtils.isBlank(doiType)) {
                        idType = IdentificationType.OTHER;
                    } else {
                        switch (doiType) {
                            case "01":
                                idType = IdentificationType.DNI;
                                break;
                            case "02":
                                idType = IdentificationType.PASSPORT;
                                break;
                            case "04":
                                idType = IdentificationType.CE;
                                break;
                            default:
                                idType = IdentificationType.OTHER;
                                break;
                        }
                    }
                }
                ret = Response.ok().entity(ContactDto.builder()
                                .withOID(eval.inst().getOid())
                                .withName(eval.get(CIContacts.Contact.Name))
                                .withIdType(idType)
                                .withIdNumber(idNumber)
                                .build())
                                .build();
            } else {
                ret = Response.status(javax.ws.rs.core.Response.Status.NOT_FOUND).build();
            }
        } else {
            ret = Response.status(javax.ws.rs.core.Response.Status.BAD_REQUEST).build();
        }
        return ret;
    }

    public Response addContact(final String _identifier,
                               final ContactDto _contactDto)
        throws EFapsException
    {
        LOG.debug("Recieved: {}", _contactDto);
        checkAccess(_identifier);
        final ContactDto dto;
        if (_contactDto.getOid() == null) {
            final Insert insert = new Insert(CIContacts.Contact);
            insert.add(CIContacts.Contact.Name, _contactDto.getName());
            insert.add(CIContacts.Contact.Status, Status.find(CIContacts.ContactStatus.Active));
            insert.execute();

            final Instance contactInst = insert.getInstance();

            if (IdentificationType.RUC.equals(_contactDto.getIdType())) {
                final Classification classification = (Classification) CIContacts.ClassOrganisation.getType();
                final Insert relInsert = new Insert(classification.getClassifyRelationType());
                relInsert.add(classification.getRelLinkAttributeName(), contactInst);
                relInsert.add(classification.getRelTypeAttributeName(), classification.getId());
                relInsert.execute();

                final Insert classInsert = new Insert(classification);
                classInsert.add(classification.getLinkAttributeName(), contactInst);
                classInsert.add(CIContacts.ClassOrganisation.TaxNumber, _contactDto.getIdNumber());
                classInsert.execute();
            } else {
                final Classification classification = (Classification) CIContacts.ClassPerson.getType();
                final Insert relInsert = new Insert(classification.getClassifyRelationType());
                relInsert.add(classification.getRelLinkAttributeName(), contactInst);
                relInsert.add(classification.getRelTypeAttributeName(), classification.getId());
                relInsert.execute();

                final Insert classInsert = new Insert(classification);
                classInsert.add(classification.getLinkAttributeName(), contactInst);
                classInsert.add(CIContacts.ClassPerson.IdentityCard, _contactDto.getIdNumber());
                classInsert.add(CIContacts.ClassPerson.Forename, " ");
                classInsert.add(CIContacts.ClassPerson.FirstLastName, " ");

                String doiType;
                switch (_contactDto.getIdType()) {
                    case DNI:
                        doiType = "01";
                        break;
                    case PASSPORT:
                        doiType = "02";
                        break;
                    case CE:
                        doiType = "04";
                        break;
                    default:
                        doiType = null;
                }
                if (doiType != null) {
                    final QueryBuilder queryBldr = new QueryBuilder(CIContacts.AttributeDefinitionDOIType);
                    queryBldr.addWhereAttrEqValue(CIContacts.AttributeDefinitionDOIType.Value, doiType);
                    final List<Instance> result = queryBldr.getQuery().execute();
                    if (!result.isEmpty()) {
                        classInsert.add(CIContacts.ClassPerson.DOITypeLink, result.get(0));
                    }
                }
                classInsert.execute();
            }
            final Classification clientClass = (Classification) CIContacts.ClassClient.getType();
            final Insert relClientInsert = new Insert(clientClass.getClassifyRelationType());
            relClientInsert.add(clientClass.getRelLinkAttributeName(), contactInst);
            relClientInsert.add(clientClass.getRelTypeAttributeName(), clientClass.getId());
            relClientInsert.execute();

            final Insert clientClassInsert = new Insert(clientClass);
            clientClassInsert.add(clientClass.getLinkAttributeName(), contactInst);
            clientClassInsert.execute();

            if (Pos.CONTACT_ACIVATEEMAIL.get() && StringUtils.isNotEmpty(_contactDto.getEmail())) {
                final Classification classification = (Classification) CIContacts.Class.getType();
                final Insert relInsert = new Insert(classification.getClassifyRelationType());
                relInsert.add(classification.getRelLinkAttributeName(), contactInst);
                relInsert.add(classification.getRelTypeAttributeName(), classification.getId());
                relInsert.execute();

                final Insert classInsert = new Insert(classification);
                classInsert.add(classification.getLinkAttributeName(), contactInst);
                classInsert.execute();

                final var attrSet = AttributeSet.find(CIContacts.Class.getType().getName(),
                                CIContacts.Class.EmailSet.name);
                final Insert attrSetInsert = new Insert(attrSet);
                attrSetInsert.add(attrSet.getAttribute(CIContacts.Class.EmailSet.name), classInsert.getId());
                attrSetInsert.add(attrSet.getAttribute("Email"), _contactDto.getEmail());
                attrSetInsert.add(attrSet.getAttribute("ElectronicBilling"), true);
                attrSetInsert.execute();
            }

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
