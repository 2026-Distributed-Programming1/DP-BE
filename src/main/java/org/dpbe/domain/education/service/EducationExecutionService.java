package org.dpbe.domain.education.service;

import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.education.dto.EducationExecutionRequest;
import org.dpbe.domain.education.dto.EducationExecutionRequest.AttendanceRecord;
import org.dpbe.domain.education.dto.EducationExecutionResponse;
import org.dpbe.domain.education.dto.EducationExecutionResponse.AttendanceDetail;
import org.dpbe.domain.education.entity.Attendance;
import org.dpbe.domain.education.entity.EducationExecution;
import org.dpbe.domain.education.entity.EducationPreparation;
import org.dpbe.domain.education.repository.EducationExecutionRepository;
import org.dpbe.domain.education.repository.EducationPreparationRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EducationExecutionService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final EducationExecutionRepository repository;
    private final EducationPreparationRepository prepRepository;
    private final AuthAccessService authAccessService;

    public EducationExecutionService(EducationExecutionRepository repository,
                                     EducationPreparationRepository prepRepository,
                                     AuthAccessService authAccessService) {
        this.repository = repository;
        this.prepRepository = prepRepository;
        this.authAccessService = authAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponse<EducationExecutionResponse> getExecutions(String prepNo, int page, int size) {
        authAccessService.requireEducationOperationAccess();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = repository.countByPrepNo(prepNo);
        List<EducationExecutionResponse> items = repository.findPageByPrepNo(prepNo, normalizedSize, offset).stream()
                .map(e -> EducationExecutionResponse.from(e, repository.findAttendances(e.getExecutionNo())))
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
            throw ApiException.badRequest("유효하지 않은 번호: " + no);
        }
    }

    @Transactional(readOnly = true)
    public EducationExecutionResponse getExecution(String executionNo) {
        authAccessService.requireEducationOperationAccess();
        EducationExecution exec = repository.findById(parseId(executionNo));
        if (exec == null) throw ApiException.notFound("교육 진행 기록을 찾을 수 없습니다: " + executionNo);
        List<AttendanceDetail> attendances = repository.findAttendances(exec.getExecutionNo());
        return EducationExecutionResponse.from(exec, attendances);
    }

    @Transactional
    public EducationExecutionResponse createExecution(EducationExecutionRequest req) {
        authAccessService.requireEducationOperationAccess();
        if (prepRepository.findById(parseId(req.prepNo())) == null) {
            throw ApiException.notFound("교육 제반을 찾을 수 없습니다: " + req.prepNo());
        }
        if (req.attendances() == null || req.attendances().isEmpty()) {
            throw ApiException.badRequest("출석 대상자가 없습니다.");
        }

        List<Attendance> markedList = new ArrayList<>();
        for (AttendanceRecord ar : req.attendances()) {
            markedList.add(new Attendance(ar.attendeeName(), ar.attended()));
        }

        EducationPreparation prepShell = new EducationPreparation(
                0, null, null, req.trainerName(), null, null, markedList);
        prepShell.setPrepNo(req.prepNo());

        EducationExecution exec = new EducationExecution(prepShell);
        if (req.memo() != null && !req.memo().isBlank()) exec.setMemo(req.memo());
        exec.complete();

        repository.save(exec);

        List<AttendanceDetail> attendances = repository.findAttendances(exec.getExecutionNo());
        return EducationExecutionResponse.from(exec, attendances);
    }
}
