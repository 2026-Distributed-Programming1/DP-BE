package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.consultation.entity.ConsultationRequest;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class ConsultationRequestRepository {

    private static final String COLS =
            "id, consultation_type, location, contact, content,"
            + " status, scheduled_at, received_at, accepted_at";

    private final SqlExecutor sql;

    public ConsultationRequestRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<ConsultationRequest> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM consultation_requests ORDER BY id DESC", this::mapRow);
    }

    public ConsultationRequest findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM consultation_requests WHERE id=?",
                this::mapRow, id);
    }

    /** 상담 신청 저장 — INSERT 후 id에서 consult_no 파생. */
    public void save(ConsultationRequest r) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO consultation_requests"
                + " (consultation_type, location, contact, content, status, scheduled_at, received_at)"
                + " VALUES (?,?,?,?,?,?,?)",
                r.getType(), r.getLocation(), r.getContact(), r.getContent(),
                r.getStatus(), r.getScheduledAt(), r.getReceivedAt());
        r.setId(id);
        r.setConsultNo("CSL" + String.format("%05d", id));
    }

    /** 상담 수락 갱신 (status, accepted_at). */
    public void updateAccept(ConsultationRequest r) {
        sql.executeUpdate(
                "UPDATE consultation_requests SET status=?, accepted_at=? WHERE id=?",
                r.getStatus(), r.getAcceptedAt(), r.getId());
    }

    private ConsultationRequest mapRow(ResultSet rs) throws SQLException {
        java.sql.Timestamp schTs = rs.getTimestamp("scheduled_at");
        java.sql.Timestamp recTs = rs.getTimestamp("received_at");
        java.sql.Timestamp accTs = rs.getTimestamp("accepted_at");
        ConsultationRequest cr = new ConsultationRequest(
                0,
                rs.getString("consultation_type"),
                schTs != null ? schTs.toLocalDateTime() : null,
                rs.getString("location"),
                rs.getString("contact"),
                rs.getString("content"),
                rs.getString("status"));
        cr.setId(rs.getLong("id"));
        cr.setConsultNo("CSL" + String.format("%05d", rs.getLong("id")));
        cr.setReceivedAt(recTs != null ? recTs.toLocalDateTime() : null);
        cr.setAcceptedAt(accTs != null ? accTs.toLocalDateTime() : null);
        return cr;
    }
}