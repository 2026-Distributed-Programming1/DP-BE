package org.dpbe.domain.claim.service;

import org.dpbe.domain.claim.dto.PaymentCreateRequest;
import org.dpbe.domain.claim.dto.PaymentExecuteRequest;
import org.dpbe.domain.claim.dto.PaymentResponse;
import org.dpbe.domain.claim.entity.ClaimCalculation;
import org.dpbe.domain.claim.entity.ClaimPayment;
import org.dpbe.domain.claim.repository.ClaimCalculationRepository;
import org.dpbe.domain.claim.repository.ClaimPaymentRepository;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.domain.common.enums.CalculationStatus;
import org.dpbe.domain.common.enums.ClaimPaymentStatus;
import org.dpbe.domain.common.enums.PaymentType;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC '보험금을 지급한다' API 서비스.
 * 보험금 지급 규칙을 처리하되, OTP·예약 대응을 위해 생성/실행을 분리한다:
 *  - create: 지급건 생성(WAITING, 예약이면 SCHEDULED). (실 시스템에선 여기서 OTP 발송)
 *  - execute: OTP 검증 후 이체(COMPLETED) 또는 실패(FAILED, E2).
 * 수령인·계좌는 산출→조사→청구 조인으로 청구 단계 값을 승계한다.
 */
@Service
@Transactional(readOnly = true)
public class ClaimPaymentService {

    private final ClaimPaymentRepository paymentRepository;
    private final ClaimCalculationRepository calculationRepository;

    public ClaimPaymentService(ClaimPaymentRepository paymentRepository,
                               ClaimCalculationRepository calculationRepository) {
        this.paymentRepository = paymentRepository;
        this.calculationRepository = calculationRepository;
    }

    public PaymentResponse findByCalculationNo(String calculationNo) {
        ClaimPayment p = paymentRepository.findByCalculationNo(calculationNo);
        if (p == null) {
            throw ApiException.notFound("해당 산출의 지급 건이 없습니다: " + calculationNo);
        }
        return PaymentResponse.from(p);
    }

    private Long parseId(String no) {
        try {
            return Long.parseLong(no.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 번호: " + no);
        }
    }

    /** 지급 생성 — 승인된 산출 건에 대해 지급건을 만든다(WAITING/SCHEDULED). */
    @Transactional
    public PaymentResponse create(String calculationNo, PaymentCreateRequest request) {
        ClaimCalculation calc = calculationRepository.findById(parseId(calculationNo));
        if (calc == null) {
            throw ApiException.notFound("산출을 찾을 수 없습니다: " + calculationNo);
        }
        if (calc.getStatus() != CalculationStatus.APPROVED) {
            throw ApiException.badRequest("승인된 산출만 지급할 수 있습니다: " + calculationNo);
        }
        if (paymentRepository.findByCalculationNo(calculationNo) != null) {
            throw ApiException.badRequest("이미 지급건이 생성된 산출입니다: " + calculationNo);
        }

        // 산출액·청구 계좌를 조인으로 한 번에 로드
        ClaimPaymentRepository.PayoutSource src = paymentRepository.loadPayoutSource(parseId(calculationNo));
        if (src == null) {
            throw ApiException.notFound("지급 정보를 구성할 수 없습니다: " + calculationNo);
        }

        // 산출 셸 + 금액으로 지급건 구성 (paymentNo는 save에서 파생)
        ClaimCalculation calcShell = new ClaimCalculation(
                calculationNo, null, 0, 0, src.finalAmount(), false, false, calc.getStatus());
        calcShell.setId(calc.getId());
        ClaimPayment payment = new ClaimPayment(
                "TBD", calcShell, src.finalAmount(), ClaimPaymentStatus.WAITING);

        // 청구 계좌 승계 + 수령인
        BankAccount account = new BankAccount();
        account.enter(src.bankName(), src.accountNo(), src.accountHolder());
        account.verify();
        payment.setAccount(account);
        payment.setRecipientFromName(src.recipientName());

        PaymentType type = parsePaymentType(request.paymentType());
        payment.selectPaymentType(type);
        if (type == PaymentType.SCHEDULED) {
            if (request.scheduledAt() == null) {
                throw ApiException.badRequest("예약 지급에는 예약 일시가 필요합니다.");
            }
            payment.setScheduledDateTime(request.scheduledAt());
            payment.schedule();   // 상태 SCHEDULED
        }

        paymentRepository.save(payment);
        return PaymentResponse.from(payment);
    }

    /** 이체 실행 — OTP 검증 후 이체(COMPLETED) 또는 실패(FAILED). */
    @Transactional
    public PaymentResponse execute(String paymentNo, PaymentExecuteRequest request) {
        ClaimPayment payment = paymentRepository.findById(parseId(paymentNo));
        if (payment == null) {
            throw ApiException.notFound("지급 건을 찾을 수 없습니다: " + paymentNo);
        }
        if (payment.getStatus() == ClaimPaymentStatus.COMPLETED) {
            throw ApiException.badRequest("이미 지급 완료된 건입니다: " + paymentNo);
        }

        // OTP 검증 (현재 더미: 6자리 — 실제 OTP 도입 시 이 부분만 교체)
        payment.enterOTP(request.otp());
        if (!payment.verifyOTP()) {
            throw ApiException.badRequest("OTP 인증에 실패했습니다.");
        }

        // 단일 테이블 복원이라 계좌가 번호만 있음 → 청구 계좌를 조인으로 다시 승계
        String calcNo = payment.getCalculation() != null
                ? payment.getCalculation().getCalculationNo() : null;
        ClaimPaymentRepository.PayoutSource src =
                calcNo != null ? paymentRepository.loadPayoutSource(parseId(calcNo)) : null;
        if (src != null) {
            BankAccount account = new BankAccount();
            account.enter(src.bankName(), src.accountNo(), src.accountHolder());
            account.verify();
            payment.setAccount(account);
        }

        payment.execute();   // 성공 시 COMPLETED, 실패 시 내부에서 FAILED 처리(E2)
        // 이체 실패(FAILED)도 정상 업무 결과이므로 예외를 던지지 않고 저장한다
        // (던지면 @Transactional 롤백으로 FAILED 기록이 사라짐) — 클라이언트는 status로 결과 판별.
        paymentRepository.update(payment);
        return PaymentResponse.from(payment);
    }

    private PaymentType parsePaymentType(String type) {
        if (type == null) {
            throw ApiException.badRequest("지급 유형을 선택해야 합니다.");
        }
        try {
            return PaymentType.valueOf(type);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("알 수 없는 지급 유형입니다: " + type);
        }
    }
}
