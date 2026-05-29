package org.dpbe.contract;

import java.time.LocalDate;

/**
 * 계약 검색 필터 (ContractFilter)
 * 계약 정보를 조회하는 유스케이스에서 필터 조건을 담는 클래스이다.
 */
public class ContractFilter {
    private String customerId;      // 고객 번호
    private String name;            // 이름
    private String ssn;             // 주민등록번호
    private String phone;           // 연락처
    private String contractNo;      // 계약번호
    private String insuranceType;   // 보험 종류
    private LocalDate contractDate; // 계약 일자

    public void apply() {}

    public void reset() {
        // 모든 필터 조건을 초기 상태(null)로 초기화한다
        this.customerId = null;
        this.name = null;
        this.ssn = null;
        this.phone = null;
        this.contractNo = null;
        this.insuranceType = null;
        this.contractDate = null;
        System.out.println("  [시스템] 필터 조건이 초기화되었습니다.");
    }
}