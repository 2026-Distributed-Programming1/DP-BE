package org.dpbe.domain.consultation.service;

import org.dpbe.domain.consultation.dto.ConsultationCreateRequest;
import org.dpbe.domain.consultation.dto.ConsultationResponse;
import org.dpbe.domain.consultation.entity.ConsultationRequest;
import org.dpbe.domain.consultation.repository.ConsultationRequestRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsultationService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ConsultationRequestRepository consultationRepo;
    private final AuthAccessService authAccessService;

    public ConsultationService(ConsultationRequestRepository consultationRepo,
                               AuthAccessService authAccessService) {
        this.consultationRepo = consultationRepo;
        this.authAccessService = authAccessService;
    }

    public PageResponse<ConsultationResponse> findAll(int page, int size) {
        authAccessService.requireConsultationManageAccess();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = consultationRepo.countAll();
        var items = consultationRepo.findPage(normalizedSize, offset).stream()
                .map(ConsultationResponse::from)
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

    public ConsultationResponse findByConsultNo(String consultNo) {
        authAccessService.requireConsultationManageAccess();
        ConsultationRequest r = consultationRepo.findById(parseId(consultNo));
        if (r == null) throw ApiException.notFound("상담 요청을 찾을 수 없습니다: " + consultNo);
        return ConsultationResponse.from(r);
    }

    /** 상담 신청 — 필수항목 검증 후 저장. */
    @Transactional
    public ConsultationResponse create(ConsultationCreateRequest req) {
        if (req.type() == null || req.type().isBlank())
            throw ApiException.badRequest("상담 유형을 선택해야 합니다.");
        if (req.contact() == null || req.contact().isBlank())
            throw ApiException.badRequest("연락처는 필수입니다.");
        if (req.content() == null || req.content().isBlank())
            throw ApiException.badRequest("상담 내용은 필수입니다.");

        ConsultationRequest r = new ConsultationRequest();
        r.selectType(req.type());
        r.enterConsultationInfo(req.scheduledAt(), req.location(), req.contact(), req.content());
        r.submit();
        consultationRepo.save(r);
        return ConsultationResponse.from(r);
    }

    /** 상담 수락. */
    @Transactional
    public ConsultationResponse accept(String consultNo) {
        authAccessService.requireConsultationManageAccess();
        ConsultationRequest r = consultationRepo.findById(parseId(consultNo));
        if (r == null) throw ApiException.notFound("상담 요청을 찾을 수 없습니다: " + consultNo);
        r.accept();
        consultationRepo.updateAccept(r);
        return ConsultationResponse.from(r);
    }
}
