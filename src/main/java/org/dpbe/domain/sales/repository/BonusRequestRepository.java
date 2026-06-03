package org.dpbe.domain.sales.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.common.enums.ChannelType;
import org.dpbe.domain.common.enums.EvaluationGrade;
import org.dpbe.domain.sales.entity.BonusRequest;
import org.dpbe.global.exception.ApiException;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class BonusRequestRepository {

    private static final String COLS =
            "id, channel_name, evaluation_id, channel_type, evaluation_grade,"
            + " amount, reason, status, created_at";

    private final SqlExecutor sql;

    public BonusRequestRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public int countAll() {
        return sql.queryOne("SELECT COUNT(*) AS cnt FROM bonus_requests", rs -> rs.getInt("cnt"));
    }

    public List<BonusRequest> findPage(int limit, int offset) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM bonus_requests ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRow, limit, offset);
    }

    public BonusRequest findByNo(String requestNo) {
        long id = Long.parseLong(requestNo.replaceAll("\\D", ""));
        return sql.queryOne(
                "SELECT " + COLS + " FROM bonus_requests WHERE id=?",
                this::mapRow, id);
    }

    private BonusRequest mapRow(ResultSet rs) throws SQLException {
        BonusRequest r = new BonusRequest();
        long id = rs.getLong("id");
        r.setId(id);
        r.setRequestNo("BNS" + String.format("%05d", id));
        r.setChannelName(rs.getString("channel_name"));
        String ct = rs.getString("channel_type");
        if (ct != null) {
            try { r.setChannelType(ChannelType.valueOf(ct)); } catch (IllegalArgumentException ignored) {}
        }
        String grade = rs.getString("evaluation_grade");
        if (grade != null) {
            try { r.setEvaluationGrade(EvaluationGrade.valueOf(grade)); } catch (IllegalArgumentException ignored) {}
        }
        long evaluationId = rs.getLong("evaluation_id");
        if (evaluationId > 0) r.setEvaluationNo("EVL" + String.format("%05d", evaluationId));
        r.setRequestReason(rs.getString("reason"));
        java.sql.Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) r.setRequestedAt(ts.toLocalDateTime());
        return r;
    }

    public void save(BonusRequest r) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO bonus_requests"
                + " (channel_name, evaluation_id, channel_type, evaluation_grade,"
                + "  amount, reason, status, created_at)"
                + " VALUES (?,?,?,?,?,?,?,?)",
                r.getChannelName(),
                parseId(r.getEvaluationNo()),
                r.getChannelType() != null ? r.getChannelType().name() : null,
                r.getEvaluationGrade() != null ? r.getEvaluationGrade().name() : null,
                r.getBonusAmount() != null ? r.getBonusAmount().longValue() : 0L,
                r.getRequestReason(),
                "SUBMITTED",
                r.getRequestedAt());
        r.setId(id);
        r.setRequestNo("BNS" + String.format("%05d", id));
    }

    private Long parseId(String businessNo) {
        if (businessNo == null || businessNo.isBlank()) {
            return null;
        }
        String digits = businessNo.replaceAll("\\D", "");
        if (digits.isBlank()) {
            throw ApiException.badRequest("유효하지 않은 평가번호: " + businessNo);
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException e) {
            throw ApiException.badRequest("유효하지 않은 평가번호: " + businessNo);
        }
    }
}
