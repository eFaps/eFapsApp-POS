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
import java.util.List;

import javax.ws.rs.core.Response;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIPOS;
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
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Category);
        queryBldr.addWhereAttrEqValue(CIPOS.Category.Status, Status.find(CIPOS.CategoryStatus.Active));
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPOS.Category.Name, CIPOS.Category.Weight);
        multi.execute();
        while (multi.next()) {
            categories.add(CategoryDto.builder()
                .withOID(multi.getCurrentInstance().getOid())
                .withName(multi.getAttribute(CIPOS.Category.Name))
                .withWeight(multi.getAttribute(CIPOS.Category.Weight))
                .build());
        }
        final Response ret = Response.ok()
                        .entity(categories)
                        .build();
        return ret;
    }

}
