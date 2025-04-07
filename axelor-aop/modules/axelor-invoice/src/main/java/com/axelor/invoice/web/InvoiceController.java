package com.axelor.invoice.web;

import com.axelor.rpc.ActionRequest; // For ActionRequest
import com.axelor.rpc.ActionResponse; // For ActionResponse
import com.axelor.i18n.I18n;
import com.axelor.inject.Beans; // For Beans
import org.slf4j.Logger; // For Logger
import org.slf4j.LoggerFactory; // For LoggerFactory
import com.axelor.invoice.db.Invoice; // For Invoice
import com.axelor.invoice.db.repo.InvoiceRepository; // For InvoiceRepository
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.invoice.db.InvoiceStatusSelect; // For InvoiceStatusSelect5
import javax.persistence.EntityManager; // For EntityManager
import javax.persistence.EntityTransaction; // For EntityTransaction
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import com.axelor.invoice.db.InvoiceLine;



public class InvoiceController {

	private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

	@SuppressWarnings("unchecked")
	public void mergeInvoices(ActionRequest request, ActionResponse response) {
        EntityManager em = Beans.get(EntityManager.class);
        EntityTransaction transaction = em.getTransaction();

        try {
            // Get the context from the request
            Map<String, Object> context = request.getContext();

            // Retrieve the list of selected invoices safely
            Object mergeToInvoiceObj = context.get("mergeToInvoice");
            List<Map<String, Object>> mergeToInvoice;
            
            if (mergeToInvoiceObj instanceof Set) {
                mergeToInvoice = new ArrayList<>((Set<Map<String, Object>>) mergeToInvoiceObj);
            } else if (mergeToInvoiceObj instanceof List) {
                mergeToInvoice = (List<Map<String, Object>>) mergeToInvoiceObj;
            } else {
                response.setError("Invalid data format for mergeToInvoice.");
                return;
            }
            
            logger.info("Selected invoices: {}", mergeToInvoice);

            // Extract IDs of selected invoices
            List<Long> invoiceIds = mergeToInvoice.stream()
                .filter(invoice -> Boolean.TRUE.equals(invoice.get("selected")))
                .map(invoice -> ((Number) invoice.get("id")).longValue())
                .collect(Collectors.toList());

            if (invoiceIds.size() < 2) {
                response.setError("Please select at least two invoices to merge.");
                return;
            }

            transaction.begin();

            List<Invoice> invoices = Beans.get(InvoiceRepository.class).all().filter("self.id IN (?1)", invoiceIds)
                .fetch();

            Invoice mergedInvoice = new Invoice();
            mergedInvoice.setInvoiceDateT(LocalDateTime.now());
            mergedInvoice.setStatusSelect(InvoiceStatusSelect.DRAFT);
            mergedInvoice.setCustomer(invoices.get(0).getCustomer());

            Beans.get(InvoiceRepository.class).save(mergedInvoice);

            // Merge invoice lines
            List<InvoiceLine> allLines = new ArrayList<>();
            List<Invoice> mergedInvoiceList = new ArrayList<>();

            // Collect all invoice lines and add invoices to the merged list
            for (Invoice invoice : invoices) {
                allLines.addAll(invoice.getInvoiceLineList());
                mergedInvoiceList.add(invoice); // Add selected invoices to merged list
            }

            // Group by Product ID, Unit Price, Tax Rate, and Description
            Map<String, List<InvoiceLine>> groupedLines = allLines.stream()
                .collect(Collectors.groupingBy(line -> line.getProduct().getId() + "-" 
                    + line.getUnitPriceUntaxed() + "-" 
                    + line.getTaxRate() + "-" 
                    + line.getDescription()));

            List<InvoiceLine> mergedLines = new ArrayList<>();

            for (Map.Entry<String, List<InvoiceLine>> entry : groupedLines.entrySet()) {
                List<InvoiceLine> lines = entry.getValue();

                if (lines.size() > 1) {  // If there are multiple matching product lines
                    BigDecimal totalQuantity = BigDecimal.ZERO;
                    BigDecimal totalExTax = BigDecimal.ZERO;
                    BigDecimal totalInTax = BigDecimal.ZERO;

                    for (InvoiceLine line : lines) {
                        totalQuantity = totalQuantity.add(line.getQuantity());
                        totalExTax = totalExTax.add(line.getExTaxTotal());
                        totalInTax = totalInTax.add(line.getInTaxTotal());
                    }

                    // Create a merged invoice line
                    InvoiceLine mergedLine = new InvoiceLine();
                    mergedLine.setProduct(lines.get(0).getProduct());
                    mergedLine.setUnitPriceUntaxed(lines.get(0).getUnitPriceUntaxed());
                    mergedLine.setTaxRate(lines.get(0).getTaxRate());
                    mergedLine.setDescription(lines.get(0).getDescription());
                    mergedLine.setQuantity(totalQuantity);
                    mergedLine.setExTaxTotal(totalExTax);
                    mergedLine.setInTaxTotal(totalInTax);

                    mergedLines.add(mergedLine);
                } else {  // If only one invoice line exists (different product)
                    InvoiceLine originalLine = lines.get(0);

                    // Clone the invoice line to avoid duplicate key constraint issues
                    InvoiceLine clonedLine = new InvoiceLine();
                    clonedLine.setProduct(originalLine.getProduct());
                    clonedLine.setUnitPriceUntaxed(originalLine.getUnitPriceUntaxed());
                    clonedLine.setTaxRate(originalLine.getTaxRate());
                    clonedLine.setDescription(originalLine.getDescription());
                    clonedLine.setQuantity(originalLine.getQuantity());
                    clonedLine.setExTaxTotal(originalLine.getExTaxTotal());
                    clonedLine.setInTaxTotal(originalLine.getInTaxTotal());

                    mergedLines.add(clonedLine);
                }
            }
            	System.out.println(mergedInvoiceList);
            	 logger.info("mergedInvoiceList invoices: {}", mergedInvoiceList);
            	
            // Set merged invoice lines and merged invoice list in the new generated invoice
            mergedInvoice.setInvoiceLineList(mergedLines);
            mergedInvoice.setMergedInvoiceList(mergedInvoiceList); // Set selected invoices

            // Save the generated invoice
            Beans.get(InvoiceRepository.class).save(mergedInvoice);
            
            
            

            for (Invoice invoice : invoices) {
                invoice.setGeneratedInvoice(mergedInvoice);
				/* invoice.setArchived(true); */
                Beans.get(InvoiceRepository.class).save(invoice);
            }
            
            

            transaction.commit();
            
           
            response.setReload(true);
            
            response.setView(
                    ActionView.define(I18n.get("Invoices"))

                        .model("com.axelor.invoice.db.Invoice")
                        .add("form", "invoice-form")     
                        .param("forceEdit", Boolean.TRUE.toString())
                        .context("_showRecord", String.valueOf(mergedInvoice.getId()))
                        .map()
                );

        } catch (Exception e) {
            if (transaction.isActive()) {
                transaction.rollback(); // Rollback the transaction in case of error
            }
            logger.error("Error merging invoices", e);
            response.setError("An error occurred while merging invoices.");
        }
	}
	
	
	
