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
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.commons.collections4.CollectionUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.pos.listener.IInventoryProvider;
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
    public Response getInventory(final String identifier,
                                 final Set<String> productOids)
        throws EFapsException
    {
        checkAccess(identifier);
        final List<InventoryEntryDto> entries = new ArrayList<>();

        boolean carryOn = true;
        for (final var provider : Listener.get().<IInventoryProvider>invoke(IInventoryProvider.class)) {
            carryOn = provider.evalInventory(entries, productOids);
        }

        if (carryOn) {
            final var query = EQL.builder().print().query(CIProducts.InventoryAbstract);
            if (CollectionUtils.isNotEmpty(productOids)) {
                query.where()
                                .attribute(CIProducts.InventoryAbstract.Product)
                                .in(productOids.stream().map(Instance::get).toList());
            }
            final var eval = query.select()
                            .attribute(CIProducts.InventoryAbstract.Quantity, CIProducts.InventoryAbstract.Modified)
                            .linkto(CIProducts.InventoryAbstract.Product).oid().as("productOid")
                            .linkto(CIProducts.InventoryAbstract.Storage).oid().as("warehouseOid")
                            .evaluate();
            while (eval.next()) {
                entries.add(InventoryEntryDto.builder()
                                .withOid(eval.inst().getOid())
                                .withQuantity(eval.get(CIProducts.InventoryAbstract.Quantity))
                                .withUpdatedAt(eval.get(CIProducts.InventoryAbstract.Modified))
                                .withProductOid(eval.get("productOid"))
                                .withWarehouseOid(eval.get("warehouseOid"))
                                .build());
            }
        }
        return Response.ok().entity(entries).build();
    }
}
