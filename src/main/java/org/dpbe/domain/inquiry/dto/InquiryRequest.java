package org.dpbe.domain.inquiry.dto;

import org.dpbe.domain.common.enums.InquiryType;

public record InquiryRequest(
        String customerName,
        InquiryType inquiryType,
        String title,
        String content,
        String attachmentFileName,
        Long attachmentFileSize
) {}