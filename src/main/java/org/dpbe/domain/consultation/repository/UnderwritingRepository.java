package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.consultation.dto.PendingApplicationResponse;
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

    public int countPendingApplications() {
        return sql.queryOne(
                "SELECT ("
                + " SELECT COUNT(*) FROM policy_applications WHERE status='신청'"
                + ") + ("
                + " SELECT COUNT(*) FROM insurance_applications WHERE status='신청'"
                + ") AS cnt",
                rs -> rs.getInt("cnt"));
    }

    public List<PendingApplicationResponse> findPendingApplicationsPage(int limit, int offset) {
        return sql.executeQuery(
                "SELECT application_type, application_no, customer_name, product_name, payment_method, status"
                + " FROM ("
                + " SELECT 1 AS type_order, id, '청약' AS application_type,"
                + "        CONCAT('POL', LPAD(id, 5, '0')) AS application_no,"
                + "        customer_name, product_name, payment_method, status"
                + " FROM policy_applications WHERE status='신청'"
                + " UNION ALL"
                + " SELECT 2 AS type_order, id, '보험신청' AS application_type,"
                + "        CONCAT('APP', LPAD(id, 5, '0')) AS application_no,"
                + "        customer_name, product_name, payment_method, status"
                + " FROM insurance_applications WHERE status='신청'"
                + " ) pending"
                + " ORDER BY type_order ASC, id ASC LIMIT ? OFFSET ?",
                rs -> new PendingApplicationResponse(
                        rs.getString("application_type"),
                        rs.getString("application_no"),
                        rs.getString("customer_name"),
                        rs.getString("product_name"),
                        rs.getString("payment_method"),
                        rs.getString("status")),
                limit, offset);
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
