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
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
	public KRHostResponse preAuthStatus(@PathVariable(value = "bookingRef") String requestId) {
		KRHostResponse kRHostResponse = new KRHostResponse();
		
		try {
			KrisPayTransaction krisPayTransaction = txnLogRepo.findByRequestId(requestId);
			if (krisPayTransaction != null) {
				CreateOrderResponse orderResponse = krisPayService.getOrderStatus(krisPayTransaction);
				kRHostResponse.setStatus(orderResponse.getStatus());
				kRHostResponse.setRequestId(requestId);
				kRHostResponse.setBookingRef(krisPayTransaction.getBookingRef());
			}
		} catch (RetriableException e) {
			// TODO: handle exception
			kRHostResponse.setStatus(ResponseStatus.ERROR.name());
		} catch (Exception e) {
			kRHostResponse.setStatus(ResponseStatus.ERROR.name());
		}
		return kRHostResponse;
	}
	
	@PutMapping("/auth/:bookingRef/cancel")
	public KRHostResponse cancelPreAuth(@PathVariable(value = "bookingRef") String bookingRef, @Valid @RequestBody CancelPreAuth cancelPreAuth) {
		KRHostResponse kRHostResponse = new KRHostResponse();
		
		try {
			KrisPayTransaction krisPayTransaction = txnLogRepo.findByBookingRef(bookingRef);
			if (krisPayTransaction != null) {
				krisPayTransaction.setCancelReason(cancelPreAuth.getReason());
				CreateOrderResponse orderResponse = krisPayService.cancelPreAuth(krisPayTransaction);
				kRHostResponse.setStatus(orderResponse.getStatus());
				kRHostResponse.setRequestId(krisPayTransaction.getRequestId());
				kRHostResponse.setBookingRef(krisPayTransaction.getBookingRef());
				
				krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.CANCEL_PRE_AUTH);
				krisPayTransaction.setStatus(orderResponse.getStatus());
				txnLogRepo.save(krisPayTransaction);
			}
		} catch (RetriableException e) {
			// TODO: handle exception
			kRHostResponse.setStatus(ResponseStatus.ERROR.name());
		} catch (Exception e) {
			kRHostResponse.setStatus(ResponseStatus.ERROR.name());
		}
		return kRHostResponse;
	}

	@PutMapping("/auth/:bookingRef/capture")
	public KRHostResponse capture(@PathVariable(value = "bookingRef") String bookingRef, @Valid @RequestBody CaptureRequest request) {
		KRHostResponse kRHostResponse = new KRHostResponse();
		
		try {
			KrisPayTransaction transactionDto = new KrisPayTransaction();
			BeanUtils.copyProperties(request, transactionDto);
			BeanUtils.copyProperties(kRHostResponse, transactionDto, null)
			
			KrisPayTransaction krisPayTransaction = txnLogRepo.captureTranx(transactionDto, bookingRef);
			
			
			if (krisPayTransaction != null) {
				krisPayTransaction.setCancelReason(cancelPreAuth.getReason());
				CreateOrderResponse orderResponse = krisPayService.cancelPreAuth(krisPayTransaction);
				kRHostResponse.setStatus(orderResponse.getStatus());
				kRHostResponse.setRequestId(krisPayTransaction.getRequestId());
				kRHostResponse.setBookingRef(krisPayTransaction.getBookingRef());
				
				krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.CANCEL_PRE_AUTH);
				krisPayTransaction.setStatus(orderResponse.getStatus());
				txnLogRepo.save(krisPayTransaction);
			}
		} catch (RetriableException e) {
			// TODO: handle exception
			kRHostResponse.setStatus(ResponseStatus.ERROR.name());
		} catch (Exception e) {
			kRHostResponse.setStatus(ResponseStatus.ERROR.name());
		}
		return kRHostResponse;
	}
	
	@PostMapping("/v1/orders")
	public KRHostResponse authorise(@RequestHeader("Idempotency-Key") String idempotencyKey,
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

			KRHostResponse kRHostResponse = new KRHostResponse();
			kRHostResponse.setBookingRef(krisPayTransaction.getBookingRef());
			kRHostResponse.setRequestId(krisPayTransaction.getRequestId());
			kRHostResponse.setStatus(krisPayTransaction.getStatus());

			return kRHostResponse;
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
		KRHostResponse kRHostResponse = new KRHostResponse();
		kRHostResponse.setBookingRef(krisPayTransaction.getBookingRef());
		kRHostResponse.setRequestId(krisPayTransaction.getRequestId());

		txnLogRepo.save(krisPayTransaction);

		CreateOrderResponse orderResponse;
		try {
			orderResponse = krisPayService.createOrder(krisPayTransaction);

			kRHostResponse.setStatus(orderResponse.getStatus());
			krisPayTransaction.setStatus(orderResponse.getStatus());
		} catch (NonRetriableException e) {

			krisPayTransaction.setStatus(ResponseStatus.ERROR.name());
			kRHostResponse.setStatus(krisPayTransaction.getStatus());

		} catch (RetriableException e) {

			krisPayTransaction.setStatus(ResponseStatus.ERROR.name());
			kRHostResponse.setStatus(krisPayTransaction.getStatus());

		}

		txnLogRepo.save(krisPayTransaction);
		return kRHostResponse;

	}

}
