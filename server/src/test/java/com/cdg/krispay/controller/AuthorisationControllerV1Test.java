package com.cdg.krispay.controller;


import com.cdg.krispay.domain.Authorisation;
import com.cdg.krispay.domain.KrisPayMessageType;
import com.cdg.krispay.domain.KrisPayTransaction;
import com.cdg.krispay.domain.ResponseStatus;
import com.cdg.krispay.dto.CreateOrder;
import com.cdg.krispay.exception.NonRetriableException;
import com.cdg.krispay.exception.RetriableException;
import com.cdg.krispay.repo.TxnLogRepo;
import com.cdg.krispay.service.KrisPayService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthorisationControllerV1Test {

    @MockBean
    TxnLogRepo txnLogRepo;

    @MockBean
    KrisPayService krisPayService;

    @Autowired
    private MockMvc mvc;

    @Captor
    ArgumentCaptor<KrisPayTransaction> krisPayTransactionArgumentCaptor;

    @Test
    public void testAuthorisationNoIdempotencyKey() throws Exception {
        mvc.perform( MockMvcRequestBuilders
                .post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON) )
                .andExpect( status().isBadRequest() );
    }

    @Test
    public void testAuthorisationFailValidation() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String idempotencyKey = UUID.randomUUID().toString();

        Authorisation authorisation = new Authorisation();
        authorisation.setBookingRef(UUID.randomUUID().toString());

        Mockito.when( txnLogRepo.findByIdempotencyKey(eq(idempotencyKey)) )
                .thenReturn( null );

        mvc.perform( MockMvcRequestBuilders
                .post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(authorisation)))
                .andExpect( status().isBadRequest() );
    }

    @Test
    public void testFailedAuthoriseDueToServiceExceptionNoRepeat() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String idempotencyKey = UUID.randomUUID().toString();

        Authorisation authorisation = new Authorisation();
        authorisation.setBookingRef(UUID.randomUUID().toString());
        authorisation.setAmount(10.0);
        authorisation.setSessionId(UUID.randomUUID().toString());
        authorisation.setPlatform("iOS");
        authorisation.setRequestId(UUID.randomUUID().toString());

        Mockito.when( txnLogRepo.findByIdempotencyKey(eq(idempotencyKey)) )
                .thenReturn( null );
        Mockito.when( krisPayService.createOrder(any()) )
                .thenThrow(new NonRetriableException("exception"));

        mvc.perform( MockMvcRequestBuilders
                .post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(authorisation)))
                .andExpect( status().isOk() )
                .andExpect( jsonPath("$.requestId").value(authorisation.getRequestId()));

        // Verify the txnlog is correctly saved
        Mockito.verify(txnLogRepo, times(2)).save(krisPayTransactionArgumentCaptor.capture());
        Assertions.assertEquals(KrisPayMessageType.AUTHORISE, krisPayTransactionArgumentCaptor.getValue().getKrisPayMessageType());
        Assertions.assertEquals(authorisation.getAmount(), krisPayTransactionArgumentCaptor.getValue().getAmount());
        Assertions.assertEquals(authorisation.getSessionId(), krisPayTransactionArgumentCaptor.getValue().getSessionId());
        Assertions.assertEquals(authorisation.getRequestId(), krisPayTransactionArgumentCaptor.getValue().getRequestId());
        Assertions.assertEquals(authorisation.getBookingRef(), krisPayTransactionArgumentCaptor.getValue().getBookingRef());

        Assertions.assertEquals(ResponseStatus.FAILED.name(), krisPayTransactionArgumentCaptor.getValue().getStatus());

    }

    @Test
    public void testSuccessAuthoriseNoRepeat() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String idempotencyKey = UUID.randomUUID().toString();

        Authorisation authorisation = new Authorisation();
        authorisation.setBookingRef(UUID.randomUUID().toString());
        authorisation.setAmount(10.0);
        authorisation.setSessionId(UUID.randomUUID().toString());
        authorisation.setPlatform("iOS");
        authorisation.setRequestId(UUID.randomUUID().toString());

        Mockito.when( txnLogRepo.findByIdempotencyKey(eq(idempotencyKey)) )
                .thenReturn( null );

        CreateOrder createOrder = new CreateOrder();
        createOrder.setPaymentStatus("UNPAID");

        Mockito.when( krisPayService.createOrder(any()) )
                .thenReturn( createOrder  );

        mvc.perform( MockMvcRequestBuilders
                .post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(authorisation)))
                .andExpect( status().isOk() )
                .andExpect( jsonPath("$.requestId").value(authorisation.getRequestId()))
                .andExpect( jsonPath("$.bookingRef").value(authorisation.getBookingRef()))
                .andExpect( jsonPath("$.status").value(ResponseStatus.SUCCESS.name()));;

        // Verify the txnlog is correctly saved
        Mockito.verify(txnLogRepo, times(2)).save(krisPayTransactionArgumentCaptor.capture());
        Assertions.assertEquals(KrisPayMessageType.AUTHORISE, krisPayTransactionArgumentCaptor.getValue().getKrisPayMessageType());
        Assertions.assertEquals(authorisation.getAmount(), krisPayTransactionArgumentCaptor.getValue().getAmount());
        Assertions.assertEquals(authorisation.getSessionId(), krisPayTransactionArgumentCaptor.getValue().getSessionId());
        Assertions.assertEquals(authorisation.getRequestId(), krisPayTransactionArgumentCaptor.getValue().getRequestId());
        Assertions.assertEquals(authorisation.getBookingRef(), krisPayTransactionArgumentCaptor.getValue().getBookingRef());

        Assertions.assertEquals(ResponseStatus.SUCCESS.name(), krisPayTransactionArgumentCaptor.getValue().getStatus());

    }

    @Test
    public void testFailedAuthoriseNoRepeat() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String idempotencyKey = UUID.randomUUID().toString();

        Authorisation authorisation = new Authorisation();
        authorisation.setBookingRef(UUID.randomUUID().toString());
        authorisation.setAmount(10.0);
        authorisation.setSessionId(UUID.randomUUID().toString());
        authorisation.setPlatform("iOS");
        authorisation.setRequestId(UUID.randomUUID().toString());

        Mockito.when( txnLogRepo.findByIdempotencyKey(eq(idempotencyKey)) )
                .thenReturn( null );

        CreateOrder createOrder = new CreateOrder();
        createOrder.setPaymentStatus("ERROR");

        Mockito.when( krisPayService.createOrder(any()) )
                .thenReturn( createOrder  );

        mvc.perform( MockMvcRequestBuilders
                .post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(authorisation)))
                .andExpect( status().isOk() )
                .andExpect( jsonPath("$.requestId").value(authorisation.getRequestId()))
                .andExpect( jsonPath("$.bookingRef").value(authorisation.getBookingRef()))
                .andExpect( jsonPath("$.status").value(ResponseStatus.DECLINED.name()));;

        // Verify the txnlog is correctly saved
        Mockito.verify(txnLogRepo, times(2)).save(krisPayTransactionArgumentCaptor.capture());
        Assertions.assertEquals(KrisPayMessageType.AUTHORISE, krisPayTransactionArgumentCaptor.getValue().getKrisPayMessageType());
        Assertions.assertEquals(authorisation.getAmount(), krisPayTransactionArgumentCaptor.getValue().getAmount());
        Assertions.assertEquals(authorisation.getSessionId(), krisPayTransactionArgumentCaptor.getValue().getSessionId());
        Assertions.assertEquals(authorisation.getRequestId(), krisPayTransactionArgumentCaptor.getValue().getRequestId());
        Assertions.assertEquals(authorisation.getBookingRef(), krisPayTransactionArgumentCaptor.getValue().getBookingRef());

        Assertions.assertEquals(ResponseStatus.DECLINED.name(), krisPayTransactionArgumentCaptor.getValue().getStatus());

    }

    @Test
    public void testSuccessAuthoriseRepeat() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String idempotencyKey = UUID.randomUUID().toString();

        Authorisation authorisation = new Authorisation();
        authorisation.setBookingRef(UUID.randomUUID().toString());
        authorisation.setAmount(10.0);
        authorisation.setSessionId(UUID.randomUUID().toString());
        authorisation.setPlatform("iOS");
        authorisation.setRequestId(UUID.randomUUID().toString());

        KrisPayTransaction krisPayTransaction = new KrisPayTransaction();
        krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.AUTHORISE);
        krisPayTransaction.setStatus(ResponseStatus.SUCCESS.name());
        krisPayTransaction.setSessionId(authorisation.getSessionId());
        krisPayTransaction.setBookingRef(authorisation.getBookingRef());

        Mockito.when( txnLogRepo.findByIdempotencyKey(eq(idempotencyKey)) )
                .thenReturn( krisPayTransaction );

        mvc.perform( MockMvcRequestBuilders
                .post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(authorisation)))
                .andExpect( status().isOk() )
                .andExpect( jsonPath("$.requestId").value(krisPayTransaction.getRequestId()))
                .andExpect( jsonPath("$.bookingRef").value(krisPayTransaction.getBookingRef()))
                .andExpect( jsonPath("$.status").value(krisPayTransaction.getStatus()));

        Mockito.verify(krisPayService, times(0)).createOrder(any());
        Mockito.verify(txnLogRepo, times(0) ).save(any());

    }

    @Test
    public void testDeclineAuthoriseRepeat() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String idempotencyKey = UUID.randomUUID().toString();

        Authorisation authorisation = new Authorisation();
        authorisation.setBookingRef(UUID.randomUUID().toString());
        authorisation.setAmount(10.0);
        authorisation.setSessionId(UUID.randomUUID().toString());
        authorisation.setPlatform("iOS");
        authorisation.setRequestId(UUID.randomUUID().toString());

        KrisPayTransaction krisPayTransaction = new KrisPayTransaction();
        krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.AUTHORISE);
        krisPayTransaction.setStatus(ResponseStatus.DECLINED.name());
        krisPayTransaction.setSessionId(authorisation.getSessionId());
        krisPayTransaction.setBookingRef(authorisation.getBookingRef());

        Mockito.when( txnLogRepo.findByIdempotencyKey(eq(idempotencyKey)) )
                .thenReturn( krisPayTransaction );

        mvc.perform( MockMvcRequestBuilders
                .post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(authorisation)))
                .andExpect( status().isOk() )
                .andExpect( jsonPath("$.requestId").value(krisPayTransaction.getRequestId()))
                .andExpect( jsonPath("$.bookingRef").value(krisPayTransaction.getBookingRef()))
                .andExpect( jsonPath("$.status").value(krisPayTransaction.getStatus()));

        Mockito.verify(krisPayService, times(0)).createOrder(any());
        Mockito.verify(txnLogRepo, times(0) ).save(any());

    }


    @Test
    public void testMismatchIdempotencyAuthoriseRepeat() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();

        String idempotencyKey = UUID.randomUUID().toString();

        Authorisation authorisation = new Authorisation();
        authorisation.setBookingRef(UUID.randomUUID().toString());
        authorisation.setAmount(10.0);
        authorisation.setSessionId(UUID.randomUUID().toString());
        authorisation.setPlatform("iOS");
        authorisation.setRequestId(UUID.randomUUID().toString());

        KrisPayTransaction krisPayTransaction = new KrisPayTransaction();
        krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.CAPTURE);
        krisPayTransaction.setStatus(ResponseStatus.SUCCESS.name());
        krisPayTransaction.setSessionId(authorisation.getSessionId());
        krisPayTransaction.setBookingRef(authorisation.getBookingRef());

        Mockito.when( txnLogRepo.findByIdempotencyKey(eq(idempotencyKey)) )
                .thenReturn( krisPayTransaction );

        mvc.perform( MockMvcRequestBuilders
                .post("/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", idempotencyKey)
                .content(objectMapper.writeValueAsString(authorisation)))
                .andExpect( status().isBadRequest() );
    }

}
