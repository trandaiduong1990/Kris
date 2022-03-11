package com.cdg.krispay.service;

import com.cdg.krispay.domain.KrisPayMessageType;
import com.cdg.krispay.domain.KrisPayProcessorTransactionLog;
import com.cdg.krispay.domain.KrisPayTransaction;
import com.cdg.krispay.dto.CreateOrder;
import com.cdg.krispay.dto.CreateOrderResponse;
import com.cdg.krispay.exception.NonRetriableException;
import com.cdg.krispay.repo.ProcessorTxnLogRepo;
import com.cdg.krispay.exception.RetriableException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.Before;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
public class CreateOrderTest {

    private static MockWebServer mockBackEnd;

    KrisPayService krisPayService;

    @Mock
    ProcessorTxnLogRepo txnLogRepository;

    @Captor
    ArgumentCaptor<KrisPayProcessorTransactionLog> krisPayProcessorTransactionArgumentCaptor;

    DateTimeFormatter dtFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM-dd'T'HH:mm:ss")
            .withZone(ZoneId.systemDefault());

    @Before
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @BeforeAll
    static void setUp() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @BeforeEach
    void initialize() {
        String baseUrl = String.format("http://localhost:%s", mockBackEnd.getPort());
        krisPayService = new KrisPayService(baseUrl, 5);
        krisPayService.setProcessorTxnLogRepo(txnLogRepository);
    }


    @Test
    public void testSuccessCreateOrder() throws JsonProcessingException, NoSuchAlgorithmException, NonRetriableException, RetriableException {
        ObjectMapper objectMapper = new ObjectMapper();

        KrisPayTransaction krisPayTransaction = new KrisPayTransaction();
        krisPayTransaction.setSessionId(UUID.randomUUID().toString());
        krisPayTransaction.setBookingRef(UUID.randomUUID().toString());
        krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.CREATE_ORDER);
        krisPayTransaction.setAmount(0.10);
        krisPayTransaction.setId(1L);

        CreateOrder createOrderRequest = new CreateOrder(
                krisPayTransaction.getBookingRef(),
                krisPayTransaction.getAmount(),
                krisPayTransaction.getSessionId());

        CreateOrder createOrderMockResponse = new CreateOrder();
        createOrderMockResponse.setCreatedAt( dtFormatter.format(Instant.now() ) );
        createOrderMockResponse.setOrderExpiry( dtFormatter.format(Instant.now() ) );
        createOrderMockResponse.setPaymentStatus("UNPAID");
        createOrderMockResponse.setPartnerOrderId(createOrderRequest.getPartnerOrderId());

        mockBackEnd.enqueue(new MockResponse()
                .setBody(objectMapper.writeValueAsString(createOrderMockResponse))
                .addHeader("Content-Type", "application/json"));

        CreateOrderResponse createOrderResponse = krisPayService.createOrder(krisPayTransaction);

        // Verify the return object status is correct
        Assertions.assertNotNull(createOrderResponse);
        Assertions.assertEquals(createOrderResponse.getData().getPartnerOrderId(), krisPayTransaction.getBookingRef());
        Assertions.assertEquals("UNPAID", createOrderResponse.getStatus());

