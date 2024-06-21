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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.promotions.PromotionService;
import org.efaps.pos.dto.PromoInfoSyncDto;
import org.efaps.promotionengine.dto.PromotionDetailDto;
import org.efaps.promotionengine.dto.PromotionInfoDto;
import org.efaps.util.EFapsException;

@EFapsUUID("a3abf58f-11b0-4b2f-a5c8-cf69bf897b41")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class Promotion
    extends AbstractRest
{

    @Path("/{identifier}/promotions")
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response getPromotions(@PathParam("identifier") final String identifier)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE);
        final var promotions = new PromotionService().getPromotions();
        final Response ret = Response.ok()
                        .entity(promotions)
                        .build();
        return ret;
    }

    @Path("/{identifier}/promotion-infos")
    @POST
    @Produces({ MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN })
    public Response registerPromotionInfo(@PathParam("identifier") final String identifier,
                                          final PromoInfoSyncDto dto)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE);
        final var promoInfo = dto.getPromoInfo();
        final var promotionInfoDto = PromotionInfoDto.builder()
                        .withNetTotalDiscount(promoInfo.getNetTotalDiscount())
                        .withCrossTotalDiscount(promoInfo.getCrossTotalDiscount())
                        .withPromotionOids(promoInfo.getPromotionOids())
                        .withDetails(promoInfo.getDetails().stream()
                                        .map(promoDetail -> PromotionDetailDto.builder()
                                                        .withNetUnitDiscount(promoDetail.getNetUnitDiscount())
                                                        .withNetDiscount(promoDetail.getNetDiscount())
                                                        .withCrossUnitDiscount(promoDetail.getCrossUnitDiscount())
                                                        .withCrossDiscount(promoDetail.getCrossDiscount())
                                                        .withIndex(promoDetail.getIndex())
                                                        .withPromotionOid(promoDetail.getPromotionOid())
                                                        .build())
                                        .toList())
                        .build();
        final var instance = new PromotionService().registerPromotionInfoForDoc(dto.getDocumentOid(), promotionInfoDto,
                        dto.getPromotions());
        final Response ret = Response.ok()
                        .entity(InstanceUtils.isValid(instance) ? instance.getOid() : instance)
                        .build();
        return ret;
    }
}
