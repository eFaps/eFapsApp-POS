/*
 * Copyright 2003 - 2023 The eFaps Team
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
import java.util.HashSet;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.Listener;
import org.efaps.ci.CIType;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.common.parameter.ParameterUtil;
import org.efaps.esjp.products.listener.IOnTransaction;
import org.efaps.pos.dto.StocktakingDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("9c5ece77-9940-4bfd-9514-24f6e3e771c9")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Stocktaking
    extends AbstractRest
{

    private static final Logger LOG = LoggerFactory.getLogger(Stocktaking.class);

    @Path("/{identifier}/stocktakings")
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response addStocktaking(@PathParam("identifier") final String identifier,
                                   final StocktakingDto dto)
        throws EFapsException
    {
        checkAccess(identifier);
        LOG.debug("StocktakingDto: {}", dto);

        final var userEval = EQL.builder().print(dto.getUserOid())
                        .attribute(CIPOS.User.Name)
                        .linkto(CIPOS.User.EmployeeLink).attribute(CIHumanResource.Employee.FirstName).as("firstName")
                        .linkto(CIPOS.User.EmployeeLink).attribute(CIHumanResource.Employee.LastName).as("lastName")
                        .evaluate();

        final var userName = userEval.get(CIPOS.User.Name);
        final var firstName = userEval.get("firstName");
        final var lastName = userEval.get("lastName");

        final var descr = "Inventario de " + userName + " (" + firstName + " " + lastName + ")";

        final var warehouseInst = Instance.get(dto.getWarehouseOid());
        final var eval = EQL.builder()
                        .print()
                        .query(CIProducts.Inventory)
                        .where()
                        .attribute(CIProducts.Inventory.Storage).eq(warehouseInst)
                        .select()
                        .attribute(CIProducts.Inventory.Quantity, CIProducts.Inventory.UoM)
                        .linkto(CIProducts.Inventory.Product).instance().as("prodInst")
                        .evaluate();
        final var transLists = new ArrayList<Instance>();
        final var products = new HashSet<String>();

        while (eval.next()) {
            final BigDecimal currentStock = eval.get(CIProducts.Inventory.Quantity);
            final var uoMId = eval.get(CIProducts.Inventory.UoM);
            final Instance prodInst = eval.get("prodInst");
            products.add(prodInst.getOid());
            final var entryOpt = dto.getEntries().stream().filter(entry -> {
                return prodInst.getOid().equals(entry.getProductOid());
            }).findFirst();

            final var targetStock = entryOpt.isEmpty() ? BigDecimal.ZERO : entryOpt.get().getQuantity();
            final var movement = targetStock.subtract(currentStock);

            CIType transCiType;
            if (movement.compareTo(BigDecimal.ZERO) > 0) {
                transCiType = CIProducts.TransactionInbound;
            } else {
                transCiType = CIProducts.TransactionOutbound;
            }

            final var instance = EQL.builder().insert(transCiType)
                            .set(CIProducts.TransactionAbstract.Quantity, movement.abs())
                            .set(CIProducts.TransactionAbstract.Storage, warehouseInst)
                            .set(CIProducts.TransactionAbstract.UoM, uoMId)
                            .set(CIProducts.TransactionAbstract.Date, dto.getEndAt())
                            .set(CIProducts.TransactionAbstract.Product, prodInst)
                            .set(CIProducts.TransactionAbstract.Description, descr)
                            .execute();
            transLists.add(instance);
        }
        for (final var entry : dto.getEntries()) {
            if (!products.contains(entry.getProductOid())) {
                final var prodInst = Instance.get(entry.getProductOid());
                final var prodEval = EQL.builder().print(prodInst)
                                .attribute(CIProducts.ProductAbstract.DefaultUoM)
                                .evaluate();
                final var instance = EQL.builder().insert(CIProducts.TransactionInbound)
                                .set(CIProducts.TransactionAbstract.Quantity, entry.getQuantity())
                                .set(CIProducts.TransactionAbstract.Storage, warehouseInst)
                                .set(CIProducts.TransactionAbstract.UoM,
                                                prodEval.get(CIProducts.ProductAbstract.DefaultUoM))
                                .set(CIProducts.TransactionAbstract.Date, dto.getEndAt())
                                .set(CIProducts.TransactionAbstract.Product, prodInst)
                                .set(CIProducts.TransactionAbstract.Description, descr)
                                .execute();
                transLists.add(instance);
            }
        }

        try {
            if (!transLists.isEmpty()) {
                for (final IOnTransaction listener : Listener.get().<IOnTransaction>invoke(IOnTransaction.class)) {
                    listener.createDocuments4Transactions(ParameterUtil.instance(),
                                    transLists.toArray(new Instance[transLists.size()]));
                }
            }
        } catch (final Exception e) {
            LOG.error("Catched", e);
        }
        String docOid = "";
        if (!transLists.isEmpty()) {
            final var evalTrans = EQL.builder()
                            .print(transLists.get(0))
                            .linkto(CIProducts.TransactionAbstract.Document).oid().as("docOid")
                            .evaluate();
            docOid = evalTrans.get("docOid");
        }
        final Response ret = Response.ok()
                        .entity(docOid)
                        .build();
        return ret;
    }
}
