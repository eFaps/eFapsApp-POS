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
@XmlRootElement(name = "pos4a_TaxInfo")
@XmlType(name = "pos4a.TaxInfo")
@EFapsUUID("7f6ec6cf-c2a4-42a0-bcf6-db895cd5cd6d")
@EFapsRevision("$Rev$")
public class TaxInfo
{
    /**
     * The UUID of this Tax.
     */
    @XmlAttribute(name = "id")
    private String uuid;

    /**
     * The name of this Tax.
     */
    @XmlAttribute(name = "name")
    private String name;

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
}
