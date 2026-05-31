package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;

public record InterviewScheduleUpdateRequest(
        String interviewType,
        LocalDateTime scheduledAt,
        String location,
        String preparation
) {}