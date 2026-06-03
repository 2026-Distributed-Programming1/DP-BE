package org.dpbe.domain.claim.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.claim.dto.DispatchRecordResponse;
import org.dpbe.domain.claim.dto.DispatchResponse;
import org.dpbe.domain.claim.entity.Dispatch;
import org.dpbe.domain.claim.entity.DispatchRecord;
import org.dpbe.domain.claim.repository.DispatchRecordRepository;
import org.dpbe.domain.claim.repository.DispatchRepository;
import org.dpbe.domain.common.entity.Attachment;
import org.dpbe.domain.common.enums.DispatchRecordStatus;
import org.dpbe.global.auth.dto.AuthenticatedUser;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * UC '현장 출동을 기록한다' API 서비스.
 * 출동요원이 현장 사진(multipart)·특이사항을 제출하면, 사진은 로컬 파일시스템
 * (uploads/dispatch/{recordNo}/)에 저장하고 메타는 dispatch_photos에 기록한다.
 * 사진 1장 이상 필수(E1) 후 transmit()으로 전송완료 처리한다.
 */
@Service
@Transactional(readOnly = true)
public class DispatchRecordService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final DispatchRecordRepository recordRepository;
    private final DispatchRepository dispatchRepository;
    private final AuthAccessService authAccessService;
    private final String uploadRoot;

    public DispatchRecordService(DispatchRecordRepository recordRepository,
                                 DispatchRepository dispatchRepository,
                                 AuthAccessService authAccessService,
                                 @Value("${app.upload.dispatch-dir:uploads/dispatch}") String uploadRoot) {
        this.recordRepository = recordRepository;
        this.dispatchRepository = dispatchRepository;
        this.authAccessService = authAccessService;
        this.uploadRoot = uploadRoot;
    }

    /** 현장 출동 목록 (기록 대상 선택용). */
    public PageResponse<DispatchResponse> listDispatches(int page, int size) {
        authAccessService.requireDispatchRecordAccess();

        AuthenticatedUser user = authAccessService.currentUser();
        String customerNo = authAccessService.isCustomer() ? user.linkedCustomerNo() : null;
        if (authAccessService.isCustomer() && customerNo == null) {
            return emptyPage(page, size);
        }

        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = dispatchRepository.countByCustomerNo(customerNo);
        List<DispatchResponse> items = dispatchRepository
                .findPageByCustomerNo(customerNo, normalizedSize, offset)
                .stream()
                .map(DispatchResponse::from)
                .toList();

        return new PageResponse<>(normalizedPage, normalizedSize, total, items);
    }

    private int normalizePage(int page) {
        return page < 1 ? DEFAULT_PAGE : page;
    }

    private int normalizeSize(int size) {
        if (size < 1) {
            return DEFAULT_SIZE;
        }
        return Math.min(size, MAX_SIZE);
    }

    private <T> PageResponse<T> emptyPage(int page, int size) {
        return new PageResponse<>(normalizePage(page), normalizeSize(size), 0, List.of());
    }

    public DispatchRecordResponse findByDispatchNo(String dispatchNo) {
        authAccessService.requireDispatchRecordAccess();
        DispatchRecord rec = recordRepository.findByDispatchNo(dispatchNo);
        if (rec == null) {
            throw ApiException.notFound("해당 출동의 기록이 없습니다: " + dispatchNo);
        }
        return DispatchRecordResponse.from(rec, recordRepository.findPhotoNames(rec.getRecordId()));
    }

    private Long parseId(String no) {
        try {
            return Long.parseLong(no.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.notFound("유효하지 않은 번호: " + no);
        }
    }

    /** 출동 기록 등록 — 사진 저장 + 기록·사진 메타 영속화(@Transactional). */
    @Transactional
    public DispatchRecordResponse create(String dispatchNo, String agentName,
                                         boolean policeRequired, boolean towingRequired,
                                         String notes, MultipartFile[] photos) {
        authAccessService.requireDispatchRecordAccess();
        Dispatch dispatch = dispatchRepository.findById(parseId(dispatchNo));
        if (dispatch == null) {
            throw ApiException.notFound("출동 건을 찾을 수 없습니다: " + dispatchNo);
        }
        if (recordRepository.findByDispatchNo(dispatchNo) != null) {
            throw ApiException.badRequest("이미 기록이 등록된 출동입니다: " + dispatchNo);
        }
        if (photos == null || photos.length == 0) {
            throw ApiException.badRequest("[E1] 현장 사진을 1장 이상 첨부해야 합니다.");
        }

        DispatchRecord rec = new DispatchRecord(dispatch);
        rec.setPoliceRequired(policeRequired);
        rec.setTowingRequired(towingRequired);
        if (notes != null) rec.enterNotes(notes);

        // 기록 먼저 저장 → record_no(DRC) 확정 (사진 디렉터리·FK 키로 사용)
        recordRepository.save(rec, agentName);
        String recordNo = rec.getRecordId();

        // 사진을 uploads/dispatch/{recordNo}/ 에 저장하고 엔터티/메타에 반영
        List<Attachment> saved = storePhotos(recordNo, photos);
        for (Attachment a : saved) {
            rec.uploadPhoto("현장", a);
            recordRepository.savePhoto(recordNo, a);
        }

        // 전송 처리 (E1: 사진 1장 이상이면 통과) → transmittedAt·status 갱신
        rec.transmit();
        if (rec.getStatus() != DispatchRecordStatus.TRANSMITTED) {
            throw ApiException.badRequest("출동 기록 전송에 실패했습니다.");
        }
        recordRepository.markTransmitted(rec);

        List<String> names = saved.stream().map(Attachment::getFileName).toList();
        return DispatchRecordResponse.from(rec, names);
    }

    private List<Attachment> storePhotos(String recordNo, MultipartFile[] photos) {
        Path dir = Paths.get(uploadRoot, recordNo);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new RuntimeException("업로드 디렉터리 생성 실패: " + dir, e);
        }
        List<Attachment> result = new ArrayList<>();
        for (MultipartFile mf : photos) {
            if (mf.isEmpty()) continue;
            String original = mf.getOriginalFilename() != null ? mf.getOriginalFilename() : "photo";
            Path target = dir.resolve(original);
            try {
                mf.transferTo(target.toAbsolutePath());
            } catch (IOException e) {
                throw new RuntimeException("사진 저장 실패: " + target, e);
            }
            result.add(new Attachment(new File(target.toString())));
        }
        if (result.isEmpty()) {
            throw ApiException.badRequest("[E1] 유효한 현장 사진이 없습니다.");
        }
        return result;
    }
}
