package org.dpbe.domain.payment.repository;

import org.dpbe.global.jdbc.SqlExecutor;
import org.dpbe.domain.payment.entity.Payment;
import org.dpbe.domain.payment.entity.PaymentItem;
import org.springframework.stereotype.Repository;

/**
 * 납입 저장 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id. payment_no는 INSERT 후 id에서 파생(저장형).
 * 호출 서비스의 @Transactional 경계 안에서 원자적으로 실행된다.
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

        long id = sql.executeInsertReturningKey(
                "INSERT INTO payments (customer_id, customer_name, total_amount,"
                + " payment_method, requested_at, status) VALUES (?,?,?,?,?,?)",
                customerId, customerName, p.getDiscountedAmount(), method, p.getRequestedAt(), status);
        p.setId(id);
        p.setPaymentNo("PAY" + String.format("%05d", id));
        sql.executeUpdate("UPDATE payments SET payment_no=? WHERE id=?", p.getPaymentNo(), id);

        for (PaymentItem item : p.getItems()) {
            String contractNo = item.getContract() != null ? item.getContract().getContractNo() : null;
            sql.executeUpdate(
                    "INSERT INTO payment_items (payment_no, contract_no, count, subtotal)"
                    + " VALUES (?,?,?,?)",
                    p.getPaymentNo(), contractNo, item.getCount(), item.getSubtotal());
        }
    }
}
