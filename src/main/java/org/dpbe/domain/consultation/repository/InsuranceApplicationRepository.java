package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.consultation.entity.InsuranceApplication;
import org.dpbe.domain.consultation.entity.InsuranceProduct;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class InsuranceApplicationRepository {

    private static final String COLS =
            "id, application_no, customer_id, customer_name, product_name,"
            + " monthly_premium, payment_method, applied_at, status";

    private final SqlExecutor sql;

    public InsuranceApplicationRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 심사 대기 중인 신청 목록 (status='신청'). */
    public List<InsuranceApplication> findPending() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM insurance_applications WHERE status='신청' ORDER BY id",
                this::mapRow);
    }

    /** 보험 신청 저장 — INSERT 후 id에서 application_no 파생. */
    public void save(InsuranceApplication a) {
        String customerId   = a.getCustomer() != null ? a.getCustomer().getCustomerId() : null;
        String customerName = a.getCustomer() != null ? a.getCustomer().getName() : null;
        String productName  = a.getProduct() != null ? a.getProduct().getProductName() : null;
        long premium        = a.getProduct() != null ? a.getProduct().getMonthlyPremium() : 0L;
        long id = sql.executeInsertReturningKey(
                "INSERT INTO insurance_applications"
                + " (customer_id, customer_name, product_name, monthly_premium,"
                + " payment_method, applied_at, status)"
                + " VALUES (?,?,?,?,?,?,?)",
                customerId, customerName, productName, premium,
                a.getPaymentMethod(), a.getAppliedAt(),
                a.getStatus() != null ? a.getStatus() : "신청");
        a.setId(id);
        a.setApplicationNo("APP" + String.format("%05d", id));
        sql.executeUpdate("UPDATE insurance_applications SET application_no=? WHERE id=?",
                a.getApplicationNo(), id);
    }

    /** 심사 결과 반영 (status 갱신). */
    public void updateStatus(InsuranceApplication a) {
        sql.executeUpdate(
                "UPDATE insurance_applications SET status=? WHERE id=?",
                a.getStatus(), a.getId());
    }

    private InsuranceApplication mapRow(ResultSet rs) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp("applied_at");
        String customerId   = rs.getString("customer_id");
        String customerName = rs.getString("customer_name");
        String productName  = rs.getString("product_name");
        long premium        = rs.getLong("monthly_premium");
        InsuranceApplication a = InsuranceApplication.fromDb(
                0, customerId, customerName, productName, premium,
                rs.getString("payment_method"));
        a.setId(rs.getLong("id"));
        a.setApplicationNo(rs.getString("application_no"));
        a.setStatus(rs.getString("status"));
        if (ts != null) a.apply();
        return a;
    }
}