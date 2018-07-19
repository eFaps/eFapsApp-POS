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
import org.efaps.esjp.ci.CIProducts;
import org.efaps.pos.dto.WarehouseDto;
import org.efaps.util.EFapsException;

@EFapsUUID("5304130a-0f0b-4320-84c6-25a3d63f6a6e")
@EFapsApplication("eFapsApp-POS")
public abstract class Warehouse_Base
    extends AbstractRest
{

    /**
     * Gets the warehouses.
     *
     * @return the warehouses
     * @throws EFapsException the eFaps exception
     */
    public Response getWarehouses(final String _identifier)
        throws EFapsException
    {
        final List<WarehouseDto> warehouses = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIProducts.Warehouse);
        queryBldr.addWhereAttrEqValue(CIProducts.Warehouse.Status, Status.find(
                        CIProducts.StorageAbstractStatus.Active));
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIProducts.Warehouse.Name);
        multi.execute();
        while (multi.next()) {
            warehouses.add(WarehouseDto.builder()
                .withOID(multi.getCurrentInstance().getOid())
                .withName(multi.getAttribute(CIProducts.Warehouse.Name))
                .build());
        }
        final Response ret = Response.ok().entity(warehouses).build();
        return ret;
    }
}
