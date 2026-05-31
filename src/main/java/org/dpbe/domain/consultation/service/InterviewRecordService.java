package org.dpbe.domain.consultation.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.consultation.dto.InterviewRecordCreateRequest;
import org.dpbe.domain.consultation.dto.InterviewRecordResponse;
import org.dpbe.domain.consultation.dto.InterviewRecordUpdateRequest;
import org.dpbe.domain.consultation.entity.InterviewRecord;
import org.dpbe.domain.consultation.repository.InterviewRecordRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InterviewRecordService {

    private final InterviewRecordRepository recordRepo;

    public InterviewRecordService(InterviewRecordRepository recordRepo) {
        this.recordRepo = recordRepo;
    }

    public List<InterviewRecordResponse> findAll() {
        return recordRepo.findAll().stream()
                .map(InterviewRecordResponse::from)
                .collect(Collectors.toList());
    }

    public InterviewRecordResponse findByRecordNo(String recordNo) {
        InterviewRecord r = recordRepo.findByRecordNo(recordNo);
        if (r == null) throw ApiException.notFound("면담기록을 찾을 수 없습니다: " + recordNo);
        return InterviewRecordResponse.from(r);
    }

    /** 면담기록 등록 — E1: 필수항목 검증. */
    @Transactional
    public InterviewRecordResponse create(InterviewRecordCreateRequest req) {
        if (req.customerName() == null || req.customerName().isBlank())
            throw ApiException.badRequest("고객명은 필수입니다.");
        if (req.interviewedAt() == null)
            throw ApiException.badRequest("면담 일시는 필수입니다.");
        if (req.content() == null || req.content().isBlank())
            throw ApiException.badRequest("면담 내용은 필수입니다.");

        InterviewRecord r = new InterviewRecord();
        r.setCustomerName(req.customerName());
        r.setInterviewedAt(req.interviewedAt());
        r.save(req.content(),
                req.customerReaction() != null ? req.customerReaction() : "",
                req.followUpAction());
        recordRepo.save(r);
        return InterviewRecordResponse.from(r);
    }

    /** 면담기록 수정 — E2: 면담 내용 필수. */
    @Transactional
    public InterviewRecordResponse update(String recordNo, InterviewRecordUpdateRequest req) {
        InterviewRecord r = recordRepo.findByRecordNo(recordNo);
        if (r == null) throw ApiException.notFound("면담기록을 찾을 수 없습니다: " + recordNo);
        if (req.content() == null || req.content().isBlank())
            throw ApiException.badRequest("면담 내용은 필수입니다.");

        r.modify(req.content(),
                req.customerReaction() != null ? req.customerReaction() : "",
                req.followUpAction());
        recordRepo.update(r);
        return InterviewRecordResponse.from(r);
    }
}