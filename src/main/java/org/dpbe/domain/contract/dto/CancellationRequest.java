package org.dpbe.domain.contract.dto;

public record CancellationRequest(String reason, String detailReason, boolean noticeAgreed) {}