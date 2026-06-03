package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.consultation.entity.InterviewRecord;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class InterviewRecordRepository {

    private static final String COLS =
            "id, customer_name, content, customer_reaction,"
            + " follow_up_action, interviewed_at, recorded_at, modified_at";

    private final SqlExecutor sql;

    public InterviewRecordRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<InterviewRecord> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM interview_records ORDER BY id DESC", this::mapRow);
    }

    public int countAll() {
        return sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM interview_records",
                rs -> rs.getInt("cnt"));
    }

    public List<InterviewRecord> findPage(int limit, int offset) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM interview_records ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRow, limit, offset);
    }

    public InterviewRecord findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM interview_records WHERE id=?",
                this::mapRow, id);
    }

    /** 면담기록 등록 — INSERT 후 id에서 record_no 파생. */
    public void save(InterviewRecord r) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO interview_records"
                + " (customer_name, content, customer_reaction,"
                + " follow_up_action, interviewed_at, recorded_at)"
                + " VALUES (?,?,?,?,?,?)",
                r.getCustomerName(), r.getContent(), r.getCustomerReaction(),
                r.getFollowUpAction(), r.getInterviewedAt(), r.getRecordedAt());
        r.setId(id);
        r.setRecordNo("REC" + String.format("%05d", id));
    }

    /** 면담기록 수정. */
    public void update(InterviewRecord r) {
        sql.executeUpdate(
                "UPDATE interview_records"
                + " SET content=?, customer_reaction=?, follow_up_action=?, modified_at=?"
                + " WHERE id=?",
                r.getContent(), r.getCustomerReaction(), r.getFollowUpAction(),
                r.getModifiedAt(), r.getId());
    }

    private InterviewRecord mapRow(ResultSet rs) throws SQLException {
        java.sql.Timestamp intTs = rs.getTimestamp("interviewed_at");
        java.sql.Timestamp recTs = rs.getTimestamp("recorded_at");
        java.sql.Timestamp modTs = rs.getTimestamp("modified_at");
        InterviewRecord r = InterviewRecord.fromDb(
                0,
                rs.getString("customer_name"),
                rs.getString("content"),
                intTs != null ? intTs.toLocalDateTime() : null,
                rs.getString("customer_reaction"),
                rs.getString("follow_up_action"));
        r.setId(rs.getLong("id"));
        r.setRecordNo("REC" + String.format("%05d", rs.getLong("id")));
        r.setRecordedAt(recTs != null ? recTs.toLocalDateTime() : null);
        r.setModifiedAt(modTs != null ? modTs.toLocalDateTime() : null);
        return r;
    }
}
