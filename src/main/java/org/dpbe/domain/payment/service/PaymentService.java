package org.dpbe.domain.payment.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.actor.Customer;
import org.dpbe.global.exception.ApiException;
import org.dpbe.domain.contract.repository.ContractRepository;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.dpbe.domain.payment.dto.PaymentContractResponse;
import org.dpbe.domain.payment.dto.PaymentItemRequest;
import org.dpbe.domain.payment.dto.PaymentLineResponse;
import org.dpbe.domain.payment.dto.PaymentPreviewRequest;
import org.dpbe.domain.payment.dto.PaymentPreviewResponse;
import org.dpbe.domain.payment.dto.PaymentRecordResponse;
import org.dpbe.domain.payment.dto.PaymentResultResponse;
import org.dpbe.domain.payment.dto.PaymentSubmitRequest;
import org.dpbe.domain.payment.repository.PaymentRecordRepository;
import org.dpbe.domain.payment.repository.PaymentRepository;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.common.enums.PaymentMethod;
import org.dpbe.domain.payment.entity.Payment;
import org.dpbe.domain.payment.entity.PaymentItem;
import org.dpbe.domain.payment.entity.PaymentRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * UC '보험료를 납입한다' API 서비스.
 * 콘솔 PaymentRunner의 절차 로직을 옮긴다(콘솔 입출력 제외).
 * 다단계 흐름은 클라이언트 주도: 조회(GET) → 미리보기(POST) → 제출(POST).
 * 제출의 다중 테이블 저장은 {@code @Transactional}이 원자성을 보장한다
 * (DBA 수동 트랜잭션 제거 — Spring DataSourceTransactionManager가 경계 관리).
 */
@Service
@Transactional(readOnly = true)
public class PaymentService {

    private final ContractRepository contractRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentRecordRepository paymentRecordRepository;

    public PaymentService(ContractRepository contractRepository,
                          CustomerRepository customerRepository,
                          PaymentRepository paymentRepository,
                          PaymentRecordRepository paymentRecordRepository) {
        this.contractRepository = contractRepository;
        this.customerRepository = customerRepository;
        this.paymentRepository = paymentRepository;
        this.paymentRecordRepository = paymentRecordRepository;
    }

    /** 납입 가능한 고객 계약 목록 */
    public List<PaymentContractResponse> customerContracts(String customerId) {
        return contractRepository.findByCustomerId(customerId).stream()
                .map(c -> new PaymentContractResponse(
                        c.getContractNo(), c.getInsuranceType(), c.getMonthlyPremium()))
                .collect(Collectors.toList());
    }

    /** 미리보기 — 총액/선납할인 계산만 (저장 없음, 시퀀스 미소모) */
    public PaymentPreviewResponse preview(PaymentPreviewRequest request) {
        List<PaymentItemRequest> items = request.items();
        if (items == null || items.isEmpty()) {
            throw ApiException.badRequest("납입할 계약을 1건 이상 선택해야 합니다.");
        }

        List<PaymentLineResponse> lines = new ArrayList<>();
        long total = 0;
        int maxCount = 0;
        for (PaymentItemRequest item : items) {
            Contract c = requireContract(item.contractNo());
            if (item.count() <= 0) {
                throw ApiException.badRequest("납입 횟수는 1 이상이어야 합니다: " + item.contractNo());
            }
            long subtotal = c.getMonthlyPremium() * item.count();
            lines.add(new PaymentLineResponse(
                    c.getContractNo(), c.getMonthlyPremium(), item.count(), subtotal));
            total += subtotal;
            maxCount = Math.max(maxCount, item.count());
        }

        long earlyDiscount = maxCount >= 2 ? (long) (total * 0.01) : 0;
        long discounted = total - earlyDiscount;
        return new PaymentPreviewResponse(total, earlyDiscount, discounted, lines);
    }

    /** 제출 — 검증 후 payments + payment_items + payment_records 원자적 저장 (@Transactional) */
    @Transactional
    public PaymentResultResponse submit(PaymentSubmitRequest request) {
        Customer customer = customerRepository.findById(request.customerId());
        if (customer == null) {
            throw ApiException.notFound("고객을 찾을 수 없습니다: " + request.customerId());
        }
        if (request.items() == null || request.items().isEmpty()) {
            throw ApiException.badRequest("납입할 계약을 1건 이상 선택해야 합니다.");
        }

        PaymentMethod method = parseMethod(request.paymentMethod());

        // 요청 항목 순서대로 계약 해석 + Payment 구성
        List<Contract> contracts = new ArrayList<>();
        for (PaymentItemRequest item : request.items()) {
            contracts.add(requireContract(item.contractNo()));
        }
        Payment payment = new Payment(customer);
        payment.selectContracts(contracts);

        // 계약별 납입 횟수 입력 (contractNo로 매칭)
        for (PaymentItem pi : payment.getItems()) {
            int count = request.items().stream()
                    .filter(r -> r.contractNo().equals(pi.getContract().getContractNo()))
                    .map(PaymentItemRequest::count)
                    .findFirst().orElse(0);
            payment.enterPaymentCount(pi, count);
        }
        if (!payment.validatePaymentCount()) {
            throw ApiException.badRequest("[E1] 납입 횟수 검증에 실패했습니다.");
        }

        payment.selectPaymentMethod(method);

        // 계좌 입력 + 인증
        payment.registerNewAccount(request.bankName(), request.accountNo(), request.accountHolder());
        if (!payment.verifyAccount()) {
            throw ApiException.badRequest("계좌 인증에 실패했습니다.");
        }

        payment.calculateTotal();
        payment.submit();

        // 저장 — 같은 트랜잭션 안에서 실행되어 원자성 보장 (예외 시 자동 롤백)
        List<PaymentRecord> savedRecords = new ArrayList<>();
        paymentRepository.save(payment);
        for (PaymentItem pi : payment.getItems()) {
            PaymentRecord record = new PaymentRecord(
                    pi.getContract(), pi.getSubtotal(), method.name());
            paymentRecordRepository.save(record);
            savedRecords.add(record);
        }

        List<PaymentRecordResponse> recordDtos = savedRecords.stream()
                .map(r -> new PaymentRecordResponse(
                        r.getRecordNo(), r.getContract().getContractNo(), r.getAmount()))
                .collect(Collectors.toList());

        return new PaymentResultResponse(
                payment.getPaymentNo(),
                payment.getRequestedAt(),
                payment.getTotalAmount(),
                payment.getEarlyDiscount(),
                payment.getDiscountedAmount(),
                payment.getPaymentMethod() != null ? payment.getPaymentMethod().name() : null,
                recordDtos);
    }

    private Contract requireContract(String contractNo) {
        Contract c = contractRepository.findByContractNo(contractNo);
        if (c == null) {
            throw ApiException.notFound("계약을 찾을 수 없습니다: " + contractNo);
        }
        return c;
    }

    private PaymentMethod parseMethod(String method) {
        if (method == null) {
            throw ApiException.badRequest("납입 방법을 선택해야 합니다.");
        }
        try {
            return PaymentMethod.valueOf(method);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("알 수 없는 납입 방법입니다: " + method);
        }
    }
}