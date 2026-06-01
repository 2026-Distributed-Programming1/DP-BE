package org.dpbe.domain.education.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.dpbe.domain.education.entity.EducationPlan;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class EducationPlanRepository {

    private static final String COLS =
            "id, trainer_name, education_name, channel_type, start_date, end_date,"
            + " target_count, budget, education_goal, education_content, textbook_list,"
            + " reject_reason, approved_at, status";

    private final SqlExecutor sql;

    public EducationPlanRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<EducationPlan> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM education_plans ORDER BY id DESC", this::mapRow);
    }

    public List<EducationPlan> findByStatus(String status) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM education_plans WHERE status=? ORDER BY id DESC",
                this::mapRow, status);
    }

    public EducationPlan findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM education_plans WHERE id=?", this::mapRow, id);
    }

    /** INSERT → id 회수 → plan_no 파생 UPDATE */
    public void save(EducationPlan plan) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO education_plans"
                + " (trainer_name, education_name, channel_type, start_date, end_date,"
                + "  target_count, budget, education_goal, education_content, textbook_list,"
                + "  reject_reason, approved_at, status)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                plan.getTrainerName(), plan.getEducationName(), plan.getChannelType(),
                plan.getStartDate(), plan.getEndDate(),
                plan.getTargetCount(), plan.getBudget(),
                plan.getEducationGoal(), plan.getEducationContent(), plan.getTextbookList(),
                plan.getRejectReason(), plan.getApprovedAt(), plan.getStatus());
        plan.setId(id);
        plan.setPlanNo("PLN" + String.format("%05d", id));
    }

    /** 승인/반려 시 status·approved_at·reject_reason 갱신 */
    public void updateStatus(EducationPlan plan) {
        sql.executeUpdate(
                "UPDATE education_plans SET status=?, approved_at=?, reject_reason=? WHERE id=?",
                plan.getStatus(), plan.getApprovedAt(), plan.getRejectReason(), plan.getId());
    }

    private EducationPlan mapRow(ResultSet rs) throws SQLException {
        LocalDate startDate = rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null;
        LocalDate endDate   = rs.getDate("end_date")   != null ? rs.getDate("end_date").toLocalDate()   : null;
        EducationPlan plan = EducationPlan.fromDb(
                0,
                rs.getString("trainer_name"),
                rs.getString("education_name"),
                rs.getString("channel_type"),
                startDate, endDate,
                rs.getInt("target_count"),
                rs.getLong("budget"),
                rs.getString("education_goal"),
                rs.getString("education_content"),
                rs.getString("textbook_list"),
                rs.getString("reject_reason"),
                rs.getString("status"));
        plan.setId(rs.getLong("id"));
        plan.setPlanNo("PLN" + String.format("%05d", rs.getLong("id")));
        java.sql.Timestamp aat = rs.getTimestamp("approved_at");
        if (aat != null) plan.setApprovedAt(aat.toLocalDateTime());
        return plan;
    }
}