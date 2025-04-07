package com.axelor.invoice.web;

import com.axelor.rpc.ActionRequest;
import com.axelor.rpc.ActionResponse;
import com.axelor.sales.db.SaleOrder;
import com.axelor.inject.Beans;
import com.axelor.invoice.db.Invoice;
import com.axelor.invoice.db.InvoiceLine;
import com.axelor.invoice.db.InvoiceStatusSelect;
import com.axelor.invoice.db.repo.InvoiceRepository;
import com.axelor.meta.schema.actions.ActionView;
import com.axelor.account.db.Move;
import com.axelor.account.db.MoveLine;
import com.axelor.account.db.repo.MoveRepository;
import com.axelor.i18n.I18n;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountController{

	private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

	public void generateMoveFromInvoice(ActionRequest request, ActionResponse response) {
		EntityManager em = Beans.get(EntityManager.class);
		EntityTransaction transaction = em.getTransaction();

		try {
			transaction.begin();

			Invoice invoice = request.getContext().asType(Invoice.class);

			if (invoice == null) {
				response.setError("Invoice not found.");
				return;
			}


			// Create Move
			Move move = new Move();
			move.setInvoice(invoice);
			move.setOperationDate(LocalDate.now());

			List<MoveLine> moveLines = new ArrayList<>();

			// Create credit move lines for each invoice line
			for (InvoiceLine line : invoice.getInvoiceLineList()) {
				if (line.getProduct() == null || line.getProduct().getAccount() == null) {
					response.setError("Missing account for product: " + line.getProduct());
					return;
				}

				MoveLine creditLine = new MoveLine();
				creditLine.setMove(move);
				creditLine.setAccount(line.getProduct().getAccount());
				creditLine.setCredit(line.getInTaxTotal());
				creditLine.setDebit(BigDecimal.ZERO);
				moveLines.add(creditLine);
			}

			// Create debit move line for customer
			if (invoice.getCustomer() == null || invoice.getCustomer().getAccount() == null) {
				response.setError("Missing account for customer.");
				return;
			}

			MoveLine debitLine = new MoveLine();
			debitLine.setMove(move);
			debitLine.setAccount(invoice.getCustomer().getAccount());
			debitLine.setDebit(invoice.getInTaxTotal());
			debitLine.setCredit(BigDecimal.ZERO);
			moveLines.add(debitLine);

			// Set move lines and save
			move.setMoveLineList(moveLines);
			Beans.get(MoveRepository.class).save(move);

			// Update invoice with move reference and mark as ventilated

			transaction.commit();

			
			response.setReload(true);
			
			 response.setView(
	                    ActionView.define(I18n.get("Move"))

	                        .model("com.axelor.account.db.Move")
	                        .add("form", "move-form")     
	                        .param("forceEdit", Boolean.TRUE.toString())
	                        .context("_showRecord", String.valueOf(move.getId()))
	                        .map()
	                );

		} catch (Exception e) {
			if (transaction.isActive()) {
				transaction.rollback();
			}
			logger.error("Error generating move from invoice", e);
			response.setError("Failed to generate move: " + e.getMessage());
		}
	}
}