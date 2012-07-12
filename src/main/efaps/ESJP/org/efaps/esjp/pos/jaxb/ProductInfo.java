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


package org.efaps.esjp.pos.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "pos4a_ProductInfoExt")
@XmlType(name = "pos4a.ProductInfoExt")
@EFapsUUID("89269485-e6a8-486b-86e3-f738f4d966a1")
@EFapsRevision("$Rev$")
public class ProductInfo
{

    @XmlAttribute(name = "id")
    private String uuid;

    @XmlAttribute(name = "reference")
    private String name;

    @XmlElement(name = "name")
    private String description;

    @XmlElement(name = "categoryid")
    private String categoryUUID;

    @XmlElement(name = "code")
    private String barCode;

    @XmlElement(name = "taxcategoryid")
    private String taxUUID;

    @XmlElement(name = "priceBuy")
    private double priceBuy;

    @XmlElement(name = "priceSell")
    private double priceSell;

    /**
     * Getter method for the instance variable {@link #uuid}.
     *
     * @return value of instance variable {@link #uuid}
     */
    public String getUuid()
    {
        return this.uuid;
    }

    /**
     * Setter method for instance variable {@link #uuid}.
     *
     * @param _uuid value for instance variable {@link #uuid}
     */
    public void setUuid(final String _uuid)
    {
        this.uuid = _uuid;
    }

    /**
     * Getter method for the instance variable {@link #name}.
     *
     * @return value of instance variable {@link #name}
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Setter method for instance variable {@link #name}.
     *
     * @param _name value for instance variable {@link #name}
     */
    public void setName(final String _name)
    {
        this.name = _name;
    }

    /**
     * Getter method for the instance variable {@link #description}.
     *
     * @return value of instance variable {@link #description}
     */
    public String getDescription()
    {
        return this.description;
    }

    /**
     * Setter method for instance variable {@link #description}.
     *
     * @param _description value for instance variable {@link #description}
     */
    public void setDescription(final String _description)
    {
        this.description = _description;
    }

    /**
     * Getter method for the instance variable {@link #categoryUUID}.
     *
     * @return value of instance variable {@link #categoryUUID}
     */
    public String getCategoryUUID()
    {
        return this.categoryUUID;
    }

    /**
     * Setter method for instance variable {@link #categoryUUID}.
     *
     * @param _categoryUUID value for instance variable {@link #categoryUUID}
     */
    public void setCategoryUUID(final String _categoryUUID)
    {
        this.categoryUUID = _categoryUUID;
    }

    /**
     * Getter method for the instance variable {@link #barCode}.
     *
     * @return value of instance variable {@link #barCode}
     */
    public String getBarCode()
    {
        return this.barCode;
    }

    /**
     * Setter method for instance variable {@link #barCode}.
     *
     * @param _barCode value for instance variable {@link #barCode}
     */

    public void setBarCode(final String _barCode)
    {
        this.barCode = _barCode;
    }

    /**
     * Getter method for the instance variable {@link #taxUUID}.
     *
     * @return value of instance variable {@link #taxUUID}
     */
    public String getTaxUUID()
    {
        return this.taxUUID;
    }

    /**
     * Setter method for instance variable {@link #taxUUID}.
     *
     * @param _taxUUID value for instance variable {@link #taxUUID}
     */

    public void setTaxUUID(final String _taxUUID)
    {
        this.taxUUID = _taxUUID;
    }

    /**
     * Getter method for the instance variable {@link #priceBuy}.
     *
     * @return value of instance variable {@link #priceBuy}
     */
    public double getPriceBuy()
    {
        return this.priceBuy;
    }

    /**
     * Setter method for instance variable {@link #priceBuy}.
     *
     * @param _priceBuy value for instance variable {@link #priceBuy}
     */
    public void setPriceBuy(final double _priceBuy)
    {
        this.priceBuy = _priceBuy;
    }

    /**
     * Getter method for the instance variable {@link #priceSell}.
     *
     * @return value of instance variable {@link #priceSell}
     */
    public double getPriceSell()
    {
        return this.priceSell;
    }

    /**
     * Setter method for instance variable {@link #priceSell}.
     *
     * @param _priceSell value for instance variable {@link #priceSell}
     */

    public void setPriceSell(final double _priceSell)
    {
        this.priceSell = _priceSell;
    }
}
