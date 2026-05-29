package org.dpbe.old.dao;

import java.util.List;
import org.dpbe.domain.actor.SalesManager;
import org.dpbe.old.db.DBA;

public class SalesManagerDAO {

    public static void save(SalesManager m) {
        DBA.executeUpdate(
            "INSERT INTO sales_managers (manager_id, name, department)"
            + " VALUES (?,?,?)"
            + " ON DUPLICATE KEY UPDATE name=VALUES(name), department=VALUES(department)",
            m.getManagerId(), m.getName(), m.getDepartment());
    }

    public static List<SalesManager> findAll() {
        return DBA.executeQuery(
            "SELECT manager_id, name, department FROM sales_managers",
            rs -> SalesManager.fromDb(
                rs.getString("manager_id"),
                rs.getString("name"),
                rs.getString("department")));
    }
}