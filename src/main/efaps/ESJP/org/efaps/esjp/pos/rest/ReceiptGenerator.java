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

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIStatus;
import org.efaps.ci.CIType;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CISales;
import org.efaps.pos.dto.GenerateDocDto;
import org.efaps.util.EFapsException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@EFapsUUID("e5bee587-ad44-4a97-a723-d3f3f1bc8a9c")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class ReceiptGenerator
    extends AbstractDocumentGenerator
{

    @Path("/{identifier}/receipts/generator")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public void createReceipt(@PathParam("identifier") final String identifier,
                              final GenerateDocDto dto)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.MOBILE);
        final var calculators = evalCalculators(identifier, dto);
        final var docInstance = createDocument(identifier, dto, calculators);
        createPositions(identifier, dto, calculators, docInstance);
    }

    @Override
    protected CIType getDocumentType()
    {
        return CISales.Receipt;
    }

    @Override
    protected CIStatus getDocumentStatus()
    {
        return CISales.ReceiptStatus.Open;
    }

    @Override
    protected CIType getPositionType()
    {
        return CISales.ReceiptPosition;
    }

    @Override
    protected CIAttribute getAttribute4SerialNumber()
    {
        return CIPOS.Backend.ReceiptSerial;
    }
}
