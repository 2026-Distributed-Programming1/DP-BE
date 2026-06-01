package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.dpbe.domain.consultation.entity.ReviewResult;
import org.dpbe.domain.consultation.entity.Underwriting;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class UnderwritingRepository {

    private static final String COLS =
            "id, review_type, app_no, customer_name,"
            + " risk_grade, review_opinion, result, result_condition, rejection_reason, reviewed_at";

    private final SqlExecutor sql;

    public UnderwritingRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 인수심사 저장 — INSERT 후 id에서 underwriting_no 파생. */
    public void save(Underwriting u) {
        String result = u.getReviewResult() != null ? u.getReviewResult().getResult() : null;
        String condition = u.getReviewResult() != null ? u.getReviewResult().getCondition() : null;
        String rejection = u.getReviewResult() != null ? u.getReviewResult().getRejectionReason() : null;
        long id = sql.executeInsertReturningKey(
                "INSERT INTO underwritings"
                + " (review_type, app_no, customer_name, risk_grade, review_opinion,"
                + " result, result_condition, rejection_reason, reviewed_at)"
                + " VALUES (?,?,?,?,?,?,?,?,?)",
                u.getReviewType(), u.getAppNo(), u.getCustomerName(),
                u.getRiskGrade(), u.getReviewOpinion(),
                result, condition, rejection, u.getReviewedAt());
        u.setId(id);
        u.setUnderwritingNo("UDW" + String.format("%05d", id));
    }

    private Underwriting mapRow(ResultSet rs) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp("reviewed_at");
        Underwriting u = new Underwriting(
                0,
                ts != null ? ts.toLocalDateTime() : null,
                rs.getString("risk_grade"),
                rs.getString("review_type"),
                rs.getString("review_opinion"),
                new ReviewResult(
                        rs.getString("result"),
                        rs.getString("result_condition"),
                        rs.getString("rejection_reason")));
        u.setId(rs.getLong("id"));
        u.setUnderwritingNo("UDW" + String.format("%05d", rs.getLong("id")));
        u.setAppNo(rs.getString("app_no"));
        u.setCustomerName(rs.getString("customer_name"));
        return u;
    }
}