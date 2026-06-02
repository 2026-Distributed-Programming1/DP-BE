package org.dpbe.domain.payment.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.common.enums.PaymentRecordStatus;
import org.dpbe.domain.common.enums.RejectCategory;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.contract.repository.ContractRepository;
import org.dpbe.domain.payment.dto.PaymentRecordDetail;
import org.dpbe.domain.payment.dto.PaymentRecordRejectRequest;
import org.dpbe.domain.payment.entity.PaymentRecord;
import org.dpbe.domain.payment.repository.PaymentRecordRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentRecordService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final ContractRepository contractRepository;
    private final AuthAccessService authAccessService;

    public PaymentRecordService(PaymentRecordRepository paymentRecordRepository,
                                ContractRepository contractRepository,
                                AuthAccessService authAccessService) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.contractRepository = contractRepository;
        this.authAccessService = authAccessService;
    }

    /** 납부 내역 목록 — contractNo·status 필터 선택 */
    @Transactional(readOnly = true)
    public List<PaymentRecordDetail> getAll(String contractNo, String status) {
        List<PaymentRecord> list;
        if (contractNo != null && !contractNo.isBlank()) {
            list = paymentRecordRepository.findByContractNo(contractNo);
        } else if (status != null && !status.isBlank()) {
            PaymentRecordStatus s;
            try { s = PaymentRecordStatus.valueOf(status); }
            catch (IllegalArgumentException e) {
                throw ApiException.badRequest("유효하지 않은 상태값입니다 (WAITING/COMPLETED/REJECTED): " + status);
            }
            list = paymentRecordRepository.findByStatus(s);
        } else {
            list = paymentRecordRepository.findAll();
        }
        return list.stream()
                .filter(this::canAccessRecord)
                .map(this::toDetail)
                .collect(Collectors.toList());
    }

    private Long parseId(String recordNo) {
        try {
            return Long.parseLong(recordNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 납부내역번호: " + recordNo);
        }
    }

    /** 수납 확정 */
    @Transactional
    public PaymentRecordDetail confirm(String recordNo) {
        authAccessService.requirePaymentRecordManageAccess();
        PaymentRecord record = paymentRecordRepository.findById(parseId(recordNo))
                .orElseThrow(() -> ApiException.notFound("납부 내역을 찾을 수 없습니다: " + recordNo));

        if (record.getStatus() != PaymentRecordStatus.WAITING) {
            throw ApiException.badRequest("대기 상태인 납부 내역만 확정할 수 있습니다. 현재 상태: " + record.getStatus());
        }

        record.confirm();
        paymentRecordRepository.update(record);
        return toDetail(record);
    }

    /** 수납 반려 */
    @Transactional
    public PaymentRecordDetail reject(String recordNo, PaymentRecordRejectRequest req) {
        authAccessService.requirePaymentRecordManageAccess();
        if (req.rejectCategory() == null || req.rejectCategory().isBlank()) {
            throw ApiException.badRequest("반려 분류를 입력해야 합니다.");
        }

        PaymentRecord record = paymentRecordRepository.findById(parseId(recordNo))
                .orElseThrow(() -> ApiException.notFound("납부 내역을 찾을 수 없습니다: " + recordNo));

        if (record.getStatus() != PaymentRecordStatus.WAITING) {
            throw ApiException.badRequest("대기 상태인 납부 내역만 반려할 수 있습니다. 현재 상태: " + record.getStatus());
        }

        RejectCategory category;
        try { category = RejectCategory.valueOf(req.rejectCategory()); }
        catch (IllegalArgumentException e) {
            throw ApiException.badRequest("유효하지 않은 반려 분류입니다 (PAYMENT_ERROR/DUPLICATE_PAYMENT/CONTRACT_MISMATCH/OTHER): " + req.rejectCategory());
        }

        record.enterRejectInfo(category, req.rejectReason());
        record.reject();
        paymentRecordRepository.update(record);
        return toDetail(record);
    }

    private PaymentRecordDetail toDetail(PaymentRecord r) {
        String contractNo   = r.getContract() != null ? r.getContract().getContractNo() : null;
        String customerName = r.getContract() != null && r.getContract().getCustomer() != null
                ? r.getContract().getCustomer().getName() : null;
        String rc = r.getRejectCategory() != null ? r.getRejectCategory().name() : null;
        return new PaymentRecordDetail(
                r.getRecordNo(), contractNo, customerName,
                r.getAmount(), r.getMethod(), r.getPaymentDate(),
                r.getStatus() != null ? r.getStatus().name() : null,
                r.getConfirmedAt(), r.getRejectedAt(), rc, r.getRejectReason());
    }

    private boolean canAccessRecord(PaymentRecord record) {
        Contract contract = loadFullContract(record);
        return authAccessService.canAccessContract(contract);
    }

    private Contract loadFullContract(PaymentRecord record) {
        if (record == null || record.getContract() == null || record.getContract().getId() == null) {
            return null;
        }
        return contractRepository.findById(record.getContract().getId());
    }
}
