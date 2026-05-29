package org.dpbe.old.dao;

import java.util.List;
import org.dpbe.domain.consultation.entity.InterviewRecord;
import org.dpbe.old.db.DBA;

public class InterviewRecordDAO {

    public static void save(InterviewRecord r) {
        DBA.executeUpdate(
            "INSERT INTO interview_records (record_no, customer_name, content,"
            + " customer_reaction, follow_up_action, interviewed_at, recorded_at)"
            + " VALUES (?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE customer_name=VALUES(customer_name),"
            + " content=VALUES(content),"
            + " customer_reaction=VALUES(customer_reaction),"
            + " follow_up_action=VALUES(follow_up_action),"
            + " interviewed_at=VALUES(interviewed_at),"
            + " recorded_at=VALUES(recorded_at)",
            String.valueOf(r.getRecordNumber()),
            r.getCustomerName(),
            r.getContent(),
            r.getCustomerReaction(),
            r.getFollowUpAction(),
            r.getInterviewedAt(),
            r.getRecordedAt());
    }

    public static List<InterviewRecord> findAll() {
        return DBA.executeQuery(
            "SELECT record_no, customer_name, content, customer_reaction,"
            + " follow_up_action, interviewed_at, recorded_at FROM interview_records",
            rs -> {
                String recordNo = rs.getString("record_no");
                int recordNumber = 0;
                if (recordNo != null) {
                    try { recordNumber = Integer.parseInt(recordNo); }
                    catch (NumberFormatException ignored) {}
                }
                java.sql.Timestamp its = rs.getTimestamp("interviewed_at");
                java.time.LocalDateTime interviewedAt = its != null ? its.toLocalDateTime() : null;
                java.sql.Timestamp rts = rs.getTimestamp("recorded_at");
                java.time.LocalDateTime recordedAt = rts != null ? rts.toLocalDateTime() : null;
                InterviewRecord rec = InterviewRecord.fromDb(
                        recordNumber,
                        rs.getString("customer_name"),
                        rs.getString("content"),
                        interviewedAt,
                        rs.getString("customer_reaction"),
                        rs.getString("follow_up_action"));
                rec.setRecordedAt(recordedAt);
                return rec;
            });
    }
}
