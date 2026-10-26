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
package org.efaps.esjp.pos.graphql;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsListener;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Checkin;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.listener.ISignedUrl;
import org.efaps.esjp.common.serialization.SerializationUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.graphql.AbstractFileMutation;
import org.efaps.esjp.graphql.Caching;
import org.efaps.util.EFapsException;
import org.efaps.util.RandomUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;

import graphql.schema.DataFetchingEnvironment;

@EFapsUUID("927c0e65-3b6d-436b-99f4-04c5dd08e8f1")
@EFapsApplication("eFapsApp-POS")
@EFapsListener
public class PosFileMutation
    extends AbstractFileMutation
    implements ISignedUrl
{

    private static final Logger LOG = LoggerFactory.getLogger(PosFileMutation.class);

    @Override
    public Object get(final DataFetchingEnvironment environment)
        throws Exception
    {
        final var props = getProperties(environment);
        final var values = evalArgumentValues(environment, props);

        final var refKey = RandomUtil.randomAlphanumeric(8);
        final var value = SerializationUtil.getObjectMapper().writeValueAsString(values);
        LOG.info("caching: {} - {}", refKey, value);
        Caching.getUtilCache().put(refKey, value, 30, TimeUnit.MINUTES);
        return evalUrl(refKey);
    }

    @Override
    public int getWeight()
    {
        return 0;
    }

    @Override
    public boolean applies(String reference)
    {
        final var applies = Caching.getUtilCache().containsKey(reference);
        LOG.info("checking for reference: {} -> {}", reference, applies);
        return applies;
    }

    @Override
    public Object onUpload(final File file,
                           final String reference)
    {
        String oid = null;
        LOG.info("preparing for file - reference: {} -> {}", file, reference);
        final var mapStr = Caching.getUtilCache().get(reference);
        LOG.info("mapstr: {}", mapStr);
        try {
            final var map = SerializationUtil.getObjectMapper().readValue(mapStr,
                            new TypeReference<Map<String, Object>>()
                            {
                            });
            LOG.info("map: {}", map);
            final var name = (String) map.get("name");
            if (StringUtils.isEmpty(name)) {
                LOG.error("Missing required value for 'name'");
            } else {

                final var description = (String) map.get("description");
                final var inst = EQL.builder().insert(evalType(map))
                                .set(CIPOS.FileAbstract.Name, name)
                                .set(CIPOS.FileAbstract.Description, description)
                                .execute();

                final var checkin = new Checkin(inst);
                final var inputStream = new FileInputStream(file);
                checkin.execute(file.getName(), inputStream, Long.valueOf(file.length()).intValue());
                oid = inst.getOid();

                if (map.containsKey("productOid") || map.containsKey("productRel")) {
                    final Object productObject = map.getOrDefault("productOid", map.get("productRel"));
                    if (productObject instanceof final Collection prodObjectList) {
                        for (final var entry : prodObjectList) {
                            connect(inst, entry);
                        }
                    } else {
                        connect(inst, productObject);
                    }
                }
            }
            Caching.getUtilCache().remove(reference);
        } catch (final JsonProcessingException | EFapsException | FileNotFoundException e) {
            LOG.error("catched", e);
        }
        return oid;
    }

    private CIType evalType(final Map<String, Object> map)
    {
        return map.containsKey("productOid") || map.containsKey("productRel") ? CIPOS.FileProduct : CIPOS.File;
    }

    @SuppressWarnings("unchecked")
    private void connect(final Instance fileInst,
                         final Object productObject)
        throws EFapsException
    {
        LOG.info("Connecting fileInst: {} with productObject: {}", fileInst, productObject);
        if (productObject != null) {
            if (productObject instanceof final String productOid) {
                final var prodInst = Instance.get(productOid);
                if (InstanceUtils.isKindOf(prodInst, CIProducts.ProductAbstract)) {
                    EQL.builder().insert(CIPOS.FileProduct2Product)
                                    .set(CIPOS.FileProduct2Product.FromLink, fileInst)
                                    .set(CIPOS.FileProduct2Product.ToLink, prodInst)
                                    .set(CIPOS.FileProduct2Product.SortWeight, 1)
                                    .execute();
                }
            } else if (productObject instanceof final Map productMap) {
                final var prodOid = (String) productMap.get("productOid");
                final var prodInst = Instance.get(prodOid);
                if (InstanceUtils.isKindOf(prodInst, CIProducts.ProductAbstract)) {
                    final var sortWeight = productMap.getOrDefault("sortWeight", 1);
                    EQL.builder().insert(CIPOS.FileProduct2Product)
                                    .set(CIPOS.FileProduct2Product.FromLink, fileInst)
                                    .set(CIPOS.FileProduct2Product.ToLink, prodInst)
                                    .set(CIPOS.FileProduct2Product.SortWeight, sortWeight)
                                    .execute();
                }
            }
        }
    }

}
