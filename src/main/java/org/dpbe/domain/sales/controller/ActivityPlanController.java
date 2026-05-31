package org.dpbe.domain.sales.controller;

import java.util.List;
import org.dpbe.domain.sales.dto.ActivityPlanRequest;
import org.dpbe.domain.sales.dto.ActivityPlanResponse;
import org.dpbe.domain.sales.service.ActivityPlanService;
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
    public List<ActivityPlanResponse> findAll() {
        return service.findAll();
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