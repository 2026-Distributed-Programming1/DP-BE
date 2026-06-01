package org.dpbe.domain.inquiry.service;

import java.time.LocalDateTime;
import java.util.List;
import org.dpbe.domain.common.enums.InquiryStatus;
import org.dpbe.domain.inquiry.dto.InquiryAnswerRequest;
import org.dpbe.domain.inquiry.dto.InquiryRequest;
import org.dpbe.domain.inquiry.dto.InquiryResponse;
import org.dpbe.domain.inquiry.entity.Inquiry;
import org.dpbe.domain.inquiry.repository.InquiryRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InquiryService {

    private final InquiryRepository repository;

    public InquiryService(InquiryRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<InquiryResponse> getInquiries(String customerName, String status) {
        List<Inquiry> list;
        if (customerName != null && !customerName.isBlank() && status != null && !status.isBlank()) {
            list = repository.findByCustomerNameAndStatus(customerName, status);
        } else if (customerName != null && !customerName.isBlank()) {
            list = repository.findByCustomerName(customerName);
        } else if (status != null && !status.isBlank()) {
            list = repository.findByStatus(status);
        } else {
            list = repository.findAll();
        }
        return list.stream().map(InquiryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public InquiryResponse getInquiry(String inquiryNo) {
        Inquiry inquiry = repository.findById(parseId(inquiryNo));
        if (inquiry == null) throw ApiException.notFound("문의를 찾을 수 없습니다: " + inquiryNo);
        return InquiryResponse.from(inquiry);
    }

    private Long parseId(String inquiryNo) {
        try {
            return Long.parseLong(inquiryNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 문의번호: " + inquiryNo);
        }
    }

    @Transactional
    public InquiryResponse submit(InquiryRequest req) {
        Inquiry inquiry = new Inquiry();
        inquiry.setCustomerName(req.customerName());
        inquiry.setInquiryType(req.inquiryType());
        inquiry.setTitle(req.title());
        inquiry.setContent(req.content());
        inquiry.setAttachmentFileName(req.attachmentFileName());
        inquiry.setAttachmentFileSize(req.attachmentFileSize());

        if (!inquiry.validateRequired()) {
            throw ApiException.badRequest("필수 항목을 확인해주세요. (문의유형·제목(50자 이내)·내용(1000자 이내) 필수)");
        }
        if (!inquiry.validateFileSize()) {
            throw ApiException.badRequest("첨부 파일은 10MB 이하만 업로드 가능합니다.");
        }

        inquiry.setReceivedAt(LocalDateTime.now());
        inquiry.setStatus(InquiryStatus.PENDING);
        repository.save(inquiry);
        return InquiryResponse.from(inquiry);
    }

    @Transactional
    public InquiryResponse answer(String inquiryNo, InquiryAnswerRequest req) {
        Inquiry inquiry = repository.findById(parseId(inquiryNo));
        if (inquiry == null) throw ApiException.notFound("문의를 찾을 수 없습니다: " + inquiryNo);
        if (InquiryStatus.ANSWERED.equals(inquiry.getStatus())) {
            throw ApiException.badRequest("이미 답변이 완료된 문의입니다.");
        }
        if (req.answerContent() == null || req.answerContent().isBlank()) {
            throw ApiException.badRequest("답변 내용을 입력해주세요.");
        }
        inquiry.setAnswerContent(req.answerContent());
        inquiry.setAnsweredAt(LocalDateTime.now());
        inquiry.setStatus(InquiryStatus.ANSWERED);
        repository.updateAnswer(inquiry);
        return InquiryResponse.from(inquiry);
    }
}