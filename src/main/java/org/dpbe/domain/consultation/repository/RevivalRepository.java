package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.consultation.entity.Revival;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class RevivalRepository {

    private static final String COLS =
            "id, contract_id, customer_name, contact, unpaid_amount, payment_method, revived_at";

    private final SqlExecutor sql;

    public RevivalRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public int countAll() {
        return sql.queryOne("SELECT COUNT(*) AS cnt FROM revivals", rs -> rs.getInt("cnt"));
    }

    public List<Revival> findPage(int limit, int offset) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM revivals ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRow, limit, offset);
    }

    public Revival findByNo(String revivalNo) {
        long id = Long.parseLong(revivalNo.replaceAll("\\D", ""));
        return sql.queryOne(
                "SELECT " + COLS + " FROM revivals WHERE id=?",
                this::mapRow, id);
    }

    /** 부활 신청 저장 — INSERT 후 id에서 revival_no 파생. */
    public void save(Revival r) {
        String customerName = r.getCustomer() != null ? r.getCustomer().getName() : null;
        long id = sql.executeInsertReturningKey(
                "INSERT INTO revivals"
                + " (contract_id, customer_name, contact, unpaid_amount, payment_method, revived_at)"
                + " VALUES (?,?,?,?,?,?)",
                parseId(r.getContractNo()), customerName,
                r.getContact(), r.getUnpaidAmount(),
                r.getPaymentMethod(), r.getAppliedAt());
        r.setId(id);
        r.setRevivalNo("REV" + String.format("%05d", id));
    }

    private Revival mapRow(java.sql.ResultSet rs) throws java.sql.SQLException {
        Revival r = new Revival();
        long id = rs.getLong("id");
        r.setId(id);
        r.setRevivalNo("REV" + String.format("%05d", id));
        long contractId = rs.getLong("contract_id");
        if (contractId > 0) r.setContractNo("CON" + String.format("%05d", contractId));
        String customerName = rs.getString("customer_name");
        if (customerName != null) {
            r.setCustomer(new org.dpbe.domain.actor.Customer(null, customerName, null, null, null));
        }
        r.setContact(rs.getString("contact"));
        r.setUnpaidAmount(rs.getLong("unpaid_amount"));
        r.pay(rs.getString("payment_method"));
        java.sql.Timestamp ts = rs.getTimestamp("revived_at");
        if (ts != null) r.setAppliedAt(ts.toLocalDateTime());
        return r;
    }

    private Long parseId(String businessNo) {
        return Long.parseLong(businessNo.replaceAll("\\D", ""));
    }
}
