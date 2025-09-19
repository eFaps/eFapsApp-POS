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
import org.efaps.pos.dto.ReceiptDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("a4b4a8af-2349-497b-af84-32d3b4d2dd57")
@EFapsApplication("eFapsApp-POS")
public abstract class Receipt_Base
    extends AbstractDocument
{

    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Receipt.class);

    @Override
    protected CIType getDocumentType()
    {
        return CISales.Receipt;
    }

    @Override
    protected CIType getPositionType()
    {
        return CISales.ReceiptPosition;
    }

    @Override
    protected CIType getEmployee2DocumentType()
    {
        return CISales.Employee2Receipt;
    }

    @Override
    protected CIType getDepartment2DocumentType()
    {
        return  CISales.HumanResource_Department2Receipt;
    }

    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    public Response addReceipt(final String _identifier, final ReceiptDto receiptDto)
        throws EFapsException
    {
        checkAccess(_identifier);
        LOG.debug("Recieved: {}", receiptDto);
        final ReceiptDto dto;
        if (receiptDto.getOid() == null) {
            final Instance docInst = createDocument(Status.find(CISales.ReceiptStatus.Paid), receiptDto);
            createPositions(docInst, receiptDto);
            addPayments(docInst, receiptDto);
            createTransactions(receiptDto, docInst);
            afterCreate(docInst, receiptDto);
            dto = ReceiptDto.builder()
                            .withId(receiptDto.getId())
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

    public Response getReceipt(final String identifier,
                               final String oid)
        throws EFapsException
    {
        checkAccess(identifier);
        final Response ret;
        final var recieptInstance = Instance.get(oid);
        if (InstanceUtils.isType(recieptInstance, CISales.Receipt)) {
            final var dto = toDto(ReceiptDto.builder(), recieptInstance);
            ret = Response.ok()
                            .entity(dto)
                            .build();
        } else {
            LOG.warn("Recieved invalid GET request for receipt oid: {}", oid);
            ret = Response.status(Response.Status.PRECONDITION_FAILED)
                            .build();
        }
        return ret;
    }


    public Response retrieveReceipts(final String identifier,
                                     final String number)
        throws EFapsException
    {
        checkAccess(identifier);
        final Response ret;
        final var eval = EQL.builder().print().query(CISales.Receipt)
                        .where()
                        .attribute(CISales.Receipt.Name).eq(number)
                        .select().oid()
                        .evaluate();
        final List<AbstractDocumentDto> dtos = new ArrayList<>();
        while (eval.next()) {
            dtos.add(toDto(ReceiptDto.builder(), eval.inst()));
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
