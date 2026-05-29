package org.dpbe.domain.actor;

/**
 * 재무회계 담당자 (FinanceManager)
 *
 * 납부 내역과 환급금을 관리하는 행위자.
 */
public class FinanceManager extends Employee {

    /** 생성자 */
    public FinanceManager(String name, String dept, String position) {
        super(name, dept, position);
    }

    /** DB 로딩용 생성자 */
    public FinanceManager(String employeeId, String name, String dept, String position) {
        super(employeeId, name, dept, position);
    }
}
