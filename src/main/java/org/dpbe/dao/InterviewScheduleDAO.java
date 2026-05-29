package org.dpbe.dao;

import java.util.List;
import org.dpbe.consultation.InterviewSchedule;
import org.dpbe.db.DBA;

public class InterviewScheduleDAO {

    public static void save(InterviewSchedule s) {
        DBA.executeUpdate(
            "INSERT INTO interview_schedules (schedule_no, customer_name, designer_name, type, scheduled_at,"
            + " location, preparation, status, registered_at, modified_at, cancelled_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status), type=VALUES(type),"
            + " designer_name=VALUES(designer_name),"
            + " scheduled_at=VALUES(scheduled_at), location=VALUES(location),"
            + " preparation=VALUES(preparation), modified_at=VALUES(modified_at),"
            + " cancelled_at=VALUES(cancelled_at)",
            String.valueOf(s.getInterviewNumber()),
            s.getCustomerName(), s.getDesignerName(), s.getType(), s.getScheduledAt(), s.getLocation(),
            s.getPreparation(), s.getStatus(),
            s.getRegisteredAt(), s.getModifiedAt(), s.getCancelledAt());
    }

    public static List<InterviewSchedule> findAll() {
        return DBA.executeQuery(
            "SELECT schedule_no, customer_name, designer_name, type, scheduled_at, location, preparation,"
            + " status, registered_at, modified_at, cancelled_at"
            + " FROM interview_schedules",
            rs -> {
                String scheduleNo = rs.getString("schedule_no");
                int interviewNumber = 0;
                if (scheduleNo != null) {
                    try { interviewNumber = Integer.parseInt(scheduleNo); }
                    catch (NumberFormatException ignored) {}
                }
                java.sql.Timestamp ts = rs.getTimestamp("scheduled_at");
                java.time.LocalDateTime scheduledAt = ts != null ? ts.toLocalDateTime() : null;
                java.sql.Timestamp regTs = rs.getTimestamp("registered_at");
                java.time.LocalDateTime registeredAt = regTs != null ? regTs.toLocalDateTime() : null;
                java.sql.Timestamp modTs = rs.getTimestamp("modified_at");
                java.time.LocalDateTime modifiedAt = modTs != null ? modTs.toLocalDateTime() : null;
                java.sql.Timestamp canTs = rs.getTimestamp("cancelled_at");
                java.time.LocalDateTime cancelledAt = canTs != null ? canTs.toLocalDateTime() : null;
                InterviewSchedule s = InterviewSchedule.fromDb(
                        interviewNumber,
                        rs.getString("customer_name"),
                        rs.getString("type"),
                        scheduledAt,
                        rs.getString("location"),
                        rs.getString("preparation"),
                        rs.getString("status"),
                        registeredAt, modifiedAt, cancelledAt);
                s.setDesignerName(rs.getString("designer_name"));
                return s;
            });
    }
}
