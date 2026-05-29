package org.dpbe.claim;

import java.time.LocalDateTime;

/**
 * 추가 조사 지시 (AdditionalInvestigation)
 *
 * 손해 조사 진행 중 추가 현장 조사가 필요할 때 생성되는 클래스이다.
 * DamageInvestigation의 합성 부품으로, 단순한 메모가 아닌 별도의 조사 지시 이력으로 관리된다.
 */
public class AdditionalInvestigation {

    private String visitLocation;          // 방문지 - 현장/정비소/병원
    private LocalDateTime schedule;        // 일정
    private String reason;                 // 추가 조사 사유
    private LocalDateTime registeredAt;    // 등록일시

    /** 생성자 - registeredAt=now() */
    public AdditionalInvestigation(String location, LocalDateTime schedule, String reason) {
        this.visitLocation = location;
        this.schedule = schedule;
        this.reason = reason;
        this.registeredAt = LocalDateTime.now();
    }

    // Getter
    public String getVisitLocation() { return visitLocation; }
    public LocalDateTime getSchedule() { return schedule; }
    public String getReason() { return reason; }
    public LocalDateTime getRegisteredAt() { return registeredAt; }
}
