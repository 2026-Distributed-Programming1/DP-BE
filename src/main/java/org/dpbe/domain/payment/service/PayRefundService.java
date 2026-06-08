package org.dpbe.domain.payment.service;

import java.util.List;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.contract.repository.CancellationRepository;
import org.dpbe.domain.payment.dto.RefundPaymentResponse;
import org.dpbe.domain.payment.entity.RefundCalculation;
import org.dpbe.domain.payment.entity.RefundPayment;
import org.dpbe.domain.payment.repository.RefundPaymentRepository;
import org.dpbe.global.auth.dto.AuthenticatedUser;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC '환급금을 지급한다' API 서비스. */
@Service
public class PayRefundService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final CancellationRepository cancellationRepository;
    private final RefundPaymentRepository refundPaymentRepository;
    private final AuthAccessService authAccessService;

    public PayRefundService(CancellationRepository cancellationRepository,
                            RefundPaymentRepository refundPaymentRepository,
                            AuthAccessService authAccessService) {
        this.cancellationRepository = cancellationRepository;
        this.refundPaymentRepository = refundPaymentRepository;
        this.authAccessService = authAccessService;
    }

    /** OTP 인증 후 이체 실행 — POST /api/refund-payments/{paymentNo}/execute */
    @Transactional
    public RefundPayment execute(String paymentNo, String otpInput) {
        authAccessService.requireRefundOperationAccess();
        RefundPayment payment = refundPaymentRepository.findById(parseId(paymentNo))
                .orElseThrow(() -> ApiException.notFound("환급금 지급 건을 찾을 수 없습니다: " + paymentNo));

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
    public PageResponse<RefundPaymentResponse> getAllPayments(int page, int size) {
        String customerNo = accessibleCustomerNo();
        if (!authAccessService.isCustomer()) {
            authAccessService.requireRefundOperationAccess();
        }

        if (authAccessService.isCustomer() && customerNo == null) {
            return emptyPage(page, size);
        }

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = refundPaymentRepository.countByCustomerNo(customerNo);
        List<RefundPaymentResponse> items = refundPaymentRepository
                .findPageByCustomerNo(customerNo, normalizedSize, offset)
                .stream()
                .map(RefundPaymentResponse::from)
                .toList();

        return new PageResponse<>(normalizedPage, normalizedSize, total, items);
    }

    private Long parseId(String businessNo) {
        try {
            return Long.parseLong(businessNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 번호: " + businessNo);
        }
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

    private <T> PageResponse<T> emptyPage(int page, int size) {
        return new PageResponse<>(normalizePage(page), normalizeSize(size), 0, List.of());
    }

    private String accessibleCustomerNo() {
        AuthenticatedUser user = authAccessService.currentUser();
        return authAccessService.isCustomer() ? user.linkedCustomerNo() : null;
    }

    private void requireRefundAccess(RefundCalculation refund) {
        if (!authAccessService.isCustomer()) {
            authAccessService.requireRefundOperationAccess();
            return;
        }

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
