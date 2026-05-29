package org.dpbe.old.dao;

import org.dpbe.domain.consultation.entity.ConsultationRequest;
import org.dpbe.old.db.DBA;
import java.util.List;

public class ConsultationRequestDAO {

    public static void save(ConsultationRequest r) {
        DBA.executeUpdate(
            "INSERT INTO consultation_requests (consult_no, channel, location, contact, content, status, scheduled_at, requested_at, accepted_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE channel=VALUES(channel), status=VALUES(status),"
            + " location=VALUES(location), contact=VALUES(contact), content=VALUES(content),"
            + " scheduled_at=VALUES(scheduled_at),"
            + " requested_at=VALUES(requested_at), accepted_at=VALUES(accepted_at)",
            String.valueOf(r.getConsultationNumber()),
            r.getType(),
            r.getLocation(),
            r.getContact(),
            r.getContent(),
            r.getStatus(),
            r.getScheduledAt(),
            r.getReceivedAt(),
            r.getAcceptedAt());
    }

    public static List<ConsultationRequest> findAll() {
        return DBA.executeQuery(
            "SELECT consult_no, channel, location, contact, content, status, scheduled_at, requested_at, accepted_at"
            + " FROM consultation_requests",
            rs -> {
                String no = rs.getString("consult_no");
                int consultNo = 0;
                if (no != null) {
                    try { consultNo = Integer.parseInt(no); }
                    catch (NumberFormatException ignored) {}
                }
                java.sql.Timestamp scheduledTs = rs.getTimestamp("scheduled_at");
                java.time.LocalDateTime scheduledAt = scheduledTs != null ? scheduledTs.toLocalDateTime() : null;
                java.sql.Timestamp receivedTs = rs.getTimestamp("requested_at");
                java.time.LocalDateTime receivedAt = receivedTs != null ? receivedTs.toLocalDateTime() : null;
                java.sql.Timestamp acceptedTs = rs.getTimestamp("accepted_at");
                java.time.LocalDateTime acceptedAt = acceptedTs != null ? acceptedTs.toLocalDateTime() : null;
                ConsultationRequest cr = new ConsultationRequest(
                    consultNo,
                    rs.getString("channel"),
                    scheduledAt,
                    rs.getString("location"),
                    rs.getString("contact"),
                    rs.getString("content"),
                    rs.getString("status"));
                cr.setReceivedAt(receivedAt);
                cr.setAcceptedAt(acceptedAt);
                return cr;
            });
    }
}
