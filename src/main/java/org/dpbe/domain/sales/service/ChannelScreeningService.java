package org.dpbe.domain.sales.service;

import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.ScreeningStatus;
import org.dpbe.domain.sales.dto.ChannelScreeningRequest;
import org.dpbe.domain.sales.dto.ChannelScreeningResponse;
import org.dpbe.domain.sales.dto.ScreeningRejectRequest;
import org.dpbe.domain.sales.entity.ChannelScreening;
import org.dpbe.domain.sales.repository.ChannelScreeningRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelScreeningService {

    private final ChannelScreeningRepository repository;

    public ChannelScreeningService(ChannelScreeningRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ChannelScreeningResponse> findAll() {
        return repository.findAll().stream()
                .map(ChannelScreeningResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChannelScreeningResponse register(ChannelScreeningRequest request) {
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
        ChannelScreening s = findOrThrow(screeningNo);
        if (s.getScreeningStatus() != ScreeningStatus.PENDING) {
            throw ApiException.badRequest("대기 상태의 심사만 승인할 수 있습니다.");
        }
        s.approve();
        repository.updateReview(s);
        return ChannelScreeningResponse.from(s);
    }

    @Transactional
    public ChannelScreeningResponse reject(String screeningNo, ScreeningRejectRequest request) {
        ChannelScreening s = findOrThrow(screeningNo);
        if (s.getScreeningStatus() != ScreeningStatus.PENDING) {
            throw ApiException.badRequest("대기 상태의 심사만 거절할 수 있습니다.");
        }
        s.setRejectionReason(request.rejectionReason());
        s.reject();
        repository.updateReview(s);
        return ChannelScreeningResponse.from(s);
    }

    private ChannelScreening findOrThrow(String screeningNo) {
        ChannelScreening s = repository.findByScreeningNo(screeningNo);
        if (s == null) {
            throw ApiException.notFound("심사 내역을 찾을 수 없습니다: " + screeningNo);
        }
        return s;
    }
}