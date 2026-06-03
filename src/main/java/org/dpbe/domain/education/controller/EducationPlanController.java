package org.dpbe.domain.education.controller;

import org.dpbe.domain.education.dto.EducationPlanRejectRequest;
import org.dpbe.domain.education.dto.EducationPlanRequest;
import org.dpbe.domain.education.dto.EducationPlanResponse;
import org.dpbe.domain.education.service.EducationPlanService;
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
@RequestMapping("/api/education-plans")
public class EducationPlanController {

    private final EducationPlanService service;

    public EducationPlanController(EducationPlanService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<EducationPlanResponse> list(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getPlans(status, page, size);
    }

    @GetMapping("/{planNo}")
    public EducationPlanResponse detail(@PathVariable String planNo) {
        return service.getPlan(planNo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EducationPlanResponse create(@RequestBody EducationPlanRequest req) {
        return service.createPlan(req);
    }

    @PostMapping("/{planNo}/approve")
    public EducationPlanResponse approve(@PathVariable String planNo) {
        return service.approvePlan(planNo);
    }

    @PostMapping("/{planNo}/reject")
    public EducationPlanResponse reject(@PathVariable String planNo,
                                        @RequestBody EducationPlanRejectRequest req) {
        return service.rejectPlan(planNo, req);
    }
}
