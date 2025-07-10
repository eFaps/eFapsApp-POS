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

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.pos.dto.ExchangeRateDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("3842fbb4-03ad-4332-97c4-b0031d12f347")
@EFapsApplication("eFapsApp-POS")
public abstract class ExchangeRate_Base
    extends AbstractRest
{

    private static final Logger LOG = LoggerFactory.getLogger(ExchangeRate.class);

    public Response getExchangeRates(@PathParam("identifier") final String _identifier)
        throws EFapsException
    {
        checkAccess(_identifier);
        LOG.debug("Recieved request for ExchangeRates");
        final var rates = new ArrayList<ExchangeRateDto>();
        CurrencyInst.getAvailable().forEach(currencyInst -> {
            try {
                final var builder = ExchangeRateDto.builder();
                boolean add = false;
                switch (currencyInst.getISOCode()) {
                    case "PEN":
                        builder.withCurrency(org.efaps.pos.dto.Currency.PEN);
                        add = true;
                        break;
                    case "USD":
                        builder.withCurrency(org.efaps.pos.dto.Currency.USD);
                        add = true;
                        break;
                    default:
                        break;
                }
                if (add) {
                    final var rateInfo = new Currency().evaluateRateInfo(ParameterUtil.instance(),
                                    (String) null, currencyInst.getInstance());
                    builder.withExchangeRate(rateInfo.getRateUI());
                    rates.add(builder.build());
                }
            } catch (final EFapsException e) {
                LOG.error("Catched", e);
            }
        });
        LOG.debug("ExchangeRates: {}", rates);
        final Response ret = Response.ok()
                        .entity(rates)
                        .build();
        return ret;
    }
}
