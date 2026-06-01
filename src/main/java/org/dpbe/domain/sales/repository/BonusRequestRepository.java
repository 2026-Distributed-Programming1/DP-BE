package org.dpbe.domain.sales.repository;

import org.dpbe.domain.sales.entity.BonusRequest;
import org.dpbe.global.exception.ApiException;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class BonusRequestRepository {

    private final SqlExecutor sql;

    public BonusRequestRepository(SqlExecutor sql) {
        this.sql = sql;
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
