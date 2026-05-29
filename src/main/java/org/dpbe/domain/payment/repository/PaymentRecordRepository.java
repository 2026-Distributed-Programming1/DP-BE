package org.dpbe.domain.payment.repository;

import org.dpbe.global.jdbc.SqlExecutor;
import org.dpbe.domain.payment.entity.PaymentRecord;
import org.springframework.stereotype.Repository;

/**
 * 납부 내역 저장 리포지토리 (Spring 트랜잭션 통합 경로).
 * 기존 {@code PaymentRecordDAO.save}와 동일하나 SqlExecutor 경유.
 */
@Repository
public class PaymentRecordRepository {

    private final SqlExecutor sql;

    public PaymentRecordRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public void save(PaymentRecord r) {
        String contractNo   = r.getContract() != null ? r.getContract().getContractNo() : null;
        String customerName = r.getContract() != null && r.getContract().getCustomer() != null
                ? r.getContract().getCustomer().getName() : null;
        String status         = r.getStatus() != null ? r.getStatus().name() : null;
        String rejectCategory = r.getRejectCategory() != null ? r.getRejectCategory().name() : null;

        sql.executeUpdate(
            "INSERT INTO payment_records (record_no, contract_no, customer_name, amount,"
            + " method, payment_date, status, confirmed_at, rejected_at, reject_category, reject_reason)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status),"
            + " confirmed_at=VALUES(confirmed_at), rejected_at=VALUES(rejected_at),"
            + " reject_category=VALUES(reject_category), reject_reason=VALUES(reject_reason)",
            r.getRecordNo(), contractNo, customerName,
            r.getAmount(), r.getMethod(), r.getPaymentDate(), status,
            r.getConfirmedAt(), r.getRejectedAt(), rejectCategory, r.getRejectReason());
    }
}