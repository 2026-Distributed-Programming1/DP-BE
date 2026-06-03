package org.dpbe.domain.education.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.dpbe.domain.common.enums.PlanStatus;
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

    public int countByStatus(String status) {
        if (status != null && !status.isBlank()) {
            return sql.queryOne(
                    "SELECT COUNT(*) AS cnt FROM education_plans WHERE status=?",
                    rs -> rs.getInt("cnt"), status);
        }
        return sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM education_plans",
                rs -> rs.getInt("cnt"));
    }

    public List<EducationPlan> findPageByStatus(String status, int limit, int offset) {
        if (status != null && !status.isBlank()) {
            return sql.executeQuery(
                    "SELECT " + COLS + " FROM education_plans WHERE status=? ORDER BY id DESC LIMIT ? OFFSET ?",
                    this::mapRow, status, limit, offset);
        }
        return sql.executeQuery(
                "SELECT " + COLS + " FROM education_plans ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRow, limit, offset);
    }

    public EducationPlan findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM education_plans WHERE id=?", this::mapRow, id);
    }

    /** INSERT → id 회수 → plan_no 파생 UPDATE */
    public void save(EducationPlan plan) {
        String statusName = plan.getStatus() != null ? plan.getStatus().name() : null;
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
                plan.getRejectReason(), plan.getApprovedAt(), statusName);
        plan.setId(id);
        plan.setPlanNo("PLN" + String.format("%05d", id));
    }

    /** 승인/반려 시 status·approved_at·reject_reason 갱신 */
    public void updateStatus(EducationPlan plan) {
        String statusName = plan.getStatus() != null ? plan.getStatus().name() : null;
        sql.executeUpdate(
                "UPDATE education_plans SET status=?, approved_at=?, reject_reason=? WHERE id=?",
                statusName, plan.getApprovedAt(), plan.getRejectReason(), plan.getId());
    }

    private EducationPlan mapRow(ResultSet rs) throws SQLException {
        LocalDate startDate = rs.getDate("start_date") != null ? rs.getDate("start_date").toLocalDate() : null;
        LocalDate endDate   = rs.getDate("end_date")   != null ? rs.getDate("end_date").toLocalDate()   : null;
        String st = rs.getString("status");
        PlanStatus status = PlanStatus.TEMP_SAVE;
        if (st != null) {
            try { status = PlanStatus.valueOf(st); } catch (IllegalArgumentException ignored) {}
        }
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
                status);
        plan.setId(rs.getLong("id"));
        plan.setPlanNo("PLN" + String.format("%05d", rs.getLong("id")));
        java.sql.Timestamp aat = rs.getTimestamp("approved_at");
        if (aat != null) plan.setApprovedAt(aat.toLocalDateTime());
        return plan;
    }
}
