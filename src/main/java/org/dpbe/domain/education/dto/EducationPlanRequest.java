package org.dpbe.domain.education.dto;

import java.time.LocalDate;

/**
 * action: "TEMP_SAVE" | "REQUEST_APPROVAL"
 */
public record EducationPlanRequest(
        String trainerName,
        String educationName,
        String channelType,
        LocalDate startDate,
        LocalDate endDate,
        int targetCount,
        long budget,
        String educationGoal,
        String educationContent,
        String textbookList,
        String action
) {}