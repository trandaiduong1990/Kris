package com.cdg.krispay.domain;

import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.util.Date;

@Data
@Entity
@Table(name = "TMTB_KRIS_PROCESSOR_TXN_LOG")
public class KrisPayProcessorTransactionLog {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQGEN_ADMIN_USER")
    @SequenceGenerator(name = "SEQGEN_ADMIN_USER", sequenceName = "TMSQ_ADMIN_USER")
    @Column(name = "id")
    Long Id;

    @CreationTimestamp
    private Date createDt;

    KrisPayMessageType msgType;
    KrisPayRequestStatus status;

    String partnerOrderId;
    String sessionId;
    String createdAt;
    String orderExpiry;
    String request;
    String response;
    Long txnId;

    public KrisPayProcessorTransactionLog(KrisPayMessageType msgType) {
        this.msgType = msgType;
    }

}


