package org.dpbe.domain.consultation.service;

import org.dpbe.domain.consultation.dto.InterviewScheduleCreateRequest;
import org.dpbe.domain.consultation.dto.InterviewScheduleResponse;
import org.dpbe.domain.consultation.dto.InterviewScheduleUpdateRequest;
import org.dpbe.domain.consultation.entity.InterviewSchedule;
import org.dpbe.domain.consultation.repository.InterviewScheduleRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InterviewScheduleService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final InterviewScheduleRepository scheduleRepo;
    private final AuthAccessService authAccessService;

    public InterviewScheduleService(InterviewScheduleRepository scheduleRepo,
                                    AuthAccessService authAccessService) {
        this.scheduleRepo = scheduleRepo;
        this.authAccessService = authAccessService;
    }

    public PageResponse<InterviewScheduleResponse> findAll(int page, int size) {
        authAccessService.requireInterviewManageAccess();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = scheduleRepo.countAll();
        var items = scheduleRepo.findPage(normalizedSize, offset).stream()
                .map(InterviewScheduleResponse::from)
                .toList();
        return new PageResponse<>(normalizedPage, normalizedSize, total, items);
    }

    private int normalizePage(int page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private Long parseId(String no) {
        try {
            return Long.parseLong(no.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 번호: " + no);
        }
    }

    public InterviewScheduleResponse findByScheduleNo(String scheduleNo) {
        authAccessService.requireInterviewManageAccess();
        InterviewSchedule s = scheduleRepo.findById(parseId(scheduleNo));
        if (s == null) throw ApiException.notFound("면담일정을 찾을 수 없습니다: " + scheduleNo);
        return InterviewScheduleResponse.from(s);
    }

    /** 면담일정 등록 — E1: 필수항목 검증. */
    @Transactional
    public InterviewScheduleResponse create(InterviewScheduleCreateRequest req) {
        authAccessService.requireInterviewManageAccess();
        if (req.customerName() == null || req.customerName().isBlank())
            throw ApiException.badRequest("고객명은 필수입니다.");
        if (req.interviewType() == null || req.interviewType().isBlank())
            throw ApiException.badRequest("면담 유형은 필수입니다.");
        if (req.scheduledAt() == null)
            throw ApiException.badRequest("면담 일시는 필수입니다.");

        InterviewSchedule s = new InterviewSchedule();
        s.setDesignerName(req.designerName());
        s.setType(req.interviewType());
        s.register(req.customerName(), req.scheduledAt(),
                req.location() != null ? req.location() : "",
                req.preparation() != null ? req.preparation() : "");
        scheduleRepo.save(s);
        return InterviewScheduleResponse.from(s);
    }

    /** 면담일정 수정 — E2: 필수항목 검증. */
    @Transactional
    public InterviewScheduleResponse update(String scheduleNo, InterviewScheduleUpdateRequest req) {
        authAccessService.requireInterviewManageAccess();
        InterviewSchedule s = scheduleRepo.findById(parseId(scheduleNo));
        if (s == null) throw ApiException.notFound("면담일정을 찾을 수 없습니다: " + scheduleNo);
        if (req.interviewType() == null || req.interviewType().isBlank())
            throw ApiException.badRequest("면담 유형은 필수입니다.");
        if (req.scheduledAt() == null)
            throw ApiException.badRequest("면담 일시는 필수입니다.");

        s.setType(req.interviewType());
        s.modify(req.scheduledAt(),
                req.location() != null ? req.location() : "",
                req.preparation() != null ? req.preparation() : "");
        scheduleRepo.update(s);
        return InterviewScheduleResponse.from(s);
    }

    /** 면담 취소 (A5). */
    @Transactional
    public InterviewScheduleResponse cancel(String scheduleNo) {
        authAccessService.requireInterviewManageAccess();
        InterviewSchedule s = scheduleRepo.findById(parseId(scheduleNo));
        if (s == null) throw ApiException.notFound("면담일정을 찾을 수 없습니다: " + scheduleNo);
        s.cancel();
        scheduleRepo.updateCancel(s);
        return InterviewScheduleResponse.from(s);
    }
}
