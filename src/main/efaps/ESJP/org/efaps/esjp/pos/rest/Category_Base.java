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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.store.Resource;
import org.efaps.db.store.Store;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.pos.dto.CategoryDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("f1c816e2-1543-4975-b69a-799b4809802b")
@EFapsApplication("eFapsApp-POS")
public abstract class Category_Base
    extends AbstractRest
{
    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Category.class);

    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    public Response getCategories(final String _identifier)
        throws EFapsException
    {
        checkAccess(_identifier);
        LOG.debug("Responding to request for Categories for {}", _identifier);
        final List<CategoryDto> categories = new ArrayList<>();

        final var eval = EQL.builder().print().query(CIPOS.Category)
            .where()
            .attribute(CIPOS.Category.Status).eq(CIPOS.CategoryStatus.Active)
            .select()
            .attribute(CIPOS.Category.Name,
                        CIPOS.Category.Description,
                        CIPOS.Category.Label,
                        CIPOS.Category.Weight)
            .linkto(CIPOS.Category.ParentLink).oid().as("parentOid")
            .evaluate();

        final var store = Store.get(CIPOS.Category.getType().getStoreId());

        while (eval.next()) {
            String imageOid = null;
            if (Pos.CATEGORY_ACTIVATEIMAGE.get()) {
                final Resource resource = store.getResource(eval.inst());
                if (resource.exists()) {
                    imageOid = eval.inst().getOid();
                }
            }
            final String parentOid = eval.get("parentOid");
            categories.add(CategoryDto.builder()
                .withOID(eval.inst().getOid())
                .withName(eval.get(CIPOS.Category.Name))
                .withDescription(eval.get(CIPOS.Category.Description))
                .withLabel(eval.get(CIPOS.Category.Label))
                .withWeight(eval.get(CIPOS.Category.Weight))
                .withImageOid(imageOid)
                .withParentOid(parentOid)
                .build());
        }
        LOG.debug("categories: {}", categories);
        final Response ret = Response.ok()
                        .entity(categories)
                        .build();
        return ret;
    }
}
