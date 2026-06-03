package org.dpbe.domain.claim.entity;

import java.time.LocalDateTime;
import org.dpbe.domain.actor.DispatchAgent;
import org.dpbe.domain.common.enums.DispatchStatus;
import org.dpbe.global.exception.ApiException;

/**
 * 현장 출동 (Dispatch)
 *
 * 사고 접수에 따른 현장출동 신청과 처리 과정을 관리하는 클래스이다.
 * 어떤 사고에서 비롯된 출동인지(accident), 누가 출동하는지(agent), 도착 예정 시간과
 * 실제 도착 시간, 그리고 현재 상태(신청/배정/출발/도착/취소/완료)를 추적한다.
 */
public class Dispatch {

    private Long id;                          // DB 대리키(PK)
    private String dispatchNo;                // 출동번호
    private AccidentReport accident;          // 사고 접수
    private DispatchAgent agent;              // 출동 직원
    private LocalDateTime estimatedArrival;   // 도착 예정 시간
    private LocalDateTime arrivalTime;        // 실제 도착 시간
    private DispatchStatus status;            // 상태
    private String cancelReason;              // 취소 사유

    /** DB 로딩용 생성자 */
    public Dispatch(String dispatchNo, AccidentReport accident, DispatchStatus status) {
        this.dispatchNo = dispatchNo;
        this.accident = accident;
        this.status = status;
    }

    /** 신규 출동 생성자 */
    public Dispatch(AccidentReport accident) {
        this.accident = accident;
        this.status = DispatchStatus.REQUESTED;
    }

    /** 직원 배정 - status="배정" */
    public void assignAgent(DispatchAgent agent) {
        if (this.status != DispatchStatus.REQUESTED) {
            throw ApiException.badRequest("신청(REQUESTED) 상태에서만 배정할 수 있습니다.");
        }
        this.agent = agent;
        this.status = DispatchStatus.ASSIGNED;
    }

    /** 도착 예정 시간 설정 */
    public void setEstimatedArrival(LocalDateTime time) {
        this.estimatedArrival = time;
    }

    /** 현장 출발 - status="출발" */
    public void depart() {
        if (this.status != DispatchStatus.ASSIGNED) {
            throw ApiException.badRequest("배정(ASSIGNED) 상태에서만 출발할 수 있습니다.");
        }
        this.status = DispatchStatus.DEPARTED;
    }

    /** 현장 도착 - arrivalTime=now(), status="도착" */
    public void arrive() {
        if (this.status != DispatchStatus.DEPARTED) {
            throw ApiException.badRequest("출발(DEPARTED) 상태에서만 도착 처리할 수 있습니다.");
        }
        this.arrivalTime = LocalDateTime.now();
        this.status = DispatchStatus.ARRIVED;
    }

    /** 위치 정보 갱신 (A3) */
    public void updateLocation(String newLocation) {
        if (this.accident != null) {
            this.accident.enterLocation(newLocation);
        }
    }

    /** 출동 취소 (A4) */
    public void cancel(String reason) {
        if (this.status == DispatchStatus.COMPLETED || this.status == DispatchStatus.CANCELED) {
            throw ApiException.badRequest("완료 또는 이미 취소된 출동은 취소할 수 없습니다.");
        }
        this.status = DispatchStatus.CANCELED;
        this.cancelReason = reason;
    }

    /** 출동 완료 - status="완료" */
    public void complete() {
        if (this.status != DispatchStatus.ARRIVED) {
            throw ApiException.badRequest("도착(ARRIVED) 상태에서만 완료 처리할 수 있습니다.");
        }
        this.status = DispatchStatus.COMPLETED;
    }

    // Getter
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getDispatchNo() { return dispatchNo; }
    public void setDispatchNo(String dispatchNo) { this.dispatchNo = dispatchNo; }
    public AccidentReport getAccident() { return accident; }
    public DispatchAgent getAgent() { return agent; }
    public LocalDateTime getEstimatedArrival() { return estimatedArrival; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public DispatchStatus getStatus() { return status; }
    public String getCancelReason() { return cancelReason; }
}
