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

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIERP;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.pos.dto.CompanyDto;
import org.efaps.pos.dto.PosDto;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("b530eab3-497c-4bb6-bc7e-64a6041e9ebd")
@EFapsApplication("eFapsApp-POS")
public abstract class POS_Base
{
    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    public Response getPOSs()
        throws EFapsException
    {
        final List<PosDto> poss = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.POS);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selCurrency = SelectBuilder.get()
                        .linkto(CIPOS.POS.AccountLink)
                        .linkto(CISales.AccountAbstract.CurrencyLink)
                        .attribute(CIERP.Currency.ISOCode);
        final SelectBuilder selContactOid = SelectBuilder.get()
                        .linkto(CIPOS.POS.DefaultContactLink)
                        .oid();
        multi.addSelect(selCurrency, selContactOid);
        multi.addAttribute(CIPOS.POS.Name);
        multi.execute();
        while (multi.next()) {
            poss.add(PosDto.builder()
                .withOID(multi.getCurrentInstance().getOid())
                .withName(multi.getAttribute(CIPOS.POS.Name))
                .withCurrency(multi.getSelect(selCurrency))
                .withDefaultContactOid(multi.getSelect(selContactOid))
                .withCompany(CompanyDto.builder()
                                .withName(ERP.COMPANY_NAME.get())
                                .withTaxNumber(ERP.COMPANY_TAX.get())
                                .build())
                .build());
        }
        final Response ret = Response.ok()
                        .entity(poss)
                        .build();
        return ret;
    }

}
