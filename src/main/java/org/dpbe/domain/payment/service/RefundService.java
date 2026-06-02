package org.dpbe.domain.payment.service;

import java.util.List;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.contract.repository.CancellationRepository;
import org.dpbe.domain.common.enums.RefundPaymentStatus;
import org.dpbe.domain.common.enums.RefundStatus;
import org.dpbe.domain.payment.entity.RefundCalculation;
import org.dpbe.domain.payment.entity.RefundPayment;
import org.dpbe.domain.payment.repository.RefundCalculationRepository;
import org.dpbe.domain.payment.repository.RefundPaymentRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {

    private final CancellationRepository cancellationRepository;
    private final RefundCalculationRepository refundCalculationRepository;
    private final RefundPaymentRepository refundPaymentRepository;
    private final AuthAccessService authAccessService;

    public RefundService(CancellationRepository cancellationRepository,
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
        if (refund.getStatus() == RefundStatus.CALCULATION_PENDING) {
            throw ApiException.badRequest("환급금 산출에 필요한 데이터가 누락되었습니다.");
        }

        refundCalculationRepository.save(refund);
        return refund;
    }

    private Long parseId(String businessNo) {
        try {
            return Long.parseLong(businessNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 번호: " + businessNo);
        }
    }

    /** 환급금 확정 + 지급 이관 — POST /api/refund-calculations/{refundNo}/confirm */
    @Transactional
    public RefundPayment confirm(String refundNo) {
        authAccessService.requireRefundOperationAccess();
        RefundCalculation refund = refundCalculationRepository.findById(parseId(refundNo))
                .orElseThrow(() -> ApiException.notFound("환급금 산출 건을 찾을 수 없습니다: " + refundNo));

        if (refund.getStatus() != RefundStatus.CALCULATED) {
            throw ApiException.badRequest("산출 완료 상태인 경우에만 확정할 수 있습니다.");
        }
        if (refundPaymentRepository.findByRefundNo(refundNo).isPresent()) {
            throw ApiException.badRequest("이미 지급 이관된 환급금 건입니다: " + refundNo);
        }

        RefundPayment payment = new RefundPayment(refund);
        refundPaymentRepository.save(payment);
        return payment;
    }

    /** OTP 인증 후 이체 실행 — POST /api/refund-payments/{paymentNo}/execute */
    @Transactional
    public RefundPayment execute(String paymentNo, String otpInput) {
        authAccessService.requireRefundOperationAccess();
        RefundPayment payment = refundPaymentRepository.findById(parseId(paymentNo))
                .orElseThrow(() -> ApiException.notFound("환급금 지급 건을 찾을 수 없습니다: " + paymentNo));

        if (payment.getStatus() == RefundPaymentStatus.COMPLETED) {
            throw ApiException.badRequest("이미 완료된 지급 건입니다.");
        }
        if (payment.getStatus() == RefundPaymentStatus.LOCKED) {
            throw ApiException.badRequest("OTP 5회 실패로 잠금된 지급 건입니다. 관리자에게 문의하세요.");
        }

        payment.enterOTP(otpInput);
        boolean verified = payment.verifyOTP();

        if (!verified) {
            refundPaymentRepository.update(payment);
            if (payment.isLocked()) {
                throw ApiException.badRequest("OTP 5회 실패로 잠금 처리되었습니다.");
            }
            throw ApiException.badRequest("OTP 인증에 실패했습니다. 남은 시도: " + (5 - payment.getOtpFailCount()) + "회");
        }

        // 계좌 연동은 외부 시스템 — stub 계좌를 주입해 execute() 통과
        BankAccount stubAccount = new BankAccount();
        stubAccount.enter("시스템", "000-0000", "자동이체");
        stubAccount.verify();
        payment.setAccount(stubAccount);

        payment.execute();
        payment.sendNotice();
        refundPaymentRepository.update(payment);
        return payment;
    }

    /** 환급금 산출 단건 조회 */
    @Transactional(readOnly = true)
    public RefundCalculation getCalculation(String refundNo) {
        RefundCalculation refund = refundCalculationRepository.findById(parseId(refundNo))
                .orElseThrow(() -> ApiException.notFound("환급금 산출 건을 찾을 수 없습니다: " + refundNo));
        requireRefundAccess(refund);
        return refund;
    }

    /** 환급금 산출 목록 */
    @Transactional(readOnly = true)
    public List<RefundCalculation> getAllCalculations() {
        return refundCalculationRepository.findAll().stream()
                .filter(this::canAccessRefund)
                .toList();
    }

    /** 환급금 지급 단건 조회 */
    @Transactional(readOnly = true)
    public RefundPayment getPayment(String paymentNo) {
        RefundPayment payment = refundPaymentRepository.findById(parseId(paymentNo))
                .orElseThrow(() -> ApiException.notFound("환급금 지급 건을 찾을 수 없습니다: " + paymentNo));
        requireRefundAccess(payment.getRefund());
        return payment;
    }

    /** 환급금 지급 목록 */
    @Transactional(readOnly = true)
    public List<RefundPayment> getAllPayments() {
        return refundPaymentRepository.findAll().stream()
                .filter(payment -> canAccessRefund(payment.getRefund()))
                .toList();
    }

    private void requireRefundAccess(RefundCalculation refund) {
        if (!canAccessRefund(refund)) {
            throw ApiException.forbidden("본인 환급 데이터만 접근할 수 있습니다.");
        }
    }

    private boolean canAccessRefund(RefundCalculation refund) {
        if (refund == null || refund.getCancellation() == null) {
            return !authAccessService.isCustomer();
        }
        Cancellation fullCancellation = cancellationRepository.findById(refund.getCancellation().getId())
                .orElse(null);
        Contract contract = fullCancellation != null ? fullCancellation.getContract() : null;
        return authAccessService.canAccessContract(contract);
    }
}
