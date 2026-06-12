package org.dpbe.domain.payment.controller;

import org.dpbe.domain.payment.dto.CalculateRefundResponse;
import org.dpbe.domain.payment.dto.ConfirmRefundPaymentResponse;
import org.dpbe.domain.payment.service.CalculateRefundService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC '해약 환급금을 산출한다' REST 엔드포인트 */
@RestController
@RequestMapping("/api")
public class CalculateRefundController {

    private final CalculateRefundService calculateRefundService;

    public CalculateRefundController(CalculateRefundService calculateRefundService) {
        this.calculateRefundService = calculateRefundService;
    }

    /** 환급금 산출 */
    @PostMapping("/cancellations/{cancellationNo}/refund-calculation")
    public ResponseEntity<CalculateRefundResponse> calculate(@PathVariable String cancellationNo) {
        return ResponseEntity.ok(calculateRefundService.calculate(cancellationNo));
    }

    /** 환급금 확정 + 지급 이관 */
    @PostMapping("/refund-calculations/{refundNo}/confirm")
    public ResponseEntity<ConfirmRefundPaymentResponse> confirm(@PathVariable String refundNo) {
        return ResponseEntity.ok(calculateRefundService.confirm(refundNo));
    }
}
