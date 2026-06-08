package org.dpbe.domain.payment.service;

import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.repository.CancellationRepository;
import org.dpbe.domain.payment.entity.RefundCalculation;
import org.dpbe.domain.payment.entity.RefundPayment;
import org.dpbe.domain.payment.repository.RefundCalculationRepository;
import org.dpbe.domain.payment.repository.RefundPaymentRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC '해약 환급금을 산출한다' API 서비스. */
@Service
public class CalculateRefundService {

    private final CancellationRepository cancellationRepository;
    private final RefundCalculationRepository refundCalculationRepository;
    private final RefundPaymentRepository refundPaymentRepository;
    private final AuthAccessService authAccessService;

    public CalculateRefundService(CancellationRepository cancellationRepository,
                                  RefundCalculationRepository refundCalculationRepository,
                                  RefundPaymentRepository refundPaymentRepository,
                                  AuthAccessService authAccessService) {
        this.cancellationRepository = cancellationRepository;
        this.refundCalculationRepository = refundCalculationRepository;
        this.refundPaymentRepository = refundPaymentRepository;
        this.authAccessService = authAccessService;
    }

    /** 환급금 산출 — POST /api/cancellations/{cancellationNo}/refund-calculation */
    @Transactional
    public RefundCalculation calculate(String cancellationNo) {
        authAccessService.requireRefundOperationAccess();
        Cancellation cancellation = cancellationRepository.findById(parseId(cancellationNo))
                .orElseThrow(() -> ApiException.notFound("해지 건을 찾을 수 없습니다: " + cancellationNo));

        if (refundCalculationRepository.findByCancellationNo(cancellationNo).isPresent()) {
            throw ApiException.badRequest("이미 환급금 산출이 완료된 해지 건입니다: " + cancellationNo);
        }

        RefundCalculation refund = new RefundCalculation(cancellation);
        refundCalculationRepository.save(refund);
        return refund;
    }

    /** 환급금 확정 + 지급 이관 — POST /api/refund-calculations/{refundNo}/confirm */
    @Transactional
    public RefundPayment confirm(String refundNo) {
        authAccessService.requireRefundOperationAccess();
        RefundCalculation refund = refundCalculationRepository.findById(parseId(refundNo))
                .orElseThrow(() -> ApiException.notFound("환급금 산출 건을 찾을 수 없습니다: " + refundNo));

        if (refundPaymentRepository.findByRefundNo(refundNo).isPresent()) {
            throw ApiException.badRequest("이미 지급 이관된 환급금 건입니다: " + refundNo);
        }

        RefundPayment payment = refund.confirm();
        refundCalculationRepository.updateStatus(refund);
        refundPaymentRepository.save(payment);
        return payment;
    }

    private Long parseId(String businessNo) {
        try {
            return Long.parseLong(businessNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 번호: " + businessNo);
        }
    }
}
