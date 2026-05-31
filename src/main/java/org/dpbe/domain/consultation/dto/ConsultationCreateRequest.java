package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;

public record ConsultationCreateRequest(
        String type,
        String location,
        String contact,
        String content,
        LocalDateTime scheduledAt
) {}