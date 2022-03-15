package com.cdg.krispay.service;

import com.cdg.krispay.domain.KrisPayMessageType;
import com.cdg.krispay.domain.KrisPayProcessorTransactionLog;
import com.cdg.krispay.domain.KrisPayRequestStatus;
import com.cdg.krispay.domain.KrisPayTransaction;
import com.cdg.krispay.domain.ResponseStatus;
import com.cdg.krispay.dto.*;
import com.cdg.krispay.exception.NonRetriableException;
import com.cdg.krispay.exception.RetriableException;
import com.cdg.krispay.repo.ProcessorTxnLogRepo;
import com.google.common.base.Strings;

import io.netty.handler.logging.LogLevel;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.logging.AdvancedByteBufFormat;

import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@Data
@Component
public class KrisPayService {

	@Autowired
	ProcessorTxnLogRepo processorTxnLogRepo;

	@Value("${krispay.api-key}")
	String apiKey;
	@Value("${krispay.secret}")
	String secret;

	WebClient webClient;

	public KrisPayService(@Value("${krispay.url}") String url, @Value("${krispay.timeout}") int timeout) {

		HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(timeout))
				.wiretap("reactor.netty.http.client.HttpClient", LogLevel.DEBUG, AdvancedByteBufFormat.TEXTUAL);

