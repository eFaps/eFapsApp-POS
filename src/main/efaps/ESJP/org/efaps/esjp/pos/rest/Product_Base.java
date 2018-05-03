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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.pos.dto.ProductDto;
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
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selCat = SelectBuilder.get()
                        .linkfrom(CIPOS.Category2Product.ToLink)
                        .linkto(CIPOS.Category2Product.FromLink)
                        .oid();
        multi.addSelect(selCat);
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
            final ProductDto dto = ProductDto.builder()
                .withSKU(multi.getAttribute(CIProducts.ProductAbstract.Name))
                .withDescription(multi.getAttribute(CIProducts.ProductAbstract.Description))
                .withOID(multi.getCurrentInstance().getOid())
                .withCategoryOids(catOids)
                .build();
            products.add(dto);
        }

        final Response ret = Response.ok()
                        .entity(products)
                        .build();
        return ret;
    }
}
