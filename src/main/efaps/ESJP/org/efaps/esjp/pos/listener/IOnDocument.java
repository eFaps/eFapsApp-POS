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
package org.efaps.esjp.pos.listener;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.db.Instance;
import org.efaps.pos.dto.AbstractDocumentDto;
import org.efaps.util.EFapsException;

@EFapsUUID("64261c7f-7e66-494d-9e8e-1c4c46eb67d4")
@EFapsApplication("eFapsApp-POS")
public interface IOnDocument
    extends IEsjpListener
{

    void afterCreate(final Instance docInst,
                     final AbstractDocumentDto payload)
        throws EFapsException;

    @Override
    default int getWeight()
    {
        return 0;
    }
}
