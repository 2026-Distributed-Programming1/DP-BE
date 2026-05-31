package org.dpbe.domain.sales.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.dpbe.domain.sales.entity.ChannelScreening;

public record ChannelScreeningResponse(
        Long id,
        String screeningNo,
        String applicantName,
        String channelType,
        LocalDate applicationDate,
        String career,
        List<String> certifications,
        String status,
        String approvalNo,
        LocalDateTime reviewedAt,
        String rejectionReason
) {
    public static ChannelScreeningResponse from(ChannelScreening s) {
        return new ChannelScreeningResponse(
                s.getId(),
                s.getScreeningNo(),
                s.getApplicantName(),
                s.getChannelType() != null ? s.getChannelType().name() : null,
                s.getApplicationDate(),
                s.getCareer(),
                s.getCertifications(),
                s.getScreeningStatus() != null ? s.getScreeningStatus().name() : null,
                s.getApprovalNo(),
                s.getReviewedAt(),
                s.getRejectionReason());
    }
}