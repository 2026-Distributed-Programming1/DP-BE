package org.dpbe.dao;

import java.util.List;
import org.dpbe.actor.InsuranceReviewer;
import org.dpbe.db.DBA;

public class InsuranceReviewerDAO {

    public static void save(InsuranceReviewer r) {
        DBA.executeUpdate(
            "INSERT INTO insurance_reviewers (employee_id, name, department, position)"
            + " VALUES (?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE name=VALUES(name),"
            + " department=VALUES(department), position=VALUES(position)",
            r.getEmployeeId(), r.getName(), r.getDepartment(), r.getPosition());
    }

    public static List<InsuranceReviewer> findAll() {
        return DBA.executeQuery(
            "SELECT employee_id, name, department, position FROM insurance_reviewers",
            rs -> new InsuranceReviewer(
                rs.getString("employee_id"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getString("position")));
    }
}