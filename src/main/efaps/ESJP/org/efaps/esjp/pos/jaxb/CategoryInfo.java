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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.EFapsApplication;


/**
 * TODO comment!
 *
 * @author The eFaps Team
 * @version $Id$
 */
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "pos4a_CategoryInfo")
@XmlType(name = "pos4a.CategoryInfo")
@EFapsUUID("e7e9f5e6-cf90-4680-babf-c50a9ba49419")
@EFapsApplication("eFapsApp-POS")
public class CategoryInfo
{
    @XmlAttribute(name = "id")
    private String uuid;

    @XmlAttribute(name = "name")
    private String name;

    @XmlElement(name="parentId")
    private String parentUUID;

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
     * Getter method for the instance variable {@link #parentUUID}.
     *
     * @return value of instance variable {@link #parentUUID}
     */
    public String getParentUUID()
    {
        return this.parentUUID;
    }

    /**
     * Setter method for instance variable {@link #parentUUID}.
     *
     * @param _parentUUID value for instance variable {@link #parentUUID}
     */
    public void setParentUUID(final String _parentUUID)
    {
        this.parentUUID = _parentUUID;
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this).append("uuid", this.uuid).append("name", this.name).toString();
    }
}
