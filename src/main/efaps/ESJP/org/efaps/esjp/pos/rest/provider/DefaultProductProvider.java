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
package org.efaps.esjp.pos.rest.provider;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.efaps.admin.datamodel.AttributeSet;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkout;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.store.Resource;
import org.efaps.db.store.Store;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Print;
import org.efaps.eql2.StmtFlag;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.pos.listener.IProductProvider;
import org.efaps.esjp.pos.rest.Product;
import org.efaps.esjp.pos.util.DocumentUtils;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.ICalculatorConfig;
import org.efaps.pos.dto.BOMActionDto;
import org.efaps.pos.dto.BOMActionType;
import org.efaps.pos.dto.BOMGroupConfigDto;
import org.efaps.pos.dto.BarcodeDto;
import org.efaps.pos.dto.ConfigurationBOMDto;
import org.efaps.pos.dto.IndicationDto;
import org.efaps.pos.dto.IndicationSetDto;
import org.efaps.pos.dto.Product2CategoryDto;
import org.efaps.pos.dto.ProductDto;
import org.efaps.pos.dto.ProductRelationDto;
import org.efaps.pos.dto.ProductRelationType;
import org.efaps.pos.dto.ProductStatus;
import org.efaps.pos.dto.ProductType;
import org.efaps.pos.dto.TaxDto;
import org.efaps.util.EFapsException;
import org.efaps.util.cache.CacheReloadException;
import org.efaps.util.cache.InfinispanCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("d060a73e-b633-485a-a828-1fe0e644b3ca")
@EFapsApplication("eFapsApp-POS")
public class DefaultProductProvider
    implements IProductProvider
{

    private static final Logger LOG = LoggerFactory.getLogger(Product.class);

    @SuppressWarnings("unchecked")
    @Override
    public List<ProductDto> evalProducts(final String identifier,
                                         final Print print,
                                         final boolean caching)
        throws EFapsException
    {
        final List<ProductDto> products = new ArrayList<>();

        print.attribute(CIProducts.ProductAbstract.ID, CIProducts.ProductAbstract.Name,
                        CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.Note,
                        CIProducts.ProductAbstract.DefaultUoM, CIProducts.ProductAbstract.Individual,
                        CIProducts.ProductAbstract.StatusAbstract)
                        .linkfrom(CIPOS.Category2Product.ToLink).linkto(CIPOS.Category2Product.FromLink).oid()
                        .as("selCat")
                        .linkfrom(CIPOS.Category2Product.ToLink).attribute(CIPOS.Category2Product.SortWeight)
                        .as("selCatWeight")
                        .linkfrom(CIProducts.Product2ImageAbstract.ProductAbstractLink)
                        .linkto(CIProducts.Product2ImageAbstract.ImageAbstractLink)
                        .oid().as("selImageOid");

        if (Pos.PROD_SEL.exists()) {
            final var properties = Pos.PROD_SEL.get();
            final var selects = PropertiesUtil.analyseProperty(properties, "Select", 0);
            PropertiesUtil.analyseProperty(properties, "Key", 0);
            for (final var entry : selects.entrySet()) {
                print.select(entry.getValue()).as("PRODSEL_" + entry.getKey());
            }
        }

        if (Pos.PROD_REL.exists()) {
            final Properties properties = Pos.PROD_REL.get();
            final var relQuantity = PropertiesUtil.analyseProperty(properties, "QuantitySelect", 0);
            final var relSelects = PropertiesUtil.analyseProperty(properties, "Select", 0);
            for (final Entry<Integer, String> entry : relSelects.entrySet()) {
                print.select(entry.getValue()).as("relation" + entry.getKey());
            }
            for (final Entry<Integer, String> entry : relQuantity.entrySet()) {
                print.select(entry.getValue()).as("quantity" + entry.getKey());
            }
        }
        final var productEval = print.evaluate();

        while (productEval.next()) {
            final var productType = getProductType(productEval.inst());

            final Object cats = productEval.get("selCat");
            final Object catWeights = productEval.get("selCatWeight");
            final Set<Product2CategoryDto> prod2cats = new HashSet<>();
            if (cats instanceof List) {
                final var weightIter = ((Collection<? extends Integer>) catWeights).iterator();
                for (final String element : (Collection<? extends String>) cats) {
                    if (element != null) {
                        prod2cats.add(Product2CategoryDto.builder().withCategoryOid(element)
                                        .withWeight(weightIter.next())
                                        .build());
                    }
                }
            } else if (cats instanceof String) {
                prod2cats.add(Product2CategoryDto.builder().withCategoryOid((String) cats)
                                .withWeight((Integer) catWeights).build());
            }
            final Object imageOids = productEval.get("selImageOid");
            final String imageOid;
            if (imageOids instanceof List) {
                imageOid = (String) ((List<?>) imageOids).get(0);
            } else if (imageOids instanceof String) {
                imageOid = (String) imageOids;
            } else {
                imageOid = switch (productType) {
                    case PARTLIST -> {
                        if (new Checkout(productEval.inst().getOid()).exists()) {
                            yield productEval.inst().getOid();
                        }
                        yield null;
                    }
                    default -> null;
                };
            }

            final Parameter parameter = ParameterUtil.instance();
            ParameterUtil.setParameterValues(parameter, "identifier", identifier);

            final Calculator calculator = new Calculator(parameter, null, productEval.inst(), BigDecimal.ONE,
                            null, BigDecimal.ZERO, true, getCalcConf());

            final Set<TaxDto> taxes = new HashSet<>();
            calculator.getTaxes().forEach(tax -> {
                try {
                    taxes.add(DocumentUtils.getDtoTax(tax));
                } catch (final EFapsException e) {
                    LOG.error("Catched", e);
                }
            });

            Map<String, Object> extension = null;
            if (Pos.PROD_SEL.exists()) {
                extension = new HashMap<>();
                final var properties = Pos.PROD_SEL.get();
                final var keyMap = PropertiesUtil.analyseProperty(properties, "Key", 0);
                for (final var entry : keyMap.entrySet()) {
                    final var value = productEval.get("PRODSEL_" + entry.getKey());
                    if (value != null) {
                        extension.put(entry.getValue(), value);
                    }
                }
                LOG.debug("extension {}", extension);
            }

            final Set<ProductRelationDto> relations = new HashSet<>();
            if (Pos.PROD_REL.exists()) {
                final Properties properties = Pos.PROD_REL.get();
                final Map<Integer, String> relSelects = PropertiesUtil.analyseProperty(properties, "Select", 0);
                final Map<Integer, String> relLabels = PropertiesUtil.analyseProperty(properties, "Label", 0);
                final Map<Integer, String> relTypes = PropertiesUtil.analyseProperty(properties, "RelationType", 0);

                for (final Entry<Integer, String> entry : relSelects.entrySet()) {
                    final String label = relLabels.get(entry.getKey());
                    final String relType = relTypes.get(entry.getKey());
                    final Object relation = productEval.get("relation" + entry.getKey());
                    Object quantity = productEval.get("quantity" + entry.getKey());

                    if (relation instanceof List) {
                        if (quantity == null) {
                            quantity = Arrays.asList(new Object[((List<?>) relation).size()]);
                        }
                        final var quantities = ((List<? extends BigDecimal>) quantity).iterator();
                        ((Collection<? extends String>) relation).forEach(oid -> {
                            if (oid != null) {
                                relations.add(ProductRelationDto.builder()
                                                .withLabel(label)
                                                .withQuantity(quantities.hasNext() ? quantities.next() : null)
                                                .withProductOid(oid)
                                                .withType(ProductRelationType.valueOf(relType))
                                                .build());
                            }
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
            }

            final var configBomPair = evalConfigurationBOM(productEval.inst());

            final var barcodes = evalBarcodes(productEval.inst(), caching);

            final ProductIndividual productIndividual = productEval.get(CIProducts.ProductAbstract.Individual);
            evalIndividual(productEval.inst(), productIndividual, relations, caching);

            final UoM uoM = Dimension.getUoM(productEval.get(CIProducts.ProductAbstract.DefaultUoM));

            final var currencyInst = CurrencyInst.get(calculator.getProductNetUnitPrice().getCurrentCurrencyInstance());
            var currency = EnumUtils.getEnum(org.efaps.pos.dto.Currency.class, currencyInst.getISOCode());
            if (currency == null) {
                currency = org.efaps.pos.dto.Currency.PEN;
            }

            final ProductDto dto = ProductDto.builder()
                            .withSku(productEval.get(CIProducts.ProductAbstract.Name))
                            .withType(productType)
                            .withDescription(productEval.get(CIProducts.ProductAbstract.Description))
                            .withNote(productEval.get(CIProducts.ProductAbstract.Note))
                            .withOid(productEval.inst().getOid())
                            .withCategories(prod2cats)
                            .withNetPrice(calculator.getNetUnitPrice())
                            .withCrossPrice(calculator.getCrossUnitPrice())
                            .withCurrency(currency)
                            .withTaxes(taxes)
                            .withImageOid(imageOid)
                            .withUoM(uoM.getSymbol())
                            .withUoMCode(uoM.getCommonCode())
                            .withRelations(relations)
                            .withIndicationSets(getIndicationSets(productEval.inst()))
                            .withBarcodes(barcodes)
                            .withBomGroupConfigs(configBomPair.getLeft())
                            .withConfigurationBOMs(configBomPair.getRight())
                            .withIndividual(EnumUtils.getEnum(org.efaps.pos.dto.ProductIndividual.class,
                                            productIndividual == null ? null : productIndividual.name()))
                            .withStatus(evalStatus(productEval.get(CIProducts.ProductAbstract.StatusAbstract),
                                            productEval.inst().getOid()))
                            .withExtension(extension)
                            .build();
            LOG.debug("Product {}", dto);
            products.add(dto);
        }
        return products;
    }

    protected ProductType getProductType(final Instance instance)
    {
        ProductType ret;
        if (InstanceUtils.isType(instance, CIProducts.ProductStandart)) {
            ret = ProductType.STANDART;
        } else if (InstanceUtils.isType(instance, CIProducts.ProductService)) {
            ret = ProductType.SERVICE;
        } else if (InstanceUtils.isType(instance, CIProducts.ProductTextPosition)) {
            ret = ProductType.TEXT;
        } else if (InstanceUtils.isType(instance, CIProducts.ProductSalesPartList)) {
            ret = ProductType.PARTLIST;
        } else if (InstanceUtils.isType(instance, CIProducts.ProductBatch)) {
            ret = ProductType.BATCH;
        } else if (InstanceUtils.isType(instance, CIProducts.ProductIndividual)) {
            ret = ProductType.INDIVIDUAL;
        } else {
            ret = ProductType.OTHER;
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
                return true;
            }
        };
    }

    protected Pair<Set<BOMGroupConfigDto>, Set<ConfigurationBOMDto>> evalConfigurationBOM(final Instance producInst)
        throws EFapsException
    {
        final var bomGroupConfigs = new HashSet<BOMGroupConfigDto>();
        final var configurationBOMs = new HashSet<ConfigurationBOMDto>();
        if (Products.STANDART_ACTCONFBOM.get() || Products.SALESPARTLIST_ACTCONFBOM.get()) {
            final var groupConfigEval = EQL.builder()
                            .print()
                            .query(CIProducts.BOMGroupConfiguration)
                            .where()
                            .attribute(CIProducts.BOMGroupConfiguration.ProductLink)
                            .eq(producInst)
                            .select()
                            .attribute(CIProducts.BOMGroupConfiguration.Name,
                                            CIProducts.BOMGroupConfiguration.Description,
                                            CIProducts.BOMGroupConfiguration.Weight,
                                            CIProducts.BOMGroupConfiguration.Config,
                                            CIProducts.BOMGroupConfiguration.MaxQuantity,
                                            CIProducts.BOMGroupConfiguration.MinQuantity)
                            .evaluate();
            while (groupConfigEval.next()) {
                final Collection<Products.BOMGroupConfig> flags = groupConfigEval
                                .get(CIProducts.BOMGroupConfiguration.Config);
                final var flagsBitValue = flags == null ? 0
                                : flags.stream().filter(Objects::nonNull)
                                                .map(Products.BOMGroupConfig::getInt)
                                                .reduce(0, Integer::sum);

                bomGroupConfigs.add(BOMGroupConfigDto.builder()
                                .withOid(groupConfigEval.inst().getOid())
                                .withName(groupConfigEval.get(CIProducts.BOMGroupConfiguration.Name))
                                .withDescription(groupConfigEval.get(CIProducts.BOMGroupConfiguration.Description))
                                .withWeight(groupConfigEval.get(CIProducts.BOMGroupConfiguration.Weight))
                                .withFlags(flagsBitValue)
                                .withMaxQuantity(groupConfigEval.get(CIProducts.BOMGroupConfiguration.MaxQuantity))
                                .withMinQuantity(groupConfigEval.get(CIProducts.BOMGroupConfiguration.MinQuantity))
                                .withProductOid(producInst.getOid())
                                .build());
            }

            final var configBomEval = EQL.builder()
                            .print()
                            .query(CIProducts.ConfigurationBOM)
                            .where()
                            .attribute(CIProducts.ConfigurationBOM.From).eq(producInst)
                            .select()
                            .attribute(CIProducts.ConfigurationBOM.Position, CIProducts.ConfigurationBOM.Quantity,
                                            CIProducts.ConfigurationBOM.UoM, CIProducts.ConfigurationBOM.Flags)
                            .linkto(CIProducts.ConfigurationBOM.BOMGroupConfigurationLink).oid().as("bomGroupOid")
                            .linkto(CIProducts.ConfigurationBOM.To).oid().as("toOid")
                            .evaluate();
            while (configBomEval.next()) {
                LOG.debug("Found Configuration BOM: {}", configBomEval.inst().getOid());

                final var uoMId = configBomEval.<Long>get(CIProducts.ConfigurationBOM.UoM);
                final String uoM = uoMId == null ? null : Dimension.getUoM(uoMId).getCommonCode();

                final Collection<Products.ConfigurationBOMFlag> flags = configBomEval
                                .get(CIProducts.ConfigurationBOM.Flags);
                final var flagsBitValue = flags == null ? 0
                                : flags.stream().filter(Objects::nonNull)
                                                .map(Products.ConfigurationBOMFlag::getInt)
                                                .reduce(0, Integer::sum);

                final List<BOMActionDto> actions = new ArrayList<>();
                final var actionEval = EQL.builder()
                                .print()
                                .query(CIProducts.ConfigurationBOMActionAbstract)
                                .where()
                                .attribute(CIProducts.ConfigurationBOMActionAbstract.ConfigurationBOMLink)
                                .eq(configBomEval.inst())
                                .select()
                                .attribute(CIProducts.ConfigurationBOMActionAbstract.Dec1)
                                .evaluate();
                while (actionEval.next()) {
                    final var actionInst = actionEval.inst();
                    LOG.debug("Found Action : {}", actionInst.getOid());
                    if (InstanceUtils.isType(actionInst, CIProducts.ConfigurationBOMPriceAdjustmentAction)) {
                        final BigDecimal netAmount = actionEval.get(CIProducts.ConfigurationBOMActionAbstract.Dec1);
                        final Calculator calculator = new Calculator(ParameterUtil.instance(), null, producInst,
                                        BigDecimal.ONE, netAmount, BigDecimal.ZERO, false, getCalcConf());
                        actions.add(BOMActionDto.builder()
                                        .withType(BOMActionType.PRICEADJUSTMENT)
                                        .withNetAmount(netAmount)
                                        .withCrossAmount(calculator.getCrossUnitPrice())
                                        .build());
                    }
                }
                configurationBOMs.add(ConfigurationBOMDto.builder()
                                .withOid(configBomEval.inst().getOid())
                                .withToProductOid(configBomEval.get("toOid"))
                                .withPosition(configBomEval.get(CIProducts.ConfigurationBOM.Position))
                                .withQuantity(configBomEval.get(CIProducts.ConfigurationBOM.Quantity))
                                .withBomGroupOid(configBomEval.get("bomGroupOid"))
                                .withFlags(flagsBitValue)
                                .withActions(actions)
                                .withUoM(uoM)
                                .build());
            }
        }
        return Pair.of(bomGroupConfigs, configurationBOMs);
    }

    protected Set<BarcodeDto> evalBarcodes(final Instance prodInst,
                                           final boolean caching)
        throws EFapsException
    {
        Set<BarcodeDto> barcodes;
        if (Products.STANDART_ACTBARCODES.get()) {
            if (caching) {
                final var cache = InfinispanCache.get().<String, Set<BarcodeDto>>getCache(Product.BARCODE_CACHE);
                if (cache.isEmpty()) {
                    LOG.debug("Loading all barcodes into cache");
                    final var attrSet = AttributeSet.find(CIProducts.ProductAbstract.getType().getName(),
                                    CIProducts.ProductAbstract.Barcodes.name);
                    final var barcodeEval = EQL.builder()
                                    .with(StmtFlag.TRIGGEROFF)
                                    .print()
                                    .query(attrSet.getUUID().toString())
                                    .select()
                                    .attribute("Code").as("Code")
                                    .linkto(attrSet.getAttributeName()).oid().as("prodOid")
                                    .linkto("BarcodeType").attribute("Value").as("BarcodeType")
                                    .evaluate();
                    while (barcodeEval.next()) {
                        final String prodOid = barcodeEval.get("prodOid");
                        if (prodOid != null) {
                            if (!cache.containsKey(prodOid)) {
                                cache.put(prodOid, new HashSet<>(), 30, TimeUnit.MINUTES);
                            }
                            final var currentBarCodes = cache.get(prodOid);
                            currentBarCodes.add(BarcodeDto.builder()
                                            .withCode(barcodeEval.get("Code"))
                                            .withType(barcodeEval.get("BarcodeType"))
                                            .build());
                            cache.put(prodOid, currentBarCodes, 30, TimeUnit.MINUTES);
                        }
                    }
                }
                barcodes = cache.get(prodInst.getOid());
            } else {
                barcodes = new HashSet<>();
                final var attrSet = AttributeSet.find(CIProducts.ProductAbstract.getType().getName(),
                                CIProducts.ProductAbstract.Barcodes.name);
                final var barcodeEval = EQL.builder()
                                .with(StmtFlag.TRIGGEROFF)
                                .print()
                                .query(attrSet.getUUID().toString())
                                .where()
                                .attribute(attrSet.getAttributeName()).eq(prodInst)
                                .select()
                                .attribute("Code").as("Code")
                                .linkto("BarcodeType").attribute("Value").as("BarcodeType")
                                .evaluate();
                while (barcodeEval.next()) {
                    barcodes.add(BarcodeDto.builder()
                                    .withCode(barcodeEval.get("Code"))
                                    .withType(barcodeEval.get("BarcodeType"))
                                    .build());
                }
            }

        } else {
            barcodes = new HashSet<>();
        }
        return barcodes;
    }

    protected void evalIndividual(final Instance prodInst,
                                  final ProductIndividual productIndividual,
                                  final Set<ProductRelationDto> relations,
                                  final boolean caching)
        throws EFapsException
    {
        if (productIndividual != null && (productIndividual.equals(ProductIndividual.BATCH) ||
                        productIndividual.equals(ProductIndividual.INDIVIDUAL))) {

            if (caching) {
                LOG.debug("using cache for individual");
                final var cache = InfinispanCache.get()
                                .<String, Set<ProductRelationDto>>getCache(Product.INDIVIDUAL_CACHE);
                if (cache.isEmpty()) {
                    final var eval = EQL.builder()
                                    .print()
                                    .query(CIProducts.StoreableProductAbstract2IndividualAbstract)
                                    .select()
                                    .linkto(CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract).oid()
                                    .as("productOid")
                                    .linkto(CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract).oid()
                                    .as("individualOid")
                                    .evaluate();

                    while (eval.next()) {
                        final String productOid = eval.get("productOid");
                        final String individualOid = eval.get("individualOid");
                        if (!cache.containsKey(productOid)) {
                            cache.put(productOid, new HashSet<>(), 30, TimeUnit.MINUTES);
                        }
                        if (!cache.containsKey(individualOid)) {
                            cache.put(individualOid, new HashSet<>(), 30, TimeUnit.MINUTES);
                        }

                        final var current4productOid = cache.get(productOid);
                        current4productOid.add(ProductRelationDto.builder()
                                        .withProductOid(individualOid)
                                        .withType(ProductRelationType.BATCH)
                                        .build());
                        cache.put(productOid, current4productOid, 30, TimeUnit.MINUTES);

                        final var current4individualOid = cache.get(individualOid);
                        current4individualOid.add(ProductRelationDto.builder()
                                        .withProductOid(productOid)
                                        .withType(ProductRelationType.BATCH)
                                        .build());
                        cache.put(individualOid, current4individualOid, 30, TimeUnit.MINUTES);

                        LOG.info("Updated entries for productOid {} with {}", productOid, current4productOid);
                        LOG.info("Updated entries for individualOid {} with {}", individualOid, current4individualOid);
                    }
                }
                if (cache.containsKey(prodInst.getOid())) {
                    relations.addAll(cache.get(prodInst.getOid()));
                }
            } else {
                final Set<ProductRelationDto> individuals = new HashSet<>();
                if (InstanceUtils.isKindOf(prodInst, CIProducts.ProductIndividualAbstract)) {
                    final var eval = EQL.builder()
                                    .print()
                                    .query(CIProducts.StoreableProductAbstract2IndividualAbstract)
                                    .where()
                                    .attribute(CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract)
                                    .eq(prodInst)
                                    .select()
                                    .linkto(CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract)
                                    .oid()
                                    .as("parentOid")
                                    .evaluate();
                    while (eval.next()) {
                        individuals.add(ProductRelationDto.builder()
                                        .withProductOid(eval.get("parentOid"))
                                        .withType(ProductRelationType.BATCH)
                                        .build());
                    }
                } else {
                    final var eval = EQL.builder()
                                    .print()
                                    .query(CIProducts.StoreableProductAbstract2IndividualAbstract)
                                    .where()
                                    .attribute(CIProducts.StoreableProductAbstract2IndividualAbstract.FromAbstract)
                                    .eq(prodInst)
                                    .select()
                                    .linkto(CIProducts.StoreableProductAbstract2IndividualAbstract.ToAbstract)
                                    .oid()
                                    .as("individualOid")
                                    .evaluate();
                    while (eval.next()) {
                        individuals.add(ProductRelationDto.builder()
                                        .withProductOid(eval.get("individualOid"))
                                        .withType(ProductRelationType.BATCH)
                                        .build());
                    }
                }
                relations.addAll(individuals);
            }
        }
    }

    protected ProductStatus evalStatus(final Long statusId,
                                       final String oid)
        throws CacheReloadException
    {
        if (statusId == null) {
            LOG.warn("Product has no status id: {}", oid);
            return ProductStatus.ACTIVE;
        }
        final ProductStatus status;
        final var dbStatus = Status.get(statusId);
        if (CIProducts.ProductStatus.Active.key.equals(dbStatus.getKey())) {
            status = ProductStatus.ACTIVE;
        } else if (CIProducts.ProductStatus.Archived.key.equals(dbStatus.getKey())) {
            status = ProductStatus.ARCHIVED;
        } else if (CIProducts.ProductStatus.Draft.key.equals(dbStatus.getKey())) {
            status = ProductStatus.DRAFT;
        } else if (CIProducts.ProductStatus.Unlisted.key.equals(dbStatus.getKey())) {
            status = ProductStatus.UNLISTED;
        } else {
            status = ProductStatus.ACTIVE;
            LOG.warn("Product without valid status id: {}", oid);
        }
        return status;
    }

    protected Set<IndicationSetDto> getIndicationSets(final Instance productInstance)
    {
        final Set<IndicationSetDto> ret = new HashSet<>();
        try {
            if (Pos.INDICATIONSET_ACTIVATE.get()) {
                final QueryBuilder attrQueryBldr = new QueryBuilder(CIPOS.IndicationSet2Product);
                attrQueryBldr.addWhereAttrEqValue(CIPOS.IndicationSet2Product.ToLink, productInstance);
                final QueryBuilder isQueryBldr = new QueryBuilder(CIPOS.IndicationSet);
                isQueryBldr.addWhereAttrInQuery(CIPOS.IndicationSet.ID,
                                attrQueryBldr.getAttributeQuery(CIPOS.IndicationSet2Product.FromLink));

                final MultiPrintQuery setMulti = isQueryBldr.getPrint();
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
                    multi.addAttribute(CIPOS.Indication.Value, CIPOS.Indication.Description,
                                    CIPOS.Indication.DefaultSelected, CIPOS.Indication.Weight);
                    multi.execute();
                    while (multi.next()) {
                        String imageOid = null;
                        if (Pos.INDICATION_ACTIVATEIMAGE.get()) {
                            final Resource resource = Store
                                            .get(multi.getCurrentInstance().getType().getStoreId())
                                            .getResource(multi.getCurrentInstance());
                            if (resource.exists()) {
                                imageOid = multi.getCurrentInstance().getOid();
                            }
                        }

                        indications.add(IndicationDto.builder()
                                        .withOid(multi.getCurrentInstance().getOid())
                                        .withValue(multi.getAttribute(CIPOS.Indication.Value))
                                        .withDescription(multi.getAttribute(CIPOS.Indication.Description))
                                        .withImageOid(imageOid)
                                        .withDefaultSelected(
                                                        BooleanUtils.toBoolean(multi.<Boolean>getAttribute(
                                                                        CIPOS.Indication.DefaultSelected)))
                                        .withWeight(multi.getAttribute(CIPOS.Indication.Weight))
                                        .build());
                    }
                    LOG.trace("    indications {}", indications);

                    String imageOid = null;
                    if (Pos.INDICATIONSET_ACTIVATEIMAGE.get()) {
                        final Resource resource = Store
                                        .get(setMulti.getCurrentInstance().getType().getStoreId())
                                        .getResource(setMulti.getCurrentInstance());
                        if (resource.exists()) {
                            imageOid = setMulti.getCurrentInstance().getOid();
                        }
                    }
                    ret.add(IndicationSetDto.builder()
                                    .withOid(setMulti.getCurrentInstance().getOid())
                                    .withName(setMulti.getAttribute(CIPOS.IndicationSet.Name))
                                    .withDescription(setMulti.getAttribute(CIPOS.IndicationSet.Description))
                                    .withRequired(setMulti.getAttribute(CIPOS.IndicationSet.Required))
                                    .withMultiple(setMulti.getAttribute(CIPOS.IndicationSet.Multiple))
                                    .withImageOid(imageOid)
                                    .withIndications(indications)
                                    .build());
                }

            }
        } catch (final EFapsException e) {
            LOG.error("Catched error during getIndicationSets", e);
        }
        return ret;
    }

}
