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
package org.efaps.esjp.pos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkin;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.pos.dto.UpdateDto;
import org.efaps.pos.dto.UpdateInstructionDto;
import org.efaps.util.EFapsException;

@EFapsUUID("e8a79ebb-f9df-435d-8526-46fb5533ec26")
@EFapsApplication("eFapsApp-POS")
public class UpdateDefinition
{

    public UpdateDto getUpdate()
        throws EFapsException
    {
        final var eval = EQL.builder().print().query(CIPOS.UpdateDefinition)
                        .select()
                        .attribute(CIPOS.UpdateDefinition.Created, CIPOS.UpdateDefinition.Version)
                        .orderBy("Created", true)
                        .limit(1)
                        .evaluate();
        eval.inst();
        final var instructionEval = EQL.builder().print().query(CIPOS.UpdateInstruction)
                        .select()
                        .attribute(CIPOS.UpdateInstruction.TargetPath, CIPOS.UpdateInstruction.Expand)
                        .linkto(CIPOS.UpdateInstruction.FileLink).oid().as("fileOid")
                        .evaluate();
        final var instructions = new ArrayList<UpdateInstructionDto>();
        while (instructionEval.next()) {
            instructions.add(UpdateInstructionDto.builder()
                            .withTargetPath(instructionEval.get(CIPOS.UpdateInstruction.TargetPath))
                            .withFileOid(instructionEval.get("fileOid"))
                            .withExpand(instructionEval.get(CIPOS.UpdateInstruction.Expand))
                            .build());
        }
        return UpdateDto.builder()
                        .withVersion(eval.get(CIPOS.UpdateDefinition.Version))
                        .withInstructions(instructions)
                        .build();
    }

    public Return autoComplete4UpdateFile(final Parameter _parameter)
        throws EFapsException
    {
        final String input = (String) _parameter.get(ParameterValues.OTHERS);
        final List<Map<String, String>> list = new ArrayList<>();
        if (input.length() > 0) {
            final Map<String, Map<String, String>> tmpMap = new TreeMap<>();

            final var eval = EQL.builder().print().query(CIPOS.UpdateFile)
                            .where()
                            .attribute(CIPOS.UpdateFile.Name).ilike(input)
                            .select().attribute(CIPOS.UpdateFile.Name, CIPOS.UpdateFile.Description)
                            .evaluate();

            while (eval.next()) {
                final long id = eval.inst().getId();
                final String name = eval.get(CIPOS.UpdateFile.Name);
                final String description = eval.get(CIPOS.UpdateFile.Description);
                final String choice = name + " - " + description;
                final Map<String, String> map = new HashMap<>();
                map.put("eFapsAutoCompleteKEY", String.valueOf(id));
                map.put("eFapsAutoCompleteCHOICE", choice);
                map.put("eFapsAutoCompleteVALUE", name);
                tmpMap.put(choice, map);
            }
            list.addAll(tmpMap.values());
        }
        final Return retVal = new Return();
        retVal.put(ReturnValues.VALUES, list);
        return retVal;
    }

    public Return checkinFile(final Parameter parameter)
        throws EFapsException
    {
        final Instance instance = parameter.getInstance();
        final Context context = Context.getThreadContext();
        if (context.getFileParameters().size() > 0) {
            final Context.FileParameter fileItem = context.getFileParameters().get("file");
            if (fileItem != null) {
                final Checkin checkin = new Checkin(instance);
                try {
                    checkin.execute(fileItem.getName(), fileItem.getInputStream(), (int) fileItem.getSize());
                } catch (final IOException e) {
                    throw new EFapsException(this.getClass(), "execute", e, parameter);
                }
            }
            EQL.builder().update(instance).set(CIPOS.UpdateFile.Name, fileItem.getName()).execute();
        }
        return new Return();
    }

}
