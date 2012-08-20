
package org.efaps.esjp.pos.jaxb;


import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;


@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "pos4a_CloseCash")
@XmlType(name = "pos4a.CloseCash")
@EFapsUUID("81da5bf4-bfd6-4f37-8427-441c3f33b4c6")
@EFapsRevision("$Rev: 7883 $")
public class JmsCloseCash
{
    @XmlElement(name = "date")
    private java.util.Date m_dDate;

    @XmlAttribute(name = "host")
    private String m_sHost;

    public JmsCloseCash() {
    }

    public java.util.Date getM_dDate() {
        return m_dDate;
    }

    public void setM_dDate(java.util.Date m_dDate) {
        this.m_dDate = m_dDate;
    }

    public String getM_sHost() {
        return m_sHost;
    }

    public void setM_sHost(String m_sHost) {
        this.m_sHost = m_sHost;
    }

}
