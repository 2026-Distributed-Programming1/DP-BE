package org.dpbe.domain.consultation.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.consultation.dto.ConsultationCreateRequest;
import org.dpbe.domain.consultation.dto.ConsultationResponse;
import org.dpbe.domain.consultation.entity.ConsultationRequest;
import org.dpbe.domain.consultation.repository.ConsultationRequestRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ConsultationService {

    private final ConsultationRequestRepository consultationRepo;

    public ConsultationService(ConsultationRequestRepository consultationRepo) {
        this.consultationRepo = consultationRepo;
    }

    public List<ConsultationResponse> findAll() {
        return consultationRepo.findAll().stream()
                .map(ConsultationResponse::from)
                .collect(Collectors.toList());
    }

    private Long parseId(String no) {
        try {
            return Long.parseLong(no.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 번호: " + no);
        }
    }

    public ConsultationResponse findByConsultNo(String consultNo) {
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
        ConsultationRequest r = consultationRepo.findById(parseId(consultNo));
        if (r == null) throw ApiException.notFound("상담 요청을 찾을 수 없습니다: " + consultNo);
        if (!"접수".equals(r.getStatus()))
            throw ApiException.badRequest("이미 처리된 상담 요청입니다. (현재 상태: " + r.getStatus() + ")");
        r.accept();
        consultationRepo.updateAccept(r);
        return ConsultationResponse.from(r);
    }
}