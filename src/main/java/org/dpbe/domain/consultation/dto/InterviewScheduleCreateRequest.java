package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;

public record InterviewScheduleCreateRequest(
        String customerName,
        String designerName,
        String interviewType,
        LocalDateTime scheduledAt,
        String location,
        String preparation
) {}