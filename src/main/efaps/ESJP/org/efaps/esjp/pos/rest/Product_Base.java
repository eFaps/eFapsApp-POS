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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.datamodel.AttributeSet;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Dimension.UoM;
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
import org.efaps.eql.builder.Print;
import org.efaps.eql.builder.Selectables;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.common.properties.PropertiesUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.erp.CurrencyInst;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.esjp.products.util.Products;
import org.efaps.esjp.products.util.Products.ProductIndividual;
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
import org.efaps.util.OIDUtil;
import org.efaps.util.cache.CacheReloadException;
import org.efaps.util.cache.InfinispanCache;
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

    public static String INDIVIDUAL_CACHE = Product.class.getName() + ".Individual.Cache";

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Product.class);

    public Product_Base()
    {
        if (!InfinispanCache.get().exists(INDIVIDUAL_CACHE)) {
            InfinispanCache.get().initCache(INDIVIDUAL_CACHE);
        }
    }

    public Response getProduct(final String identifier,
                               final String oid)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE, ACCESSROLE.MOBILE);
        Response response = null;
        if (OIDUtil.isOID(oid)) {
            final var prodInstance = Instance.get(oid);
            if (InstanceUtils.isKindOf(prodInstance, CIProducts.ProductAbstract)) {
                final var print = EQL.builder().print(prodInstance);
                response = Response.ok(evalProducts(identifier, print).get(0)).build();
            }
        }
        if (response == null) {
            response = Response.status(Response.Status.NOT_FOUND).build();
        }
        return response;
    }

    public Response findProducts(final String identifier,
                                 final String term,
                                 final String barcode)
        throws EFapsException
    {
        final var query = EQL.builder().print()
                        .query(CIProducts.ProductAbstract);
        if (barcode != null) {
            final var attrSet = AttributeSet.find(CIProducts.ProductAbstract.getType().getName(),
                            CIProducts.ProductAbstract.Barcodes.name);
            final var barcodeQuery = EQL.builder().nestedQuery(attrSet)
                            .where()
                            .attribute("Code").eq(barcode).up()
                            .selectable(Selectables.attribute("Barcodes"));
            query.where()
                            .attribute(CIProducts.ProductAbstract.ID)
                            .in(barcodeQuery);
        }
        final var select = query.select();
        return Response.ok(evalProducts(identifier, select)).build();
    }

    public Response getProducts(final String identifier,
                                final int limit,
                                final int offset,
                                final OffsetDateTime after,
                                final String term,
                                final String barcode)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE, ACCESSROLE.MOBILE);
        Response ret;
        if (term == null && barcode == null) {
            ret = getProducts(identifier, limit, offset, after);
        } else {
            ret = findProducts(identifier, term, barcode);
        }
        return ret;
    }

    /**
     * Gets the products.
     *
     * @return the products
     * @throws EFapsException the eFaps exception
     */
    public Response getProducts(final String _identifier,
                                final int limit,
                                final int offset,
                                final OffsetDateTime after)
        throws EFapsException
    {

        LOG.debug("Received request for product sync from {}", _identifier);
        final var query = EQL.builder().print()
                        .query(CIProducts.ProductAbstract);

        if (after == null) {
            if (Pos.CATEGORY_ACTIVATE.get() && Pos.CATEGORY_PRODFILTER.get()) {
                final var categoryQuery = EQL.builder().nestedQuery(CIPOS.Category)
                                .where()
                                .attribute(CIPOS.Category.Status).eq(CIPOS.CategoryStatus.Active)
                                .up();
                final var catRelQuery = EQL.builder().nestedQuery(CIPOS.Category2Product)
                                .where()
                                .attribute(CIPOS.Category2Product.FromLink).in(categoryQuery).up()
                                .selectable(Selectables.attribute(CIPOS.Category2Product.ToLink));

                final var textPosQuery = EQL.builder().nestedQuery(CIProducts.ProductTextPosition)
                                .where()
                                .attribute(CIProducts.ProductTextPosition.Active).eq("true").up();
                query.where()
                                .attribute(CIProducts.ProductAbstract.ID)
                                .in(catRelQuery)
                                .or()
                                .attribute(CIProducts.ProductAbstract.ID)
                                .in(textPosQuery);
            } else {
                query.where().attribute(CIProducts.ProductAbstract.Active).eq("true").up();
            }
        } else {
            final var posQuery = EQL.builder().nestedQuery(CIProducts.ProductPricelistPosition)
                            .where()
                            .attribute(CIProducts.ProductPricelistPosition.Modified).greater(String.valueOf(after))
                            .up()
                            .selectable(Selectables.attribute(CIProducts.ProductPricelistPosition.ProductPricelist));

            final var listQuery = EQL.builder().nestedQuery(CIProducts.ProductPricelistAbstract)
                            .where()
                            .attribute(CIProducts.ProductPricelistAbstract.ID).in(posQuery)
                            .up()
                            .selectable(Selectables.attribute(CIProducts.ProductPricelistAbstract.ProductAbstractLink));

            final var listQuery2 = EQL.builder().nestedQuery(CIProducts.ProductPricelistAbstract)
                            .where()
                            .attribute(CIProducts.ProductPricelistAbstract.Modified).greater(String.valueOf(after))
                            .up()
                            .selectable(Selectables.attribute(CIProducts.ProductPricelistAbstract.ProductAbstractLink));

            query.where()
                            .attribute(CIProducts.ProductAbstract.ID).in(listQuery)
                            .or()
                            .attribute(CIProducts.ProductAbstract.ID).in(listQuery2)
                            .or()
                            .attribute(CIProducts.ProductAbstract.Modified).greater(String.valueOf(after));
        }
        final var select = query.select().orderBy(CIProducts.ProductAbstract.ID);

        if (limit > 0) {
            select.limit(limit);
        }
        if (offset > 0) {
            select.offset(offset);
        }

        return Response.ok(evalProducts(_identifier, select)).build();
    }

    @SuppressWarnings("unchecked")
    protected List<ProductDto> evalProducts(final String identifier,
                                            final Print print)
        throws CacheReloadException, EFapsException
    {
        final List<ProductDto> products = new ArrayList<>();

        print.attribute(CIProducts.ProductAbstract.ID, CIProducts.ProductAbstract.Name,
                        CIProducts.ProductAbstract.Description, CIProducts.ProductAbstract.Note,
                        CIProducts.ProductAbstract.DefaultUoM, CIProducts.ProductAbstract.Individual)
                        .linkfrom(CIPOS.Category2Product.ToLink).linkto(CIPOS.Category2Product.FromLink).oid()
                        .as("selCat")
                        .linkfrom(CIPOS.Category2Product.ToLink).attribute(CIPOS.Category2Product.SortWeight)
                        .as("selCatWeight")
                        .linkfrom(CIProducts.Product2ImageThumbnail.ProductLink)
                        .linkto(CIProducts.Product2ImageThumbnail.ImageLink)
                        .oid().as("selImageOid");

        if (Pos.PRODREL.exists()) {
            final Properties properties = Pos.PRODREL.get();
            final Map<Integer, String> relQuantity = PropertiesUtil.analyseProperty(properties,
                            "QuantitySelect", 0);
            final Map<Integer, String> relSelects = PropertiesUtil.analyseProperty(properties, "Select", 0);
            for (final Entry<Integer, String> entry : relSelects.entrySet()) {
                print.select(entry.getValue()).as("relation" + entry.getKey());
            }
            for (final Entry<Integer, String> entry : relQuantity.entrySet()) {
                print.select(entry.getValue()).as("quantity" + entry.getKey());
            }
        }
        final var productEval = print.evaluate();

        while (productEval.next()) {
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
                imageOid = null;
            }

            final Parameter parameter = ParameterUtil.instance();
            ParameterUtil.setParameterValues(parameter, "identifier", identifier);

            final Calculator calculator = new Calculator(parameter, null, productEval.inst(), BigDecimal.ONE,
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
            if (Pos.PRODREL.exists()) {
                final Properties properties = Pos.PRODREL.get();
                PropertiesUtil.analyseProperty(properties, "QuantitySelect",
                                0);
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
            final var barcodes = new HashSet<BarcodeDto>();
            if (Products.STANDART_ACTBARCODES.get()) {
                final var attrSet = AttributeSet.find(CIProducts.ProductAbstract.getType().getName(),
                                CIProducts.ProductAbstract.Barcodes.name);
                // attrSet.getType();
                final QueryBuilder barcodeQueryBldr = new QueryBuilder(attrSet);
                barcodeQueryBldr.addWhereAttrEqValue(attrSet.getAttributeName(), productEval.inst());

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
                final var eval = EQL.builder()
                                .print()
                                .query(CIProducts.BOMGroupConfiguration)
                                .where()
                                .attribute(CIProducts.BOMGroupConfiguration.ProductLink)
                                .eq(productEval.inst())
                                .select()
                                .attribute(CIProducts.BOMGroupConfiguration.Name,
                                                CIProducts.BOMGroupConfiguration.Description,
                                                CIProducts.BOMGroupConfiguration.Weight,
                                                CIProducts.BOMGroupConfiguration.Config)
                                .evaluate();
                while (eval.next()) {
                    final Collection<Products.BOMGroupConfig> flags = eval.get(CIProducts.BOMGroupConfiguration.Config);
                    final var flagsBitValue = flags == null ? 0
                                    : flags.stream().filter(Objects::nonNull)
                                                    .map(Products.BOMGroupConfig::getInt)
                                                    .reduce(0, Integer::sum);

                    bomGroupConfigs.add(BOMGroupConfigDto.builder()
                                    .withOID(eval.inst().getOid())
                                    .withName(eval.get(CIProducts.BOMGroupConfiguration.Name))
                                    .withDescription(eval.get(CIProducts.BOMGroupConfiguration.Description))
                                    .withWeight(eval.get(CIProducts.BOMGroupConfiguration.Weight))
                                    .withFlags(flagsBitValue)
                                    .withProductOid(productEval.inst().getOid())
                                    .build());
                }

                final var eval2 = EQL.builder()
                                .print()
                                .query(CIProducts.ConfigurationBOM)
                                .where()
                                .attribute(CIProducts.ConfigurationBOM.From)
                                .eq(productEval.inst())
                                .select()
                                .attribute(CIProducts.ConfigurationBOM.Position, CIProducts.ConfigurationBOM.Quantity,
                                                CIProducts.ConfigurationBOM.UoM)
                                .linkto(CIProducts.ConfigurationBOM.BOMGroupConfigurationLink)
                                .oid()
                                .as("bomGroupOid")
                                .linkto(CIProducts.ConfigurationBOM.To)
                                .oid()
                                .as("toOid")
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

            final ProductIndividual productIndividual = productEval.get(CIProducts.ProductAbstract.Individual);
            evalIndividual(productEval.inst(), productIndividual, relations);

            final UoM uoM = Dimension.getUoM(productEval.get(CIProducts.ProductAbstract.DefaultUoM));

            final var currencyInst = CurrencyInst.get(calculator.getProductNetUnitPrice().getCurrentCurrencyInstance());
            var currency = EnumUtils.getEnum(org.efaps.pos.dto.Currency.class, currencyInst.getISOCode());
            if (currency == null) {
                currency = org.efaps.pos.dto.Currency.PEN;
            }

            final ProductDto dto = ProductDto.builder()
                            .withSKU(productEval.get(CIProducts.ProductAbstract.Name))
                            .withType(getProductType(productEval.inst()))
                            .withDescription(productEval.get(CIProducts.ProductAbstract.Description))
                            .withNote(productEval.get(CIProducts.ProductAbstract.Note))
                            .withOID(productEval.inst().getOid())
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
                            .withBomGroupConfigs(bomGroupConfigs)
                            .withConfigurationBOMs(configurationBOMs)
                            .withIndividual(EnumUtils.getEnum(org.efaps.pos.dto.ProductIndividual.class,
                                            productIndividual == null ? null : productIndividual.name()))
                            .build();
            LOG.debug("Product {}", dto);
            products.add(dto);
        }
        return products;
    }

    protected void evalIndividual(final Instance prodInst,
                                  final ProductIndividual productIndividual,
                                  final Set<ProductRelationDto> relations)
        throws EFapsException
    {
        if (productIndividual != null && (productIndividual.equals(ProductIndividual.BATCH) ||
                        productIndividual.equals(ProductIndividual.INDIVIDUAL))) {

            final var cache = InfinispanCache.get().<String, Set<ProductRelationDto>>getCache(INDIVIDUAL_CACHE);
            if (cache.containsKey(prodInst.getOid())) {
                relations.addAll(cache.get(prodInst.getOid()));
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
                cache.put(prodInst.getOid(), individuals, 5, TimeUnit.MINUTES);
                relations.addAll(individuals);
            }
        }
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
        } else if (InstanceUtils.isType(_instance, CIProducts.ProductBatch)) {
            ret = ProductType.BATCH;
        } else if (InstanceUtils.isType(_instance, CIProducts.ProductIndividual)) {
            ret = ProductType.INDIVIDUAL;
        } else {
            ret = ProductType.OTHER;
        }
        return ret;
    }

    protected Set<IndicationSetDto> getIndicationSets(final Instance productInstance)
    {
        final Set<IndicationSetDto> ret = new HashSet<>();
        try {
            if (Pos.INDICATIONSET_ACIVATE.get()) {
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
