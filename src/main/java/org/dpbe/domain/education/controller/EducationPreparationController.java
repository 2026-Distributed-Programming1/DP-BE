package org.dpbe.domain.education.controller;

import org.dpbe.domain.education.dto.EducationPreparationRequest;
import org.dpbe.domain.education.dto.EducationPreparationResponse;
import org.dpbe.domain.education.service.EducationPreparationService;
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
@RequestMapping("/api/education-preparations")
public class EducationPreparationController {

    private final EducationPreparationService service;

    public EducationPreparationController(EducationPreparationService service) {
        this.service = service;
    }

    @GetMapping
    public PageResponse<EducationPreparationResponse> list(
            @RequestParam(required = false) String planNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.getPreparations(planNo, page, size);
    }

    @GetMapping("/{prepNo}")
    public EducationPreparationResponse detail(@PathVariable String prepNo) {
        return service.getPreparation(prepNo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EducationPreparationResponse create(@RequestBody EducationPreparationRequest req) {
        return service.createPreparation(req);
    }
}
