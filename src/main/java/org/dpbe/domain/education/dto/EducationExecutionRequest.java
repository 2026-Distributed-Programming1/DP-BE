package org.dpbe.domain.education.dto;

import java.util.List;

public record EducationExecutionRequest(
        String prepNo,
        String trainerName,
        List<AttendanceRecord> attendances,
        String memo
) {
    public record AttendanceRecord(String attendeeName, boolean attended) {}
}