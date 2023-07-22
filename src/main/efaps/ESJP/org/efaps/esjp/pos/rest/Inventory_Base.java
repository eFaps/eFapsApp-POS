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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.products.Inventory;
import org.efaps.esjp.products.Inventory_Base.InventoryBean;
import org.efaps.pos.dto.InventoryEntryDto;
import org.efaps.util.EFapsException;

@EFapsUUID("1d543515-3b7a-4c1e-8202-aadbb4869958")
@EFapsApplication("eFapsApp-POS")
public abstract class Inventory_Base
    extends AbstractRest
{

    /**
     * Gets the inventory.
     *
     * @return the inventory
     * @throws EFapsException the e faps exception
     */
    public Response getInventory(final String _identifier)
        throws EFapsException
    {
        checkAccess(_identifier);
        final List<InventoryEntryDto> entries = new ArrayList<>();
        final QueryBuilder attrQueryBldr = new QueryBuilder(CIPOS.Category);
        attrQueryBldr.addWhereAttrEqValue(CIPOS.Category.Status, Status.find(CIPOS.CategoryStatus.Active));

        final Set<Instance> prodInstances = new HashSet<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Category2Product);
        queryBldr.addWhereAttrInQuery(CIPOS.Category2Product.FromLink,
                        attrQueryBldr.getAttributeQuery(CIPOS.Category.ID));
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selProdInst = SelectBuilder.get().linkto(CIPOS.Category2Product.ToLink).instance();
        multi.addSelect(selProdInst);
        multi.executeWithoutAccessCheck();
        while (multi.next()) {
            prodInstances.add(multi.getSelect(selProdInst));
        }

        final Inventory inventory = new Inventory();
        final Parameter parameter = ParameterUtil.instance();
        inventory.setShowStorage(true);
        final List<? extends InventoryBean> beans = inventory.getInventory(parameter);
        for (final InventoryBean bean : beans) {
            if (prodInstances.contains(bean.getProdInstance())) {
                entries.add(InventoryEntryDto.builder()
                            .withQuantity(bean.getQuantity())
                            .withProductOid(bean.getProdOID())
                            .withWarehouseOid(bean.getStorageInstance().getOid())
                            .build());
            }
        }
        final Response ret = Response.ok().entity(entries).build();
        return ret;
    }
}
