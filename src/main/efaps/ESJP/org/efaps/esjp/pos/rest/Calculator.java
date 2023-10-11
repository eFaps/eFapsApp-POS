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

import java.math.BigDecimal;
import java.util.ArrayList;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.sales.ICalculatorConfig;
import org.efaps.pos.dto.CalculationDto;
import org.efaps.pos.dto.CalculationResponseDto;
import org.efaps.util.EFapsException;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@EFapsUUID("be9d1159-20dd-48d2-91cd-1cc6a015c0f5")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Calculator
    extends AbstractRest
{

    @Path("/{identifier}/calculator")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response calculate(@PathParam("identifier") final String identifier,
                              final CalculationDto dto)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.MOBILE);

        final Parameter parameter = ParameterUtil.instance();
        ParameterUtil.setParameterValues(parameter, "identifier", identifier);
        final var calculators = new ArrayList<org.efaps.esjp.sales.Calculator>();
        for (final var item : dto.getItems()) {
            final var prodInst = Instance.get(item.getProductOid());
            final var calculator = new org.efaps.esjp.sales.Calculator(parameter, null, prodInst, item.getQuantity(),
                            null, BigDecimal.ZERO, true, getCalcConf());
            calculators.add(calculator);
        }
        final var crossTotal = org.efaps.esjp.sales.Calculator.getCrossTotal(parameter, calculators);
        final var netTotal = org.efaps.esjp.sales.Calculator.getNetTotal(parameter, calculators);

        return Response.ok(
                        CalculationResponseDto.builder()
                        .withCrossTotal(crossTotal)
                        .withNetTotal(netTotal)
                        .build()).build();
    }

    protected ICalculatorConfig getCalcConf()
    {
        return new ICalculatorConfig()
        {

            @Override
            public String getSysConfKey4Doc(final Parameter _parameter)
                throws EFapsException
            {
                return CISales.Receipt.getType().getName();
            }

            @Override
            public String getSysConfKey4Pos(final Parameter _parameter)
                throws EFapsException
            {
                return "DefaultPosition";
            }

            @Override
            public boolean priceFromUIisNet(final Parameter _parameter)
                throws EFapsException
            {
                return false;
            }
        };
    }
}
