package org.dpbe.domain.payment.controller;

import java.util.List;
import org.dpbe.domain.payment.dto.PaymentContractResponse;
import org.dpbe.domain.payment.dto.PaymentPreviewRequest;
import org.dpbe.domain.payment.dto.PaymentPreviewResponse;
import org.dpbe.domain.payment.dto.PaymentResultResponse;
import org.dpbe.domain.payment.dto.PaymentSubmitRequest;
import org.dpbe.domain.payment.service.PaymentService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC '보험료를 납입한다' REST 엔드포인트 (클라이언트 주도 다단계 흐름) */
@RestController
@RequestMapping("/api")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    /** 납입 가능한 고객 계약 목록 */
    @GetMapping("/customers/{customerId}/contracts")
    public List<PaymentContractResponse> customerContracts(@PathVariable String customerId) {
        return paymentService.customerContracts(customerId);
    }

    /** 미리보기 — 총액/선납할인 계산 (저장 없음) */
    @PostMapping("/payments/preview")
    public PaymentPreviewResponse preview(@RequestBody PaymentPreviewRequest request) {
        return paymentService.preview(request);
    }

    /** 제출 — 검증 후 트랜잭션 저장 */
    @PostMapping("/payments")
    public PaymentResultResponse submit(@RequestBody PaymentSubmitRequest request) {
        return paymentService.submit(request);
    }
}