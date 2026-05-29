package org.dpbe.old.dao;

import java.util.List;
import org.dpbe.domain.actor.FinanceManager;
import org.dpbe.old.db.DBA;

public class FinanceManagerDAO {

    public static void save(FinanceManager m) {
        DBA.executeUpdate(
            "INSERT INTO finance_managers (employee_id, name, department, position)"
            + " VALUES (?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE name=VALUES(name),"
            + " department=VALUES(department), position=VALUES(position)",
            m.getEmployeeId(), m.getName(), m.getDepartment(), m.getPosition());
    }

    public static List<FinanceManager> findAll() {
        return DBA.executeQuery(
            "SELECT employee_id, name, department, position FROM finance_managers",
            rs -> new FinanceManager(
                rs.getString("employee_id"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getString("position")));
    }
}