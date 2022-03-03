package com.cdg.krispay;

import com.cdg.krispay.domain.KrisPayMessageType;
import com.cdg.krispay.domain.KrisPayProcessorTransactionLog;
import com.cdg.krispay.dto.CreateOrder;
import com.cdg.krispay.service.KrisPayService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class KrispayApplication implements ApplicationRunner  {

	@Autowired
	KrisPayService krisPayService;

	public static void main(String[] args) {
		SpringApplication.run(KrispayApplication.class, args);
	}

	@Override
	public void run(ApplicationArguments applicationArguments) throws Exception {
		try {
			KrisPayProcessorTransactionLog krisPayTransaction = new KrisPayProcessorTransactionLog(KrisPayMessageType.CREATE_ORDER);
			//krisPayTransaction.setAmount(1.99);
			krisPayTransaction.setPartnerOrderId("partnerId111");
			krisPayTransaction.setSessionId("sessionId");
			CreateOrder createOrderRequest = new CreateOrder("a", 1., "2");
			//krisPayService.createOrder(createOrderRequest);
		}
		catch(Exception e) {}
		//krisPayService.getPaymentStatus("ABCD-Mickey");
		//krisPayService.cancelOrder("cancelorder-123", "cancel reason");
		//krisPayService.capture("capture-123");
	}

}
