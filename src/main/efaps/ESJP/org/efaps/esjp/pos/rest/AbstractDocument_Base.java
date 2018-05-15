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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.PrintQuery;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.util.ERP;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.Tax_Base;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.pos.dto.AbstractDocItemDto;
import org.efaps.pos.dto.AbstractPayableDocumentDto;
import org.efaps.pos.dto.TaxEntryDto;
import org.efaps.util.EFapsException;

@EFapsUUID("2c0b2e38-14cb-474a-8b49-0859f38784c5")
@EFapsApplication("eFapsApp-POS")
public abstract class AbstractDocument_Base
{

    protected Instance createDocument(final CIType _ciType, final AbstractPayableDocumentDto _dto)
        throws EFapsException
    {
        final Insert insert = new Insert(_ciType);
        insert.add(CISales.Receipt.Name, _dto.getNumber());
        insert.add(CISales.Receipt.Date, _dto.getDate());
        insert.add(CISales.Receipt.Status, Status.find(CISales.ReceiptStatus.Paid));

        final BigDecimal netTotal = _dto.getNetTotal() == null ? BigDecimal.ZERO : _dto.getNetTotal();
        final BigDecimal crossTotal = _dto.getCrossTotal() == null ? BigDecimal.ZERO : _dto.getCrossTotal();

        insert.add(CISales.Receipt.NetTotal, netTotal);
        insert.add(CISales.Receipt.CrossTotal, crossTotal);
        insert.add(CISales.Receipt.DiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Receipt.RateNetTotal, netTotal);
        insert.add(CISales.Receipt.RateCrossTotal, crossTotal);
        insert.add(CISales.Receipt.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CISales.Receipt.CurrencyId, ERP.CURRENCYBASE.get());
        insert.add(CISales.Receipt.RateCurrencyId, ERP.CURRENCYBASE.get());
        insert.add(CISales.Receipt.Rate, new Object[] { 1, 1 });
        insert.add(CISales.Receipt.Taxes, getTaxes(_dto.getDate(), _dto.getTaxes()));
        insert.execute();
        return insert.getInstance();
    }

    protected Instance createPosition(final Instance _docInstance, final CIType _ciType, final AbstractDocItemDto _dto)
        throws EFapsException
    {
        final Insert insert = new Insert(CISales.ReceiptPosition);
        insert.add(CISales.PositionAbstract.PositionNumber, _dto.getIndex());
        insert.add(CISales.PositionAbstract.DocumentAbstractLink, _docInstance);

        final Instance prodInst = Instance.get(_dto.getProductOid());
        insert.add(CISales.PositionAbstract.Product, prodInst);

        final PrintQuery print = new PrintQuery(prodInst);
        print.addAttribute(CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.DefaultUoM);
        print.execute();
        insert.add(CISales.PositionAbstract.ProductDesc, print.<String>getAttribute(
                        CIProducts.ProductAbstract.Description));
        insert.add(CISales.PositionAbstract.UoM, print.<Long>getAttribute(CIProducts.ProductAbstract.DefaultUoM));
        insert.add(CISales.PositionSumAbstract.Quantity, _dto.getQuantity());
        insert.add(CISales.PositionSumAbstract.Discount, BigDecimal.ZERO);
        insert.add(CISales.PositionSumAbstract.DiscountNetUnitPrice, _dto.getNetUnitPrice());
        insert.add(CISales.PositionSumAbstract.CrossUnitPrice, _dto.getCrossUnitPrice());
        insert.add(CISales.PositionSumAbstract.NetUnitPrice, _dto.getNetUnitPrice());
        insert.add(CISales.PositionSumAbstract.CrossPrice, _dto.getCrossPrice());
        insert.add(CISales.PositionSumAbstract.NetPrice, _dto.getNetPrice());
        insert.add(CISales.PositionSumAbstract.CurrencyId, ERP.CURRENCYBASE.get());
        insert.add(CISales.PositionSumAbstract.Rate, new Object[] { 1, 1 });
        insert.add(CISales.PositionSumAbstract.RateCurrencyId, ERP.CURRENCYBASE.get());
        insert.add(CISales.PositionSumAbstract.RateNetUnitPrice, _dto.getNetUnitPrice());
        insert.add(CISales.PositionSumAbstract.RateCrossUnitPrice, _dto.getCrossUnitPrice());
        insert.add(CISales.PositionSumAbstract.RateDiscountNetUnitPrice, _dto.getCrossUnitPrice());
        insert.add(CISales.PositionSumAbstract.RateNetPrice, _dto.getNetPrice());
        insert.add(CISales.PositionSumAbstract.RateCrossPrice, _dto.getCrossPrice());
        insert.add(CISales.PositionSumAbstract.Tax, getTax(_dto.getTaxes()).getInstance());
        insert.execute();
        return insert.getInstance();
    }

    protected Taxes getTaxes(final LocalDate _date, final Set<TaxEntryDto> _taxes)
        throws EFapsException
    {
        final Taxes ret = new Taxes();
        for (final TaxEntryDto dto : _taxes) {
            final TaxEntry taxentry = new TaxEntry();
            taxentry.setAmount(dto.getAmount());
            taxentry.setUUID(getTax(_taxes).getUUID());
            taxentry.setCatUUID(getTax(_taxes).getTaxCat().getUuid());
            taxentry.setCurrencyUUID(CurrencyInst.get(ERP.CURRENCYBASE.get()).getUUID());
            taxentry.setDate(new org.joda.time.LocalDate(_date.getYear(), _date.getMonthValue(), _date.getDayOfMonth())
                            .toDateTimeAtCurrentTime().withTimeAtStartOfDay());
            ret.getEntries().add(taxentry);
        }
        return ret;
    }

    protected Tax getTax(final Set<TaxEntryDto> _set)
        throws EFapsException
    {
        return Tax_Base.get(UUID.fromString("ed28d3c0-e55d-45e5-8025-e48fc989c9dd"), UUID.fromString(
                        "06e40be6-40d8-44f4-9d8f-585f2f97ce63"));
    }
}
