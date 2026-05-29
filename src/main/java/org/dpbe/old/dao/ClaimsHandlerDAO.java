package org.dpbe.old.dao;

import org.dpbe.domain.actor.ClaimsHandler;
import org.dpbe.old.db.DBA;
import java.util.List;

public class ClaimsHandlerDAO {

    public static void save(ClaimsHandler h) {
        DBA.executeUpdate(
            "INSERT INTO claims_handlers (employee_id, name, department, position, transfer_limit)"
            + " VALUES (?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE name=VALUES(name), transfer_limit=VALUES(transfer_limit),"
            + " department=VALUES(department), position=VALUES(position)",
            h.getEmployeeId(), h.getName(), h.getDepartment(), h.getPosition(), h.getTransferLimit());
    }

    public static List<ClaimsHandler> findAll() {
        return DBA.executeQuery(
            "SELECT employee_id, name, department, position, transfer_limit FROM claims_handlers",
            rs -> new ClaimsHandler(
                rs.getString("employee_id"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getString("position"),
                rs.getLong("transfer_limit")));
    }
}