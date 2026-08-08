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

import java.util.List;
import java.util.Set;

import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.pos.dto.InventoryEntryDto;

@EFapsUUID("76d45b9b-5c64-4c8d-bf25-3d9956dfe47c")
@EFapsApplication("eFapsApp-POS")
public interface IInventoryProvider
    extends IEsjpListener
{

    boolean evalInventory(final List<InventoryEntryDto> entries,
                          final Set<String> productOids);
}
