package org.dpbe.domain.claim.controller;

import org.dpbe.domain.claim.dto.PaymentCreateRequest;
import org.dpbe.domain.claim.dto.PaymentExecuteRequest;
import org.dpbe.domain.claim.dto.PaymentResponse;
import org.dpbe.domain.claim.service.ClaimPaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * UC '보험금을 지급한다' REST 엔드포인트 (무상태).
 * 지급건 생성(산출 기준)과 이체 실행(OTP)을 분리한다.
 */
@RestController
@RequestMapping("/api")
public class ClaimPaymentController {

    private final ClaimPaymentService paymentService;

    public ClaimPaymentController(ClaimPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 지급 생성 (WAITING/SCHEDULED) */
    @PostMapping("/calculations/{calculationNo}/payment")
    public PaymentResponse create(@PathVariable String calculationNo,
                                  @RequestBody PaymentCreateRequest request) {
        return paymentService.create(calculationNo, request);
    }

    /** 지급 조회 */
    @GetMapping("/calculations/{calculationNo}/payment")
    public PaymentResponse detail(@PathVariable String calculationNo) {
        return paymentService.findByCalculationNo(calculationNo);
    }

    /** 이체 실행 (OTP 검증 후 COMPLETED/FAILED) */
    @PostMapping("/payments/{paymentNo}/execute")
    public PaymentResponse execute(@PathVariable String paymentNo,
                                   @RequestBody PaymentExecuteRequest request) {
        return paymentService.execute(paymentNo, request);
    }
}