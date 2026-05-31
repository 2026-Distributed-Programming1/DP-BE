package org.dpbe.domain.claim.controller;

import java.util.List;
import org.dpbe.domain.claim.dto.DispatchRecordResponse;
import org.dpbe.domain.claim.dto.DispatchResponse;
import org.dpbe.domain.claim.service.DispatchRecordService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * UC '현장 출동을 기록한다' REST 엔드포인트 (무상태).
 * 출동 목록 조회 + 현장 기록 등록(사진 multipart). 기록 조회.
 */
@RestController
@RequestMapping("/api/dispatches")
public class DispatchController {

    private final DispatchRecordService recordService;

    public DispatchController(DispatchRecordService recordService) {
        this.recordService = recordService;
    }

    /** 현장 출동 목록 */
    @GetMapping
    public List<DispatchResponse> list() {
        return recordService.listDispatches();
    }

    /** 출동 기록 등록 (사진 multipart/form-data) */
    @PostMapping(path = "/{dispatchNo}/record", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DispatchRecordResponse createRecord(
            @PathVariable String dispatchNo,
            @RequestParam(required = false) String agentName,
            @RequestParam(defaultValue = "false") boolean policeRequired,
            @RequestParam(defaultValue = "false") boolean towingRequired,
            @RequestParam(required = false) String notes,
            @RequestParam(value = "photos", required = false) MultipartFile[] photos) {
        return recordService.create(dispatchNo, agentName, policeRequired, towingRequired, notes, photos);
    }

    /** 출동 기록 조회 */
    @GetMapping("/{dispatchNo}/record")
    public DispatchRecordResponse record(@PathVariable String dispatchNo) {
        return recordService.findByDispatchNo(dispatchNo);
    }
}