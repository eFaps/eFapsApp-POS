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

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.pos.dto.WorkspaceDto;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("73f3c5a8-0cf7-40c1-a926-d54c0ffe7744")
@EFapsApplication("eFapsApp-POS")
public abstract class Workspace_Base
{
    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    public Response getWorkspaces()
        throws EFapsException
    {
        final List<WorkspaceDto> poss = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Workspace);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selPosOID = SelectBuilder.get()
                        .linkto(CIPOS.Workspace.POSLink)
                        .oid();
        multi.addSelect(selPosOID);
        multi.addAttribute(CIPOS.Workspace.Name);
        multi.execute();
        while (multi.next()) {
            poss.add(WorkspaceDto.builder()
                .withOID(multi.getCurrentInstance().getOid())
                .withName(multi.getAttribute(CIPOS.Workspace.Name))
                .withPosOid(multi.getSelect(selPosOID))
                .build());
        }
        final Response ret = Response.ok()
                        .entity(poss)
                        .build();
        return ret;
    }

}
