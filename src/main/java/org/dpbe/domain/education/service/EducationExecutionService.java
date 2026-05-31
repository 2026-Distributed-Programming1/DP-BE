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
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EducationExecutionService {

    private final EducationExecutionRepository repository;
    private final EducationPreparationRepository prepRepository;

    public EducationExecutionService(EducationExecutionRepository repository,
                                     EducationPreparationRepository prepRepository) {
        this.repository = repository;
        this.prepRepository = prepRepository;
    }

    @Transactional(readOnly = true)
    public List<EducationExecutionResponse> getExecutions(String prepNo) {
        List<EducationExecution> list = (prepNo != null && !prepNo.isBlank())
                ? repository.findByPrepNo(prepNo)
                : repository.findAll();
        return list.stream()
                .map(e -> EducationExecutionResponse.from(e, repository.findAttendances(e.getExecutionNo())))
                .toList();
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
        EducationExecution exec = repository.findById(parseId(executionNo));
        if (exec == null) throw ApiException.notFound("교육 진행 기록을 찾을 수 없습니다: " + executionNo);
        List<AttendanceDetail> attendances = repository.findAttendances(exec.getExecutionNo());
        return EducationExecutionResponse.from(exec, attendances);
    }

    @Transactional
    public EducationExecutionResponse createExecution(EducationExecutionRequest req) {
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