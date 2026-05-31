package org.dpbe.domain.education.dto;

import java.util.List;

public record EducationPreparationRequest(
        String planNo,
        String instructorName,
        String venue,
        String textbookStatus,
        List<String> attendees,
        String additionalNotice
) {}