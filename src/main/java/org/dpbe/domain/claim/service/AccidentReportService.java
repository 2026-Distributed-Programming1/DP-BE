package org.dpbe.domain.claim.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.claim.dto.AccidentCreateRequest;
import org.dpbe.domain.claim.dto.AccidentResponse;
import org.dpbe.domain.claim.entity.AccidentReport;
import org.dpbe.domain.claim.entity.Dispatch;
import org.dpbe.domain.claim.repository.AccidentReportRepository;
import org.dpbe.domain.claim.repository.DispatchRepository;
import org.dpbe.domain.common.enums.AccidentReportStatus;
import org.dpbe.domain.common.enums.AccidentType;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC '사고를 접수한다' API 서비스.
 * 콘솔 AccidentReportRunner의 규칙·검증(필수입력 E1, 가입 대조)만 이관한다.
 * needs_dispatch면 접수 직후 현장출동(Dispatch)을 같은 트랜잭션에서 생성한다.
 */
@Service
@Transactional(readOnly = true)
public class AccidentReportService {

    private final AccidentReportRepository accidentRepository;
    private final DispatchRepository dispatchRepository;
    private final CustomerRepository customerRepository;

    public AccidentReportService(AccidentReportRepository accidentRepository,
                                 DispatchRepository dispatchRepository,
                                 CustomerRepository customerRepository) {
        this.accidentRepository = accidentRepository;
        this.dispatchRepository = dispatchRepository;
        this.customerRepository = customerRepository;
    }

    public List<AccidentResponse> findAll() {
        return accidentRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private Long parseId(String no) {
        try {
            return Long.parseLong(no.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 번호: " + no);
        }
    }

    public AccidentResponse findByReportNo(String reportNo) {
        AccidentReport r = accidentRepository.findById(parseId(reportNo));
        if (r == null) {
            throw ApiException.notFound("사고 접수를 찾을 수 없습니다: " + reportNo);
        }
        return toResponse(r);
    }

    /** 사고 접수 — 검증 후 저장. needs_dispatch면 출동도 함께 생성(@Transactional). */
    @Transactional
    public AccidentResponse create(AccidentCreateRequest request) {
        Customer customer = customerRepository.findById(request.customerId());
        if (customer == null) {
            throw ApiException.notFound("고객을 찾을 수 없습니다: " + request.customerId());
        }

        AccidentReport report = new AccidentReport(customer);
        report.enterVehicleInfo(request.vehicleNo(), request.ownerName(), request.phoneNo());
        report.selectAccidentType(parseAccidentType(request.accidentType()), request.damageType());
        report.enterLocation(request.location());
        report.setDispatchOption(request.needsDispatch());
        report.enterCasualtyInfo(request.casualtyCount(),
                request.injurySeverity(), request.emergencyReported());
        if (request.agreedTerms()) {
            report.agreeTerms();
        }

        if (!report.validateRequiredFields()) {
            throw ApiException.badRequest("필수 항목 누락(차량/소유자/연락처/사고유형/위치/약관동의).");
        }
        if (!report.verifyContract()) {
            throw ApiException.badRequest("[E1] 당사 가입 내역을 확인할 수 없습니다.");
        }

        report.receive();
        if (report.getStatus() != AccidentReportStatus.RECEIVED) {
            throw ApiException.badRequest("사고 접수에 실패했습니다.");
        }
        accidentRepository.save(report);

        // 현장출동 필요 시 같은 트랜잭션에서 Dispatch 생성
        String dispatchNo = null;
        Dispatch dispatch = report.requestDispatch();
        if (dispatch != null) {
            dispatchRepository.save(dispatch);
            dispatchNo = dispatch.getDispatchNo();
        }
        return AccidentResponse.from(report, dispatchNo);
    }

    private AccidentResponse toResponse(AccidentReport r) {
        Dispatch d = dispatchRepository.findByAccidentNo(r.getReportNo());
        return AccidentResponse.from(r, d != null ? d.getDispatchNo() : null);
    }

    private AccidentType parseAccidentType(String type) {
        if (type == null) {
            throw ApiException.badRequest("사고 유형을 선택해야 합니다.");
        }
        try {
            return AccidentType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("알 수 없는 사고 유형입니다: " + type);
        }
    }
}