package com.cdg.krispay.controller;

import com.cdg.krispay.domain.*;
import com.cdg.krispay.dto.CreateOrder;
import com.cdg.krispay.dto.CreateOrderResponse;
import com.cdg.krispay.exception.IdempotentKeyMessageTypeMismatchException;
import com.cdg.krispay.exception.MissingIdempotencyKeyException;
import com.cdg.krispay.exception.NonRetriableException;
import com.cdg.krispay.exception.RetriableException;
import com.cdg.krispay.repo.TxnLogRepo;
import com.cdg.krispay.service.KrisPayService;
import lombok.extern.slf4j.Slf4j;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@Slf4j
@RestController
public class AuthorisationControllerV1 extends BaseController {

	@Autowired
	TxnLogRepo txnLogRepo;

	@Autowired
	KrisPayService krisPayService;
	
	@GetMapping("/auth/:bookingRef")
	public AuthorisationStatus preAuthStatus(@PathVariable(value = "bookingRef") String bookingRef) {
		AuthorisationStatus authorisationStatus = new AuthorisationStatus();
		
		try {
			KrisPayTransaction krisPayTransaction = txnLogRepo.findByRequestId(bookingRef);
			if (krisPayTransaction != null) {
				CreateOrderResponse orderResponse = krisPayService.getOrderStatus(krisPayTransaction);
			}
		} catch (Exception e) {
			// TODO: handle exception
			authorisationStatus.setStatus(ResponseStatus.ERROR.name());
		}
		return authorisationStatus;
	}

	@PostMapping("/v1/orders")
	public AuthorisationStatus authorise(@RequestHeader("Idempotency-Key") String idempotencyKey,
			@Valid @RequestBody Authorisation authorisation) {

		if (idempotencyKey == null || idempotencyKey.isEmpty()) {
			throw new MissingIdempotencyKeyException("authorisation request");
		}

		KrisPayTransaction krisPayTransaction = txnLogRepo.findByIdempotencyKey(idempotencyKey);

		// Order info request
		if (krisPayTransaction != null) {

			if (krisPayTransaction.getKrisPayMessageType() != KrisPayMessageType.AUTHORISE) {
				throw new IdempotentKeyMessageTypeMismatchException(krisPayTransaction.getKrisPayMessageType().name());
			}

			AuthorisationStatus authorisationStatus = new AuthorisationStatus();
			authorisationStatus.setBookingRef(krisPayTransaction.getBookingRef());
			authorisationStatus.setRequestId(krisPayTransaction.getRequestId());
			authorisationStatus.setStatus(krisPayTransaction.getStatus());

			return authorisationStatus;
		}

		// Not a repeat request
		krisPayTransaction.setBookingRef(authorisation.getBookingRef());
		krisPayTransaction.setIdempotencyKey(idempotencyKey);
		krisPayTransaction.setAmount(authorisation.getAmount());
		krisPayTransaction.setSessionId(authorisation.getSessionId());
		krisPayTransaction.setPlatform(authorisation.getPlatform());
		krisPayTransaction.setRequestId(authorisation.getRequestId());
		krisPayTransaction.setCreatedAt(new DateTime().toDateTime(DateTimeZone.UTC));
		krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.CREATE_ORDER);
		AuthorisationStatus authorisationStatus = new AuthorisationStatus();
		authorisationStatus.setBookingRef(krisPayTransaction.getBookingRef());
		authorisationStatus.setRequestId(krisPayTransaction.getRequestId());

		txnLogRepo.save(krisPayTransaction);

		CreateOrderResponse orderResponse;
		try {
			orderResponse = krisPayService.createOrder(krisPayTransaction);

			authorisationStatus.setStatus(orderResponse.getStatus());
			krisPayTransaction.setStatus(orderResponse.getStatus());
		} catch (NonRetriableException e) {

			krisPayTransaction.setStatus(ResponseStatus.ERROR.name());
			authorisationStatus.setStatus(krisPayTransaction.getStatus());

		} catch (RetriableException e) {

			krisPayTransaction.setStatus(ResponseStatus.ERROR.name());
			authorisationStatus.setStatus(krisPayTransaction.getStatus());

		}

		txnLogRepo.save(krisPayTransaction);
		return authorisationStatus;

	}

}