        // Verify the txnlog is corrected saved
        Mockito.verify(txnLogRepository).save(krisPayProcessorTransactionArgumentCaptor.capture());
        Assertions.assertEquals(krisPayTransaction.getKrisPayMessageType(), krisPayProcessorTransactionArgumentCaptor.getValue().getMsgType());
        Assertions.assertEquals(krisPayTransaction.getBookingRef(), krisPayProcessorTransactionArgumentCaptor.getValue().getPartnerOrderId() );
        Assertions.assertEquals(krisPayTransaction.getSessionId(), krisPayProcessorTransactionArgumentCaptor.getValue().getSessionId() );
        Assertions.assertEquals(createOrderMockResponse.getCreatedAt(), krisPayProcessorTransactionArgumentCaptor.getValue().getCreatedAt());
        Assertions.assertEquals(createOrderMockResponse.getOrderExpiry(), krisPayProcessorTransactionArgumentCaptor.getValue().getOrderExpiry() );
        Assertions.assertEquals(krisPayTransaction.getId(),krisPayProcessorTransactionArgumentCaptor.getValue().getTxnId());
        Assertions.assertNotNull(krisPayProcessorTransactionArgumentCaptor.getValue().getRequest());
        Assertions.assertNotNull(krisPayProcessorTransactionArgumentCaptor.getValue().getMessage());

    }

    @Test
    public void testFailedWrongMessageType()  {
        KrisPayTransaction krisPayTransaction = new KrisPayTransaction();
        krisPayTransaction.setSessionId(UUID.randomUUID().toString());
        krisPayTransaction.setBookingRef(UUID.randomUUID().toString());
        krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.ORDER_STATUS);
        krisPayTransaction.setAmount(0.10);
        krisPayTransaction.setId(1L);

        CreateOrder createOrderRequest = new CreateOrder(
                krisPayTransaction.getBookingRef(),
                krisPayTransaction.getAmount(),
                krisPayTransaction.getSessionId());

        CreateOrder createOrderMockResponse = new CreateOrder();
        createOrderMockResponse.setCreatedAt( dtFormatter.format(Instant.now() ) );
        createOrderMockResponse.setOrderExpiry( dtFormatter.format(Instant.now() ) );
        createOrderMockResponse.setPaymentStatus("UNPAID");
        createOrderMockResponse.setPartnerOrderId(createOrderRequest.getPartnerOrderId());

        Assertions.assertThrows(NonRetriableException.class,
                ()->{ krisPayService.createOrder(krisPayTransaction); } );
    }

    @Test
    public void testNon200HttpStatusCreateOrder() throws JsonProcessingException, NoSuchAlgorithmException, NonRetriableException, RetriableException {

        KrisPayTransaction krisPayTransaction = new KrisPayTransaction();
        krisPayTransaction.setSessionId(UUID.randomUUID().toString());
        krisPayTransaction.setBookingRef(UUID.randomUUID().toString());
        krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.CREATE_ORDER);
        krisPayTransaction.setAmount(0.10);
        krisPayTransaction.setId(1L);

        mockBackEnd.enqueue(new MockResponse()
                .setStatus("HTTP/1.1 500 Internal server error")
                .addHeader("Content-Type", "application/json"));

        Assertions.assertThrows(RetriableException.class,
                ()->{ krisPayService.createOrder(krisPayTransaction); } );

        // Verify the txnlog is correctly saved
        Mockito.verify(txnLogRepository).save(krisPayProcessorTransactionArgumentCaptor.capture());
        Assertions.assertEquals(krisPayTransaction.getKrisPayMessageType(), krisPayProcessorTransactionArgumentCaptor.getValue().getMsgType());
        Assertions.assertEquals(krisPayTransaction.getBookingRef(), krisPayProcessorTransactionArgumentCaptor.getValue().getPartnerOrderId() );
        Assertions.assertEquals(krisPayTransaction.getSessionId(), krisPayProcessorTransactionArgumentCaptor.getValue().getSessionId() );
        Assertions.assertNull(krisPayProcessorTransactionArgumentCaptor.getValue().getCreatedAt());
        Assertions.assertNull(krisPayProcessorTransactionArgumentCaptor.getValue().getOrderExpiry() );
        Assertions.assertEquals(krisPayTransaction.getId(),krisPayProcessorTransactionArgumentCaptor.getValue().getTxnId());
        Assertions.assertNotNull(krisPayProcessorTransactionArgumentCaptor.getValue().getRequest());
        Assertions.assertNull(krisPayProcessorTransactionArgumentCaptor.getValue().getMessage());

    }

    @Test
    public void testTimeoutCreateOrder() throws JsonProcessingException, NoSuchAlgorithmException, NonRetriableException, RetriableException {

        KrisPayTransaction krisPayTransaction = new KrisPayTransaction();
        krisPayTransaction.setSessionId(UUID.randomUUID().toString());
        krisPayTransaction.setBookingRef(UUID.randomUUID().toString());
        krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.CREATE_ORDER);
        krisPayTransaction.setAmount(0.10);
        krisPayTransaction.setId(1L);

        mockBackEnd.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.NO_RESPONSE)
                .addHeader("Content-Type", "application/json"));

        Assertions.assertThrows(RetriableException.class,
                ()->{ krisPayService.createOrder(krisPayTransaction); } );

        // Verify the txnlog is correctly saved
        Mockito.verify(txnLogRepository).save(krisPayProcessorTransactionArgumentCaptor.capture());
        Assertions.assertEquals(krisPayTransaction.getKrisPayMessageType(), krisPayProcessorTransactionArgumentCaptor.getValue().getMsgType());
        Assertions.assertEquals(krisPayTransaction.getBookingRef(), krisPayProcessorTransactionArgumentCaptor.getValue().getPartnerOrderId() );
        Assertions.assertEquals(krisPayTransaction.getSessionId(), krisPayProcessorTransactionArgumentCaptor.getValue().getSessionId() );
        Assertions.assertNull(krisPayProcessorTransactionArgumentCaptor.getValue().getCreatedAt());
        Assertions.assertNull(krisPayProcessorTransactionArgumentCaptor.getValue().getOrderExpiry() );
        Assertions.assertEquals(krisPayTransaction.getId(),krisPayProcessorTransactionArgumentCaptor.getValue().getTxnId());
        Assertions.assertNotNull(krisPayProcessorTransactionArgumentCaptor.getValue().getRequest());
        Assertions.assertNull(krisPayProcessorTransactionArgumentCaptor.getValue().getMessage());

    }

    @Test
    public void testNoConnectionCreateOrder() {

        KrisPayTransaction krisPayTransaction = new KrisPayTransaction();
        krisPayTransaction.setSessionId(UUID.randomUUID().toString());
        krisPayTransaction.setBookingRef(UUID.randomUUID().toString());
        krisPayTransaction.setKrisPayMessageType(KrisPayMessageType.CREATE_ORDER);
        krisPayTransaction.setAmount(0.10);
        krisPayTransaction.setId(1L);

        mockBackEnd.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START)
                .addHeader("Content-Type", "application/json"));

        Assertions.assertThrows(RetriableException.class,
                ()->{ krisPayService.createOrder(krisPayTransaction); } );

        // Verify the txnlog is correctly saved
        Mockito.verify(txnLogRepository).save(krisPayProcessorTransactionArgumentCaptor.capture());
        Assertions.assertEquals(krisPayTransaction.getKrisPayMessageType(), krisPayProcessorTransactionArgumentCaptor.getValue().getMsgType());
        Assertions.assertEquals(krisPayTransaction.getBookingRef(), krisPayProcessorTransactionArgumentCaptor.getValue().getPartnerOrderId() );
        Assertions.assertEquals(krisPayTransaction.getSessionId(), krisPayProcessorTransactionArgumentCaptor.getValue().getSessionId() );
        Assertions.assertNull(krisPayProcessorTransactionArgumentCaptor.getValue().getCreatedAt());
        Assertions.assertNull(krisPayProcessorTransactionArgumentCaptor.getValue().getOrderExpiry() );
        Assertions.assertEquals(krisPayTransaction.getId(),krisPayProcessorTransactionArgumentCaptor.getValue().getTxnId());
        Assertions.assertNotNull(krisPayProcessorTransactionArgumentCaptor.getValue().getRequest());
        Assertions.assertNull(krisPayProcessorTransactionArgumentCaptor.getValue().getMessage());

    }

}
