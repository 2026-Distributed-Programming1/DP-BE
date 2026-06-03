package org.dpbe.domain.sales.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.ScreeningStatus;
import org.dpbe.domain.sales.entity.ChannelScreening;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class ChannelScreeningRepository {

    private static final String COLS =
            "id, applicant_name, channel_type, career, certifications,"
            + " application_date, rejection_reason, status, reviewed_at";

    private final SqlExecutor sql;

    public ChannelScreeningRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<ChannelScreening> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM channel_screenings ORDER BY id DESC",
                this::mapRow);
    }

    public int countAll() {
        return sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM channel_screenings",
                rs -> rs.getInt("cnt"));
    }

    public List<ChannelScreening> findPage(int limit, int offset) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM channel_screenings ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRow, limit, offset);
    }

    public ChannelScreening findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM channel_screenings WHERE id=?",
                this::mapRow, id);
    }

    public void save(ChannelScreening s) {
        String certsStr = s.getCertifications().isEmpty()
                ? null : String.join(",", s.getCertifications());
        long id = sql.executeInsertReturningKey(
                "INSERT INTO channel_screenings"
                + " (applicant_name, channel_type, career, certifications,"
                + "  application_date, rejection_reason, status, reviewed_at)"
                + " VALUES (?,?,?,?,?,?,?,?)",
                s.getApplicantName(),
                s.getChannelType() != null ? s.getChannelType().name() : null,
                s.getCareer(),
                certsStr,
                s.getApplicationDate(),
                s.getRejectionReason(),
                s.getScreeningStatus() != null ? s.getScreeningStatus().name() : null,
                s.getReviewedAt());
        s.setId(id);
        s.setScreeningNo("SCN" + String.format("%05d", id));
    }

    public void updateReview(ChannelScreening s) {
        sql.executeUpdate(
                "UPDATE channel_screenings"
                + " SET status=?, reviewed_at=?, approval_no=?, rejection_reason=?"
                + " WHERE id=?",
                s.getScreeningStatus() != null ? s.getScreeningStatus().name() : null,
                s.getReviewedAt(),
                s.getApprovalNo(),
                s.getRejectionReason(),
                s.getId());
    }

    private ChannelScreening mapRow(ResultSet rs) throws SQLException {
        ChannelScreening s = new ChannelScreening();
        s.setId(rs.getLong("id"));
        s.setScreeningNo("SCN" + String.format("%05d", rs.getLong("id")));
        s.setApplicantName(rs.getString("applicant_name"));
        String ct = rs.getString("channel_type");
        if (ct != null) {
            try { s.setChannelType(ChannelType.valueOf(ct)); }
            catch (IllegalArgumentException ignored) {}
        }
        s.setCareer(rs.getString("career"));
        String certsStr = rs.getString("certifications");
        if (certsStr != null && !certsStr.isEmpty()) {
            s.setCertifications(Arrays.stream(certsStr.split(","))
                    .map(String::trim).collect(Collectors.toList()));
        }
        java.sql.Date ad = rs.getDate("application_date");
        if (ad != null) s.setApplicationDate(ad.toLocalDate());
        s.setRejectionReason(rs.getString("rejection_reason"));
        String st = rs.getString("status");
        if (st != null) {
            try { s.setScreeningStatus(ScreeningStatus.valueOf(st)); }
            catch (IllegalArgumentException ignored) {}
        }
        java.sql.Timestamp rat = rs.getTimestamp("reviewed_at");
        if (rat != null) s.setReviewedAt(rat.toLocalDateTime());
        return s;
    }
}
