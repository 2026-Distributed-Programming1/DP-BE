package org.dpbe.domain.payment.service;

import java.util.List;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.repository.CancellationRepository;
import org.dpbe.domain.common.enums.RefundPaymentStatus;
import org.dpbe.domain.common.enums.RefundStatus;
import org.dpbe.domain.payment.entity.RefundCalculation;
import org.dpbe.domain.payment.entity.RefundPayment;
import org.dpbe.domain.payment.repository.RefundCalculationRepository;
import org.dpbe.domain.payment.repository.RefundPaymentRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundService {

    private final CancellationRepository cancellationRepository;
    private final RefundCalculationRepository refundCalculationRepository;
    private final RefundPaymentRepository refundPaymentRepository;

    public RefundService(CancellationRepository cancellationRepository,
                         RefundCalculationRepository refundCalculationRepository,
                         RefundPaymentRepository refundPaymentRepository) {
        this.cancellationRepository = cancellationRepository;
        this.refundCalculationRepository = refundCalculationRepository;
        this.refundPaymentRepository = refundPaymentRepository;
    }

    /** 환급금 산출 — POST /api/cancellations/{cancellationNo}/refund-calculation */
    @Transactional
    public RefundCalculation calculate(String cancellationNo) {
        Cancellation cancellation = cancellationRepository.findByCancellationNo(cancellationNo)
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

    /** 환급금 확정 + 지급 이관 — POST /api/refund-calculations/{refundNo}/confirm */
    @Transactional
    public RefundPayment confirm(String refundNo) {
        RefundCalculation refund = refundCalculationRepository.findByRefundNo(refundNo)
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
        RefundPayment payment = refundPaymentRepository.findByPaymentNo(paymentNo)
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
        return refundCalculationRepository.findByRefundNo(refundNo)
                .orElseThrow(() -> ApiException.notFound("환급금 산출 건을 찾을 수 없습니다: " + refundNo));
    }

    /** 환급금 산출 목록 */
    @Transactional(readOnly = true)
    public List<RefundCalculation> getAllCalculations() {
        return refundCalculationRepository.findAll();
    }

    /** 환급금 지급 단건 조회 */
    @Transactional(readOnly = true)
    public RefundPayment getPayment(String paymentNo) {
        return refundPaymentRepository.findByPaymentNo(paymentNo)
                .orElseThrow(() -> ApiException.notFound("환급금 지급 건을 찾을 수 없습니다: " + paymentNo));
    }

    /** 환급금 지급 목록 */
    @Transactional(readOnly = true)
    public List<RefundPayment> getAllPayments() {
        return refundPaymentRepository.findAll();
    }
}