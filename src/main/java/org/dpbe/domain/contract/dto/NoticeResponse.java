package org.dpbe.domain.contract.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NoticeResponse(
        String noticeNo,
        String contractNo,
        String contractorName,
        LocalDate expiryDate,
        String phone,
        String email,
        boolean isRenewable,
        long expectedPremium,
        LocalDateTime noticeDate,
        String noticeMemo,
        String customerResponse,
        Long renewalPremium,
        Long premiumDiff) {}