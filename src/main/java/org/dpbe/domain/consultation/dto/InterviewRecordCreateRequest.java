package org.dpbe.domain.consultation.dto;

import java.time.LocalDateTime;

public record InterviewRecordCreateRequest(
        String customerName,
        LocalDateTime interviewedAt,
        String content,
        String customerReaction,
        String followUpAction
) {}