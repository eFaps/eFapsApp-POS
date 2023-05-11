/*
 * Copyright 2003 - 2023 The eFaps Team
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
package org.efaps.esjp.pos;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Instance;
import org.efaps.eql.EQL;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.db.InstanceUtils;
import org.efaps.esjp.products.listener.IPriceListListener;
import org.efaps.util.EFapsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@EFapsUUID("7addf3b8-14b2-4942-bd88-c0d8442c8309")
@EFapsApplication("eFapsApp-POS")
public abstract class PriceGroup_Base
    implements IPriceListListener
{

    private static final Logger LOG = LoggerFactory.getLogger(PriceGroup.class);

    @Override
    public boolean groupApplies(Parameter _parameter,
                                Instance _priceGroupInstance)
        throws EFapsException
    {
        boolean ret = false;
        LOG.debug("Checking if Backend-PriceGroup applies: {}", _priceGroupInstance);
        final var identifier = _parameter.getParameterValue("identifier");
        if (InstanceUtils.isType(_priceGroupInstance, CIPOS.PriceGroupBackend) && identifier != null) {
            final var eval = EQL.builder()
                            .print()
                            .query(CIPOS.Backend)
                            .where().attribute(CIPOS.Backend.Identifier).eq(identifier)
                            .select()
                            .instance()
                            .evaluate();
            if (eval.next()) {
                final var backendInst = eval.inst();
                ret = EQL.builder()
                                .print()
                                .query(CIPOS.PriceGroupBackend2Backend)
                                .where()
                                .attribute(CIPOS.PriceGroupBackend2Backend.FromLink).eq(_priceGroupInstance)
                                .and()
                                .attribute(CIPOS.PriceGroupBackend2Backend.ToLink).eq(backendInst)
                                .select()
                                .instance()
                                .evaluate().next();
            }
        }
        LOG.debug("Evaluation result: {}", ret);
        return ret;
    }
}
