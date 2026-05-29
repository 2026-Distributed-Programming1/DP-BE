package org.dpbe.old.dao;

import java.util.List;
import org.dpbe.domain.actor.EducationTrainer;
import org.dpbe.old.db.DBA;

public class EducationTrainerDAO {

    public static void save(EducationTrainer t) {
        DBA.executeUpdate(
            "INSERT INTO education_trainers (employee_id, name, department, position)"
            + " VALUES (?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE name=VALUES(name),"
            + " department=VALUES(department), position=VALUES(position)",
            t.getEmployeeId(), t.getName(), t.getDepartment(), t.getPosition());
    }

    public static List<EducationTrainer> findAll() {
        return DBA.executeQuery(
            "SELECT employee_id, name, department, position FROM education_trainers",
            rs -> new EducationTrainer(
                rs.getString("employee_id"),
                rs.getString("name"),
                rs.getString("department"),
                rs.getString("position")));
    }
}