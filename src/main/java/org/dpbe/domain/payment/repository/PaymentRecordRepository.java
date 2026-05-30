package org.dpbe.domain.payment.repository;

import org.dpbe.global.jdbc.SqlExecutor;
import org.dpbe.domain.payment.entity.PaymentRecord;
import org.springframework.stereotype.Repository;

/**
 * 납부 내역 저장 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id. record_no는 INSERT 후 id에서 파생(저장형).
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

        long id = sql.executeInsertReturningKey(
                "INSERT INTO payment_records (contract_no, customer_name, amount, method, payment_date,"
                + " status, confirmed_at, rejected_at, reject_category, reject_reason)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?)",
                contractNo, customerName, r.getAmount(), r.getMethod(), r.getPaymentDate(),
                status, r.getConfirmedAt(), r.getRejectedAt(), rejectCategory, r.getRejectReason());
        r.setId(id);
        r.setRecordNo("PRC" + String.format("%05d", id));
        sql.executeUpdate("UPDATE payment_records SET record_no=? WHERE id=?", r.getRecordNo(), id);
    }
}