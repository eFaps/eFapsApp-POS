package org.efaps.esjp.pos.documents;

import java.math.BigDecimal;
import java.util.UUID;

import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.pos.jaxb.TicketInfo;
import org.efaps.esjp.pos.jaxb.TicketLineInfo;
import org.efaps.esjp.sales.document.DocumentSum;
import org.efaps.util.EFapsException;


@EFapsUUID("a5ee4a8e-ccd3-44b0-8ca5-66aea9c11bcc")
@EFapsRevision("$Rev: 6430 $")
public class PosReceipt_Base
    extends DocumentSum
{

    public Return createTicketInfo(final TicketInfo _ticket)
        throws EFapsException
    {
        final CreatedDoc doc = createTicket(_ticket);

        // create classifications
        final Classification classification1 = (Classification) CIPOS.ReceiptClass.getType();
        final Insert relInsert1 = new Insert(classification1.getClassifyRelationType());
        relInsert1.add(classification1.getRelLinkAttributeName(), doc.getInstance().getId());
        relInsert1.add(classification1.getRelTypeAttributeName(), classification1.getId());
        relInsert1.execute();

        final Insert classInsert1 = new Insert(classification1);
        classInsert1.add(classification1.getLinkAttributeName(), doc.getInstance().getId());
        classInsert1.execute();

        return new Return();
    }

    protected CreatedDoc createTicket(final TicketInfo _ticket)
        throws EFapsException
    {
        final Insert insert = new Insert(CIPOS.Receipt);

        insert.add(CIPOS.Receipt.RateCrossTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.RateNetTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.RateDiscountTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.CrossTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.NetTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.DiscountTotal, BigDecimal.ZERO);
        insert.add(CIPOS.Receipt.Date, _ticket.getDate());
        insert.add(CIPOS.Receipt.Name, String.valueOf(_ticket.getHost() + " - " + _ticket.getTicketId()));
        insert.add(CIPOS.Receipt.Status, ((Long) Status.find(CIPOS.ReceiptStatus.uuid, "Open").getId()).toString());
        insert.add(CIPOS.Receipt.Note, "");
        insert.add(CIPOS.Receipt.DueDate, _ticket.getDate());
        insert.add(CIPOS.Receipt.CurrencyId, 1);
        insert.add(CIPOS.Receipt.RateCurrencyId, 1);
        insert.add(CIPOS.Receipt.Rate, new Object[] { BigDecimal.ONE, BigDecimal.ONE });
        insert.execute();

        final CreatedDoc createdDoc = new CreatedDoc(insert.getInstance());
        createPositions(_ticket, createdDoc);
        return createdDoc;

    }

    protected void createPositions(final TicketInfo _ticket,
                                   final CreatedDoc _createdDoc)
        throws EFapsException
    {
        for (final TicketLineInfo t : _ticket.getTicketLines()) {
            final Insert posIns = new Insert(CIPOS.ReceiptPosition);

            final QueryBuilder queryBldr = new QueryBuilder(CIProducts.ProductAbstract);
            queryBldr.addWhereAttrEqValue(CIProducts.ProductAbstract.UUID, t.getProductUUID());
            final MultiPrintQuery multi = queryBldr.getPrint();
            multi.execute();
            long idProd = 0;
            while (multi.next()) {
                idProd = multi.getCurrentInstance().getId();
            }
            if (idProd > 0) {
                posIns.add(CIPOS.ReceiptPosition.ReceiptLink, _createdDoc.getInstance().getId());
                posIns.add(CIPOS.ReceiptPosition.PositionNumber, t.getLineId());
                posIns.add(CIPOS.ReceiptPosition.Product, idProd);
                posIns.add(CIPOS.ReceiptPosition.ProductDesc, t.getProductName());
                posIns.add(CIPOS.ReceiptPosition.Quantity, t.getQuantity());
                posIns.add(CIPOS.ReceiptPosition.UoM, Dimension.get(UUID.fromString("0aa00110-fdf1-4c85-ab72-22cb4e53422a")).getBaseUoM()
                                .getId());
                posIns.add(CIPOS.ReceiptPosition.Tax, 1);
                posIns.add(CIPOS.ReceiptPosition.Discount, BigDecimal.ZERO);
                posIns.add(CIPOS.ReceiptPosition.CurrencyId, 1);
                posIns.add(CIPOS.ReceiptPosition.Rate, new Object[] { BigDecimal.ONE, BigDecimal.ONE });
                posIns.add(CIPOS.ReceiptPosition.RateCurrencyId, 1);
                posIns.add(CIPOS.ReceiptPosition.CrossUnitPrice, t.getPrice());
                posIns.add(CIPOS.ReceiptPosition.NetUnitPrice, t.getPrice());
                posIns.add(CIPOS.ReceiptPosition.CrossPrice, t.getPrice());
                posIns.add(CIPOS.ReceiptPosition.NetPrice, t.getTotal());
                posIns.add(CIPOS.ReceiptPosition.DiscountNetUnitPrice, BigDecimal.ZERO);

                posIns.execute();
                _createdDoc.addPosition(posIns.getInstance());
            }
        }
    }
}
