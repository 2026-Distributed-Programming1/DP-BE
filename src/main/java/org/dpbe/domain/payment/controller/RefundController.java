package org.dpbe.domain.payment.controller;

import org.dpbe.domain.payment.dto.RefundCalculationResponse;
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

    /** 환급금 산출 단건 조회 */
    @GetMapping("/refund-calculations/{refundNo}")
    public ResponseEntity<RefundCalculationResponse> getCalculation(@PathVariable String refundNo) {
        return ResponseEntity.ok(refundService.getCalculation(refundNo));
    }

    /** 환급금 산출 목록 */
    @GetMapping("/refund-calculations")
    public ResponseEntity<PageResponse<RefundCalculationResponse>> getAllCalculations(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(refundService.getAllCalculations(page, size));
    }
}
