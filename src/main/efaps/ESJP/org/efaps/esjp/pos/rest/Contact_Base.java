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
package org.efaps.esjp.pos.rest;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.datamodel.AttributeSet;
import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.QueryBuilder;
import org.efaps.db.stmt.selection.Evaluator;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Print;
import org.efaps.eql.builder.Selectables;
import org.efaps.eql.builder.Where;
import org.efaps.eql2.StmtFlag;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.pos.rest.dto.DumpDto;
import org.efaps.esjp.pos.util.DumpUtils;
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

    public Response getContacts(final String _identifier,
                                final int limit,
                                final int offset,
                                final OffsetDateTime afterParameter,
                                final String doiNumber)
        throws EFapsException
    {
        checkAccess(_identifier, ACCESSROLE.BE, ACCESSROLE.MOBILE);

        final List<ContactDto> dtos = new ArrayList<>();
        final var query = EQL.builder()
                        .with(StmtFlag.TRIGGEROFF)
                        .print().query(CIContacts.Contact);

        Where where = null;
        if (afterParameter != null) {
            where = query.where();
            query.where().attribute(CIContacts.Contact.Modified).greater(afterParameter.toString());
        }

        if (StringUtils.isNotEmpty(doiNumber)) {
            if (where == null) {
                where = query.where();
            }

            if (doiNumber.length() == 11) {
                where.attribute(CIContacts.Contact.ID).in(
                                EQL.builder()
                                                .nestedQuery(CIContacts.ClassOrganisation)
                                                .where()
                                                .attribute(CIContacts.ClassOrganisation.TaxNumber).eq(doiNumber)
                                                .up()
                                                .selectable(Selectables.attribute(
                                                                CIContacts.ClassOrganisation.ContactLink)));

            } else {
                where.attribute(CIContacts.Contact.ID).in(
                                EQL.builder()
                                                .nestedQuery(CIContacts.ClassPerson)
                                                .where()
                                                .attribute(CIContacts.ClassPerson.IdentityCard).eq(doiNumber)
                                                .up()
                                                .selectable(Selectables.attribute(
                                                                CIContacts.ClassPerson.ContactLink)));
            }
        }

        final var print = query.select()
                        .orderBy(CIContacts.Contact.ID);
        if (limit > 0) {
            print.limit(limit);
        }
        if (offset > 0) {
            print.offset(offset);
        }

        select(print);

        final var eval = print.evaluate();
        while (eval.next()) {
            dtos.add(toDto(eval));
        }
        final Response ret = Response.ok()
                        .entity(dtos)
                        .build();
        return ret;
    }

    public Response getContact(final String identifier,
                               final String oid)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE, ACCESSROLE.MOBILE);
        Response ret = null;
        final var contactInstance = Instance.get(oid);
        if (InstanceUtils.isType(contactInstance, CIContacts.Contact)) {
            final var dto = toDto(contactInstance);
            if (dto != null) {
                ret = Response.ok()
                                .entity(dto)
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
                               final ContactDto contactDto)
        throws EFapsException
    {
        LOG.debug("Recieved: {}", contactDto);
        checkAccess(_identifier, ACCESSROLE.BE, ACCESSROLE.MOBILE);
        final ContactDto dto;
        if (contactDto.getOid() == null) {
            final var contactInstance = createContactInstance(contactDto);
            dto = ContactDto.builder()
                            .withOID(contactInstance.getOid())
                            .build();
        } else {
            dto = ContactDto.builder().build();
        }
        final Response ret = Response.ok()
                        .entity(dto)
                        .build();
        return ret;
    }

    public Response updateContact(final String identifier,
                                  final String oid,
                                  final ContactDto contactDto)
        throws EFapsException
    {
        LOG.debug("Recieved update contact for : {}", contactDto);
        checkAccess(identifier, ACCESSROLE.BE, ACCESSROLE.MOBILE);
        if (contactDto.getOid() != null && contactDto.getOid().equals(oid)) {
            EQL.builder().update(oid)
                            .set(CIContacts.Contact.Name, contactDto.getName())
                            .set(CIContacts.Contact.Status, CIContacts.ContactStatus.Active)
                            .execute();

            if (Pos.CONTACT_ACTIVATEEMAIL.get() && StringUtils.isNotEmpty(contactDto.getEmail())) {
                final var contactInst = Instance.get(oid);
                final var classification = (Classification) CIContacts.Class.getType();
                final var eval = EQL.builder().print().query(CIContacts.Class)
                                .where()
                                .attribute(classification.getLinkAttributeName()).eq(contactInst)
                                .select()
                                .instance()
                                .evaluate();
                Instance classInst;
                final var attrSet = AttributeSet.find(CIContacts.Class.getType().getName(),
                                CIContacts.Class.EmailSet.name);
                if (eval.next()) {
                    classInst = eval.inst();

                    final var emailEval = EQL.builder().print().query(attrSet.getUUID().toString())
                                    .where()
                                    .attribute(attrSet.getAttribute(CIContacts.Class.EmailSet.name).getName())
                                    .eq(classInst)
                                    .select()
                                    .instance()
                                    .evaluate();
                    while (emailEval.next()) {
                        EQL.builder().delete(emailEval.inst()).stmt().execute();
                    }
                } else {
                    EQL.builder().insert(classification.getClassifyRelationType())
                                    .set(classification.getRelLinkAttributeName(), String.valueOf(contactInst.getId()))
                                    .set(classification.getRelTypeAttributeName(),
                                                    String.valueOf(classification.getId()))
                                    .execute();

                    classInst = EQL.builder().insert(classification)
                                    .set(classification.getLinkAttributeName(), String.valueOf(contactInst.getId()))
                                    .execute();
                }
                EQL.builder().insert(attrSet)
                                .set(CIContacts.Class.EmailSet, classInst.getId())
                                .set("Email", contactDto.getEmail())
                                .set("ElectronicBilling", "true")
                                .execute();
            }
        }

        final Response ret = Response.ok()
                        .build();
        return ret;
    }

    public Instance createContactInstance(final ContactDto contactDto)
        throws EFapsException
    {
        final Insert insert = new Insert(CIContacts.Contact);
        insert.add(CIContacts.Contact.Name, contactDto.getName());
        insert.add(CIContacts.Contact.Status, Status.find(CIContacts.ContactStatus.Active));
        insert.execute();

        final Instance contactInst = insert.getInstance();

        if (IdentificationType.RUC.equals(contactDto.getIdType())) {
            final Classification classification = (Classification) CIContacts.ClassOrganisation.getType();
            final Insert relInsert = new Insert(classification.getClassifyRelationType());
            relInsert.add(classification.getRelLinkAttributeName(), contactInst);
            relInsert.add(classification.getRelTypeAttributeName(), classification.getId());
            relInsert.execute();

            final Insert classInsert = new Insert(classification);
            classInsert.add(classification.getLinkAttributeName(), contactInst);
            classInsert.add(CIContacts.ClassOrganisation.TaxNumber, contactDto.getIdNumber());
            classInsert.execute();
        } else {
            final Classification classification = (Classification) CIContacts.ClassPerson.getType();
            final Insert relInsert = new Insert(classification.getClassifyRelationType());
            relInsert.add(classification.getRelLinkAttributeName(), contactInst);
            relInsert.add(classification.getRelTypeAttributeName(), classification.getId());
            relInsert.execute();

            final Insert classInsert = new Insert(classification);
            classInsert.add(classification.getLinkAttributeName(), contactInst);
            classInsert.add(CIContacts.ClassPerson.IdentityCard, contactDto.getIdNumber());
            classInsert.add(CIContacts.ClassPerson.Forename, contactDto.getForename());
            classInsert.add(CIContacts.ClassPerson.FirstLastName, contactDto.getFirstLastName());
            classInsert.add(CIContacts.ClassPerson.SecondLastName, contactDto.getSecondLastName());

            final String doiType = switch (contactDto.getIdType()) {
                case DNI -> "01";
                case PASSPORT -> "07";
                case CE -> "04";
                case OTHER -> "00";
                default -> null;
            };
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

        if (Pos.CONTACT_ACTIVATEEMAIL.get() && StringUtils.isNotEmpty(contactDto.getEmail())) {
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
            attrSetInsert.add(attrSet.getAttribute("Email"), contactDto.getEmail());
            attrSetInsert.add(attrSet.getAttribute("ElectronicBilling"), true);
            attrSetInsert.execute();
        }
        return insert.getInstance();
    }

    public ContactDto toDto(final Instance contactInstance)
        throws EFapsException
    {
        ContactDto ret = null;

        final var print = EQL.builder()
                        .with(StmtFlag.TRIGGEROFF)
                        .print(contactInstance);

        select(print);

        final var eval = print.evaluate();

        if (eval.next()) {
            ret = toDto(eval);
        }
        return ret;
    }

    public ContactDto toDto(Evaluator eval)
        throws EFapsException
    {
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
                idType = switch (doiType) {
                    case "01" -> IdentificationType.DNI;
                    case "07" -> IdentificationType.PASSPORT;
                    case "04" -> IdentificationType.CE;
                    default -> IdentificationType.OTHER;
                };
            }
        }
        String email = null;
        if (Pos.CONTACT_ACTIVATEEMAIL.get()) {
            final Object emailObj = eval.get("email");
            final Object electronicBillingObj = eval.get("electronicBilling");
            if (emailObj instanceof final Collection emails) {
                @SuppressWarnings("unchecked") final var electronicBillings = ((Collection<Boolean>) electronicBillingObj)
                                .iterator();
                for (final var emailStr : emails) {
                    if (BooleanUtils.isTrue(electronicBillings.next())) {
                        email = (String) emailStr;
                    }
                }
                // in case there is no with the flag electronicBilling
                if (email == null) {
                    email = (String) emails.iterator().next();
                }
            } else if (emailObj != null) {
                email = (String) emailObj;
            }
        }
        return ContactDto.builder()
                        .withOID(eval.inst().getOid())
                        .withName(eval.get(CIContacts.Contact.Name))
                        .withIdType(idType)
                        .withIdNumber(idNumber)
                        .withForename(eval.get("forename"))
                        .withFirstLastName(eval.get("firstLastName"))
                        .withSecondLastName(eval.get("secondLastName"))
                        .withEmail(email)
                        .build();
    }

    public List<Object> getContacts(int limit,
                                    int offset)
        throws EFapsException
    {
        final List<Object> ret = new ArrayList<>();
        final var print = EQL.builder()
                        .with(StmtFlag.TRIGGEROFF)
                        .print().query(CIContacts.Contact)
                        .select()
                        .limit(limit).offset(offset)
                        .orderBy(CIContacts.Contact.ID);

        select(print);

        final var eval = print.evaluate();
        while (eval.next()) {
            ret.add(toDto(eval));
        }
        return ret;
    }

    protected void select(Print print)
    {
        print.attribute(CIContacts.Contact.ID, CIContacts.Contact.Name)
                        .clazz(CIContacts.ClassOrganisation).attribute(CIContacts.ClassOrganisation.TaxNumber)
                        .as("taxNumber")
                        .clazz(CIContacts.ClassPerson).attribute(CIContacts.ClassPerson.IdentityCard)
                        .as("identityCard")
                        .clazz(CIContacts.ClassPerson).linkto(CIContacts.ClassPerson.DOITypeLink)
                        .attribute(CIContacts.AttributeDefinitionDOIType.Value).as("doiType")
                        .clazz(CIContacts.ClassPerson).attribute(CIContacts.ClassPerson.Forename)
                        .as("forename")
                        .clazz(CIContacts.ClassPerson).attribute(CIContacts.ClassPerson.FirstLastName)
                        .as("firstLastName")
                        .clazz(CIContacts.ClassPerson).attribute(CIContacts.ClassPerson.SecondLastName)
                        .as("secondLastName")
                        .clazz(CIContacts.Class).attributeSet(CIContacts.Class.EmailSet).attribute("Email").as("email")
                        .clazz(CIContacts.Class).attributeSet(CIContacts.Class.EmailSet).attribute("ElectronicBilling")
                        .as("electronicBilling");
    }

    public Return prepareContactsDump(final Parameter parameter)
        throws EFapsException
    {
        LOG.info("Preparing dump for Contacts");

        new DumpUtils()
        {

            @Override
            protected List<Object> getObjects(int limit,
                                              int offset)
                throws EFapsException
            {
                return getContacts(limit, offset);
            }

            @Override
            protected int getLimit()
            {
                return 2500;
            }

        }.prepareDump(CIPOS.ContactsDump);

        return new Return();
    }

    public Response getContactsDump(@PathParam("identifier") final String _identifier)
        throws EFapsException
    {
        Response ret = null;
        if (Pos.CONTACT_DUMP_ACTIVATE.get()) {
            final var eval = EQL.builder().print()
                            .query(CIPOS.ContactsDump)
                            .select()
                            .attribute(CIPOS.ContactsDump.UpdatedAt)
                            .evaluate();
            if (eval.next()) {
                ret = Response.ok().entity(
                                DumpDto.builder()
                                                .withOid(eval.inst().getOid())
                                                .withUpdateAt(eval.get(CIPOS.ContactsDump.UpdatedAt))
                                                .build())
                                .build();
            }
        }
        return ret == null ? Response.noContent().build() : ret;
    }

}