	@SuppressWarnings("unchecked")
	public void cancelInvoices(ActionRequest request, ActionResponse response) {
		EntityManager em = Beans.get(EntityManager.class); // Get the EntityManager
		EntityTransaction transaction = em.getTransaction(); // Start a transaction
		
		try {
			transaction.begin(); // Begin the transaction

			List<Long> invoiceIds = (List<Long>) request.getContext().get("_ids");

			// Log the selected invoice IDs
			logger.info("Selected Invoice IDs: {}", invoiceIds);

			if (invoiceIds == null || invoiceIds.isEmpty()) {
				response.setError("No invoices selected.");
				return;
			}

			// Fetch all selected invoices
			List<Invoice> invoices = Beans.get(InvoiceRepository.class).all().filter("self.id IN (?1)", invoiceIds)
					.fetch();

			for (Invoice invoice : invoices) {

				if (invoice.getStatusSelect() == InvoiceStatusSelect.VENTILATED) {
					response.setError("Invoice " + invoice.getInvoiceSeq()
							+ " cannot be canceled because it is already ventilated.");
					continue; // Skip this invoice and move to the next one
				}
				invoice.setStatusSelect(InvoiceStatusSelect.CANCELLED); // Use the enum value
				Beans.get(InvoiceRepository.class).save(invoice); // Save the invoice
			}

			transaction.commit(); // Commit the transaction
			response.setAlert("Selected invoices have been cancelled.");
			response.setReload(true);

		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback(); // Rollback the transaction in case of error
			}
			logger.error("Error canceling invoices", e);
			response.setError("An error occurred while canceling invoices.");
		}
	}
	
}
