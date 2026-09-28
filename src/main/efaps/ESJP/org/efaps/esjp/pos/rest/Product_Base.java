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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.efaps.admin.datamodel.AttributeSet;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Checkin;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Print;
import org.efaps.eql.builder.Selectables;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.common.history.History;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.pos.listener.IProductProvider;
import org.efaps.esjp.pos.rest.dto.DumpDto;
import org.efaps.esjp.pos.rest.provider.DefaultProductProvider;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.esjp.ui.util.ValueUtils;
import org.efaps.pos.dto.ProductDto;
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

    public static String BARCODE_CACHE = Product.class.getName() + ".Barcode.Cache";

    /**
     * Logging instance used in this class.
     */
    private static final Logger LOG = LoggerFactory.getLogger(Product.class);

    public Product_Base()
    {
        if (!InfinispanCache.get().exists(INDIVIDUAL_CACHE)) {
            InfinispanCache.get().initCache(INDIVIDUAL_CACHE);
        }
        if (!InfinispanCache.get().exists(BARCODE_CACHE)) {
            InfinispanCache.get().initCache(BARCODE_CACHE);
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
                response = Response.ok(evalProducts(identifier, print, false).get(0)).build();
            }
        }
        if (response == null) {
            response = Response.status(Response.Status.NOT_FOUND).build();
        }
        return response;
    }

    public List<ProductDto> findProducts(final String identifier,
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
        return evalProducts(identifier, select, false);
    }

    public Response getProducts(final String identifier,
                                final int limit,
                                final int offset,
                                final OffsetDateTime afterParameter,
                                final String term,
                                final String barcode)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE, ACCESSROLE.MOBILE);
        Response ret;
        if (term == null && barcode == null) {
            ret = Response.ok(getProducts(identifier, limit, offset, afterParameter, afterParameter == null)).build();
        } else {
            ret = Response.ok(findProducts(identifier, term, barcode)).build();
        }
        return ret;
    }

    /**
     * Gets the products.
     *
     * @return the products
     * @throws EFapsException the eFaps exception
     */
    public List<ProductDto> getProducts(final String identifier,
                                        final int limit,
                                        final int offset,
                                        final OffsetDateTime afterParameter,
                                        final boolean caching)
        throws EFapsException
    {
        LOG.debug("Received request for product sync from {}", identifier);
        Print print = null;
        if (afterParameter == null) {
            final var query = EQL.builder().print()
                            .query(CIProducts.ProductAbstract);
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
                query.where().attribute(CIProducts.ProductAbstract.StatusAbstract)
                                .in(CIProducts.ProductStatus.Active, CIProducts.ProductStatus.Draft,
                                                CIProducts.ProductStatus.Unlisted)
                                .up();
            }

            print = query.select().orderBy(CIProducts.ProductAbstract.ID);
            if (limit > 0) {
                print.limit(limit);
            }
            if (offset > 0) {
                print.offset(offset);
            }

        } else {
            final var prodInstanceSet = new History().getLatest(afterParameter, CIProducts.ProductAbstract);
            LOG.info("Found {} altered product instances since {} for {}", prodInstanceSet.size(), afterParameter,
                            identifier);
            if (prodInstanceSet.size() > 0) {
                LOG.debug("Instances: {}", prodInstanceSet);

                final var prodInstances = prodInstanceSet.stream()
                                .sorted((inst0,
                                         inst1) -> Long.valueOf(inst0.getId()).compareTo(inst1.getId()))
                                .toList();

                var toIndex = prodInstances.size();
                if (offset + limit < prodInstances.size()) {
                    toIndex = offset + limit;
                }
                LOG.info("sublist fromIndex: {} - toIndex: {}", offset, toIndex);
                final var page = prodInstances.subList(offset, toIndex);
                LOG.info("Page: {}", page);
                if (page.size() > 0) {
                    print = EQL.builder().print(page.toArray(new Instance[page.size()]));
                }
            }
        }
        return print == null ? Collections.emptyList() : evalProducts(identifier, print, caching);
    }

    protected List<ProductDto> evalProducts(final String identifier,
                                            final Print print,
                                            final boolean caching)
        throws CacheReloadException, EFapsException
    {
        final var listener = Listener.get().<IProductProvider>invoke(IProductProvider.class);
        IProductProvider productProvider;
        if (!listener.isEmpty()) {
            productProvider = listener.get(0);
        } else {
            productProvider = new DefaultProductProvider();
        }
        return productProvider.evalProducts(identifier, print, caching);
    }

    public Return prepareProductDump(final Parameter parameter)
        throws EFapsException
    {
        LOG.info("Preparing dump");
        final var allFiles = new ArrayList<File>();
        final var limit = 1500;
        var next = true;
        var i = 0;
        while (next) {
            final var offset = i * limit;
            LOG.info("- Products Batch {} - {}", offset, offset + limit);
            final var products = getProducts("TODO", limit, offset, null, false);
            i++;
            next = !(products.size() < limit);
            final var fileName = String.format("products_%03d", i);
            LOG.info("Preparing file ");
            final var objectMapper = ValueUtils.getObjectMapper();
            final var jsonFile = new FileUtil().getFile(fileName, "json");
            try {
                objectMapper.writeValue(jsonFile, products);
                LOG.info("Json file: {}", jsonFile);
                allFiles.add(jsonFile);
            } catch (final IOException e) {
                LOG.error("Catched", e);
            }
            Context.save();
        }
        LOG.info("All files: {}", allFiles);

        final var zipFile = new FileUtil().getFile("products", "zip");
        LOG.info("Creating zipfile: {}", zipFile);
        try (ZipArchiveOutputStream archive = new ZipArchiveOutputStream(new FileOutputStream(zipFile))) {
            for (final var file : allFiles) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    final var entry = new ZipArchiveEntry(file, file.getName());
                    archive.putArchiveEntry(entry);
                    IOUtils.copy(fis, archive);
                    archive.closeArchiveEntry();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
            archive.finish();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        Instance dumpInst;
        final var eval = EQL.builder().print().query(CIPOS.ProductDump).select().instance().evaluate();
        if (eval.next()) {
            dumpInst = eval.inst();
            EQL.builder().update(dumpInst).set(CIPOS.ProductDump.UpdatedAt, OffsetDateTime.now()).execute();
        } else {
            dumpInst = EQL.builder().insert(CIPOS.ProductDump)
                            .set(CIPOS.ProductDump.UpdatedAt, OffsetDateTime.now())
                            .execute();
        }
        LOG.info("Checkin for: {}", dumpInst.getOid());
        final var checkin = new Checkin(dumpInst);
        try {
            final var inputStream = new FileInputStream(zipFile);
            checkin.execute(zipFile.getName(), inputStream, Long.valueOf(zipFile.length()).intValue());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        LOG.info("deleting files");
        for (final var file : allFiles) {
            file.delete();
        }
        return new Return();
    }

    public Response getProductDump(@PathParam("identifier") final String _identifier)
        throws EFapsException
    {
        Response ret = null;
        if (Pos.PROD_DUMP_ACTIVATE.get()) {
            final var eval = EQL.builder().print()
                            .query(CIPOS.ProductDump)
                            .select()
                            .attribute(CIPOS.ProductDump.UpdatedAt)
                            .evaluate();
            if (eval.next()) {
                ret = Response.ok().entity(
                                DumpDto.builder()
                                                .withOid(eval.inst().getOid())
                                                .withUpdateAt(eval.get(CIPOS.ProductDump.UpdatedAt))
                                                .build())
                                .build();
            }
        }
        return ret == null ? Response.noContent().build() : ret;
    }
}
