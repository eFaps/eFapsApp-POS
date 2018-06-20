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
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIAdminProgram;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.pos.util.Pos.DocType;
import org.efaps.esjp.pos.util.Pos.PrintTarget;
import org.efaps.esjp.pos.util.Pos.SpotConfig;
import org.efaps.pos.dto.PrintCmdDto;
import org.efaps.pos.dto.WorkspaceDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("73f3c5a8-0cf7-40c1-a926-d54c0ffe7744")
@EFapsApplication("eFapsApp-POS")
public abstract class Workspace_Base
{
    /** The Constant LOG. */
    private static final Logger LOG = LoggerFactory.getLogger(Workspace.class);

    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    @SuppressWarnings("unchecked")
    public Response getWorkspaces()
        throws EFapsException
    {
        LOG.debug("Responding to request for Workspaces");
        final List<WorkspaceDto> workspaces = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Workspace);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selPosOID = SelectBuilder.get()
                        .linkto(CIPOS.Workspace.POSLink)
                        .oid();
        final SelectBuilder selWarehouseOID = SelectBuilder.get()
                        .linkto(CIPOS.Workspace.WarehouseLink)
                        .oid();
        multi.addSelect(selPosOID, selWarehouseOID);
        multi.addAttribute(CIPOS.Workspace.Name, CIPOS.Workspace.DocTypes, CIPOS.Workspace.SpotConfig);
        multi.execute();
        while (multi.next()) {
            final Set<org.efaps.pos.dto.DocType> dtoDocTypes = new HashSet<>();
            final Collection<DocType> docTypes = multi.getAttribute(CIPOS.Workspace.DocTypes);
            if (docTypes != null) {
                for (final DocType docType : docTypes) {
                    dtoDocTypes.add(EnumUtils.getEnum(org.efaps.pos.dto.DocType.class, docType.name()));
                }
            }
            final Set<PrintCmdDto> printCmdDtos = new HashSet<>();
            final PrintQuery print = new PrintQuery(multi.getCurrentInstance());
            print.addAttributeSet(CIPOS.Workspace.PrintCmdSet.name);
            print.executeWithoutAccessCheck();
            final Map<String, Object> printCmds = print.getAttributeSet(CIPOS.Workspace.PrintCmdSet.name);
            if (printCmds != null) {
                LOG.trace("PrintCmds: {} for {}", printCmds, multi.getCurrentInstance());
                final Iterator<Long> printerLinkIter = ((ArrayList<Long>) printCmds.get("PrinterLink")).iterator();
                final Iterator<PrintTarget> printTargetIter = ((ArrayList<PrintTarget>) printCmds.get("PrintTarget"))
                                .iterator();
                final Iterator<Long> targetLinkIter = ((ArrayList<Long>) printCmds.get("TargetLink")).iterator();
                final Iterator<Long> reportLinkIter = ((ArrayList<Long>) printCmds.get("ReportLink")).iterator();

                while (printerLinkIter.hasNext()) {
                    printCmdDtos.add(PrintCmdDto.builder()
                            .withPrinterOid(Instance.get(CIPOS.Printer.getType(), printerLinkIter.next()).getOid())
                            .withTarget(EnumUtils.getEnum(org.efaps.pos.dto.PrintTarget.class,
                                            printTargetIter.next().name()))
                            .withTargetOid(Instance.get(CIPOS.Category.getType(), targetLinkIter.next()).getOid())
                            .withReportOid(Instance.get(CIAdminProgram.JasperReportCompiled.getType(),
                                            reportLinkIter.next()).getOid())
                            .build());
                }
            }
            final SpotConfig spotConfig = multi.getAttribute(CIPOS.Workspace.SpotConfig);

            workspaces.add(WorkspaceDto.builder()
                .withOID(multi.getCurrentInstance().getOid())
                .withName(multi.getAttribute(CIPOS.Workspace.Name))
                .withPosOid(multi.getSelect(selPosOID))
                .withWarehouseOid(multi.getSelect(selWarehouseOID))
                .withDocTypes(dtoDocTypes)
                .withSpotConfig(EnumUtils.getEnum(org.efaps.pos.dto.SpotConfig.class, spotConfig.name()))
                .withPrintCmds(printCmdDtos)
                .build());
        }
        LOG.debug("Workspaces: {}", workspaces);
        final Response ret = Response.ok()
                        .entity(workspaces)
                        .build();
        return ret;
    }

}
