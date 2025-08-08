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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.pos.dto.AbstractDocumentDto;
import org.efaps.pos.dto.CreditNoteDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("9a514a7d-5e78-4b81-97a0-224775d23a61")
@EFapsApplication("eFapsApp-POS")
public abstract class CreditNote_Base
    extends AbstractDocument
{

    private static final Logger LOG = LoggerFactory.getLogger(CreditNote.class);

    @Override
    protected CIType getDocumentType()
    {
        return CISales.CreditNote;
    }

    @Override
    protected CIType getPositionType()
    {
        return CISales.CreditNotePosition;
    }

    @Override
    protected CIType getEmployee2DocumentType()
    {
        return CISales.Employee2CreditNote;
    }

    @Override
    protected CIType getDepartment2DocumentType()
    {
        return CISales.HumanResource_Department2CreditNote;
    }

    protected Response addCreditNote(final String _identifier,
                                     final CreditNoteDto _creditNoteDto)
        throws EFapsException
    {
        checkAccess(_identifier);
        LOG.debug("Recieved: {}", _creditNoteDto);
        final CreditNoteDto dto;
        if (_creditNoteDto.getOid() == null) {
            final Instance docInst = createDocument(Status.find(CISales.CreditNoteStatus.Paid), _creditNoteDto);
            createPositions(docInst, _creditNoteDto);
            addPayments(docInst, _creditNoteDto);
            // connect CreditNote and source document
            final var sourceDocInst = Instance.get(_creditNoteDto.getSourceDocOid());
            final Insert insert = new Insert(
                            InstanceUtils.isKindOf(sourceDocInst, CISales.Invoice) ? CISales.CreditNote2Invoice
                                            : CISales.CreditNote2Receipt);
            insert.add(CISales.Document2DocumentAbstract.FromAbstractLink, docInst);
            insert.add(CISales.Document2DocumentAbstract.ToAbstractLink, sourceDocInst);
            insert.executeWithoutAccessCheck();

            createTransactions(_creditNoteDto, docInst);
            afterCreate(docInst);
            // createTransactionDocument(_creditNoteDto, docInst);
            dto = CreditNoteDto.builder()
                            .withId(_creditNoteDto.getId())
                            .withOID(docInst.getOid())
                            .build();
        } else {
            dto = CreditNoteDto.builder().build();
        }
        final Response ret = Response.ok()
                        .entity(dto)
                        .build();
        return ret;
    }

    public Response getCreditNote(final String identifier,
                                  final String oid)
        throws EFapsException
    {
        checkAccess(identifier);
        final Response ret;
        final var instance = Instance.get(oid);
        if (InstanceUtils.isType(instance, CISales.CreditNote)) {
            final var dto = toDto(CreditNoteDto.builder(), instance);
            ret = Response.ok()
                            .entity(dto)
                            .build();
        } else {
            LOG.warn("Recieved invalid GET request for creditNote oid: {}", oid);
            ret = Response.status(Response.Status.PRECONDITION_FAILED)
                            .build();
        }
        return ret;
    }

    public Response retrieveCreditNotes(final String identifier,
                                        final String number)
        throws EFapsException
    {
        checkAccess(identifier);
        final Response ret;
        final var eval = EQL.builder().print().query(CISales.CreditNote)
                        .where()
                        .attribute(CISales.CreditNote.Name).eq(number)
                        .select().oid()
                        .evaluate();
        final List<AbstractDocumentDto> dtos = new ArrayList<>();
        while (eval.next()) {
            dtos.add(toDto(CreditNoteDto.builder(), eval.inst()));
        }
        if (dtos.isEmpty()) {
            ret = Response.status(Response.Status.NOT_FOUND)
                            .build();
        } else {
            ret = Response.ok()
                            .entity(dtos)
                            .build();
        }
        return ret;
    }

}
