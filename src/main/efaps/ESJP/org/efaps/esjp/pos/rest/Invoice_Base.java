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
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.pos.dto.AbstractDocumentDto;
import org.efaps.pos.dto.InvoiceDto;
import org.efaps.pos.dto.ReceiptDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("7f36fc98-6a5f-40ab-9a29-4735a921def9")
@EFapsApplication("eFapsApp-POS")
public abstract class Invoice_Base
    extends AbstractDocument
{

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Receipt.class);

    @Override
    protected CIType getDocumentType()
    {
        return CISales.Invoice;
    }

    @Override
    protected CIType getPositionType()
    {
        return CISales.InvoicePosition;
    }

    @Override
    protected CIType getEmployee2DocumentType()
    {
        return CISales.Employee2Invoice;
    }

    @Override
    protected CIType getDepartment2DocumentType()
    {
        return CISales.HumanResource_Department2Invoice;
    }

    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    public Response addInvoice(final String _identifier,
                               final InvoiceDto _invoiceDto)
        throws EFapsException
    {
        checkAccess(_identifier);
        LOG.debug("Recieved: {}", _invoiceDto);
        final ReceiptDto dto;
        if (_invoiceDto.getOid() == null) {
            final Instance docInst = createDocument(Status.find(CISales.InvoiceStatus.Paid), _invoiceDto);
            createPositions(docInst, _invoiceDto);
            addPayments(docInst, _invoiceDto);
            createTransactions(_invoiceDto, docInst);
            afterCreate(docInst);
            dto = ReceiptDto.builder()
                            .withId(_invoiceDto.getId())
                            .withOID(docInst.getOid())
                            .build();
        } else {
            dto = ReceiptDto.builder().build();
        }
        final Response ret = Response.ok()
                        .entity(dto)
                        .build();
        return ret;
    }

    public Response getInvoice(final String identifier,
                               final String oid)
        throws EFapsException
    {
        checkAccess(identifier);
        final Response ret;
        final var invoiceInstance = Instance.get(oid);
        if (InstanceUtils.isType(invoiceInstance, CISales.Invoice)) {
            final var dto = toDto(InvoiceDto.builder(), invoiceInstance);
            ret = Response.ok()
                            .entity(dto)
                            .build();
        } else {
            LOG.warn("Recieved invalid get request for invoice oid: {}", oid);
            ret = Response.status(Response.Status.PRECONDITION_FAILED)
                            .build();
        }
        return ret;
    }

    public Response retrieveInvoices(final String identifier,
                                     final String number)
        throws EFapsException
    {
        checkAccess(identifier);
        final Response ret;
        final var eval = EQL.builder().print().query(CISales.Invoice)
                        .where()
                        .attribute(CISales.Invoice.Name).eq(number)
                        .select().oid()
                        .evaluate();
        final List<AbstractDocumentDto> dtos = new ArrayList<>();
        while (eval.next()) {
            dtos.add(toDto(InvoiceDto.builder(), eval.inst()));
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
