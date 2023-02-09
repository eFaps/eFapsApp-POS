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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.datamodel.AttributeSet;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.db.store.Resource;
import org.efaps.db.store.Store;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.ICalculatorConfig;
import org.efaps.pos.dto.BOMGroupConfigDto;
import org.efaps.pos.dto.BarcodeDto;
import org.efaps.pos.dto.ConfigurationBOMDto;
import org.efaps.pos.dto.IndicationDto;
import org.efaps.pos.dto.IndicationSetDto;
import org.efaps.pos.dto.Product2CategoryDto;
import org.efaps.pos.dto.ProductDto;
import org.efaps.pos.dto.ProductRelationDto;
import org.efaps.pos.dto.ProductRelationType;
import org.efaps.pos.dto.ProductType;
import org.efaps.pos.dto.TaxDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Class Product_Base.
 */
@EFapsUUID("f1c816e2-1543-4975-b69a-799b4809802b")
@EFapsApplication("eFapsApp-POS")
public abstract class Product_Base
    extends AbstractRest
{

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Product.class);

    /**
     * Gets the products.
     *
     * @return the products
     * @throws EFapsException the eFaps exception
     */
    @SuppressWarnings("unchecked")
    public Response getProducts(final String _identifier,
                                final int limit,
                                final int offset)
        throws EFapsException
    {
        checkAccess(_identifier);
        LOG.debug("Received request for product sync from {}", _identifier);
        final List<ProductDto> products = new ArrayList<>();

        final Map<Integer, String> relSelects;
        final Map<Integer, String> relLabels;
        final Map<Integer, String> relQuantity;
        final Map<Integer, String> relTypes;
        if (Pos.PRODREL.exists()) {
            final Properties properties = Pos.PRODREL.get();
            relQuantity = PropertiesUtil.analyseProperty(properties, "QuantitySelect", 0);
            relSelects = PropertiesUtil.analyseProperty(properties, "Select", 0);
            relLabels = PropertiesUtil.analyseProperty(properties, "Label", 0);
            relTypes = PropertiesUtil.analyseProperty(properties, "RelationType", 0);
        } else {
            relSelects = new HashMap<>();
            relQuantity = new HashMap<>();
            relLabels = new HashMap<>();
            relTypes = new HashMap<>();
        }

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
        if (Pos.CATEGORY_ACTIVATE.get()) {
            queryBldr.setOr(true);
            final QueryBuilder attrQueryBldr = new QueryBuilder(CIPOS.Category);
            attrQueryBldr.addWhereAttrEqValue(CIPOS.Category.Status, Status.find(CIPOS.CategoryStatus.Active));

            final QueryBuilder relAttrQueryBldr = new QueryBuilder(CIPOS.Category2Product);
            relAttrQueryBldr.addWhereAttrInQuery(CIPOS.Category2Product.FromLink,
                            attrQueryBldr.getAttributeQuery(CIPOS.Category.ID));

            queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                            relAttrQueryBldr.getAttributeQuery(CIPOS.Category2Product.ToLink));
            // we need the textpositions
            final QueryBuilder attrQueryBldr2 = new QueryBuilder(CIProducts.ProductTextPosition);
            attrQueryBldr2.addWhereAttrEqValue(CIProducts.ProductTextPosition.Active, true);
            queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                            attrQueryBldr2.getAttributeQuery(CIProducts.ProductTextPosition.ID));
        } else {
            queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.Active, true);
        }
        queryBldr.addOrderByAttributeAsc(CIProducts.ProductAbstract.ID);
        if (limit > 0) {
            queryBldr.setLimit(limit);
        }
        if (offset > 0) {
            queryBldr.setOffset(offset);
        }

        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selCat = SelectBuilder.get()
                        .linkfrom(CIPOS.Category2Product.ToLink)
                        .linkto(CIPOS.Category2Product.FromLink)
                        .oid();
        final SelectBuilder selCatWeight = SelectBuilder.get()
                        .linkfrom(CIPOS.Category2Product.ToLink)
                        .attribute(CIPOS.Category2Product.SortWeight);
        final SelectBuilder selImageOid = SelectBuilder.get()
                        .linkfrom(CIProducts.Product2ImageThumbnail.ProductLink)
                        .linkto(CIProducts.Product2ImageThumbnail.ImageLink)
                        .oid();
        multi.addSelect(selCat, selCatWeight, selImageOid);
        SelectBuilder selIndication = null;
        if (Pos.INDICATIONSET_ACIVATE.get()) {
            selIndication = SelectBuilder.get()
                            .linkfrom(CIPOS.IndicationSet2Product.ToLink)
                            .linkto(CIPOS.IndicationSet2Product.FromLink)
                            .instance();
            multi.addSelect(selIndication);
        }

        for (final Entry<Integer, String> entry : relSelects.entrySet()) {
            multi.addSelect(entry.getValue());
        }
        for (final Entry<Integer, String> entry : relQuantity.entrySet()) {
            multi.addSelect(entry.getValue());
        }
        multi.addAttribute(CIProducts.ProductAbstract.Name,
                        CIProducts.ProductAbstract.Description,
                        CIProducts.ProductAbstract.Note,
                        CIProducts.ProductAbstract.DefaultUoM);
        multi.execute();
        while (multi.next()) {
            final Object cats = multi.getSelect(selCat);
            final Object catWeights = multi.getSelect(selCatWeight);
            final Set<Product2CategoryDto> prod2cats = new HashSet<>();
            if (cats instanceof List) {
                final var weightIter = ((Collection<? extends Integer>) catWeights).iterator();
                for (final String element : (Collection<? extends String>) cats) {
                    prod2cats.add(Product2CategoryDto.builder()
                                    .withCategoryOid(element)
                                    .withWeight(weightIter.next())
                                    .build());
                }
            } else if (cats instanceof String) {
                prod2cats.add(Product2CategoryDto.builder()
                                .withCategoryOid((String) cats)
                                .withWeight((Integer) catWeights)
                                .build());
            }
            final Object imageOids = multi.getSelect(selImageOid);
            final String imageOid;
            if (imageOids instanceof List) {
                imageOid = (String) ((List<?>) imageOids).get(0);
            } else if (imageOids instanceof String) {
                imageOid = (String) imageOids;
            } else {
                imageOid = null;
            }

            final Parameter parameter = ParameterUtil.instance();

            final Calculator calculator = new Calculator(parameter, null, multi.getCurrentInstance(), BigDecimal.ONE,
                            null, BigDecimal.ZERO, true, getCalcConf());

            final Set<TaxDto> taxes = new HashSet<>();
            calculator.getTaxes().forEach(tax -> {
                try {
                    taxes.add(TaxDto.builder()
                                    .withOID(tax.getInstance().getOid())
                                    .withKey(tax.getUUID().toString())
                                    .withCatKey(tax.getTaxCat().getUuid().toString())
                                    .withName(tax.getName())
                                    .withType(EnumUtils.getEnum(org.efaps.pos.dto.TaxType.class,
                                                    tax.getTaxType().name()))
                                    .withAmount(tax.getAmount())
                                    .withPercent(tax.getFactor().multiply(BigDecimal.valueOf(100)))
                                    .build());
                } catch (final EFapsException e) {
                    LOG.error("Catched", e);
                }
            });

            final Set<ProductRelationDto> relations = new HashSet<>();
            for (final Entry<Integer, String> entry : relSelects.entrySet()) {
                final String label = relLabels.get(entry.getKey());
                final String relType = relTypes.get(entry.getKey());
                final Object relation = multi.getSelect(entry.getValue());
                final Object quantity = multi.getSelect(relQuantity.get(entry.getKey()));

                if (relation instanceof List) {
                    final var quantities = ((List<? extends BigDecimal>) quantity).iterator();
                    ((Collection<? extends String>) relation).forEach(oid -> {
                        relations.add(ProductRelationDto.builder()
                                        .withLabel(label)
                                        .withQuantity(quantities.hasNext() ? quantities.next() : null)
                                        .withProductOid(oid)
                                        .withType(ProductRelationType.valueOf(relType))
                                        .build());
                    });

                } else if (relation instanceof String) {
                    relations.add(ProductRelationDto.builder()
                                    .withLabel(label)
                                    .withQuantity((BigDecimal) quantity)
                                    .withProductOid((String) relation)
                                    .withType(ProductRelationType.valueOf(relType))
                                    .build());
                }
            }
            final var barcodes = new HashSet<BarcodeDto>();
            if (Products.STANDART_ACTBARCODES.get()) {
                final var attrSet = AttributeSet.find(CIProducts.ProductAbstract.getType().getName(),
                                CIProducts.ProductAbstract.Barcodes.name);
                // attrSet.getType();
                final QueryBuilder barcodeQueryBldr = new QueryBuilder(attrSet);
                barcodeQueryBldr.addWhereAttrEqValue(attrSet.getAttributeName(), multi.getCurrentInstance());

                final var barcodePrint = barcodeQueryBldr.getPrint();
                barcodePrint.addAttribute("Code");
                final SelectBuilder selBarcodeType = SelectBuilder.get().linkto("BarcodeType").attribute("Value");
                barcodePrint.addSelect(selBarcodeType);
                barcodePrint.executeWithoutAccessCheck();
                while (barcodePrint.next()) {
                    barcodes.add(BarcodeDto.builder()
                                    .withCode(barcodePrint.getAttribute("Code"))
                                    .withType(barcodePrint.getSelect(selBarcodeType))
                                    .build());
                }
            }

            final var bomGroupConfigs = new HashSet<BOMGroupConfigDto>();
            final var configurationBOMs = new HashSet<ConfigurationBOMDto>();
            if (Products.STANDART_ACTCONFBOM.get()) {
                final var eval = EQL.builder().print()
                                .query(CIProducts.BOMGroupConfiguration)
                                .where()
                                .attribute(CIProducts.BOMGroupConfiguration.ProductLink).eq(multi.getCurrentInstance())
                                .select()
                                .attribute(CIProducts.BOMGroupConfiguration.Name,
                                                CIProducts.BOMGroupConfiguration.Description)
                                .evaluate();
                while (eval.next()) {
                    LOG.info("found out for {}", multi.getCurrentInstance());

                    final Collection<Products.BOMGroupConfig> flags = eval.get(CIProducts.BOMGroupConfiguration.Config);

                    final var flagsBitValue = flags == null ? 0
                                    : flags.stream().map(Products.BOMGroupConfig::getInt).reduce(0, Integer::sum);

                    eval.<Integer>get(CIProducts.BOMGroupConfiguration.Config);
                    bomGroupConfigs.add(BOMGroupConfigDto.builder()
                                    .withOID(eval.inst().getOid())
                                    .withName(eval.get(CIProducts.BOMGroupConfiguration.Name))
                                    .withDescription(eval.get(CIProducts.BOMGroupConfiguration.Description))
                                    .withFlags(flagsBitValue)
                                    .withProductOid(multi.getCurrentInstance().getOid())
                                    .build());
                }

                final var eval2 = EQL.builder().print()
                                .query(CIProducts.ConfigurationBOM)
                                .where()
                                .attribute(CIProducts.ConfigurationBOM.From).eq(multi.getCurrentInstance())
                                .select()
                                .attribute(CIProducts.ConfigurationBOM.Position, CIProducts.ConfigurationBOM.Quantity,
                                                CIProducts.ConfigurationBOM.UoM)
                                .linkto(CIProducts.ConfigurationBOM.BOMGroupConfigurationLink).oid().as("bomGroupOid")
                                .linkto(CIProducts.ConfigurationBOM.To).oid().as("toOid")
                                .evaluate();
                while (eval2.next()) {
                    final var uoMId = eval2.<Long>get(CIProducts.ConfigurationBOM.UoM);
                    final String uoM = uoMId == null ? null : Dimension.getUoM(uoMId).getCommonCode();
                    configurationBOMs.add(ConfigurationBOMDto.builder()
                                    .withOID(eval2.inst().getOid())
                                    .withToProductOid(eval2.get("toOid"))
                                    .withPosition(eval2.get(CIProducts.ConfigurationBOM.Position))
                                    .withQuantity(eval2.get(CIProducts.ConfigurationBOM.Quantity))
                                    .withBomGroupOid(eval2.get("bomGroupOid"))
                                    .withUoM(uoM)
                                    .build());
                }
            }

            final UoM uoM = Dimension.getUoM(multi.getAttribute(CIProducts.ProductAbstract.DefaultUoM));

            final var currencyInst = CurrencyInst.get(calculator.getProductNetUnitPrice().getCurrentCurrencyInstance());
            var currency = EnumUtils.getEnum(org.efaps.pos.dto.Currency.class, currencyInst.getISOCode());
            if (currency == null) {
                currency = org.efaps.pos.dto.Currency.PEN;
            }
            final ProductDto dto = ProductDto.builder()
                            .withSKU(multi.getAttribute(CIProducts.ProductAbstract.Name))
                            .withType(getProductType(multi.getCurrentInstance()))
                            .withDescription(multi.getAttribute(CIProducts.ProductAbstract.Description))
                            .withNote(multi.getAttribute(CIProducts.ProductAbstract.Note))
                            .withOID(multi.getCurrentInstance().getOid())
                            .withCategories(prod2cats)
                            .withNetPrice(calculator.getNetUnitPrice())
                            .withCrossPrice(calculator.getCrossUnitPrice())
                            .withCurrency(currency)
                            .withTaxes(taxes)
                            .withImageOid(imageOid)
                            .withUoM(uoM.getSymbol())
                            .withUoMCode(uoM.getCommonCode())
                            .withRelations(relations)
                            .withIndicationSets(getIndicationSets(multi, selIndication))
                            .withBarcodes(barcodes)
                            .withBomGroupConfigs(bomGroupConfigs)
                            .withConfigurationBOMs(configurationBOMs)
                            .build();
            LOG.debug("Product {}", dto);
            products.add(dto);
        }

        final Response ret = Response.ok()
                        .entity(products)
                        .build();
        return ret;
    }

    protected ProductType getProductType(final Instance _instance)
    {
        ProductType ret;
        if (InstanceUtils.isType(_instance, CIProducts.ProductStandart)) {
            ret = ProductType.STANDART;
        } else if (InstanceUtils.isType(_instance, CIProducts.ProductService)) {
            ret = ProductType.SERVICE;
        } else if (InstanceUtils.isType(_instance, CIProducts.ProductTextPosition)) {
            ret = ProductType.TEXT;
        } else if (InstanceUtils.isType(_instance, CIProducts.ProductSalesPartList)) {
            ret = ProductType.PARTLIST;
        } else {
            ret = ProductType.OTHER;
        }
        return ret;
    }

    @SuppressWarnings("unchecked")
    protected Set<IndicationSetDto> getIndicationSets(final MultiPrintQuery _multi,
                                                      final SelectBuilder _selIndication)

    {
        final Set<IndicationSetDto> ret = new HashSet<>();
        try {
            if (Pos.INDICATIONSET_ACIVATE.get()) {
                final Object indicationSets = _multi.getSelect(_selIndication);
                LOG.trace("Loading indication Sets {}", indicationSets);
                if (indicationSets != null) {
                    LOG.trace("Class {}", indicationSets.getClass());
                    List<Instance> indSetInsts = new ArrayList<>();
                    if (indicationSets instanceof List) {
                        indSetInsts = (List<Instance>) indicationSets;
                    } else if (indicationSets instanceof Instance) {
                        indSetInsts.add((Instance) indicationSets);
                    }
                    indSetInsts = indSetInsts.stream().filter(inst -> {
                        return InstanceUtils.isValid(inst);
                    }).collect(Collectors.toList());
                    LOG.trace(" Instances {}", indSetInsts);
                    if (!indSetInsts.isEmpty()) {
                        final MultiPrintQuery setMulti = new MultiPrintQuery(indSetInsts);
                        setMulti.addAttribute(CIPOS.IndicationSet.Name, CIPOS.IndicationSet.Description,
                                        CIPOS.IndicationSet.Required, CIPOS.IndicationSet.Multiple);
                        setMulti.execute();
                        while (setMulti.next()) {
                            LOG.trace(" Instance {}", setMulti.getCurrentInstance());
                            final Set<IndicationDto> indications = new HashSet<>();
                            final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Indication);
                            queryBldr.addWhereAttrEqValue(CIPOS.Indication.IndicationSetLink,
                                            setMulti.getCurrentInstance());
                            final MultiPrintQuery multi = queryBldr.getPrint();
                            multi.addAttribute(CIPOS.Indication.Value, CIPOS.Indication.Description);
                            multi.execute();
                            while (multi.next()) {
                                String imageOid = null;
                                if (Pos.INDICATION_ACIVATEIMAGE.get()) {
                                    final Resource resource = Store
                                                    .get(multi.getCurrentInstance().getType().getStoreId())
                                                    .getResource(multi.getCurrentInstance());
                                    if (resource.exists()) {
                                        imageOid = multi.getCurrentInstance().getOid();
                                    }
                                }

                                indications.add(IndicationDto.builder()
                                                .withOID(multi.getCurrentInstance().getOid())
                                                .withValue(multi.getAttribute(CIPOS.Indication.Value))
                                                .withDescription(multi.getAttribute(CIPOS.Indication.Description))
                                                .withImageOid(imageOid)
                                                .build());
                            }
                            LOG.trace("    indications {}", indications);

                            String imageOid = null;
                            if (Pos.INDICATIONSET_ACIVATEIMAGE.get()) {
                                final Resource resource = Store
                                                .get(setMulti.getCurrentInstance().getType().getStoreId())
                                                .getResource(setMulti.getCurrentInstance());
                                if (resource.exists()) {
                                    imageOid = setMulti.getCurrentInstance().getOid();
                                }
                            }
                            ret.add(IndicationSetDto.builder()
                                            .withOID(setMulti.getCurrentInstance().getOid())
                                            .withName(setMulti.getAttribute(CIPOS.IndicationSet.Name))
                                            .withDescription(setMulti.getAttribute(CIPOS.IndicationSet.Description))
                                            .withRequired(setMulti.getAttribute(CIPOS.IndicationSet.Required))
                                            .withMultiple(setMulti.getAttribute(CIPOS.IndicationSet.Multiple))
                                            .withImageOid(imageOid)
                                            .withIndications(indications)
                                            .build());
                        }
                    }
                }
            }
        } catch (final EFapsException e) {
            LOG.error("Catched error during getIndicationSets", e);
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
