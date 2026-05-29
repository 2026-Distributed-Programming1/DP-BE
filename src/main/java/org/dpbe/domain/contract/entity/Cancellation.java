package org.dpbe.domain.contract.entity;

import java.time.LocalDateTime;

/**
 * 해지 (Cancellation)
 *
 * RefundCalculation이 함께 참조한다.
 */
public class Cancellation {

    private static int sequence = 0;

    private String cancellationNo;       // 해지번호 - 자동 부여
    private Contract contract;           // 대상 계약
    private String reason;               // 해지 사유 - 경제적 사정/타사가입/.../기타
    private String detailReason;         // 상세 사유 - 기타 시 필수
    private boolean noticeAgreed;        // 유의사항 동의 여부
    private boolean authResult;          // 본인인증 결과
    private long expectedRefund;         // 예상 환급금
    private LocalDateTime canceledAt;    // 해지일시 - confirm() 시 설정
    private String status;               // 상태 - 작성중/완료/실패

    /** 생성자 - 해지번호 자동 부여 */
    public Cancellation(Contract contract) {
        sequence += 1;
        this.cancellationNo = "CAN" + String.format("%05d", sequence);
        this.contract = contract;
        this.status   = "작성중";
    }

    /** DB 로딩용 생성자 */
    public Cancellation(String cancellationNo, Contract contract, String reason,
                        long expectedRefund, String status) {
        this.cancellationNo = cancellationNo;
        this.contract = contract;
        this.reason = reason;
        this.expectedRefund = expectedRefund;
        this.status = status;
    }

    /** 해지 사유 선택 - A1 */
    public void selectReason(String reason) {
        this.reason = reason;
    }

    /** 상세 사유 입력 - 기타 시 필수 */
    public void enterDetailReason(String detail) {
        this.detailReason = detail;
    }

    /** 기타 선택 시 1자 이상 검증 */
    public boolean validateReasonInput() {
        if ("기타".equals(reason)) {
            return detailReason != null && !detailReason.isEmpty();
        }
        return reason != null && !reason.isEmpty();
    }

    /** 유의사항 동의 */
    public void agreeToNotice() {
        this.noticeAgreed = true;
    }

    /** 본인인증 - E1 (시뮬레이션에서 Runner가 제어) */
    public boolean authenticate() {
        this.authResult = true;
        return authResult;
    }

    /**
     * 예상 환급금 산출
     * 단순화: 월 보험료 × 12
     */
    public long calculateExpectedRefund() {
        if (contract != null) {
            this.expectedRefund = contract.getMonthlyPremium() * 12;
        }
        return this.expectedRefund;
    }

    /** 해약 신청 확정 - canceledAt=now(), status="완료" */
    public void submit() {
        this.canceledAt = LocalDateTime.now();
        this.status = "완료";
    }

    /** 신청 처리 오류 - E2 */
    public void handleSubmitError() {
        this.status = "실패";
    }

    /** 해약 확정 (8 도메인 호환 - submit() 과 동일) */
    public void confirm() {
        this.canceledAt = LocalDateTime.now();
        this.status = "완료";
    }

    /** 중간 취소 - A2 */
    public void cancel() {
        this.status = "취소";
    }

    // Getter
    public String getCancellationNo() { return cancellationNo; }
    public Contract getContract()     { return contract; }
    public String getReason()         { return reason; }
    public String getDetailReason()   { return detailReason; }
    public long getExpectedRefund()   { return expectedRefund; }
    public LocalDateTime getCanceledAt() { return canceledAt; }
    public void setCanceledAt(LocalDateTime canceledAt) { this.canceledAt = canceledAt; }
    public String getStatus()         { return status; }
    public boolean isNoticeAgreed()   { return noticeAgreed; }
    public boolean isAuthResult()     { return authResult; }
}