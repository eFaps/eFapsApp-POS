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

import jakarta.ws.rs.core.Response;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.pos.util.Pos.PrinterType;
import org.efaps.pos.dto.PrinterDto;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("eb597164-1d07-4d43-8f81-006c3035cd58")
@EFapsApplication("eFapsApp-POS")
public abstract class Printer_Base
    extends AbstractRest
{
    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    public Response getPrinters(final String _identifier)
        throws EFapsException
    {
        checkAccess(_identifier);
        final List<PrinterDto> printers = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Printer);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPOS.Printer.Name, CIPOS.Printer.PrinterType);
        multi.execute();
        while (multi.next()) {
            final PrinterType printerType = multi.getAttribute(CIPOS.Printer.PrinterType);
            printers.add(PrinterDto.builder()
                .withOID(multi.getCurrentInstance().getOid())
                .withName(multi.getAttribute(CIPOS.Printer.Name))
                .withType(EnumUtils.getEnum(org.efaps.pos.dto.PrinterType.class, printerType.name()))
                .build());
        }
        final Response ret = Response.ok()
                        .entity(printers)
                        .build();
        return ret;
    }
}
