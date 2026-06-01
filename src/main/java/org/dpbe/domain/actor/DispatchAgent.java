package org.dpbe.domain.actor;

/**
 * 현장출동 직원 (DispatchAgent)
 *
 * 사고 현장 출동 및 기록을 담당하는 행위자.
 */
public class DispatchAgent extends Employee {

    private String region;       // 담당 지역
    private String vehicleNo;    // 출동 차량 번호

    /** DB 로딩용 생성자 */
    public DispatchAgent(String employeeId, String name, String dept, String position,
                         String region, String vehicleNo) {
        super(employeeId, name, dept, position);
        this.region = region;
        this.vehicleNo = vehicleNo;
    }

    // Getter
    public String getRegion() { return region; }
    public String getVehicleNo() { return vehicleNo; }
}
