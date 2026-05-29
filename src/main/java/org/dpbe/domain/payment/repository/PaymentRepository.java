package org.dpbe.domain.payment.repository;

import org.dpbe.global.jdbc.SqlExecutor;
import org.dpbe.domain.payment.entity.Payment;
import org.dpbe.domain.payment.entity.PaymentItem;
import org.springframework.stereotype.Repository;

/**
 * 납입 저장 리포지토리 (Spring 트랜잭션 통합 경로).
 * 기존 {@code PaymentDAO.save}와 동일하나 SqlExecutor 경유 →
 * 호출하는 서비스의 {@code @Transactional} 경계 안에서 원자적으로 실행된다.
 */
@Repository
public class PaymentRepository {

    private final SqlExecutor sql;

    public PaymentRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public void save(Payment p) {
        String customerId   = p.getCustomer() != null ? p.getCustomer().getCustomerId() : null;
        String customerName = p.getCustomer() != null ? p.getCustomer().getName() : null;
        String method       = p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null;
        String status       = p.getStatus() != null ? p.getStatus().name() : null;

        sql.executeUpdate(
            "INSERT INTO payments (payment_no, customer_id, customer_name, total_amount,"
            + " payment_method, requested_at, status)"
            + " VALUES (?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status)",
            p.getPaymentNo(), customerId, customerName,
            p.getDiscountedAmount(), method, p.getRequestedAt(), status);

        for (PaymentItem item : p.getItems()) {
            String contractNo = item.getContract() != null ? item.getContract().getContractNo() : null;
            sql.executeUpdate(
                "INSERT INTO payment_items (payment_no, contract_no, count, subtotal)"
                + " VALUES (?,?,?,?)",
                p.getPaymentNo(), contractNo, item.getCount(), item.getSubtotal());
        }
    }
}