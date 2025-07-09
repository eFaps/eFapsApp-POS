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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.abacus.api.ICalcPosition;
import org.efaps.abacus.api.IConfig;
import org.efaps.abacus.api.ITax;
import org.efaps.abacus.api.TaxType;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.promotions.PromotionService;
import org.efaps.esjp.sales.CalculatorConfig;
import org.efaps.esjp.sales.PriceUtil;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.TaxCat_Base;
import org.efaps.pos.dto.CalculatorPositionResponseDto;
import org.efaps.pos.dto.CalculatorRequestDto;
import org.efaps.pos.dto.CalculatorResponseDto;
import org.efaps.pos.dto.PromoDetailDto;
import org.efaps.pos.dto.PromoInfoDto;
import org.efaps.pos.dto.TaxDto;
import org.efaps.pos.dto.TaxEntryDto;
import org.efaps.promotionengine.api.IDocument;
import org.efaps.promotionengine.pojo.Document;
import org.efaps.promotionengine.pojo.Position;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(Category.class);

    @Path("/{identifier}/calculator")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces({ MediaType.APPLICATION_JSON })
    public Response calculate(@PathParam("identifier") final String identifier,
                              final CalculatorRequestDto dto)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.MOBILE);

        final Parameter parameter = ParameterUtil.instance();
        final var taxMap = new HashMap<String, Tax>();
        final var document = new Document();
        int idx = 0;
        for (final var pos : dto.getPositions()) {
            final var prodInst = Instance.get(pos.getProductOid());
            final var prodPrice = new PriceUtil().getPrice(parameter, DateTime.now(), prodInst,
                            CIProducts.ProductPricelistRetail.uuid, "DefaultPosition", false);

            final var prodEval = EQL.builder()
                            .print(prodInst)
                            .attribute(CIProducts.ProductAbstract.TaxCategory)
                            .evaluate();
            prodEval.next();

            final var taxCatId = prodEval.<Long>get(CIProducts.ProductAbstract.TaxCategory);
            final List<ITax> taxes = TaxCat_Base.get(taxCatId).getTaxes().stream()
                            .map(tax -> {
                                try {
                                    taxMap.put(tax.getName(), tax);

                                    return (ITax) new org.efaps.abacus.pojo.Tax()
                                                    .setKey(tax.getName())
                                                    .setPercentage(tax.getFactor().multiply(new BigDecimal("100")))
                                                    .setAmount(tax.getAmount())
                                                    .setType(EnumUtils.getEnum(TaxType.class, tax.getTaxType().name()));
                                } catch (final EFapsException e) {
                                    LOG.error("Catched", e);
                                }
                                return null;
                            })
                            .toList();

            document.addPosition(new Position()
                            .setNetUnitPrice(prodPrice.getCurrentPrice())
                            .setTaxes(taxes)
                            .setIndex(idx++)
                            .setProductOid(pos.getProductOid())
                            .setQuantity(pos.getQuantity()));
        }
        final var result = calculate(document);

        final var payload = CalculatorResponseDto.builder()
                        .withNetTotal(result.getNetTotal())
                        .withTaxTotal(result.getTaxTotal())
                        .withCrossTotal(result.getCrossTotal())
                        .withPayableAmount(result.getCrossTotal())
                        .withTaxes(toDto(taxMap, result.getTaxes()))
                        .withPositions(result.getPositions().stream()
                                        .map(pos -> CalculatorPositionResponseDto.builder()
                                                        .withIndex(pos.getIndex())
                                                        .withParentIdx(evalParentIndex(pos, dto))
                                                        .withQuantity(pos.getQuantity())
                                                        .withProductOid(pos.getProductOid())
                                                        .withNetUnitPrice(pos.getNetUnitPrice())
                                                        .withNetPrice(pos.getNetPrice())
                                                        .withCrossUnitPrice(pos.getCrossUnitPrice())
                                                        .withCrossPrice(pos.getCrossPrice())
                                                        .withTaxAmount(pos.getTaxAmount())
                                                        .withTaxes(toDto(taxMap, pos.getTaxes()))
                                                        .withBomOid(evalBomOid(pos, dto))
                                                        .build())
                                        .toList())
                        .withPromotionInfo(getPromoInfo(result))
                        .build();

        return Response.ok(payload).build();
    }

    private Integer evalParentIndex(final ICalcPosition pos,
                                    final CalculatorRequestDto calculatorPayloadDto)
    {

        Integer ret = null;
        final var reqPos = calculatorPayloadDto.getPositions().stream()
                        .filter(p -> p.getIndex().equals(pos.getIndex()))
                        .findFirst();
        if (reqPos.isPresent()) {
            ret = reqPos.get().getParentIdx();
        }
        return ret;
    }

    private String evalBomOid(final ICalcPosition pos,
                              final CalculatorRequestDto calculatorPayloadDto)
    {
        String ret = null;
        final var reqPos = calculatorPayloadDto.getPositions().stream()
                        .filter(p -> p.getIndex().equals(pos.getIndex())).findFirst();
        if (reqPos.isPresent()) {
            ret = reqPos.get().getBomOid();
        }
        return ret;
    }

    public IDocument calculate(final IDocument document)
        throws EFapsException
    {
        final var calculator = new org.efaps.promotionengine.Calculator(getConfig());
        final var promotionService = new PromotionService();
        calculator.calc(document, promotionService.getPromotions());
        return document;
    }

    public PromoInfoDto getPromoInfo(final IDocument result)
    {
        PromoInfoDto ret = null;
        if (result != null && result instanceof Document) {
            final var info = ((Document) result).getPromotionInfo();
            if (info != null) {
                ret = PromoInfoDto.builder()
                                .withNetTotalDiscount(info.getNetTotalDiscount())
                                .withCrossTotalDiscount(info.getCrossTotalDiscount())
                                .withPromotionOids(info.getPromotionOids())
                                .withDetails(info.getDetails().stream()
                                                .map(pos -> PromoDetailDto.builder()
                                                                .withPositionIndex(pos.getPositionIndex())
                                                                .withNetBase(pos.getNetBase())
                                                                .withNetUnitBase(pos.getNetUnitBase())
                                                                .withNetDiscount(pos.getNetDiscount())
                                                                .withNetUnitDiscount(pos.getNetUnitDiscount())
                                                                .withNetDiscount(pos.getNetDiscount())
                                                                .withCrossUnitDiscount(pos.getCrossUnitDiscount())
                                                                .withCrossDiscount(pos.getCrossDiscount())
                                                                .withPromotionOid(pos.getPromotionOid())
                                                                .build())
                                                .toList())
                                .build();
            }
        }
        return ret;
    }

    protected IConfig getConfig()
    {
        return new CalculatorConfig(CISales.Invoice.getType().getName());
    }

    protected List<TaxEntryDto> toDto(final Map<String, Tax> taxMap,
                                      final List<ITax> list)
    {
        return list == null ? new ArrayList<>() : list.stream().map(itax -> toDto(taxMap, itax)).toList();
    }

    protected TaxEntryDto toDto(final Map<String, Tax> taxMap,
                                final ITax tax)
    {
        return TaxEntryDto.builder()
                        .withAmount(tax.getAmount())
                        .withBase(tax.getBase())
                        .withTax(toDto(taxMap.get(tax.getKey())))
                        .build();
    }

    public static TaxDto toDto(final Tax tax)
    {
        try {
            return TaxDto.builder()
                            .withKey(tax.getName())
                            .withCatKey(tax.getTaxCat().getName())
                            .withType(EnumUtils.getEnum(org.efaps.pos.dto.TaxType.class, tax.getTaxType().name()))
                            .withName(tax.getName())
                            .withPercent(tax.getFactor() == null ? null : tax.getFactor().multiply(new BigDecimal(100)))
                            .withAmount(tax.getAmount())
                            .build();
        } catch (final EFapsException e) {
            LOG.error("Catched", e);
        }
        return null;
    }

}
