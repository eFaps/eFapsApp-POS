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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.pos.dto.FileDto;
import org.efaps.util.EFapsException;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@EFapsUUID("901bc326-f5cf-433e-a24f-3105ab608b29")
@EFapsApplication("eFapsApp-POS")
@Path("/pos")
public class File
    extends AbstractRest
{

    @Path("/{identifier}/files")
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public Response getFiles(@PathParam("identifier") final String identifier)
        throws EFapsException
    {
        checkAccess(identifier, ACCESSROLE.BE);

        final List<FileDto> files = new ArrayList<>();
        final var eval = EQL.builder().print()
                        .query(CIPOS.File)
                        .select()
                        .file().label().as("fileName")
                        .attribute(CIPOS.File.Name, CIPOS.File.Description)
                        .attributeSet(CIPOS.File.TagSet).attribute("Tag").as("TagValue")
                        .attributeSet(CIPOS.File.TagSet)
                            .linkto("TagTypeLink")
                            .attribute(CIPOS.AttributeDefinitionFileTagType.Value).as("TagKey")
                        .evaluate();

        while (eval.next()) {
            final Map<String, String> tags = new HashMap<>();
            final var tagValues = eval.<List<String>>get("TagValue");
            final var tagKeys = eval.<List<String>>get("TagKey");
            if (tagKeys != null) {
                final var tagValuesIter = tagValues.iterator();
                for (final String tagKey : tagKeys) {
                    tags.put(tagKey, tagValuesIter.next());
                }
            }

            files.add(FileDto.builder()
                            .withOID(eval.inst().getOid())
                            .withName(eval.get(CIPOS.File.Name))
                            .withFileName(eval.get("fileName"))
                            .withDescription(eval.get(CIPOS.File.Description))
                            .withTags(tags)
                            .build());
        }
        final Response ret = Response.ok()
                        .entity(files)
                        .build();
        return ret;
    }

}
