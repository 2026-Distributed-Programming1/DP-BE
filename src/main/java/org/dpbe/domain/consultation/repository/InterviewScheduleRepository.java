package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.consultation.entity.InterviewSchedule;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class InterviewScheduleRepository {

    private static final String COLS =
            "id, customer_name, designer_name, interview_type,"
            + " scheduled_at, location, preparation, status,"
            + " registered_at, modified_at, cancelled_at";

    private final SqlExecutor sql;

    public InterviewScheduleRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<InterviewSchedule> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM interview_schedules ORDER BY id DESC", this::mapRow);
    }

    public InterviewSchedule findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM interview_schedules WHERE id=?",
                this::mapRow, id);
    }

    /** 면담일정 등록 — INSERT 후 id에서 schedule_no 파생. */
    public void save(InterviewSchedule s) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO interview_schedules"
                + " (customer_name, designer_name, interview_type, scheduled_at,"
                + " location, preparation, status, registered_at)"
                + " VALUES (?,?,?,?,?,?,?,?)",
                s.getCustomerName(), s.getDesignerName(), s.getType(),
                s.getScheduledAt(), s.getLocation(), s.getPreparation(),
                s.getStatus(), s.getRegisteredAt());
        s.setId(id);
        s.setScheduleNo("SCH" + String.format("%05d", id));
    }

    /** 면담일정 수정 (scheduledAt, location, preparation, type, modifiedAt). */
    public void update(InterviewSchedule s) {
        sql.executeUpdate(
                "UPDATE interview_schedules"
                + " SET interview_type=?, scheduled_at=?, location=?, preparation=?, modified_at=?"
                + " WHERE id=?",
                s.getType(), s.getScheduledAt(), s.getLocation(),
                s.getPreparation(), s.getModifiedAt(), s.getId());
    }

    /** 면담 취소 (status='취소', cancelledAt). */
    public void updateCancel(InterviewSchedule s) {
        sql.executeUpdate(
                "UPDATE interview_schedules SET status=?, cancelled_at=? WHERE id=?",
                s.getStatus(), s.getCancelledAt(), s.getId());
    }

    private InterviewSchedule mapRow(ResultSet rs) throws SQLException {
        java.sql.Timestamp schTs = rs.getTimestamp("scheduled_at");
        java.sql.Timestamp regTs = rs.getTimestamp("registered_at");
        java.sql.Timestamp modTs = rs.getTimestamp("modified_at");
        java.sql.Timestamp canTs = rs.getTimestamp("cancelled_at");
        InterviewSchedule s = InterviewSchedule.fromDb(
                0,
                rs.getString("customer_name"),
                rs.getString("interview_type"),
                schTs != null ? schTs.toLocalDateTime() : null,
                rs.getString("location"),
                rs.getString("preparation"),
                rs.getString("status"),
                regTs != null ? regTs.toLocalDateTime() : null,
                modTs != null ? modTs.toLocalDateTime() : null,
                canTs != null ? canTs.toLocalDateTime() : null);
        s.setId(rs.getLong("id"));
        s.setScheduleNo("SCH" + String.format("%05d", rs.getLong("id")));
        s.setDesignerName(rs.getString("designer_name"));
        return s;
    }
}