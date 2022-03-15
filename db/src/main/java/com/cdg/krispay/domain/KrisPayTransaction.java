package com.cdg.krispay.domain;

import lombok.Data;

import javax.persistence.*;
import org.joda.time.DateTime;  
import org.joda.time.DateTimeZone;  
@Data
@Entity
@Table(name = "TMTB_KRIS_TXN")
public class KrisPayTransaction {

    @javax.persistence.Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "SEQGEN_ADMIN_USER")
    @SequenceGenerator(name = "SEQGEN_ADMIN_USER", sequenceName = "TMSQ_ADMIN_USER")
    @Column(name = "id")
    Long Id;

    KrisPayMessageType krisPayMessageType;

    String idempotencyKey;

    String bookingRef;
    String requestId;
    String sessionId;
    Double amount;
    String platform;
    String status;
    DateTime orderExpiry;
    DateTime createdAt;
    String cancelReason;
    Double jobNumber;
    String entryMode;
    String vehicleId;
    String entity;
    Double adminAmount;
    Double gstAmount;
    Double fareAmount;
}
