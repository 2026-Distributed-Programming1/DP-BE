package org.dpbe.old.dao;

import java.util.ArrayList;
import java.util.List;
import org.dpbe.old.db.DBA;
import org.dpbe.domain.education.entity.Attendance;
import org.dpbe.domain.education.entity.EducationExecution;
import org.dpbe.domain.education.entity.EducationPreparation;

public class EducationExecutionDAO {

    public static void save(EducationExecution e) {
        String execNo = String.valueOf(e.getCompletionNumber());
        DBA.executeUpdate(
            "INSERT INTO education_executions (execution_no, prep_no, trainer_name,"
            + " executed_at, attendee_count, memo, status)"
            + " VALUES (?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE trainer_name=VALUES(trainer_name),"
            + " attendee_count=VALUES(attendee_count),"
            + " memo=VALUES(memo), status=VALUES(status), executed_at=VALUES(executed_at)",
            execNo,
            e.getPreparation() != null ? String.valueOf(e.getPreparation().getSetupNumber()) : null,
            e.getPreparation() != null ? e.getPreparation().getInstructorName() : null,
            e.getExecutedAt(),
            e.getAttendanceCount(),
            e.getMemo(),
            e.getStatus());

        if (e.getPreparation() != null) {
            for (Attendance a : e.getPreparation().getAttendanceList()) {
                DBA.executeUpdate(
                    "INSERT INTO education_attendances (execution_no, attendee_name, is_attended)"
                    + " VALUES (?,?,?)"
                    + " ON DUPLICATE KEY UPDATE is_attended=VALUES(is_attended)",
                    execNo,
                    a.getAttendeeName(),
                    a.isAttended());
            }
        }
    }

    public static List<EducationExecution> findAll() {
        return DBA.executeQuery(
            "SELECT execution_no, prep_no, trainer_name, executed_at,"
            + " attendee_count, memo, status FROM education_executions",
            rs -> {
                int execNo = 0;
                try { execNo = Integer.parseInt(rs.getString("execution_no")); }
                catch (NumberFormatException ignored) {}

                int prepNo = 0;
                try { prepNo = Integer.parseInt(rs.getString("prep_no")); }
                catch (NumberFormatException ignored) {}

                java.sql.Timestamp ts = rs.getTimestamp("executed_at");
                java.time.LocalDateTime completedAt = ts != null ? ts.toLocalDateTime() : null;

                int attendeeCount = rs.getInt("attendee_count");
                EducationPreparation prepShell = new EducationPreparation(
                        prepNo, null, null, rs.getString("trainer_name"), null, null, new ArrayList<>());

                EducationExecution exec = new EducationExecution(
                        execNo, completedAt, attendeeCount, attendeeCount, rs.getString("memo"), prepShell);
                exec.setStatus(rs.getString("status"));
                return exec;
            });
    }
}