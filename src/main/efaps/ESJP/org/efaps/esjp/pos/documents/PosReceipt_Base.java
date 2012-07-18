package org.efaps.esjp.pos.documents;

import java.math.BigDecimal;

import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.pos.jaxb.TicketInfo;
import org.efaps.esjp.sales.document.DocumentSum;
import org.efaps.util.EFapsException;


@EFapsUUID("a5ee4a8e-ccd3-44b0-8ca5-66aea9c11bcc")
@EFapsRevision("$Rev: 6430 $")
public class PosReceipt_Base extends DocumentSum{

	public Return createTicketInfo(final TicketInfo _ticket) throws EFapsException
	    {
	        createTicket(_ticket);
	        return new Return();
	    }

	protected CreatedDoc createTicket(final TicketInfo _ticket) throws EFapsException
	    {
        final Insert insert = new Insert(CIPOS.Receipt);

        insert.add(CIPOS.Receipt.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.RateNetTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.CrossTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.NetTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.DiscountTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.Date, _ticket.getDate());
        insert.add(CIPOS.Receipt.Name, _ticket.getHost()+" - "+_ticket.getTicketId());
        insert.add(CIPOS.Receipt.Status, ((Long) Status.find(CIPOS.ReceiptStatus.uuid, "Open").getId()).toString());
        insert.add(CIPOS.Receipt.Note, "");
        insert.add(CIPOS.Receipt.DueDate, _ticket.getDate());
        insert.add(CIPOS.Receipt.CurrencyId, 1);
        insert.add(CIPOS.Receipt.RateCurrencyId, 1);
        insert.add(CIPOS.Receipt.Rate, new Object[]{ BigDecimal.ONE, BigDecimal.ONE });
        insert.execute();

        final CreatedDoc createdDoc = new CreatedDoc(insert.getInstance());
        //createPositions(_parameter, calcList, createdDoc);
        return createdDoc;

	    }

}
