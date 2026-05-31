package org.dpbe.domain.education.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.dpbe.domain.education.entity.EducationExecution;

public record EducationExecutionResponse(
        Long id,
        String executionNo,
        String prepNo,
        String trainerName,
        LocalDateTime executedAt,
        int attendeeCount,
        int totalCount,
        String memo,
        String status,
        List<AttendanceDetail> attendances
) {
    public record AttendanceDetail(String attendeeName, boolean attended) {}

    public static EducationExecutionResponse from(EducationExecution exec, List<AttendanceDetail> attendances) {
        String prepNo = exec.getPreparation() != null ? exec.getPreparation().getPrepNo() : null;
        String trainerName = exec.getPreparation() != null ? exec.getPreparation().getInstructorName() : null;
        return new EducationExecutionResponse(
                exec.getId(), exec.getExecutionNo(), prepNo,
                trainerName,
                exec.getExecutedAt(),
                exec.getAttendanceCount(), exec.getTotalCount(),
                exec.getMemo(), exec.getStatus(),
                attendances
        );
    }
}