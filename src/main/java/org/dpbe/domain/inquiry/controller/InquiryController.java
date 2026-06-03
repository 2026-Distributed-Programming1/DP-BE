package org.dpbe.domain.inquiry.controller;

import org.dpbe.domain.inquiry.dto.InquiryAnswerRequest;
import org.dpbe.domain.inquiry.dto.InquiryRequest;
import org.dpbe.domain.inquiry.dto.InquiryResponse;
import org.dpbe.domain.inquiry.service.InquiryService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inquiries")
public class InquiryController {

    private final InquiryService service;

    public InquiryController(InquiryService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<InquiryResponse> list(
            @RequestParam(required = false) String customerName,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getInquiries(customerName, status, page, size);
    }

    @GetMapping("/{inquiryNo}")
    public InquiryResponse detail(@PathVariable String inquiryNo) {
        return service.getInquiry(inquiryNo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InquiryResponse submit(@RequestBody InquiryRequest req) {
        return service.submit(req);
    }

    @PostMapping("/{inquiryNo}/answer")
    public InquiryResponse answer(@PathVariable String inquiryNo,
                                  @RequestBody InquiryAnswerRequest req) {
        return service.answer(inquiryNo, req);
    }
}
