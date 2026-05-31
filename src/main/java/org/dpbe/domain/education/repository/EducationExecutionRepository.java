package org.dpbe.domain.education.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.education.dto.EducationExecutionResponse.AttendanceDetail;
import org.dpbe.domain.education.entity.Attendance;
import org.dpbe.domain.education.entity.EducationExecution;
import org.dpbe.domain.education.entity.EducationPreparation;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class EducationExecutionRepository {

    private static final String EXEC_COLS =
            "id, execution_no, prep_no, trainer_name, executed_at, attendee_count, total_count, memo, status";

    private final SqlExecutor sql;

    public EducationExecutionRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<EducationExecution> findAll() {
        return sql.executeQuery(
                "SELECT " + EXEC_COLS + " FROM education_executions ORDER BY id DESC", this::mapRow);
    }

    public List<EducationExecution> findByPrepNo(String prepNo) {
        return sql.executeQuery(
                "SELECT " + EXEC_COLS + " FROM education_executions WHERE prep_no=? ORDER BY id DESC",
                this::mapRow, prepNo);
    }

    public EducationExecution findByExecutionNo(String executionNo) {
        return sql.queryOne(
                "SELECT " + EXEC_COLS + " FROM education_executions WHERE execution_no=?",
                this::mapRow, executionNo);
    }

    public List<AttendanceDetail> findAttendances(String executionNo) {
        return sql.executeQuery(
                "SELECT attendee_name, is_attended FROM education_attendances WHERE execution_no=?",
                rs -> new AttendanceDetail(rs.getString("attendee_name"), rs.getBoolean("is_attended")),
                executionNo);
    }

    /** INSERT execution → id → execution_no 파생 UPDATE → INSERT attendances */
    public void save(EducationExecution exec) {
        String prepNo = exec.getPreparation() != null ? exec.getPreparation().getPrepNo() : null;
        String trainerName = exec.getPreparation() != null ? exec.getPreparation().getInstructorName() : null;
        long id = sql.executeInsertReturningKey(
                "INSERT INTO education_executions (prep_no, trainer_name, executed_at, attendee_count, total_count, memo, status)"
                + " VALUES (?,?,?,?,?,?,?)",
                prepNo, trainerName,
                exec.getExecutedAt(), exec.getAttendanceCount(), exec.getTotalCount(),
                exec.getMemo(), exec.getStatus());
        exec.setId(id);
        exec.setExecutionNo("EXC" + String.format("%05d", id));
        sql.executeUpdate("UPDATE education_executions SET execution_no=? WHERE id=?",
                exec.getExecutionNo(), id);

        for (Attendance a : exec.getPreparation().getAttendanceList()) {
            sql.executeUpdate(
                    "INSERT INTO education_attendances (execution_no, attendee_name, is_attended) VALUES (?,?,?)",
                    exec.getExecutionNo(), a.getAttendeeName(), a.isAttended());
        }
    }

    private EducationExecution mapRow(ResultSet rs) throws SQLException {
        EducationPreparation prepShell = new EducationPreparation(
                0, null, null, rs.getString("trainer_name"), null, null, new ArrayList<>());
        prepShell.setPrepNo(rs.getString("prep_no"));

        java.sql.Timestamp ts = rs.getTimestamp("executed_at");
        EducationExecution exec = new EducationExecution(
                0,
                ts != null ? ts.toLocalDateTime() : null,
                rs.getInt("attendee_count"),
                rs.getInt("total_count"),
                rs.getString("memo"),
                prepShell);
        exec.setId(rs.getLong("id"));
        exec.setExecutionNo(rs.getString("execution_no"));
        exec.setStatus(rs.getString("status"));
        return exec;
    }
}