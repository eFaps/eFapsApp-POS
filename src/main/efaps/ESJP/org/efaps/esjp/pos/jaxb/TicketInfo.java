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

import java.util.Date;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
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
@XmlRootElement(name = "pos4a_TicketInfo")
@XmlType(name = "pos4a.TicketInfo")
@EFapsUUID("67ae60c8-dbf9-48b6-b2c7-67b99c2e290f")
@EFapsRevision("$Rev$")
public class TicketInfo
{

    @XmlAttribute(name = "id")
    private String uuid;

    @XmlElement(name = "ticketType")
    private int ticketType;

    @XmlAttribute(name = "ticketId")
    private int ticketId;

    @XmlElement(name = "date")
    private Date date;

    @XmlElement(name = "user")
    private UserInfo user;


    @XmlAttribute(name = "activeCash")
    private String activeCash;

    @XmlAttribute(name = "host")
    private String host;


	@XmlElementWrapper(name = "ticketLineInfos")
    @XmlElement(name = "ticketLineInfo")
    private List<TicketLineInfo> ticketLines;

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
     * Getter method for the instance variable {@link #ticketType}.
     *
     * @return value of instance variable {@link #ticketType}
     */
    public int getTicketType()
    {
        return this.ticketType;
    }

    /**
     * Setter method for instance variable {@link #ticketType}.
     *
     * @param _ticketType value for instance variable {@link #ticketType}
     */

    public void setTicketType(final int _ticketType)
    {
        this.ticketType = _ticketType;
    }

    /**
     * Getter method for the instance variable {@link #ticketId}.
     *
     * @return value of instance variable {@link #ticketId}
     */
    public int getTicketId()
    {
        return this.ticketId;
    }

    /**
     * Setter method for instance variable {@link #ticketId}.
     *
     * @param _ticketId value for instance variable {@link #ticketId}
     */
    public void setTicketId(final int _ticketId)
    {
        this.ticketId = _ticketId;
    }

    /**
     * Getter method for the instance variable {@link #date}.
     *
     * @return value of instance variable {@link #date}
     */
    public Date getDate()
    {
        return this.date;
    }

    /**
     * Setter method for instance variable {@link #date}.
     *
     * @param _date value for instance variable {@link #date}
     */
    public void setDate(final Date _date)
    {
        this.date = _date;
    }

    /**
     * Getter method for the instance variable {@link #user}.
     *
     * @return value of instance variable {@link #user}
     */
    public UserInfo getUser()
    {
        return this.user;
    }

    /**
     * Setter method for instance variable {@link #user}.
     *
     * @param _user value for instance variable {@link #user}
     */

    public void setUser(final UserInfo _user)
    {
        this.user = _user;
    }

    /**
     * Getter method for the instance variable {@link #activeCash}.
     *
     * @return value of instance variable {@link #activeCash}
     */
    public String getActiveCash()
    {
        return this.activeCash;
    }

    /**
     * Setter method for instance variable {@link #activeCash}.
     *
     * @param _activeCash value for instance variable {@link #activeCash}
     */

    public void setActiveCash(final String _activeCash)
    {
        this.activeCash = _activeCash;
    }

    public String getHost() {
		return this.host;
	}

	public void setHost(String _host) {
		this.host = _host;
	}


    /**
     * Getter method for the instance variable {@link #ticketLines}.
     *
     * @return value of instance variable {@link #ticketLines}
     */
    public List<TicketLineInfo> getTicketLines()
    {
        return this.ticketLines;
    }

    /**
     * Setter method for instance variable {@link #ticketLines}.
     *
     * @param _ticketLines value for instance variable {@link #ticketLines}
     */

    public void setTicketLines(final List<TicketLineInfo> _ticketLines)
    {
        this.ticketLines = _ticketLines;
    }
}
