package org.dpbe.domain.payment.controller;

import org.dpbe.domain.payment.dto.PaymentRecordDetail;
import org.dpbe.domain.payment.dto.PaymentRecordRejectRequest;
import org.dpbe.domain.payment.service.PaymentRecordService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment-records")
public class PaymentRecordController {

    private final PaymentRecordService paymentRecordService;

    public PaymentRecordController(PaymentRecordService paymentRecordService) {
        this.paymentRecordService = paymentRecordService;
    }

    /** 납부 내역 목록 (?contractNo=, ?status= 선택 필터) */
    @GetMapping
    public PageResponse<PaymentRecordDetail> list(
            @RequestParam(required = false) String contractNo,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return paymentRecordService.getAll(contractNo, status, page, size);
    }

    /** 수납 확정 */
    @PostMapping("/{recordNo}/confirm")
    public PaymentRecordDetail confirm(@PathVariable String recordNo) {
        return paymentRecordService.confirm(recordNo);
    }

    /** 수납 반려 */
    @PostMapping("/{recordNo}/reject")
    public PaymentRecordDetail reject(@PathVariable String recordNo,
                                      @RequestBody PaymentRecordRejectRequest req) {
        return paymentRecordService.reject(recordNo, req);
    }
}
