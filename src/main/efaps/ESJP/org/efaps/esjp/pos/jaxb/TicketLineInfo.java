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
import javax.xml.bind.annotation.XmlElement;
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
@XmlType(name = "pos4a.TicketLineInfo")
@EFapsUUID("375629cc-bdf8-4d73-9cf3-2ff2b9bb3ea8")
@EFapsRevision("$Rev$")
public class TicketLineInfo
{
    @XmlElement(name = "id")
    private int lineId;

    @XmlElement(name = "productid")
    private String productUUID;

    /**
     * Getter method for the instance variable {@link #lineId}.
     *
     * @return value of instance variable {@link #lineId}
     */
    public int getLineId()
    {
        return this.lineId;
    }

    /**
     * Setter method for instance variable {@link #lineId}.
     *
     * @param _lineId value for instance variable {@link #lineId}
     */
    public void setLineId(final int _lineId)
    {
        this.lineId = _lineId;
    }

    /**
     * Getter method for the instance variable {@link #productUUID}.
     *
     * @return value of instance variable {@link #productUUID}
     */
    public String getProductUUID()
    {
        return this.productUUID;
    }

    /**
     * Setter method for instance variable {@link #productUUID}.
     *
     * @param _productUUID value for instance variable {@link #productUUID}
     */

    public void setProductUUID(final String _productUUID)
    {
        this.productUUID = _productUUID;
    }
}
