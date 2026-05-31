package org.dpbe.domain.inquiry.dto;

import java.time.LocalDateTime;
import org.dpbe.domain.inquiry.entity.Inquiry;

public record InquiryResponse(
        Long id,
        String inquiryNo,
        String customerName,
        String inquiryType,
        String title,
        String content,
        String attachmentFileName,
        Long attachmentFileSize,
        String answerContent,
        LocalDateTime answeredAt,
        String status,
        LocalDateTime receivedAt
) {
    public static InquiryResponse from(Inquiry i) {
        return new InquiryResponse(
                i.getId(), i.getInquiryNo(), i.getCustomerName(),
                i.getInquiryType() != null ? i.getInquiryType().name() : null,
                i.getTitle(), i.getContent(),
                i.getAttachmentFileName(), i.getAttachmentFileSize(),
                i.getAnswerContent(), i.getAnsweredAt(),
                i.getStatus() != null ? i.getStatus().name() : null,
                i.getReceivedAt()
        );
    }
}