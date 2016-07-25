package org.efaps.esjp.pos.jaxb;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.admin.program.esjp.EFapsApplication;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "pos4a_PaymentInfo")
@XmlType(name = "pos4a.PaymentInfo")
@EFapsUUID("5ee59b9c-dd96-4347-92e0-1058901903d4")
@EFapsApplication("eFapsApp-POS")
public  class PaymentInfo
{
    @XmlElement(name = "totalPayment")
    private double paymentTotal;

    @XmlElement(name = "namePayment")
    private String paymentName;

    public double getPaymentTotal()
    {
        return this.paymentTotal;
    }

    public void setTotal(final double paymentTotal)
    {
        this.paymentTotal = paymentTotal;
    }

    public String getPaymentName()
    {
        return this.paymentName;
    }

    public void setPaymentName(final String paymentName)
    {
        this.paymentName = paymentName;
    }

}
