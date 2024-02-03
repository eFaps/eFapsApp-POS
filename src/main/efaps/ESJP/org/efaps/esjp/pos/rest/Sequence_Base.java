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
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.pos.dto.SequenceDto;
import org.efaps.util.EFapsException;

@EFapsUUID("9c5ece77-9940-4bfd-9514-24f6e3e771c9")
@EFapsApplication("eFapsApp-POS")
public abstract class Sequence_Base
    extends AbstractRest
{

    public Response getSequences(final String _identifier)
        throws EFapsException
    {
        checkAccess(_identifier);
        final List<SequenceDto> sequences = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Sequence);
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPOS.Sequence.Format, CIPOS.Sequence.Value);
        multi.execute();
        while(multi.next()) {
            sequences.add(SequenceDto.builder()
                            .withOID(multi.getCurrentInstance().getOid())
                            .withSeq(multi.getAttribute(CIPOS.Sequence.Value))
                            .withFormat(multi.getAttribute(CIPOS.Sequence.Format))
                            .build());
        }
        final Response ret = Response.ok()
                        .entity(sequences)
                        .build();
        return ret;
    }
}
