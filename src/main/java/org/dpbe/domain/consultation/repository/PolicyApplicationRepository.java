package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.consultation.entity.PolicyApplication;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class PolicyApplicationRepository {

    private static final String COLS =
            "id, customer_id, customer_name, product_name,"
            + " period, payment_method, submitted_at, uploaded_at, status";

    private final SqlExecutor sql;

    public PolicyApplicationRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 심사 대기 중인 청약 목록 (status='신청'). */
    public List<PolicyApplication> findPending() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM policy_applications WHERE status='신청' ORDER BY id",
                this::mapRow);
    }

    /** 청약서 저장 — INSERT 후 id에서 application_no 파생. */
    public void save(PolicyApplication p) {
        String customerId = p.getCustomer() != null ? p.getCustomer().getCustomerId() : null;
        long id = sql.executeInsertReturningKey(
                "INSERT INTO policy_applications"
                + " (customer_id, customer_name, product_name, period,"
                + " payment_method, submitted_at, uploaded_at, status)"
                + " VALUES (?,?,?,?,?,?,?,?)",
                customerId, p.getCustomerName(), p.getProductName(), p.getPeriod(),
                p.getPaymentMethod(), p.getSubmittedAt(), p.getUploadedAt(),
                p.getStatus() != null ? p.getStatus() : "신청");
        p.setId(id);
        p.setApplicationNo("POL" + String.format("%05d", id));
    }

    /** 심사 결과 반영 (status 갱신). */
    public void updateStatus(PolicyApplication p) {
        sql.executeUpdate(
                "UPDATE policy_applications SET status=? WHERE id=?",
                p.getStatus(), p.getId());
    }

    private PolicyApplication mapRow(ResultSet rs) throws SQLException {
        PolicyApplication p = PolicyApplication.fromDb(
                0,
                rs.getString("customer_id"),
                rs.getString("customer_name"),
                rs.getString("product_name"),
                rs.getInt("period"),
                rs.getString("payment_method"));
        p.setId(rs.getLong("id"));
        p.setApplicationNo("POL" + String.format("%05d", rs.getLong("id")));
        p.setStatus(rs.getString("status"));
        java.sql.Timestamp subTs = rs.getTimestamp("submitted_at");
        java.sql.Timestamp upTs  = rs.getTimestamp("uploaded_at");
        if (subTs != null) p.submit();
        if (upTs  != null) p.setUploadedAt(upTs.toLocalDateTime());
        return p;
    }
}