package org.dpbe.domain.actor;

/**
 * 계약관리 담당자 (ContractManager)
 * 계약 정보를 조회한다, 계약 통계 정보를 관리한다, 만기 계약을 관리한다 유스케이스의 주요 액터이다.
 */
public class ContractManager {

    private String managerId;   // 담당자ID
    private String name;        // 이름
    private String department;  // 부서

    public ContractManager(String managerId, String name, String department) {
        this.managerId = managerId;
        this.name = name;
        this.department = department;
    }
}