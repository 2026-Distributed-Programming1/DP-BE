package org.dpbe.domain.education.service;

import java.util.List;
import org.dpbe.domain.education.dto.EducationPreparationRequest;
import org.dpbe.domain.education.dto.EducationPreparationResponse;
import org.dpbe.domain.education.entity.EducationPreparation;
import org.dpbe.domain.education.repository.EducationPlanRepository;
import org.dpbe.domain.education.repository.EducationPreparationRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EducationPreparationService {

    private final EducationPreparationRepository repository;
    private final EducationPlanRepository planRepository;
    private final AuthAccessService authAccessService;

    public EducationPreparationService(EducationPreparationRepository repository,
                                       EducationPlanRepository planRepository,
                                       AuthAccessService authAccessService) {
        this.repository = repository;
        this.planRepository = planRepository;
        this.authAccessService = authAccessService;
    }

    @Transactional(readOnly = true)
    public List<EducationPreparationResponse> getPreparations(String planNo) {
        authAccessService.requireEducationOperationAccess();
        List<EducationPreparation> list = (planNo != null && !planNo.isBlank())
                ? repository.findByPlanNo(planNo)
                : repository.findAll();
        return list.stream().map(EducationPreparationResponse::from).toList();
    }

    private Long parseId(String no) {
        try {
            return Long.parseLong(no.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 번호: " + no);
        }
    }

    @Transactional(readOnly = true)
    public EducationPreparationResponse getPreparation(String prepNo) {
        authAccessService.requireEducationOperationAccess();
        EducationPreparation prep = repository.findById(parseId(prepNo));
        if (prep == null) throw ApiException.notFound("교육 제반을 찾을 수 없습니다: " + prepNo);
        return EducationPreparationResponse.from(prep);
    }

    @Transactional
    public EducationPreparationResponse createPreparation(EducationPreparationRequest req) {
        authAccessService.requireEducationOperationAccess();
        if (planRepository.findById(parseId(req.planNo())) == null) {
            throw ApiException.notFound("승인된 교육 계획안을 찾을 수 없습니다: " + req.planNo());
        }
        EducationPreparation prep = new EducationPreparation();
        prep.setPlanNo(req.planNo());
        prep.enterPreparationInfo(req.venue(), req.instructorName(), req.additionalNotice());
        prep.setTextbookStatus(req.textbookStatus());
        if (req.attendees() != null) {
            req.attendees().forEach(prep::addAttendee);
        }
        if (!prep.validateRequiredFields()) {
            throw ApiException.badRequest("필수 항목을 입력해주세요. (장소·강사명·교재현황·대상자 필수)");
        }
        prep.save();
        repository.save(prep);
        return EducationPreparationResponse.from(prep);
    }
}
