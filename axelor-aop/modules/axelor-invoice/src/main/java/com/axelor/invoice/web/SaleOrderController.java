package com.axelor.invoice.web;

import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.inject.Beans;
import com.axelor.sales.db.SaleOrder;
import com.axelor.sales.db.SaleOrderLine;
import com.axelor.invoice.db.Invoice;
import com.axelor.invoice.db.InvoiceLine;
import com.axelor.invoice.db.InvoiceStatusSelect;
import com.axelor.invoice.db.repo.InvoiceRepository;
import com.axelor.invoice.db.repo.InvoiceLineRepository;
import com.axelor.i18n.I18n;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.axelor.meta.schema.actions.ActionView;

public class SaleOrderController {

    private static final Logger logger = LoggerFactory.getLogger(SaleOrderController.class);

    public void generateInvoiceFromSaleOrder(ActionRequest request, ActionResponse response) {
        EntityManager em = Beans.get(EntityManager.class);
        EntityTransaction tx = em.getTransaction();
        
        logger.info("generateInvoiceFromSaleOrder triggered.");
        
        try {
            tx.begin();

            SaleOrder saleOrder = request.getContext().asType(SaleOrder.class);
            logger.debug(" Received SaleOrder from context with ID: {}", saleOrder.getId());
            
            saleOrder = em.find(SaleOrder.class, saleOrder.getId());
            logger.debug("Fetched SaleOrder from DB with ID: {}", saleOrder != null ? saleOrder.getId() : "null");
            
            if (saleOrder == null || saleOrder.getSaleOrderLineList() == null || saleOrder.getSaleOrderLineList().isEmpty()) {
            	logger.warn(" SaleOrder is null or has no lines.");
            	response.setError("Sale Order or its lines are empty. Cannot generate invoice.");
                return;
            }

            Invoice invoice = new Invoice();
            invoice.setSaleOrder(saleOrder);
            invoice.setCustomer(saleOrder.getCustomer());
            invoice.setStatusSelect(InvoiceStatusSelect.DRAFT);
            invoice.setInvoiceDateT(LocalDateTime.now());

            logger.info("Creating invoice for SaleOrder ID: {}", saleOrder.getId());
            
            BigDecimal totalInTax = BigDecimal.ZERO;
            List<InvoiceLine> invoiceLines = new ArrayList<>();

            for (SaleOrderLine saleLine : saleOrder.getSaleOrderLineList()) {
            	
            	 logger.debug("Processing SaleOrderLine with Product: {}", saleLine.getProduct().getName());
            	 
                InvoiceLine invoiceLine = new InvoiceLine();
                invoiceLine.setProduct(saleLine.getProduct());
                invoiceLine.setDescription(saleLine.getProduct().getName());
                invoiceLine.setQuantity(saleLine.getQuantity());
                invoiceLine.setUnitPriceUntaxed(saleLine.getUnitPriceUntaxed());
                invoiceLine.setExTaxTotal(saleLine.getExTaxTotal());
                invoiceLine.setTaxRate(saleLine.getTaxRate());

                BigDecimal inTaxTotal = saleLine.getExTaxTotal()
                    .multiply(BigDecimal.ONE.add(saleLine.getTaxRate().divide(BigDecimal.valueOf(100))));
                invoiceLine.setInTaxTotal(inTaxTotal);
                totalInTax = totalInTax.add(inTaxTotal);

                invoiceLine.setInvoice(invoice);
                invoiceLines.add(invoiceLine);
                
                
                logger.debug(" Created InvoiceLine with inTaxTotal: {}", inTaxTotal);
            }

            invoice.setInvoiceLineList(invoiceLines);
            invoice.setInTaxTotal(totalInTax);

            logger.info("Saving Invoice with Total In Tax: {}", totalInTax);
            
            invoice = Beans.get(InvoiceRepository.class).save(invoice);
            for (InvoiceLine line : invoiceLines) {
                Beans.get(InvoiceLineRepository.class).save(line);
            }

            tx.commit();

            logger.info("Saving Invoice with ID {}",invoice.getId());
            
            // Set success response with view
			/* response.setAlert("Invoice generated successfully."); */
            response.setReload(true);
            
            // Set the view using ActionView
            response.setView(
                ActionView.define(I18n.get("Invoices"))

                    .model("com.axelor.invoice.db.Invoice")
                    .add("form", "invoice-form")     
                    .param("forceEdit", Boolean.TRUE.toString())
                    .context("_showRecord", String.valueOf(invoice.getId()))
                    .map()
            );
            
            
        } catch (Exception e) {
            if (tx.isActive()) {
                tx.rollback();
            }
            logger.error("Error generating invoice from sale order", e);
            response.setError("Failed to generate invoice from sale order: " + e.getMessage());
        }
    }
}








