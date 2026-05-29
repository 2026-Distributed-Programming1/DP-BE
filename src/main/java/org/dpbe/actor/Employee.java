package org.dpbe.actor;

import java.time.LocalDate;

/**
 * 사원 (Employee) - 추상 클래스
 *
 * 보험사의 모든 임직원의 공통 부모 클래스.
 */
public abstract class Employee extends User {

    private static int sequence = 0;    // 사원 ID 자동 부여용

    protected String employeeId;       // 사원 ID
    protected String department;       // 부서
    protected String position;         // 직책
    protected LocalDate hireDate;      // 입사일

    /** 생성자 */
    public Employee(String name, String dept, String position) {
        super(name, null, null);
        sequence += 1;
        this.employeeId = "EMP" + String.format("%05d", sequence);
        this.department = dept;
        this.position = position;
        this.hireDate = LocalDate.now();
    }

    /** DB 로딩용 생성자 - 자동증가 없이 employeeId 직접 지정 */
    protected Employee(String employeeId, String name, String dept, String position) {
        super(employeeId, name, null, null);
        this.employeeId = employeeId;
        this.department = dept;
        this.position = position;
    }

    // Getter
    public String getEmployeeId() { return employeeId; }
    public String getDepartment() { return department; }
    public String getPosition() { return position; }
    public LocalDate getHireDate() { return hireDate; }
}
