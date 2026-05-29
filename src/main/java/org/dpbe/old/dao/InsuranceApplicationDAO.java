package org.dpbe.old.dao;

import org.dpbe.domain.consultation.entity.InsuranceApplication;
import org.dpbe.old.db.DBA;
import java.util.List;

public class InsuranceApplicationDAO {

    public static void save(InsuranceApplication a) {
        String customerId = a.getCustomer() != null ? a.getCustomer().getCustomerId() : null;
        String customerName = a.getCustomer() != null ? a.getCustomer().getName() : null;
        String productName = a.getProduct() != null ? a.getProduct().getProductName() : null;
        long premium = a.getProduct() != null ? a.getProduct().getMonthlyPremium() : 0L;
        DBA.executeUpdate(
            "INSERT INTO insurance_applications (application_no, customer_id, customer_name,"
            + " product_name, monthly_premium, payment_method, applied_at, status)"
            + " VALUES (?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE payment_method=VALUES(payment_method), status=VALUES(status)",
            a.getApplicationNumber(),
            customerId,
            customerName,
            productName,
            premium,
            a.getPaymentMethod(),
            a.getAppliedAt(),
            a.getStatus() != null ? a.getStatus() : "신청");
    }

    public static List<InsuranceApplication> findAll() {
        return DBA.executeQuery(
            "SELECT application_no, customer_id, customer_name, product_name,"
            + " monthly_premium, payment_method FROM insurance_applications"
            + " WHERE status='신청'",
            rs -> InsuranceApplication.fromDb(
                rs.getInt("application_no"),
                rs.getString("customer_id"),
                rs.getString("customer_name"),
                rs.getString("product_name"),
                rs.getLong("monthly_premium"),
                rs.getString("payment_method")));
    }
}
