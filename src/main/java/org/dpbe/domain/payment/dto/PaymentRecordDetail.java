package org.dpbe.domain.payment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentRecordDetail(
        String recordNo,
        String contractNo,
        String customerName,
        long amount,
        String method,
        LocalDate paymentDate,
        String status,
        LocalDateTime confirmedAt,
        LocalDateTime rejectedAt,
        String rejectCategory,
        String rejectReason) {}