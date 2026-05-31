package org.dpbe.domain.sales.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import org.dpbe.domain.sales.entity.ChannelRecruitment;

public record ChannelRecruitmentResponse(
        Long id,
        String recruitmentNo,
        String managerName,
        String channelType,
        Integer recruitCount,
        LocalDate startDate,
        LocalDate endDate,
        String condition,
        String status,
        LocalDateTime registeredAt
) {
    public static ChannelRecruitmentResponse from(ChannelRecruitment r) {
        return new ChannelRecruitmentResponse(
                r.getId(),
                r.getRecruitmentNo(),
                r.getManagerName(),
                r.getChannelType() != null ? r.getChannelType().name() : null,
                r.getRecruitCount(),
                r.getStartDate(),
                r.getEndDate(),
                r.getCondition(),
                r.getStatus(),
                r.getRegisteredAt());
    }
}