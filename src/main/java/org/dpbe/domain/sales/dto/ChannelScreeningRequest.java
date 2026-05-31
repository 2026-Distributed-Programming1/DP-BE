package org.dpbe.domain.sales.dto;

import java.time.LocalDate;
import java.util.List;

public record ChannelScreeningRequest(
        String applicantName,
        String channelType,
        LocalDate applicationDate,
        String career,
        List<String> certifications
) {}