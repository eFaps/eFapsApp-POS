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
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAttribute;
import org.efaps.ci.CIStatus;
import org.efaps.ci.CIType;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIContacts;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.contacts.util.Contacts;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.Currency;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.erp.NumberFormatter;
import org.efaps.esjp.erp.SerialNumbers;
import org.efaps.esjp.pos.util.DocumentUtils;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.ICalculatorConfig;
import org.efaps.esjp.sales.tax.Tax;
import org.efaps.esjp.sales.tax.TaxAmount;
import org.efaps.esjp.sales.tax.xml.TaxEntry;
import org.efaps.esjp.sales.tax.xml.Taxes;
import org.efaps.pos.dto.GenerateDocDto;
import org.efaps.pos.dto.GenerateDocResponseDto;
import org.efaps.util.EFapsException;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("a8a218ff-744b-479c-a5fb-10c178a5f6ce")
@EFapsApplication("eFapsApp-POS")
public abstract class AbstractDocumentGenerator
    extends AbstractRest
{

    private static final Logger LOG = LoggerFactory.getLogger(AbstractDocumentGenerator.class);

    protected abstract CIType getDocumentType();

    protected abstract CIStatus getDocumentCreateStatus();

    protected abstract CIType getPositionType();

    protected abstract CIAttribute getAttribute4SerialNumber();

    protected GenerateDocResponseDto toDto(final Instance docInstance)
        throws EFapsException
    {
        final var eval = EQL.builder()
                        .print(docInstance)
                        .attribute(CISales.DocumentSumAbstract.CrossTotal)
                        .evaluate();
        eval.next();
        final BigDecimal crossTotal = eval.get(CISales.DocumentSumAbstract.CrossTotal);

        return GenerateDocResponseDto.builder()
                        .withOid(docInstance.getOid())
                        .withPayableAmount(crossTotal)
                        .build();
    }

    protected List<Calculator> evalCalculators(final String identifier,
                                               final GenerateDocDto dto)
        throws EFapsException
    {
        final Parameter parameter = ParameterUtil.instance();
        ParameterUtil.setParameterValues(parameter, "identifier", identifier);
        final var calculators = new ArrayList<Calculator>();
        for (final var item : dto.getItems()) {
            final var prodInst = Instance.get(item.getProductOid());
            final var calculator = new Calculator(parameter, null, prodInst, item.getQuantity(),
                            null, BigDecimal.ZERO, true, getCalcConf());
            calculators.add(calculator);
        }
        return calculators;
    }

    protected Instance createDocument(final String identifier,
                                      final GenerateDocDto dto,
                                      final List<Calculator> calculators)
        throws EFapsException
    {
        final Parameter parameter = ParameterUtil.instance();
        ParameterUtil.setParameterValues(parameter, "identifier", identifier);

        final Instance baseCurrInst = Currency.getBaseCurrency();
        final Instance rateCurrInst = DocumentUtils.getCurrencyInst(dto.getCurrency()).getInstance();

        final var rateInfo = new Currency().evaluateRateInfo(parameter, DateTime.now(), rateCurrInst);
        final BigDecimal rate = rateInfo.getRate();
        final Object[] rateObj = DocumentUtils.getRate(dto.getCurrency(), rate);

        final DecimalFormat frmt = NumberFormatter.get().getFrmt4Total(getDocumentType().getType());
        final int scale = frmt.getMaximumFractionDigits();

        final BigDecimal rateCrossTotal = Calculator.getCrossTotal(parameter, calculators);
        final BigDecimal crossTotal = rateCrossTotal.divide(rate, RoundingMode.HALF_UP);
        final BigDecimal rateNetTotal = Calculator.getNetTotal(parameter, calculators);
        final BigDecimal netTotal = rateNetTotal.divide(rate, RoundingMode.HALF_UP);

        final var eql = EQL.builder().insert(getDocumentType())
                        .set(CISales.DocumentSumAbstract.Name, evaluateDocName(identifier, true))
                        .set(CISales.DocumentSumAbstract.Date, LocalDate.now(Context.getThreadContext().getZoneId()))
                        .set(CISales.DocumentSumAbstract.StatusAbstract, getDocumentCreateStatus())
                        .set(CISales.DocumentSumAbstract.Contact, getContactInstance(dto))
                        .set(CISales.DocumentSumAbstract.RateCrossTotal,
                                        rateCrossTotal.setScale(scale, RoundingMode.HALF_UP))
                        .set(CISales.DocumentSumAbstract.CrossTotal, crossTotal.setScale(scale, RoundingMode.HALF_UP))
                        .set(CISales.DocumentSumAbstract.RateNetTotal,
                                        rateNetTotal.setScale(scale, RoundingMode.HALF_UP))
                        .set(CISales.DocumentSumAbstract.NetTotal, netTotal.setScale(scale, RoundingMode.HALF_UP))
                        .set(CISales.DocumentSumAbstract.RateDiscountTotal, BigDecimal.ZERO)
                        .set(CISales.DocumentSumAbstract.RateTaxes, getRateTaxes(calculators, rateCurrInst))
                        .set(CISales.DocumentSumAbstract.Taxes, getTaxes(calculators, rate, baseCurrInst))
                        .set(CISales.DocumentSumAbstract.DiscountTotal, BigDecimal.ZERO)
                        .set(CISales.DocumentSumAbstract.CurrencyId, baseCurrInst)
                        .set(CISales.DocumentSumAbstract.Rate, rateObj)
                        .set(CISales.DocumentSumAbstract.RateCurrencyId, rateCurrInst);

        final var docInst = eql.execute();
        return docInst;
    }

    protected void createPositions(final String identifier,
                                   final GenerateDocDto dto,
                                   final List<Calculator> calculators,
                                   final Instance docInstance)
        throws EFapsException
    {
        final Parameter parameter = ParameterUtil.instance();
        ParameterUtil.setParameterValues(parameter, "identifier", identifier);

        final Instance baseCurrInst = Currency.getBaseCurrency();
        final Instance rateCurrInst = DocumentUtils.getCurrencyInst(dto.getCurrency()).getInstance();

        final var rateInfo = new Currency().evaluateRateInfo(parameter, DateTime.now(), rateCurrInst);
        final BigDecimal rate = rateInfo.getRate();
        final Object[] rateObj = DocumentUtils.getRate(dto.getCurrency(), rate);

        final DecimalFormat totalFrmt = NumberFormatter.get().getFrmt4Total(getDocumentType().getType());
        final int scale = totalFrmt.getMaximumFractionDigits();

        final DecimalFormat unitFrmt = NumberFormatter.get().getFrmt4UnitPrice(getDocumentType().getType());
        final int uScale = unitFrmt.getMaximumFractionDigits();

        Integer idx = 0;
        for (final Calculator calc : calculators) {
            final var item = dto.getItems().get(idx);
            final var productInst = Instance.get(item.getProductOid());
            final var eval = EQL.builder().print(productInst)
                            .attribute(CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.DefaultUoM)
                            .evaluate();

            final Taxes taxes = calc.getTaxes(baseCurrInst);
            taxes.getEntries().forEach(entry -> {
                entry.setAmount(entry.getAmount().divide(rate, RoundingMode.HALF_UP));
                entry.setBase(entry.getBase().divide(rate, RoundingMode.HALF_UP));
            });

            final var eql = EQL.builder().insert(getPositionType())
                            .set(CISales.PositionAbstract.PositionNumber, idx + 1)
                            .set(CISales.PositionAbstract.DocumentAbstractLink, docInstance)
                            .set(CISales.PositionAbstract.Product, productInst)
                            .set(CISales.PositionAbstract.ProductDesc, eval.get(CIProducts.ProductAbstract.Description))
                            .set(CISales.PositionAbstract.UoM, eval.get(CIProducts.ProductAbstract.DefaultUoM))
                            .set(CISales.PositionSumAbstract.Quantity, calc.getQuantity())
                            .set(CISales.PositionSumAbstract.CrossUnitPrice, calc.getCrossUnitPrice()
                                            .divide(rate, RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP))
                            .set(CISales.PositionSumAbstract.NetUnitPrice, calc.getNetUnitPrice()
                                            .divide(rate, RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP))
                            .set(CISales.PositionSumAbstract.CrossPrice, calc.getCrossPrice()
                                            .divide(rate, RoundingMode.HALF_UP).setScale(scale, RoundingMode.HALF_UP))
                            .set(CISales.PositionSumAbstract.NetPrice, calc.getNetPrice()
                                            .divide(rate, RoundingMode.HALF_UP).setScale(scale, RoundingMode.HALF_UP))
                            .set(CISales.PositionSumAbstract.Tax, calc.getTaxCatId())

                            .set(CISales.PositionSumAbstract.Taxes, taxes)
                            .set(CISales.PositionSumAbstract.Discount, calc.getDiscount())
                            .set(CISales.PositionSumAbstract.DiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                            .divide(rate, RoundingMode.HALF_UP).setScale(uScale, RoundingMode.HALF_UP))
                            .set(CISales.PositionSumAbstract.CurrencyId, baseCurrInst)
                            .set(CISales.PositionSumAbstract.Rate, rateObj)
                            .set(CISales.PositionSumAbstract.RateCurrencyId, rateCurrInst)
                            .set(CISales.PositionSumAbstract.RateNetUnitPrice, calc.getNetUnitPrice()
                                            .setScale(uScale, RoundingMode.HALF_UP))
                            .set(CISales.PositionSumAbstract.RateCrossUnitPrice, calc.getCrossUnitPrice()
                                            .setScale(uScale, RoundingMode.HALF_UP))
                            .set(CISales.PositionSumAbstract.RateDiscountNetUnitPrice, calc.getDiscountNetUnitPrice()
                                            .setScale(uScale, RoundingMode.HALF_UP))
                            .set(CISales.PositionSumAbstract.RateNetPrice,
                                            calc.getNetPrice().setScale(scale, RoundingMode.HALF_UP))
                            .set(CISales.PositionSumAbstract.RateCrossPrice,
                                            calc.getCrossPrice().setScale(scale, RoundingMode.HALF_UP))
                            .set(CISales.PositionSumAbstract.RateTaxes, calc.getTaxes(rateCurrInst));

            eql.execute();

            idx++;
        }
    }

    protected String evaluateDocName(final String identifier,
                                     final Boolean isCreate)
        throws EFapsException
    {
        String serialNumber = null;
        final var eval = EQL.builder().print().query(CIPOS.Backend).where()
                        .attribute(CIPOS.Backend.Identifier).eq(identifier)
                        .select()
                        .attribute(getAttribute4SerialNumber())
                        .evaluate();
        if (eval.next()) {
            serialNumber = eval.get(getAttribute4SerialNumber());
        }
        if (serialNumber == null) {
            LOG.error("Missing configuration for SerialNumbers for identifier: {}", identifier);
        }
        return isCreate ? SerialNumbers.getPlaceholder(getDocumentType(), serialNumber)
                        : SerialNumbers.getNext(getDocumentType(), serialNumber);
    }

    protected Instance getContactInstance(final GenerateDocDto dto)
        throws EFapsException
    {
        Instance contactInstance = null;
        if (dto.getContactOid() == null) {
            contactInstance = Instance.get(dto.getContactOid());
        }
        if (!InstanceUtils.isKindOf(contactInstance, CIContacts.ContactAbstract)) {
            contactInstance = Contacts.STRAYCOSTUMER.get();
        }
        return contactInstance;
    }

    public Taxes getRateTaxes(final List<Calculator> calculators,
                              final Instance currencyInst)
        throws EFapsException
    {
        final Map<Tax, TaxAmount> values = new HashMap<>();
        for (final Calculator calc : calculators) {
            if (!calc.isWithoutTax()) {
                for (final TaxAmount taxAmount : calc.getTaxesAmounts()) {
                    if (!values.containsKey(taxAmount.getTax())) {
                        values.put(taxAmount.getTax(), new TaxAmount().setTax(taxAmount.getTax()));
                    }
                    values.get(taxAmount.getTax())
                                    .addAmount(taxAmount.getAmount())
                                    .addBase(taxAmount.getBase());
                }
            }
        }
        final Taxes ret = new Taxes();
        if (!calculators.isEmpty()) {
            final Calculator calc = calculators.iterator().next();
            UUID currencyUUID = null;
            if (currencyInst != null) {
                final CurrencyInst curInst = CurrencyInst.get(currencyInst);
                currencyUUID = curInst.getUUID();
            }
            for (final TaxAmount taxAmount : values.values()) {
                final TaxEntry taxentry = new TaxEntry();
                taxentry.setAmount(taxAmount.getAmount());
                taxentry.setBase(taxAmount.getBase());
                taxentry.setUUID(taxAmount.getTax().getUUID());
                taxentry.setCatUUID(taxAmount.getTax().getTaxCat().getUuid());
                taxentry.setCurrencyUUID(currencyUUID);
                taxentry.setDate(calc.getDate());
                ret.getEntries().add(taxentry);
            }
        }
        return ret;
    }

    public Taxes getTaxes(final List<Calculator> calculators,
                          final BigDecimal rate,
                          final Instance baseCurrInst)
        throws EFapsException
    {
        final Taxes ret = getRateTaxes(calculators, baseCurrInst);
        for (final TaxEntry entry : ret.getEntries()) {
            entry.setAmount(entry.getAmount().divide(rate, RoundingMode.HALF_UP));
            entry.setBase(entry.getBase().divide(rate, RoundingMode.HALF_UP));
        }
        return ret;
    }

    protected ICalculatorConfig getCalcConf()
    {
        return new ICalculatorConfig()
        {

            @Override
            public String getSysConfKey4Doc(final Parameter _parameter)
                throws EFapsException
            {
                return getDocumentType().getType().getName();
            }

            @Override
            public String getSysConfKey4Pos(final Parameter _parameter)
                throws EFapsException
            {
                return getPositionType().getType().getName();
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
