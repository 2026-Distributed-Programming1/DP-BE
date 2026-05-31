package org.dpbe.domain.contract.dto;

public record NoticeCreateRequest(
        String phone,
        String email,
        boolean isRenewable,
        long expectedPremium,
        String noticeMemo) {}