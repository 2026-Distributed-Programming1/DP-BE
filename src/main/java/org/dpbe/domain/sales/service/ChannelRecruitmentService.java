package org.dpbe.domain.sales.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.sales.dto.ChannelRecruitmentRequest;
import org.dpbe.domain.sales.dto.ChannelRecruitmentResponse;
import org.dpbe.domain.sales.entity.ChannelRecruitment;
import org.dpbe.domain.sales.repository.ChannelRecruitmentRepository;
import org.dpbe.global.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChannelRecruitmentService {

    private final ChannelRecruitmentRepository repository;

    public ChannelRecruitmentService(ChannelRecruitmentRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ChannelRecruitmentResponse> findAll() {
        return repository.findAll().stream()
                .map(ChannelRecruitmentResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public ChannelRecruitmentResponse create(ChannelRecruitmentRequest request) {
        if (request.channelType() == null || request.recruitCount() == null
                || request.startDate() == null || request.endDate() == null) {
            throw ApiException.badRequest("필수 항목 누락: channelType, recruitCount, startDate, endDate");
        }
        if (request.recruitCount() <= 0) {
            throw ApiException.badRequest("모집 인원은 1명 이상이어야 합니다.");
        }
        if (request.endDate().isBefore(request.startDate())) {
            throw ApiException.badRequest("종료일은 시작일 이후여야 합니다.");
        }

        ChannelRecruitment r = new ChannelRecruitment();
        r.setManagerName(request.managerName());
        try {
            r.setChannelType(ChannelType.valueOf(request.channelType()));
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("유효하지 않은 channelType: " + request.channelType());
        }
        r.setRecruitCount(request.recruitCount());
        r.setStartDate(request.startDate());
        r.setEndDate(request.endDate());
        r.setCondition(request.condition());
        r.setRegisteredAt(LocalDateTime.now());

        repository.save(r);
        return ChannelRecruitmentResponse.from(r);
    }
}