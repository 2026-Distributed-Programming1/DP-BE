package org.dpbe.domain.actor;

import java.time.LocalDate;

/**
 * 사원 (Employee) - 추상 클래스
 *
 * 보험사의 모든 임직원의 공통 부모 클래스.
 */
public abstract class Employee extends User {

    protected String employeeId;       // 사원 ID
    protected String department;       // 부서
    protected String position;         // 직책
    protected LocalDate hireDate;      // 입사일

    protected Employee(String employeeId, String name, String dept, String position) {
        super(employeeId, name, null, null);
        this.employeeId = employeeId;
        this.department = dept;
        this.position = position;
    }

    public String getEmployeeId() { return employeeId; }
    public String getDepartment() { return department; }
    public String getPosition() { return position; }
    public LocalDate getHireDate() { return hireDate; }
}