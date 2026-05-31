package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.dpbe.domain.consultation.entity.Revival;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class RevivalRepository {

    private final SqlExecutor sql;

    public RevivalRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 부활 신청 저장 — INSERT 후 id에서 revival_no 파생. */
    public void save(Revival r) {
        String customerName = r.getCustomer() != null ? r.getCustomer().getName() : null;
        long id = sql.executeInsertReturningKey(
                "INSERT INTO revivals"
                + " (contract_no, customer_name, contact, unpaid_amount, payment_method, revived_at)"
                + " VALUES (?,?,?,?,?,?)",
                r.getContractNo(), customerName,
                r.getContact(), r.getUnpaidAmount(),
                r.getPaymentMethod(), r.getAppliedAt());
        r.setId(id);
        r.setRevivalNo("REV" + String.format("%05d", id));
        sql.executeUpdate("UPDATE revivals SET revival_no=? WHERE id=?",
                r.getRevivalNo(), id);
    }
}