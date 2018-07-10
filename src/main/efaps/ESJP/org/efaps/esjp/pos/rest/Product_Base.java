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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.ci.CISales;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.sales.Calculator;
import org.efaps.esjp.sales.ICalculatorConfig;
import org.efaps.pos.dto.ProductDto;
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
    public Response getProducts()
        throws EFapsException
    {
        final List<ProductDto> products = new ArrayList<>();
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIPOS.Category);
        attrQueryBldr.addWhereAttrEqValue(CIPOS.Category.Status, Status.find(CIPOS.CategoryStatus.Active));

        final QueryBuilder relAttrQueryBldr = new QueryBuilder(CIPOS.Category2Product);
        relAttrQueryBldr.addWhereAttrInQuery(CIPOS.Category2Product.FromLink,
                        attrQueryBldr.getAttributeQuery(CIPOS.Category.ID));

        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
        queryBldr.addWhereAttrInQuery(CIProducts.ProductAbstract.ID,
                        relAttrQueryBldr.getAttributeQuery(CIPOS.Category2Product.ToLink));
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selCat = SelectBuilder.get()
                        .linkfrom(CIPOS.Category2Product.ToLink)
                        .linkto(CIPOS.Category2Product.FromLink)
                        .oid();
        final SelectBuilder selImageOid = SelectBuilder.get()
                        .linkfrom(CIProducts.Product2ImageThumbnail.ProductLink)
                        .linkto(CIProducts.Product2ImageThumbnail.ImageLink)
                        .oid();
        multi.addSelect(selCat, selImageOid);
        multi.addAttribute(CIProducts.ProductAbstract.Name,
                        CIProducts.ProductAbstract.Description);
        multi.execute();
        while (multi.next()) {
            final Object cats = multi.getSelect(selCat);
            final Set<String> catOids = new HashSet<>();
            if (cats instanceof List) {
                catOids.addAll((Collection<? extends String>) cats);
            } else if (cats instanceof String) {
                catOids.add((String) cats);
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
                                    .withName(tax.getName())
                                    .withPercent(tax.getFactor().multiply(BigDecimal.valueOf(100)))
                                    .build());
                } catch (final EFapsException e) {
                    LOG.error("Catched", e);
                }
            });

            final ProductDto dto = ProductDto.builder()
                .withSKU(multi.getAttribute(CIProducts.ProductAbstract.Name))
                .withDescription(multi.getAttribute(CIProducts.ProductAbstract.Description))
                .withOID(multi.getCurrentInstance().getOid())
                .withCategoryOids(catOids)
                .withNetPrice(calculator.getNetUnitPrice())
                .withCrossPrice(calculator.getCrossUnitPrice())
                .withTaxes(taxes)
                .withImageOid(imageOid)
                .build();
            products.add(dto);
        }

        final Response ret = Response.ok()
                        .entity(products)
                        .build();
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
