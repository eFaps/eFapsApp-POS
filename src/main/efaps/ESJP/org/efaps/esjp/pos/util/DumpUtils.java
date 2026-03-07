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
package org.efaps.esjp.pos.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Checkin;
import org.efaps.db.Context;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.common.file.FileUtil;
import org.efaps.esjp.ui.util.ValueUtils;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("7ee9ac34-4f7b-45d0-a86f-4a4e9fb3fe98")
@EFapsApplication("eFapsApp-POS")
public class DumpUtils
{

    private static final Logger LOG = LoggerFactory.getLogger(DumpUtils.class);

    public void prepareDump(final CIType ciType)
        throws EFapsException
    {
        final var label = ciType.getType().getName();
        final var allFiles = new ArrayList<File>();
        final var limit = getLimit();
        var next = true;
        var i = 0;
        while (next) {
            final var offset = i * limit;
            LOG.info("- {} Batch {} - {}", label, offset, offset + limit);
            final var products = getObjects(limit, offset);
            i++;
            next = !(products.size() < limit);
            final var fileName = String.format("%s_%03d", label, i);
            LOG.info("Preparing file ");
            final var objectMapper = ValueUtils.getObjectMapper();
            final var jsonFile = new FileUtil().getFile(fileName, "json");
            try {
                objectMapper.writeValue(jsonFile, products);
                LOG.info("Json file: {}", jsonFile);
                allFiles.add(jsonFile);
            } catch (final IOException e) {
                LOG.error("Catched", e);
            }
            Context.save();
        }
        LOG.info("All files: {}", allFiles);

        final var zipFile = new FileUtil().getFile(label, "zip");
        LOG.info("Creating zipfile: {}", zipFile);
        try (ZipArchiveOutputStream archive = new ZipArchiveOutputStream(new FileOutputStream(zipFile))) {
            for (final var file : allFiles) {
                try (FileInputStream fis = new FileInputStream(file)) {
                    final var entry = new ZipArchiveEntry(file, file.getName());
                    archive.putArchiveEntry(entry);
                    IOUtils.copy(fis, archive);
                    archive.closeArchiveEntry();
                } catch (final IOException e) {
                    e.printStackTrace();
                }
            }
            archive.finish();
        } catch (final IOException e) {
            e.printStackTrace();
        }

        Instance dumpInst;
        final var eval = EQL.builder().print().query(ciType).select().instance().evaluate();
        if (eval.next()) {
            dumpInst = eval.inst();
            EQL.builder().update(dumpInst).set(CIPOS.AbstractDump.UpdatedAt, OffsetDateTime.now()).execute();
        } else {
            dumpInst = EQL.builder().insert(ciType)
                            .set(CIPOS.AbstractDump.UpdatedAt, OffsetDateTime.now())
                            .execute();
        }
        LOG.info("Checkin for: {}", dumpInst.getOid());
        final var checkin = new Checkin(dumpInst);
        try {
            final var inputStream = new FileInputStream(zipFile);
            checkin.execute(zipFile.getName(), inputStream, Long.valueOf(zipFile.length()).intValue());
        } catch (final IOException e) {
            e.printStackTrace();
        }
        LOG.info("deleting files");
        for (final var file : allFiles) {
            file.delete();
        }
    }

    protected List<Object> getObjects(int limit,
                                      int offset)
        throws EFapsException
    {
        return Collections.emptyList();
    }

    protected int getLimit()
    {
        return 1500;
    }
}
