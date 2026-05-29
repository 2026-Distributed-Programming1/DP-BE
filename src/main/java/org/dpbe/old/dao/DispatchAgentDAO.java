package org.dpbe.old.dao;

import org.dpbe.domain.actor.DispatchAgent;
import org.dpbe.old.db.DBA;
import java.util.List;

public class DispatchAgentDAO {

    public static void save(DispatchAgent a) {
        DBA.executeUpdate(
            "INSERT INTO dispatch_agents (employee_id, name, department, position, region, vehicle_no)"
            + " VALUES (?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE name=VALUES(name), region=VALUES(region),"
            + " department=VALUES(department), position=VALUES(position), vehicle_no=VALUES(vehicle_no)",
            a.getEmployeeId(), a.getName(), a.getDepartment(), a.getPosition(),
            a.getRegion(), a.getVehicleNo());
    }

    public static List<DispatchAgent> findAll() {
        return DBA.executeQuery(
            "SELECT employee_id, name, department, position, region, vehicle_no FROM dispatch_agents",
            rs -> new DispatchAgent(
                rs.getString("employee_id"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getString("position"),
                rs.getString("region"),
                rs.getString("vehicle_no")));
    }
}