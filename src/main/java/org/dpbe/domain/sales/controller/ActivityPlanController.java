package org.dpbe.domain.sales.controller;

import org.dpbe.domain.sales.dto.ActivityPlanRequest;
import org.dpbe.domain.sales.dto.ActivityPlanResponse;
import org.dpbe.domain.sales.service.ActivityPlanService;
import org.dpbe.global.dto.PageResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/activity-plans")
public class ActivityPlanController {

    private final ActivityPlanService service;

    public ActivityPlanController(ActivityPlanService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<ActivityPlanResponse> findAll(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findAll(page, size);
    }

    @GetMapping("/{planNo}")
    public ActivityPlanResponse findByPlanNo(@PathVariable String planNo) {
        return service.findByPlanNo(planNo);
    }

    @PostMapping
    public ResponseEntity<ActivityPlanResponse> create(@RequestBody ActivityPlanRequest request) {
        return ResponseEntity.status(201).body(service.create(request));
    }
}
