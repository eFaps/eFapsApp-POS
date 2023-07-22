/*
 * Copyright 2003 - 2022 The eFaps Team
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

import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIHumanResource;
import org.efaps.pos.dto.EmployeeDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("5c862578-1a76-46d3-8e78-9fe0d509bdfd")
@EFapsApplication("eFapsApp-POS")
public abstract class Employee_Base
    extends AbstractRest
{

    private static final Logger LOG = LoggerFactory.getLogger(Employee.class);

    public Response getEmployees(@PathParam("identifier") final String _identifier)
        throws EFapsException
    {
        checkAccess(_identifier);
        LOG.debug("Recieved request for Employees");
        final var employees = new ArrayList<EmployeeDto>();

        final var eval = EQL.builder()
                        .print()
                        .query(CIHumanResource.Employee)
                        .select()
                        .attribute(CIHumanResource.Employee.FirstName, CIHumanResource.Employee.LastName)
                        .evaluate();
        while (eval.next()) {
            employees.add(EmployeeDto.builder()
                            .withOID(eval.inst().getOid())
                            .withFirstName(eval.get(CIHumanResource.Employee.FirstName))
                            .withSurName(eval.get(CIHumanResource.Employee.LastName))
                            .build());
        }
        LOG.debug("Employees: {}", employees);
        final Response ret = Response.ok()
                        .entity(employees)
                        .build();
        return ret;
    }
}
