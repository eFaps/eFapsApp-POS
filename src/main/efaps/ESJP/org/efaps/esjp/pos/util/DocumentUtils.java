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
package org.efaps.esjp.pos.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.util.EFapsException;
import org.jfree.util.Log;

@EFapsUUID("8ea293cf-3e64-4e2d-85ec-1d28cbd1592b")
@EFapsApplication("eFapsApp-POS")
public class DocumentUtils
{

    public static CurrencyInst getCurrencyInst(final org.efaps.pos.dto.Currency currency)
        throws EFapsException
    {
        return CurrencyInst.find(currency.name()).orElseGet(() -> {
            try {
                return CurrencyInst.get(Currency.getBaseCurrency());
            } catch (final EFapsException e) {
                Log.error("Catched {}", e);
            }
            return null;
        });
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
        return currencyInst.isInvert() ? new Object[] { rate, 1 }
                        : new Object[] { 1, rate };
    }
}
