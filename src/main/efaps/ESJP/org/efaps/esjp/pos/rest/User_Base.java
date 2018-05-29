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
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.Response;

import org.apache.commons.lang3.EnumUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.pos.util.Pos.Role;
import org.efaps.pos.dto.Roles;
import org.efaps.pos.dto.UserDto;
import org.efaps.util.EFapsException;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 */
@EFapsUUID("ab1163cd-632f-4054-8ef0-e6d7cb39be91")
@EFapsApplication("eFapsApp-POS")
public abstract class User_Base
{
    /**
     * Gets the categories.
     *
     * @return the categories
     * @throws EFapsException the eFaps exception
     */
    @SuppressWarnings("unchecked")
    public Response getUsers()
        throws EFapsException
    {
        final List<UserDto> users = new ArrayList<>();
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.User);
        final MultiPrintQuery multi = queryBldr.getPrint();
        final SelectBuilder selEmployee = SelectBuilder.get()
                        .linkto(CIPOS.User.EmployeeLink);
        final SelectBuilder selEmployeeFirstName = new SelectBuilder(selEmployee)
                        .attribute(CIHumanResource.Employee.FirstName);
        final SelectBuilder selEmployeeLastName = new SelectBuilder(selEmployee)
                        .attribute(CIHumanResource.Employee.LastName);
        final SelectBuilder selWorkspaceOids = SelectBuilder.get()
                        .linkfrom(CIPOS.User2Workspace.FromLink)
                        .linkto(CIPOS.User2Workspace.ToLink).oid();
        multi.addSelect(selEmployeeFirstName, selEmployeeLastName, selWorkspaceOids);
        multi.addAttribute(CIPOS.User.Name, CIPOS.User.Password, CIPOS.User.Roles);
        multi.execute();
        while (multi.next()) {
            final Collection<Role> roles = multi.getAttribute(CIPOS.User.Roles);
            final Set<Roles> dtoRoles = new HashSet<>();
            for (final Role role : roles) {
                dtoRoles.add(EnumUtils.getEnum(Roles.class, role.name()));
            }
            final Object ws = multi.getSelect(selWorkspaceOids);
            final Set<String> wsOids = new HashSet<>();
            if (ws instanceof List) {
                wsOids.addAll((Collection<? extends String>) ws);
            } else if (ws instanceof String) {
                wsOids.add((String) ws);
            }
            users.add(UserDto.builder()
                .withOID(multi.getCurrentInstance().getOid())
                .withUsername(multi.getAttribute(CIPOS.User.Name))
                .withPassword(multi.getAttribute(CIPOS.User.Password))
                .withFirstName(multi.getSelect(selEmployeeFirstName))
                .withSurName(multi.getSelect(selEmployeeLastName))
                .withRoles(dtoRoles)
                .withWorkspaceOids(wsOids)
                .build());
        }
        final Response ret = Response.ok()
                        .entity(users)
                        .build();
        return ret;
    }
}
