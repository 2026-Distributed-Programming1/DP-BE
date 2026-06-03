package org.dpbe.domain.sales.service;

import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.ScreeningStatus;
import org.dpbe.domain.sales.dto.ChannelScreeningRequest;
import org.dpbe.domain.sales.dto.ChannelScreeningResponse;
import org.dpbe.domain.sales.dto.ScreeningRejectRequest;
import org.dpbe.domain.sales.entity.ChannelScreening;
import org.dpbe.domain.sales.repository.ChannelScreeningRepository;
import org.dpbe.global.auth.service.AuthAccessService;
import org.dpbe.global.dto.PageResponse;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelScreeningService {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final ChannelScreeningRepository repository;
    private final AuthAccessService authAccessService;

    public ChannelScreeningService(ChannelScreeningRepository repository,
                                   AuthAccessService authAccessService) {
        this.repository = repository;
        this.authAccessService = authAccessService;
    }

    @Transactional(readOnly = true)
    public PageResponse<ChannelScreeningResponse> findAll(int page, int size) {
        authAccessService.requireSalesOperationAccess();
        int normalizedPage = normalizePage(page);
        int normalizedSize = normalizeSize(size);
        int offset = (normalizedPage - 1) * normalizedSize;
        int total = repository.countAll();
        var items = repository.findPage(normalizedSize, offset).stream()
                .map(ChannelScreeningResponse::from)
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

    @Transactional
    public ChannelScreeningResponse register(ChannelScreeningRequest request) {
        authAccessService.requireSalesOperationAccess();
        if (request.applicantName() == null || request.channelType() == null) {
            throw ApiException.badRequest("필수 항목 누락: applicantName, channelType");
        }

        ChannelScreening s = new ChannelScreening();
        s.setApplicantName(request.applicantName());
        try {
            s.setChannelType(ChannelType.valueOf(request.channelType()));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("유효하지 않은 channelType: " + request.channelType());
        }
        s.setApplicationDate(request.applicationDate() != null
                ? request.applicationDate() : java.time.LocalDate.now());
        s.setCareer(request.career());
        if (request.certifications() != null) {
            s.setCertifications(request.certifications());
        }
        s.setScreeningStatus(ScreeningStatus.PENDING);

        repository.save(s);
        return ChannelScreeningResponse.from(s);
    }

    @Transactional
    public ChannelScreeningResponse approve(String screeningNo) {
        authAccessService.requireSalesOperationAccess();
        ChannelScreening s = findOrThrow(screeningNo);
        s.approve();
        repository.updateReview(s);
        return ChannelScreeningResponse.from(s);
    }

    @Transactional
    public ChannelScreeningResponse reject(String screeningNo, ScreeningRejectRequest request) {
        authAccessService.requireSalesOperationAccess();
        ChannelScreening s = findOrThrow(screeningNo);
        s.setRejectionReason(request.rejectionReason());
        s.reject();
        repository.updateReview(s);
        return ChannelScreeningResponse.from(s);
    }

    private Long parseId(String screeningNo) {
        try {
            return Long.parseLong(screeningNo.replaceAll("\\D", ""));
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 심사번호: " + screeningNo);
        }
    }

    private ChannelScreening findOrThrow(String screeningNo) {
        ChannelScreening s = repository.findById(parseId(screeningNo));
        if (s == null) {
            throw ApiException.notFound("심사 내역을 찾을 수 없습니다: " + screeningNo);
        }
        return s;
    }
}
