
package org.efaps.esjp.pos.jaxb;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.EFapsApplication;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "pos4a_CloseCash")
@XmlType(name = "pos4a.CloseCash")
@EFapsUUID("81da5bf4-bfd6-4f37-8427-441c3f33b4c6")
@EFapsApplication("eFapsApp-POS")
public class JmsCloseCash
{
    @XmlElement(name = "date")
    private java.util.Date m_dDate;

    @XmlAttribute(name = "host")
    private String m_sHost;

    public JmsCloseCash() {
    }

    public java.util.Date getM_dDate() {
        return this.m_dDate;
    }

    public void setM_dDate(final java.util.Date m_dDate) {
        this.m_dDate = m_dDate;
    }

    public String getM_sHost() {
        return this.m_sHost;
    }

    public void setM_sHost(final String m_sHost) {
        this.m_sHost = m_sHost;
    }

}
