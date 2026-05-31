package org.dpbe.domain.education.dto;

import java.time.LocalDateTime;
import java.util.List;
import org.dpbe.domain.education.entity.Attendance;
import org.dpbe.domain.education.entity.EducationPreparation;

public record EducationPreparationResponse(
        Long id,
        String prepNo,
        String planNo,
        String instructorName,
        String venue,
        boolean materialReady,
        String textbookStatus,
        List<String> attendees,
        String additionalNotice,
        String status,
        LocalDateTime registeredAt
) {
    public static EducationPreparationResponse from(EducationPreparation prep) {
        List<String> attendees = prep.getAttendanceList().stream()
                .map(Attendance::getAttendeeName)
                .toList();
        return new EducationPreparationResponse(
                prep.getId(), prep.getPrepNo(), prep.getPlanNo(),
                prep.getInstructorName(), prep.getVenue(),
                prep.isMaterialReady(), prep.getTextbookStatus(),
                attendees, prep.getAdditionalNotice(),
                prep.getStatus(), prep.getRegisteredAt()
        );
    }
}