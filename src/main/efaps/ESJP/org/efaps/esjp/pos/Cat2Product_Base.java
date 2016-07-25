/*
 * Copyright 2003 - 2012 The eFaps Team
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
 * Revision:        $Rev$
 * Last Changed:    $Date$
 * Last Changed By: $Author$
 */

package org.efaps.esjp.pos;

import java.io.StringWriter;
import java.math.BigDecimal;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.efaps.admin.event.Parameter;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsApplication;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.ci.CIType;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.PrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.SelectBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.pos.jaxb.ProductInfo;
import org.efaps.esjp.sales.Tax;
import org.efaps.esjp.sales.Tax_Base;
import org.efaps.esjp.sales.Tax_Base.TaxRate;
import org.efaps.util.EFapsException;
import org.joda.time.LocalDate;

/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@EFapsUUID("a09a00df-a428-4df4-851a-708b3f7508af")
@EFapsApplication("eFapsApp-POS")
public abstract class Cat2Product_Base
{

    protected void enque4SendMsg(final Parameter _parameter,
                                 final Instance _cat2ProdInst,
                                 final CIType _messageType)
        throws EFapsException
    {

        final PrintQuery relPrint = new PrintQuery(_cat2ProdInst);
        final SelectBuilder selCatUUID = new SelectBuilder().linkto(CIPOS.Category2Product.FromLink)
                        .attribute(CIPOS.Category.UUID);
        final SelectBuilder selCatOID = new SelectBuilder().linkto(CIPOS.Category2Product.FromLink).oid();
        final SelectBuilder prodSel = new SelectBuilder().linkto(CIPOS.Category2Product.ToLink);
        final SelectBuilder prodSelName = new SelectBuilder(prodSel).attribute(CIProducts.ProductAbstract.Name);
        final SelectBuilder prodSelDesc = new SelectBuilder(prodSel).attribute(CIProducts.ProductAbstract.Description);
        final SelectBuilder prodSelBarcode = new SelectBuilder(prodSel).attribute(CIProducts.ProductAbstract.Barcode);
        final SelectBuilder prodSelUUID = new SelectBuilder(prodSel).attribute(CIProducts.ProductAbstract.UUID);
        final SelectBuilder prodSelTaxcat= new SelectBuilder(prodSel).attribute(CIProducts.ProductAbstract.TaxCategory);

        relPrint.addAttribute(CIPOS.Category2Product.NetPrice);
        relPrint.addSelect(selCatUUID, selCatOID, prodSelName, prodSelDesc, prodSelBarcode, prodSelUUID, prodSelTaxcat);
        relPrint.execute();

        final ProductInfo product = new ProductInfo();
        product.setName(relPrint.<String>getSelect(prodSelName));
        product.setCategoryUUID(relPrint.<String>getSelect(selCatUUID));
        product.setDescription(relPrint.<String>getSelect(prodSelDesc));
        product.setBarCode(relPrint.<String>getSelect(prodSelBarcode));
        product.setUuid(relPrint.<String>getSelect(prodSelUUID));
        product.setPriceSell(relPrint.<BigDecimal>getAttribute(CIPOS.Category2Product.NetPrice).doubleValue());

        final Tax tax = Tax_Base.get(relPrint.<Long>getSelect(prodSelTaxcat));
        final TaxRate rate = tax.getTaxRate(new LocalDate());
        product.setTaxUUID(rate.getUuid());

        try {
            final JAXBContext jc = JAXBContext.newInstance(ProductInfo.class);
            final Marshaller marschaller = jc.createMarshaller();
            final StringWriter writer = new StringWriter();
            marschaller.marshal(product, writer);

            final Instance catInst = Instance.get(relPrint.<String>getSelect(selCatOID));
            final QueryBuilder queryBldr = new QueryBuilder(CIPOS.Category2SubscriptionCategory);
            queryBldr.addWhereAttrEqValue(CIPOS.Category2SubscriptionCategory.FromLink,
                            new Category().getParent(catInst).getId());
            final MultiPrintQuery multi = queryBldr.getPrint();
            final SelectBuilder jmsOidSel = new SelectBuilder().linkto(CIPOS.Category2SubscriptionCategory.ToLink)
                            .linkto(CIPOS.SubscriptionCategory.JmsLink).oid();
            multi.addSelect(jmsOidSel);
            multi.execute();
            while (multi.next()) {
                final String jmsOid = multi.<String>getSelect(jmsOidSel);
                final Instance jmsInst = Instance.get(jmsOid);
                if (jmsInst.isValid()) {
                    final Insert insert = new Insert(_messageType);
                    insert.add(CIPOS.MessageAbstract.Content, writer.toString());
                    insert.add(CIPOS.MessageAbstract.JmsLink, jmsInst.getId());
                    insert.execute();
                }
            }

        } catch (final JAXBException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    /**
     * @param _parameter parameter as passed by the eFaps API
     * @return Return
     * @throws EFapsException on error
     */
    public Return updateTrigger(final Parameter _parameter)
        throws EFapsException
    {
        enque4SendMsg(_parameter, _parameter.getInstance(), CIPOS.MessageCategory2ProductUpdate);
        return new Return();
    }

    public Return insertTrigger(final Parameter _parameter)
        throws EFapsException
    {
        enque4SendMsg(_parameter, _parameter.getInstance(), CIPOS.MessageCategory2ProductInsert);
        return new Return();
    }
}
