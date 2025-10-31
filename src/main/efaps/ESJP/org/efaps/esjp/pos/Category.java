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
package org.efaps.esjp.pos;

import java.util.List;
import java.util.Map;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkin;
import org.efaps.db.Checkout;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.pos.util.Pos;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("36135a88-a7ea-4736-9213-b85887fb48cb")
@EFapsApplication("eFapsApp-POS")
public class Category
{

    private static final Logger LOG = LoggerFactory.getLogger(Category.class);

    @SuppressWarnings("unchecked")
    public Return clone(final Parameter parameter)
        throws EFapsException
    {
        final Map<String, Object> payload = (Map<String, Object>) parameter.get(ParameterValues.PAYLOAD);
        LOG.info("{}", payload);
        final var categoryOid = ((List<String>) payload.get("eFapsSelectedOids")).get(0);

        final var baseInst = Instance.get(categoryOid);

        final var cloneInst = clone(baseInst, (String) payload.get("name"), null);

        final Return ret = new Return();
        ret.put(ReturnValues.INSTANCE, cloneInst);
        return ret;
    }

    protected Instance clone(final Instance baseCategoryInst,
                             final String name,
                             final Instance parentInst)
        throws EFapsException
    {
        final var eval = EQL.builder()
                        .print(baseCategoryInst)
                        .attribute(CIPOS.Category.Name, CIPOS.Category.Description, CIPOS.Category.Label,
                                        CIPOS.Category.Weight)
                        .evaluate();
        eval.next();

        final var clonedInst = EQL.builder().insert(CIPOS.Category)
                        .set(CIPOS.Category.Name, name == null ? eval.get(CIPOS.Category.Name) : name)
                        .set(CIPOS.Category.Description, eval.get(CIPOS.Category.Description))
                        .set(CIPOS.Category.Label, eval.get(CIPOS.Category.Label))
                        .set(CIPOS.Category.Weight, eval.get(CIPOS.Category.Weight))
                        .set(CIPOS.Category.Status, CIPOS.CategoryStatus.Inactive)
                        .set(CIPOS.Category.ParentLink, parentInst)
                        .execute();

        // clone image
        if (Pos.CATEGORY_ACTIVATEIMAGE.get()) {
            final var checkout = new Checkout(baseCategoryInst);
            if (checkout.exists()) {
                final var inputStream = checkout.execute();
                final var checkin = new Checkin(clonedInst);
                checkin.execute(checkout.getFileName(), inputStream,
                                Long.valueOf(checkout.getFileLength()).intValue());
            }
        }

        // clone product connections
        final var productEval = EQL.builder().print()
                        .query(CIPOS.Category2Product)
                        .where()
                        .attribute(CIPOS.Category2Product.FromLink).eq(baseCategoryInst)
                        .select()
                        .attribute(CIPOS.Category2Product.ToLink, CIPOS.Category2Product.SortWeight)
                        .evaluate();

        while (productEval.next()) {
            EQL.builder().insert(CIPOS.Category2Product)
                            .set(CIPOS.Category2Product.FromLink, clonedInst)
                            .set(CIPOS.Category2Product.ToLink, productEval.get(CIPOS.Category2Product.ToLink))
                            .set(CIPOS.Category2Product.SortWeight, productEval.get(CIPOS.Category2Product.SortWeight))
                            .execute();
        }

        // clone child categories
        final var categoryEval = EQL.builder().print()
                        .query(CIPOS.Category)
                        .where()
                        .attribute(CIPOS.Category.ParentLink).eq(baseCategoryInst)
                        .select()
                        .oid()
                        .evaluate();
        while (categoryEval.next()) {
            clone(categoryEval.inst(), null, clonedInst);
        }
        return clonedInst;
    }
}
