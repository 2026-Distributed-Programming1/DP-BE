package org.dpbe.domain.education.controller;

import java.util.List;
import org.dpbe.domain.education.dto.EducationExecutionRequest;
import org.dpbe.domain.education.dto.EducationExecutionResponse;
import org.dpbe.domain.education.service.EducationExecutionService;
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
@RequestMapping("/api/education-executions")
public class EducationExecutionController {

    private final EducationExecutionService service;

    public EducationExecutionController(EducationExecutionService service) {
        this.service = service;
    }

    @GetMapping
    public List<EducationExecutionResponse> list(@RequestParam(required = false) String prepNo) {
        return service.getExecutions(prepNo);
    }

    @GetMapping("/{executionNo}")
    public EducationExecutionResponse detail(@PathVariable String executionNo) {
        return service.getExecution(executionNo);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EducationExecutionResponse create(@RequestBody EducationExecutionRequest req) {
        return service.createExecution(req);
    }
}