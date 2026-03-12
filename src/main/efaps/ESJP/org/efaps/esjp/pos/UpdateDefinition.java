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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.compress.archivers.examples.Expander;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.commons.text.StringSubstitutor;
import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Parameter.ParameterValues;
import org.efaps.admin.event.Return;
import org.efaps.admin.event.Return.ReturnValues;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Checkin;
import org.efaps.db.Checkout;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.eql.builder.Print;
import org.efaps.eql.builder.Selectables;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.pos.dto.UpdateConfirmationDto;
import org.efaps.pos.dto.UpdateDto;
import org.efaps.pos.dto.UpdateInstructionDto;
import org.efaps.pos.dto.UpdateTemplateDto;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("e8a79ebb-f9df-435d-8526-46fb5533ec26")
@EFapsApplication("eFapsApp-POS")
public class UpdateDefinition
{

    private static final Logger LOG = LoggerFactory.getLogger(UpdateDefinition.class);

    public UpdateDto getUpdate(final Instance backendInst)
        throws EFapsException
    {
        UpdateDto updateDto = null;
        final var eval = EQL.builder().print()
                        .query(CIPOS.UpdateDefinition)
                        .where()
                        .attribute(CIPOS.UpdateDefinition.Status).eq(CIPOS.UpdateDefinitionStatus.Active)
                        .attribute(CIPOS.UpdateDefinition.ID).in(
                                        EQL.builder()
                                                        .nestedQuery(CIPOS.UpdateDefinition2Backend)
                                                        .where()
                                                        .attribute(CIPOS.UpdateDefinition2Backend.ToLink)
                                                        .eq(backendInst)
                                                        .up()
                                                        .selectable(Selectables.attribute(
                                                                        CIPOS.UpdateDefinition2Backend.FromLink)))
                        .select()
                        .attribute(CIPOS.UpdateDefinition.Created, CIPOS.UpdateDefinition.Version,
                                        CIPOS.UpdateDefinition.TargetFolder)
                        .orderBy(CIPOS.UpdateDefinition.Created, true)
                        .limit(1)
                        .evaluate();
        if (eval.next()) {
            updateDto = toDto(eval.inst(), eval.get(CIPOS.UpdateDefinition.Version),
                            eval.get(CIPOS.UpdateDefinition.TargetFolder));
        }
        return updateDto;
    }