		this.webClient = WebClient.builder().baseUrl(url)
				.filter(ExchangeFilterFunction.ofRequestProcessor(
						request -> Mono.just(ClientRequest.from(request).header("api-key", apiKey).build())))
				.clientConnector(new ReactorClientHttpConnector(httpClient)).build();
	}

	public CreateOrderResponse createOrder(KrisPayTransaction krisPayTransaction)
			throws NonRetriableException, RetriableException {

		// No message type or wrong message type
		if (krisPayTransaction.getKrisPayMessageType() == null
				|| !krisPayTransaction.getKrisPayMessageType().equals(KrisPayMessageType.CREATE_ORDER)) {
			throw new NonRetriableException(
					"Incorrect or null message type: " + krisPayTransaction.getKrisPayMessageType());
		}

		CreateOrder createOrder = new CreateOrder(krisPayTransaction.getBookingRef(), krisPayTransaction.getAmount(),
				krisPayTransaction.getSessionId());

		if (krisPayTransaction.getOrderExpiry() != null) {
			createOrder.setOrderExpiry(krisPayTransaction.getOrderExpiry().toString());
		}
		KrisPayProcessorTransactionLog krisPayProcessorTransactionlog = new KrisPayProcessorTransactionLog(
				krisPayTransaction.getKrisPayMessageType());
		krisPayProcessorTransactionlog.setPartnerOrderId(krisPayTransaction.getBookingRef());
		krisPayProcessorTransactionlog.setSessionId(krisPayTransaction.getSessionId());
		krisPayProcessorTransactionlog.setTxnId(krisPayTransaction.getId());
		krisPayProcessorTransactionlog.setMsgType(KrisPayMessageType.CREATE_ORDER);
		
		CreateOrderResponse response;
		try {
			response = this.webClient.post().uri("/partner/orders")
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.header("x-signature", getSignature()).header("api-key", apiKey)
					.header("kp-request-id", UUID.randomUUID().toString())
					.body(Mono.just(createOrder), CreateOrder.class).retrieve().bodyToMono(CreateOrderResponse.class)
					.block();
		} catch (Exception e) {
			log.error("Exception sending to krispay server ", e);
			krisPayProcessorTransactionlog.setStatus(KrisPayRequestStatus.ERROR);
			processorTxnLogRepo.save(krisPayProcessorTransactionlog);
			throw new RetriableException(e.getMessage());
		}

		if (response == null) {
			log.error("Connection error with kris pay");
			krisPayProcessorTransactionlog.setStatus(KrisPayRequestStatus.ERROR);
			processorTxnLogRepo.save(krisPayProcessorTransactionlog);
			throw new RetriableException("Connection error with kris pay");
		}
		krisPayProcessorTransactionlog.setStatus(KrisPayRequestStatus.SUCCESS);
		krisPayProcessorTransactionlog.setCreatedAt(response.getData().getCreatedAt());
		krisPayProcessorTransactionlog.setOrderExpiry(response.getData().getOrderExpiry());
		krisPayProcessorTransactionlog.setResponse(response.getStatus());

		processorTxnLogRepo.save(krisPayProcessorTransactionlog);

		return response;
	}

	public void getPaymentStatus(String orderId) throws NoSuchAlgorithmException {
		log.debug("this " + webClient.toString());

		CreateOrder createOrderRequest = new CreateOrder("partnetID", 8.12, "session");

		GetPaymentStatusResponse response = this.webClient.get()
				.uri(uriBuilder -> uriBuilder.path("/partner/orders/{id}").build(orderId))
				.header("x-signature", getSignature()).header("kp-request-id", UUID.randomUUID().toString()).retrieve()
				.bodyToMono(GetPaymentStatusResponse.class).block();

		log.error(response.toString());
	}

	public void cancelOrder(String orderId, String reason) throws NoSuchAlgorithmException {
		log.debug("this " + webClient.toString());

		CancelOrderRequest cancelOrderRequest = new CancelOrderRequest(reason);

		CancelOrderResponse response = this.webClient.put()
				.uri(uriBuilder -> uriBuilder.path("/partner/{orderId}/cancel").build(orderId))
				.header("x-signature", getSignature()).header("kp-request-id", UUID.randomUUID().toString()).retrieve()
				.bodyToMono(CancelOrderResponse.class).block();

		log.error(response.toString());
	}

	public void capture(String orderId) throws NoSuchAlgorithmException {
		log.debug("this " + webClient.toString());

		CaptureRequest captureRequest = new CaptureRequest(orderId, "ada|ada", 1.99);

		CaptureResponse response = this.webClient.put()
				.uri(uriBuilder -> uriBuilder.path("/partner/orders/{orderId}/capture").build(orderId))
				.header("x-signature", getSignature()).header("kp-request-id", UUID.randomUUID().toString()).retrieve()
				.bodyToMono(CaptureResponse.class).block();

		log.error(response.toString());
	}

	private String getSignature() throws NoSuchAlgorithmException {
		Long timestamp = System.currentTimeMillis() / 1000;
		String hashData = apiKey + secret + timestamp;
//        return DigestUtils.sha256(hashData).toString();
		return DigestUtils.sha256Hex(hashData);
	}

	public CreateOrderResponse getOrderStatus(KrisPayTransaction krisPayTransaction) throws RetriableException {
		CreateOrder createOrder = new CreateOrder(krisPayTransaction.getBookingRef(), krisPayTransaction.getAmount(),
				krisPayTransaction.getSessionId());

		if (krisPayTransaction.getOrderExpiry() != null) {
			createOrder.setOrderExpiry(krisPayTransaction.getOrderExpiry().toString());
		}
		KrisPayProcessorTransactionLog krisPayProcessorTransactionlog = new KrisPayProcessorTransactionLog(
				krisPayTransaction.getKrisPayMessageType());
		krisPayProcessorTransactionlog.setPartnerOrderId(krisPayTransaction.getBookingRef());
		krisPayProcessorTransactionlog.setSessionId(krisPayTransaction.getSessionId());
		krisPayProcessorTransactionlog.setTxnId(krisPayTransaction.getId());
		krisPayProcessorTransactionlog.setMsgType(KrisPayMessageType.ORDER_STATUS);

		CreateOrderResponse response = null;
		try {
			response = this.webClient.get().uri("/partner/orders/" + krisPayTransaction.getBookingRef())
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.header("x-signature", getSignature()).header("api-key", apiKey)
					.header("kp-request-id", UUID.randomUUID().toString()).retrieve()
					.bodyToMono(CreateOrderResponse.class).block();
		} catch (Exception e) {
			log.error("Exception sending to krispay server ", e);
			krisPayProcessorTransactionlog.setStatus(KrisPayRequestStatus.ERROR);
			processorTxnLogRepo.save(krisPayProcessorTransactionlog);
			throw new RetriableException(e.getMessage());
		}

		if (response == null) {
			log.error("Connection error with kris pay");
			krisPayProcessorTransactionlog.setStatus(KrisPayRequestStatus.ERROR);
			processorTxnLogRepo.save(krisPayProcessorTransactionlog);
			throw new RetriableException("Connection error with kris pay");
		}

		krisPayProcessorTransactionlog.setStatus(KrisPayRequestStatus.SUCCESS);
		krisPayProcessorTransactionlog.setResponseStatus(response.getStatus());
		
		ResponseStatus responseStatus = ResponseStatus.valueOf(response.getStatus().toUpperCase());
		
		if(responseStatus == ResponseStatus.SUCCESS) {
			krisPayProcessorTransactionlog.setCreatedAt(response.getData().getCreatedAt());
			krisPayProcessorTransactionlog.setOrderExpiry(response.getData().getOrderExpiry());
			krisPayProcessorTransactionlog.setPaymentStatus(response.getData().getPaymentStatus());
		} else {
			krisPayProcessorTransactionlog.setCode(response.getCode());
			krisPayProcessorTransactionlog.setMessage(response.getMessage());
		}
		
		
		processorTxnLogRepo.save(krisPayProcessorTransactionlog);

		return response;
	}

	public CreateOrderResponse cancelPreAuth(KrisPayTransaction krisPayTransaction) throws RetriableException {
		CreateOrder createOrder = new CreateOrder();
		createOrder.setReason(krisPayTransaction.getCancelReason());
		
		KrisPayProcessorTransactionLog krisPayProcessorTransactionlog = new KrisPayProcessorTransactionLog(
				krisPayTransaction.getKrisPayMessageType());
		krisPayProcessorTransactionlog.setPartnerOrderId(krisPayTransaction.getBookingRef());
		krisPayProcessorTransactionlog.setSessionId(krisPayTransaction.getSessionId());
		krisPayProcessorTransactionlog.setTxnId(krisPayTransaction.getId());
		krisPayProcessorTransactionlog.setRequest(createOrder.toString());
		krisPayProcessorTransactionlog.setMsgType(KrisPayMessageType.CANCEL_PRE_AUTH);
		CreateOrderResponse response = null;
		try {
			response = this.webClient.put().uri("/orders/" + krisPayTransaction.getBookingRef() + "/cancel")
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.header("x-signature", getSignature())
					.header("api-key", apiKey)
					.header("kp-request-id", UUID.randomUUID().toString())
					.body(Mono.just(createOrder), CreateOrder.class)
					.retrieve().bodyToMono(CreateOrderResponse.class).block();
		} catch (Exception e) {
			log.error("Exception sending to krispay server ", e);
			krisPayProcessorTransactionlog.setStatus(KrisPayRequestStatus.ERROR);
			processorTxnLogRepo.save(krisPayProcessorTransactionlog);
			throw new RetriableException(e.getMessage());
		}

		if (response == null) {
			log.error("Connection error with kris pay");
			krisPayProcessorTransactionlog.setStatus(KrisPayRequestStatus.ERROR);
			processorTxnLogRepo.save(krisPayProcessorTransactionlog);
			throw new RetriableException("Connection error with kris pay");
		}

		krisPayProcessorTransactionlog.setStatus(KrisPayRequestStatus.SUCCESS);
		krisPayProcessorTransactionlog.setResponseStatus(response.getStatus());
		
		ResponseStatus responseStatus = ResponseStatus.valueOf(response.getStatus().toUpperCase());
		
		if(responseStatus == ResponseStatus.SUCCESS) {
			krisPayProcessorTransactionlog.setPaymentStatus(response.getData().getPaymentStatus());
		} else {
			krisPayProcessorTransactionlog.setCode(response.getCode());
			krisPayProcessorTransactionlog.setMessage(response.getMessage());
		}
		
		
		processorTxnLogRepo.save(krisPayProcessorTransactionlog);

		return response;
	}

}
