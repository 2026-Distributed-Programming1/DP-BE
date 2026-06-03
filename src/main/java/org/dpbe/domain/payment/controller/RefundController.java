package org.dpbe.domain.payment.controller;

import org.dpbe.domain.payment.dto.RefundCalculationResponse;
import org.dpbe.domain.payment.dto.RefundPaymentExecuteRequest;
import org.dpbe.domain.payment.dto.RefundPaymentResponse;
import org.dpbe.domain.payment.service.RefundService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class RefundController {

    private final RefundService refundService;

    public RefundController(RefundService refundService) {
        this.refundService = refundService;
    }

    /** 환급금 산출 */
    @PostMapping("/cancellations/{cancellationNo}/refund-calculation")
    public ResponseEntity<RefundCalculationResponse> calculate(@PathVariable String cancellationNo) {
        return ResponseEntity.ok(RefundCalculationResponse.from(
                refundService.calculate(cancellationNo)));
    }

    /** 환급금 산출 단건 조회 */
    @GetMapping("/refund-calculations/{refundNo}")
    public ResponseEntity<RefundCalculationResponse> getCalculation(@PathVariable String refundNo) {
        return ResponseEntity.ok(RefundCalculationResponse.from(
                refundService.getCalculation(refundNo)));
    }

    /** 환급금 산출 목록 */
    @GetMapping("/refund-calculations")
    public ResponseEntity<PageResponse<RefundCalculationResponse>> getAllCalculations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(refundService.getAllCalculations(page, size));
    }

    /** 환급금 확정 + 지급 이관 */
    @PostMapping("/refund-calculations/{refundNo}/confirm")
    public ResponseEntity<RefundPaymentResponse> confirm(@PathVariable String refundNo) {
        return ResponseEntity.ok(RefundPaymentResponse.from(
                refundService.confirm(refundNo)));
    }

    /** OTP 인증 후 이체 실행 */
    @PostMapping("/refund-payments/{paymentNo}/execute")
    public ResponseEntity<RefundPaymentResponse> execute(
            @PathVariable String paymentNo,
            @RequestBody RefundPaymentExecuteRequest req) {
        return ResponseEntity.ok(RefundPaymentResponse.from(
                refundService.execute(paymentNo, req.otpInput())));
    }

    /** 환급금 지급 단건 조회 */
    @GetMapping("/refund-payments/{paymentNo}")
    public ResponseEntity<RefundPaymentResponse> getPayment(@PathVariable String paymentNo) {
        return ResponseEntity.ok(RefundPaymentResponse.from(
                refundService.getPayment(paymentNo)));
    }

    /** 환급금 지급 목록 */
    @GetMapping("/refund-payments")
    public ResponseEntity<PageResponse<RefundPaymentResponse>> getAllPayments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(refundService.getAllPayments(page, size));
    }
}
