package org.dpbe.actor;

import org.dpbe.education.EducationPlan;

/**
 * 영업 관리자 (SalesManager)
 * 영업 활동 관리, 판매채널 모집/심사/평가, 성과급 요청 유스케이스의 주요 액터이다.
 *
 * 클래스 다이어그램: managerId, name, department
 * ※ SampleData 호환을 위해 email 파라미터를 수신하는 생성자를 제공하나, email은 필드로 저장하지 않는다.
 */
public class SalesManager {

    private String managerId;    // 관리자 ID
    private String name;         // 이름
    private String department;   // 부서

    /** 생성자 - managerId 자동 부여 */
    public SalesManager(String name, String department) {
        this.managerId = "MGR-" + name;
        this.name = name;
        this.department = department;
    }

    /** SampleData 호환용 — email은 클래스 다이어그램에 없으므로 저장하지 않는다. */
    public SalesManager(String name, String department, String email) {
        this(name, department);
    }

    /**
     * DB 로딩용 팩토리 메서드 - 자동부여 없이 managerId 직접 지정
     * ※ 생성자 대신 팩토리 메서드를 사용하는 이유:
     *   SampleData 호환용 생성자 (String name, String department, String email) 와
     *   파라미터 타입 시그니처 (String, String, String) 가 겹쳐 생성자 오버로딩이 불가능하기 때문.
     */
    public static SalesManager fromDb(String managerId, String name, String department) {
        SalesManager m = new SalesManager(name, department);
        m.managerId = managerId;
        return m;
    }

    public void approveEducationPlan(EducationPlan plan) {
        plan.approve();
        System.out.println("  [영업관리자] 교육계획안이 승인되었습니다.");
    }

    public void rejectEducationPlan(EducationPlan plan, String reason) {
        plan.reject(reason);
        System.out.println("  [영업관리자] 교육계획안이 반려되었습니다. 사유: " + reason);
    }

    // Getters
    public String getManagerId() { return managerId; }
    public String getName() { return name; }
    public String getDepartment() { return department; }
}