package org.dpbe.domain.inquiry.entity;

import java.time.LocalDateTime;
import org.dpbe.domain.common.enums.FaqCategory;
import org.dpbe.domain.common.enums.InquiryStatus;
import org.dpbe.domain.common.enums.InquiryType;

/**
 * 문의 (Inquiry)
 * 고객 문의 유스케이스의 주요 도메인 클래스이다.
 */
public class Inquiry {
    private String inquiryNo;               // 문의 번호 - 자동부여
    private String customerName;            // 문의 고객명
    private InquiryType inquiryType;        // 문의 유형 - 보험료/보험금/계약변경/해지/기타 필수 (enum)
    private String title;                   // 제목 필수, 최대 50자
    private String content;                 // 내용 필수, 최대 1000자
    private Integer currentLength;          // 현재 입력 글자 수
    private String attachmentFileName;      // 첨부 파일명 선택
    private Long attachmentFileSize;        // 첨부 파일 크기 선택
    private LocalDateTime receivedAt;       // 접수 일시
    private InquiryStatus status;           // 처리 상태 - 답변대기/답변완료 (enum)
    private String answerContent;           // 담당자 답변 내용
    private LocalDateTime answeredAt;       // 답변 일시
    private FaqCategory faqCategory;        // FAQ 카테고리 - 전체/보험료/보험금/계약변경/해지/기타 (enum)
    private String faqQuestion;             // FAQ 질문
    private String faqAnswer;              // FAQ 답변

    public Inquiry() {}

    /** DB 로딩용 생성자 */
    public Inquiry(String inquiryNo, InquiryType inquiryType, String title, String content, LocalDateTime receivedAt, InquiryStatus status) {
        this.inquiryNo = inquiryNo;
        this.inquiryType = inquiryType;
        this.title = title;
        this.content = content;
        this.receivedAt = receivedAt;
        this.status = status;
    }

    public Boolean validateRequired() {
        return inquiryType != null
                && title != null && !title.isEmpty() && title.length() <= 50
                && content != null && !content.isEmpty() && content.length() <= 1000;
    }

    public void highlightError() {}

    public Boolean validateFileSize() {
        if (attachmentFileSize == null) return true;
        return attachmentFileSize <= 10 * 1024 * 1024; // 10MB 이하
    }

    public void attachFile() {}

    public void removeFile() {}

    public void submit() {
        this.receivedAt = LocalDateTime.now();
        this.inquiryNo = "INQ-" + receivedAt.toString().replaceAll("[^0-9]", "").substring(0, 14);
        this.status = InquiryStatus.PENDING;
    }

    public void showSuccessPopup() {}

    public void sendConfirmSms() {}

    public void getDetail() {}

    public void filterFaqByCategory() {}

    public void toggleFaqItem() {}

    public void getHistoryList() {}

    public void showFileSizeError() {}

    // Runner에서 실제 사용하는 getter/setter만 유지
    public String getInquiryNo() { return inquiryNo; }
    public void setInquiryNo(String inquiryNo) { this.inquiryNo = inquiryNo; }
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public InquiryType getInquiryType() { return inquiryType; }
    public void setInquiryType(InquiryType inquiryType) { this.inquiryType = inquiryType; }
    public String getTitle() { return title; }
    public void setTitle(String title) {
        this.title = title;
        this.currentLength = title != null ? title.length() : 0;
    }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getCurrentLength() { return currentLength; }
    public String getAttachmentFileName() { return attachmentFileName; }
    public void setAttachmentFileName(String attachmentFileName) { this.attachmentFileName = attachmentFileName; }
    public Long getAttachmentFileSize() { return attachmentFileSize; }
    public void setAttachmentFileSize(Long attachmentFileSize) { this.attachmentFileSize = attachmentFileSize; }
    public LocalDateTime getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDateTime receivedAt) { this.receivedAt = receivedAt; }
    public InquiryStatus getStatus() { return status; }
    public void setStatus(InquiryStatus status) { this.status = status; }
    public String getAnswerContent() { return answerContent; }
    public void setAnswerContent(String answerContent) { this.answerContent = answerContent; }
    public LocalDateTime getAnsweredAt() { return answeredAt; }
    public void setAnsweredAt(LocalDateTime answeredAt) { this.answeredAt = answeredAt; }
    public void setFaqCategory(FaqCategory faqCategory) { this.faqCategory = faqCategory; }
}