package org.dpbe.domain.payment.controller;

import org.dpbe.domain.payment.dto.RefundPaymentExecuteRequest;
import org.dpbe.domain.payment.dto.RefundPaymentResponse;
import org.dpbe.domain.payment.service.PayRefundService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** UC '환급금을 지급한다' REST 엔드포인트 */
@RestController
@RequestMapping("/api")
public class PayRefundController {

    private final PayRefundService payRefundService;

    public PayRefundController(PayRefundService payRefundService) {
        this.payRefundService = payRefundService;
    }

    /** OTP 인증 후 이체 실행 */
    @PostMapping("/refund-payments/{paymentNo}/execute")
    public ResponseEntity<RefundPaymentResponse> execute(
            @PathVariable String paymentNo,
            @RequestBody RefundPaymentExecuteRequest req) {
        return ResponseEntity.ok(RefundPaymentResponse.from(
                payRefundService.execute(paymentNo, req.otpInput())));
    }

    /** 환급금 지급 단건 조회 */
    @GetMapping("/refund-payments/{paymentNo}")
    public ResponseEntity<RefundPaymentResponse> getPayment(@PathVariable String paymentNo) {
        return ResponseEntity.ok(RefundPaymentResponse.from(
                payRefundService.getPayment(paymentNo)));
    }

    /** 환급금 지급 목록 */
    @GetMapping("/refund-payments")
    public ResponseEntity<PageResponse<RefundPaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(payRefundService.getAllPayments(page, size));
    }
}
