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
package org.efaps.esjp.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.esjp.ci.CILoyalty;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.TaxCat;
import org.efaps.esjp.sales.tax.TaxCat_Base;
import org.efaps.esjp.sales.tax.Tax_Base;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.pos.dto.DocStatus;
import org.efaps.pos.dto.PaymentType;
import org.efaps.pos.dto.TaxDto;
import org.efaps.pos.dto.TaxEntryDto;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("8ea293cf-3e64-4e2d-85ec-1d28cbd1592b")
@EFapsApplication("eFapsApp-POS")
public class DocumentUtils
{
    private static final Logger LOG = LoggerFactory.getLogger(DocumentUtils.class);

    public static CurrencyInst getCurrencyInst(final org.efaps.pos.dto.Currency currency)
        throws EFapsException
    {
        return CurrencyInst.find(currency.name()).orElseGet(() -> {
            try {
                return CurrencyInst.get(Currency.getBaseCurrency());
            } catch (final EFapsException e) {
                LOG.error("Catched {}", e);
            }
            return null;
        });
    }

    public static org.efaps.pos.dto.Currency getCurrency(final Long currencyId)
        throws EFapsException
    {
        final var currencyInst = CurrencyInst.get(currencyId);
        return EnumUtils.getEnum(org.efaps.pos.dto.Currency.class, currencyInst.getISOCode());
    }

    public static org.efaps.pos.dto.Currency getCurrency(final UUID currencyUUID)
        throws EFapsException
    {
        final var currencyInst = CurrencyInst.get(currencyUUID);
        return EnumUtils.getEnum(org.efaps.pos.dto.Currency.class, currencyInst.getISOCode());
    }

    public static BigDecimal exchange(final BigDecimal amount,
                                      final org.efaps.pos.dto.Currency currency,
                                      final BigDecimal exchangeRate)
        throws EFapsException
    {
        var ret = BigDecimal.ZERO;
        final var currencyInst = getCurrencyInst(currency);
        // if it the base currency no conversion is needed
        if (currencyInst.getInstance().equals(Currency.getBaseCurrency())) {
            ret = amount;
        } else if (currencyInst.isInvert()) {
            ret = amount.multiply(exchangeRate);
        } else if (amount.compareTo(BigDecimal.ZERO) != 0) {
            ret = amount.divide(exchangeRate, RoundingMode.HALF_DOWN);
        }
        return ret;
    }

    public static Object[] getRate(final org.efaps.pos.dto.Currency currency,
                                   final BigDecimal exchangeRate)
        throws EFapsException
    {
        BigDecimal rate;
        if (exchangeRate.compareTo(BigDecimal.ZERO) == 0) {
            rate = BigDecimal.ONE;
        } else {
            rate = exchangeRate;
        }

        final var currencyInst = getCurrencyInst(currency);
        return currencyInst.isInvert() ? new Object[] { 1, rate } : new Object[] { rate, 1 };
    }

    public static CIType getPaymentDocType(final PaymentType paymentType,
                                           final boolean negate)
    {
        final CIType ret = switch (paymentType) {
            case ELECTRONIC -> CISales.PaymentElectronic;
            case CARD -> CISales.PaymentCard;
            case CASH -> negate ? CISales.PaymentCashOut : CISales.PaymentCash;
            case CHANGE -> negate ? CISales.PaymentCash : CISales.PaymentCashOut;
            case FREE -> CISales.PaymentInternal;
            case LOYALTY_POINTS -> CILoyalty.PaymentPoints;
            case REDEEM_CREDITNOTE -> CISales.PaymentRedeemCreditNote;
            default -> CISales.PaymentInternal;
        };
        return ret;
    }

    public static Status getPaymentDocStatus(final PaymentType paymentType,
                                             final boolean negate)
        throws CacheReloadException
    {
        return switch (paymentType) {
            case ELECTRONIC -> Status.find(CISales.PaymentElectronicStatus.Closed);
            case CARD -> Status.find(CISales.PaymentCardStatus.Closed);
            case CASH -> negate ? Status.find(CISales.PaymentCashOutStatus.Closed)
                            : Status.find(CISales.PaymentCashStatus.Closed);
            case CHANGE -> negate ? Status.find(CISales.PaymentCashStatus.Closed)
                            : Status.find(CISales.PaymentCashOutStatus.Closed);
            case FREE -> Status.find(CISales.PaymentInternalStatus.Closed);
            case LOYALTY_POINTS -> Status.find(CILoyalty.PaymentPointsStatus.Closed);
            case REDEEM_CREDITNOTE -> Status.find(CISales.PaymentRedeemCreditNoteStatus.Closed);
            default -> Status.find(CISales.PaymentInternalStatus.Closed);
        };
    }

    public static DocStatus getDtoStatus(final Long statusId)
        throws CacheReloadException
    {
        final var status = Status.get(statusId);
        return switch (status.getKey()) {
            case "Booked" -> DocStatus.CLOSED;
            case "Draft" -> DocStatus.OPEN;
            case "Open" -> DocStatus.OPEN;
            case "Paid" -> DocStatus.CLOSED;
            case "Replaced" -> DocStatus.CANCELED;
            default -> DocStatus.OPEN;
        };
    }

    public static Collection<TaxEntryDto> getDtoTaxes(final Taxes taxes,
                                                      final BigDecimal exchangeRate)
        throws EFapsException
    {
        final var dtos = new ArrayList<TaxEntryDto>();
        for (final var entry : taxes.getEntries()) {
            final var currencyId = CurrencyInst.get(entry.getCurrencyUUID()).getInstance().getId();
            dtos.add(TaxEntryDto.builder()
                            .withAmount(entry.getAmount())
                            .withBase(entry.getBase())
                            .withCurrency(getCurrency(currencyId))
                            .withExchangeRate(exchangeRate)
                            .withTax(getDtoTax(entry.getCatUUID(), entry.getUUID()))
                            .build());
        }
        return dtos;
    }

    public static TaxDto getDtoTax(final UUID catUuid,
                                   final UUID taxUuid)
        throws EFapsException
    {
        return getDtoTax(Tax.get(catUuid, taxUuid));
    }

    public static TaxDto getDtoTax(final Tax tax)
        throws EFapsException
    {
        return TaxDto.builder()
                        .withOID(tax.getInstance().getOid())
                        .withKey(tax.getUUID().toString())
                        .withCatKey(tax.getTaxCat().getUuid().toString())
                        .withName(tax.getName())
                        .withType(EnumUtils.getEnum(org.efaps.pos.dto.TaxType.class,
                                        tax.getTaxType().name()))
                        .withAmount(tax.getAmount())
                        .withPercent(tax.getFactor().multiply(BigDecimal.valueOf(100)))
                        .build();
    }

    public static Tax getTax(final TaxEntryDto taxEntry)
        throws EFapsException
    {
        return Tax_Base.get(UUID.fromString(taxEntry.getTax().getCatKey()),
                        UUID.fromString(taxEntry.getTax().getKey()));
    }

    public static TaxCat getTaxCat(final TaxEntryDto taxEntry)
        throws EFapsException
    {
        return TaxCat_Base.get(UUID.fromString(taxEntry.getTax().getCatKey()));
    }

}
