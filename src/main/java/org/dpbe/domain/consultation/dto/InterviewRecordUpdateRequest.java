package org.dpbe.domain.consultation.dto;

public record InterviewRecordUpdateRequest(
        String content,
        String customerReaction,
        String followUpAction
) {}