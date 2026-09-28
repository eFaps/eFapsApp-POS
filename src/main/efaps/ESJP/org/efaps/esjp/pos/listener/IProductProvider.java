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

import org.efaps.admin.program.esjp.IEsjpListener;
import org.efaps.eql.builder.Print;
import org.efaps.pos.dto.ProductDto;
import org.efaps.util.EFapsException;

public interface IProductProvider
    extends IEsjpListener
{

    List<ProductDto> evalProducts(final String identifier,
                                  final Print print,
                                  boolean caching)
        throws EFapsException;

    @Override
    default int getWeight()
    {
        return 0;
    }
}
