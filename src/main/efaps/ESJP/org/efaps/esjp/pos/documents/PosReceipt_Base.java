package org.efaps.esjp.pos.documents;

import java.math.BigDecimal;
import java.util.UUID;

import org.efaps.admin.common.SystemConfiguration;
import org.efaps.admin.datamodel.Classification;
import org.efaps.admin.datamodel.Dimension;
import org.efaps.admin.datamodel.Status;
import org.efaps.admin.event.Return;
import org.efaps.admin.program.esjp.EFapsRevision;
import org.efaps.admin.program.esjp.EFapsUUID;
import org.efaps.db.Insert;
import org.efaps.db.Instance;
import org.efaps.db.MultiPrintQuery;
import org.efaps.db.QueryBuilder;
import org.efaps.db.Update;
import org.efaps.esjp.ci.CIPOS;
import org.efaps.esjp.ci.CIProducts;
import org.efaps.esjp.erp.CommonDocument_Base.CreatedDoc;
import org.efaps.esjp.pos.jaxb.TicketInfo;
import org.efaps.esjp.pos.jaxb.TicketLineInfo;
import org.efaps.esjp.sales.document.AbstractDocumentSum;
import org.efaps.util.EFapsException;

@EFapsUUID("a5ee4a8e-ccd3-44b0-8ca5-66aea9c11bcc")
@EFapsRevision("$Rev$")
public class PosReceipt_Base
    extends AbstractDocumentSum
{
    public Return createTicketInfo(final TicketInfo _ticket)
        throws EFapsException
    {
        final CreatedDoc doc = createTicket(_ticket);
        createdClassification(_ticket, doc);
        new PosPayment().create(_ticket, doc);
        return new Return();
    }

    protected CreatedDoc createTicket(final TicketInfo _ticket)
        throws EFapsException
    {
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");
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
        insert.add(CIPOS.Receipt.CurrencyId, baseCurrInst.getId());
        insert.add(CIPOS.Receipt.RateCurrencyId, baseCurrInst.getId());
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
        final Instance baseCurrInst = SystemConfiguration.get(
                        UUID.fromString("c9a1cbc3-fd35-4463-80d2-412422a3802f")).getLink("CurrencyBase");

        double netTotal=0;
        double crossTotal=0;

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
                crossTotal= crossTotal + t.getTotal();
                netTotal= netTotal + (t.getTotal()- t.getPriceTax());
                posIns.add(CIPOS.ReceiptPosition.ReceiptLink, _createdDoc.getInstance().getId());
                posIns.add(CIPOS.ReceiptPosition.PositionNumber, t.getLineId());
                posIns.add(CIPOS.ReceiptPosition.Product, idProd);
                posIns.add(CIPOS.ReceiptPosition.ProductDesc, t.getProductName());
                posIns.add(CIPOS.ReceiptPosition.Quantity, t.getQuantity());
                posIns.add(CIPOS.ReceiptPosition.UoM, Dimension.get(UUID.fromString("0aa00110-fdf1-4c85-ab72-22cb4e53422a")).getBaseUoM()
                                .getId());
                posIns.add(CIPOS.ReceiptPosition.Tax, 1);
                posIns.add(CIPOS.ReceiptPosition.Discount, BigDecimal.ZERO);
                posIns.add(CIPOS.ReceiptPosition.CurrencyId, baseCurrInst.getId());
                posIns.add(CIPOS.ReceiptPosition.Rate, new Object[] { BigDecimal.ONE, BigDecimal.ONE });
                posIns.add(CIPOS.ReceiptPosition.RateCurrencyId, baseCurrInst.getId());
                posIns.add(CIPOS.ReceiptPosition.CrossUnitPrice, t.getPrice());
                posIns.add(CIPOS.ReceiptPosition.NetUnitPrice, t.getPrice());
                posIns.add(CIPOS.ReceiptPosition.CrossPrice,t.getTotal());
                posIns.add(CIPOS.ReceiptPosition.NetPrice, t.getTotal()- t.getPriceTax());
                posIns.add(CIPOS.ReceiptPosition.DiscountNetUnitPrice, BigDecimal.ZERO);
                posIns.execute();

                _createdDoc.addPosition(posIns.getInstance());
            }

            final Update update = new Update(_createdDoc.getInstance());
            update.add(CIPOS.Receipt.CrossTotal, crossTotal);
            update.add(CIPOS.Receipt.NetTotal, netTotal);
            update.execute();
        }
    }

    protected void createdClassification(final TicketInfo _ticket,
                                         final CreatedDoc doc)
        throws EFapsException
    {
        final QueryBuilder queryBldr = new QueryBuilder(CIPOS.POS);
        queryBldr.addWhereAttrEqValue(CIPOS.POS.Name, _ticket.getHost());
        final MultiPrintQuery multi = queryBldr.getPrint();
        multi.addAttribute(CIPOS.POS.ID);
        multi.execute();
        Long idPos = null;
        while (multi.next()) {
            idPos = multi.<Long>getAttribute(CIPOS.POS.ID);
        }

        // create classifications
        final Classification classification1 = (Classification) CIPOS.ReceiptClass.getType();
        final Insert relInsert1 = new Insert(classification1.getClassifyRelationType());
        relInsert1.add(classification1.getRelLinkAttributeName(), doc.getInstance().getId());
        relInsert1.add(classification1.getRelTypeAttributeName(), classification1.getId());
        relInsert1.execute();

        final Insert classInsert1 = new Insert(classification1);
        classInsert1.add(classification1.getLinkAttributeName(), doc.getInstance().getId());
        classInsert1.add(CIPOS.ReceiptClass.UserName, _ticket.getUser().getName());
        classInsert1.add(CIPOS.ReceiptClass.ActiveCash, _ticket.getActiveCash());
        classInsert1.add(CIPOS.ReceiptClass.POSLink, idPos);
        classInsert1.execute();
    }



}
