package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.consultation.entity.ConsultationRequest;

public record ConsultationResponse(
        String consultNo,
        String type,
        String location,
        String contact,
        String content,
        String status,
        LocalDateTime scheduledAt,
        LocalDateTime receivedAt,
        LocalDateTime acceptedAt
) {
    public static ConsultationResponse from(ConsultationRequest r) {
        return new ConsultationResponse(
                r.getConsultNo(), r.getType(), r.getLocation(),
                r.getContact(), r.getContent(), r.getStatus(),
                r.getScheduledAt(), r.getReceivedAt(), r.getAcceptedAt());
    }
}