    public UpdateDto toDto(final Instance defInst,
                           final String version,
                           final String targetFolder)
        throws EFapsException
    {
        final var instructionEval = EQL.builder().print()
                        .query(CIPOS.UpdateInstruction)
                        .where()
                        .attribute(CIPOS.UpdateInstruction.DefinitionLink).eq(defInst)
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

        final var templateEval = EQL.builder().print()
                        .query(CIPOS.UpdateTemplate)
                        .where()
                        .attribute(CIPOS.UpdateTemplate.DefinitionLink).eq(defInst)
                        .select()
                        .attribute(CIPOS.UpdateTemplate.TargetPath, CIPOS.UpdateTemplate.Name)
                        .evaluate();
        final var templates = new ArrayList<UpdateTemplateDto>();
        while (templateEval.next()) {
            templates.add(UpdateTemplateDto.builder()
                            .withTemplateOid(templateEval.inst().getOid())
                            .withTargetPath(templateEval.get(CIPOS.UpdateTemplate.TargetPath))
                            .withName(templateEval.get(CIPOS.UpdateTemplate.Name))
                            .build());
        }
        return UpdateDto.builder()
                        .withVersion(version)
                        .withTargetFolder(targetFolder)
                        .withInstructions(instructions)
                        .withTemplates(templates)
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

    public void confirm(final Instance backendInst,
                        final UpdateConfirmationDto dto)
        throws EFapsException
    {
        LOG.info("Got update of status for version: {} for {}", dto, backendInst);
        final var eval = EQL.builder().print()
                        .query(CIPOS.UpdateDefinition2Backend)
                        .where()
                        .attribute(CIPOS.UpdateDefinition2Backend.ToLink).eq(backendInst)
                        .attribute(CIPOS.UpdateDefinition2Backend.FromLink).in(
                                        EQL.builder()
                                                        .nestedQuery(CIPOS.UpdateDefinition)
                                                        .where()
                                                        .attribute(CIPOS.UpdateDefinition.Version)
                                                        .eq(dto.getVersion())
                                                        .up()
                                                        .selectable(Selectables
                                                                        .attribute(CIPOS.UpdateDefinition.ID)))
                        .select()
                        .oid()
                        .evaluate();

        if (eval.next()) {
            final var value = EnumUtils.getEnum(org.efaps.esjp.pos.util.Pos.UpdateStatus.class, dto.getStatus().name());
            LOG.info("Setting new UpdateStatus of {} on {}", value, eval.inst());
            EQL.builder().update(eval.inst())
                            .set(CIPOS.UpdateDefinition2Backend.UpdateStatus, value)
                            .execute();
        } else {
            LOG.warn("Did not find data entry to be updated!");
        }
    }

    public Return download(final Parameter parameter)
        throws EFapsException
    {
        final var ret = new Return();
        final var defInst = parameter.getInstance();
        if (InstanceUtils.isType(defInst, CIPOS.UpdateDefinition)) {
            final var eval = EQL.builder().print(defInst)
                            .attribute(CIPOS.UpdateDefinition.Version, CIPOS.UpdateDefinition.TargetFolder)
                            .evaluate();
            if (eval.next()) {
                final var dto = toDto(defInst, eval.get(CIPOS.UpdateDefinition.Version),
                                eval.get(CIPOS.UpdateDefinition.TargetFolder));
                LOG.info("evaluated UpdateDto: {}", dto);
                final var zipFile = adhereInstructions(dto);
                if (zipFile != null) {
                    ret.put(ReturnValues.VALUES, zipFile);
                }
            }
        } else {
            LOG.error("What? {}", defInst);
        }
        return ret;
    }

    protected File adhereInstructions(final UpdateDto updateDto)
        throws EFapsException
    {
        File ret = null;
        try {
            final var tempFolder = new FileUtil().getUserTemp();

            final var defPath = tempFolder.toPath().resolve("UpdateDefinition-" + updateDto.getVersion());
            if (defPath.toFile().exists()) {
                FileUtils.deleteDirectory(defPath.toFile());
            }

            Files.deleteIfExists(defPath);

            final var defFolder = Files.createDirectories(defPath);

            LOG.info("UpdateFolder: {}", defFolder);
            final var basePath = defFolder;

            for (final var instruction : updateDto.getInstructions()) {
                if (instruction.getFileOid() != null) {
                    final var checkout = new Checkout(instruction.getFileOid());
                    final var inputStream = checkout.execute();

                    final var targetPath = basePath.resolve(instruction.getTargetPath()).normalize();
                    LOG.info("targetPath: {}", targetPath);
                    final var localFile = new File(targetPath.toFile(), checkout.getFileName());
                    FileUtils.createParentDirectories(localFile);
                    Files.createFile(localFile.toPath());
                    IOUtils.copy(inputStream, new FileOutputStream(localFile));

                    if (instruction.isExpand()) {
                        new Expander().expand(localFile.toPath(), targetPath);
                        Files.delete(localFile.toPath());
                    }
                }
            }
            final var zipPath = tempFolder.toPath().resolve("UpdateDefinition-" + updateDto.getVersion() + ".zip");
            Files.deleteIfExists(zipPath);

            ret = zipPath.toFile();
            final var fos = new FileOutputStream(ret);
            final var zipOut = new ZipOutputStream(fos);

            zipFile(defFolder.toFile(), defFolder.toFile().getName(), zipOut);
            zipOut.close();
            fos.close();

        } catch (final Exception e) {
            LOG.error("Catched", e);
        }
        return ret;
    }

    private static void zipFile(File fileToZip,
                                String fileName,
                                ZipOutputStream zipOut)
        throws IOException
    {
        if (fileToZip.isHidden()) {
            return;
        }
        if (fileToZip.isDirectory()) {
            if (fileName.endsWith("/")) {
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.closeEntry();
            } else {
                zipOut.putNextEntry(new ZipEntry(fileName + "/"));
                zipOut.closeEntry();
            }
            final File[] children = fileToZip.listFiles();
            for (final File childFile : children) {
                zipFile(childFile, fileName + "/" + childFile.getName(), zipOut);
            }
            return;
        }
        final FileInputStream fis = new FileInputStream(fileToZip);
        final ZipEntry zipEntry = new ZipEntry(fileName);
        zipOut.putNextEntry(zipEntry);
        final byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zipOut.write(bytes, 0, length);
        }
        fis.close();
    }

    public Return getTemplateMappingPreview(final Parameter parameter)
        throws EFapsException
    {
        final var ret = new Return();
        final var mappingInst = parameter.getInstance();
        LOG.info("Evaluating TemplateMappingPreview for {}", mappingInst);

        final var print = EQL.builder().print(mappingInst);
        final var content = fillInTemplate(print);

        ret.put(ReturnValues.VALUES, content);
        return ret;
    }

    public String fillInTemplate(final Print print)
        throws EFapsException
    {
        String content = "no template?";
        final var eval = print.linkto(CIPOS.UpdateTemplateMapping.TemplateLink).attribute(CIPOS.UpdateTemplate.Template)
                        .attributeSet(CIPOS.UpdateTemplateMapping.MappingSet).attribute("Key").as("key")
                        .attributeSet(CIPOS.UpdateTemplateMapping.MappingSet).attribute("Value").as("value")
                        .evaluate();
        if (eval.next()) {
            final var template = eval.get(CIPOS.UpdateTemplate.Template);
            LOG.info("template: {}", template);

            final var mapping = new HashMap<String, Object>();
            final var keys = eval.<List<String>>get("key");
            final var values = eval.<List<String>>get("value");
            if (keys != null) {
                final var keysIter = keys.iterator();
                for (final String value : values) {
                    mapping.put(keysIter.next(), value);
                }
            }
            content = StringSubstitutor.replace(template, mapping);
        }

        return content;
    }

    public String getFilledInTemplate(final Instance backendInst,
                                      final String templateOid)
        throws EFapsException
    {
        final var templateInst = Instance.get(templateOid);
        final var print = EQL.builder().print().query(CIPOS.UpdateTemplateMapping).where()
                        .attribute(CIPOS.UpdateTemplateMapping.BackendLink).eq(backendInst)
                        .and()
                        .attribute(CIPOS.UpdateTemplateMapping.TemplateLink).eq(templateInst)
                        .select();
        return fillInTemplate(print);
    }

}
