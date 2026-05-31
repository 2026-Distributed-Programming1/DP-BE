package org.dpbe.domain.claim.controller;

import java.util.List;
import org.dpbe.domain.claim.dto.AccidentCreateRequest;
import org.dpbe.domain.claim.dto.AccidentResponse;
import org.dpbe.domain.claim.service.AccidentReportService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC '사고를 접수한다' REST 엔드포인트 (무상태). 접수 시 needs_dispatch면 출동 동반 생성. */
@RestController
@RequestMapping("/api/accidents")
public class AccidentController {

    private final AccidentReportService accidentService;

    public AccidentController(AccidentReportService accidentService) {
        this.accidentService = accidentService;
    }

    /** 사고 접수 */
    @PostMapping
    public AccidentResponse create(@RequestBody AccidentCreateRequest request) {
        return accidentService.create(request);
    }

    /** 사고 접수 목록 */
    @GetMapping
    public List<AccidentResponse> list() {
        return accidentService.findAll();
    }

    /** 사고 접수 상세 */
    @GetMapping("/{accidentNo}")
    public AccidentResponse detail(@PathVariable String accidentNo) {
        return accidentService.findByReportNo(accidentNo);
    }
}