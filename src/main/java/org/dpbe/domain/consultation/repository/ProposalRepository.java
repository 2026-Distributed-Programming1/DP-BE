package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.consultation.entity.InsuranceProduct;
import org.dpbe.domain.consultation.entity.Proposal;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class ProposalRepository {

    private static final String COLS =
            "id, customer_name, product_name, monthly_premium, sent_at";

    private final SqlExecutor sql;

    public ProposalRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<Proposal> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM proposals ORDER BY id DESC", this::mapRow);
    }

    public int countAll() {
        return sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM proposals",
                rs -> rs.getInt("cnt"));
    }

    public List<Proposal> findPage(int limit, int offset) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM proposals ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRow, limit, offset);
    }

    /** 제안서 저장 — INSERT 후 id에서 proposal_no 파생. */
    public void save(Proposal p) {
        String productName = p.getInsuranceProduct() != null ? p.getInsuranceProduct().getProductName() : null;
        long premium = p.getInsuranceProduct() != null ? p.getInsuranceProduct().getMonthlyPremium() : 0L;
        long id = sql.executeInsertReturningKey(
                "INSERT INTO proposals (customer_name, product_name, monthly_premium, sent_at)"
                + " VALUES (?,?,?,?)",
                p.getCustomerName(), productName, premium, p.getSentAt());
        p.setId(id);
        p.setProposalNo("PRO" + String.format("%05d", id));
    }

    private Proposal mapRow(ResultSet rs) throws SQLException {
        java.sql.Timestamp ts = rs.getTimestamp("sent_at");
        String productName = rs.getString("product_name");
        long premium = rs.getLong("monthly_premium");
        InsuranceProduct product = productName != null
                ? new InsuranceProduct(productName, null, premium, null, null) : null;
        Proposal p = new Proposal(0, ts != null ? ts.toLocalDateTime() : null,
                rs.getString("customer_name"), product);
        p.setId(rs.getLong("id"));
        p.setProposalNo("PRO" + String.format("%05d", rs.getLong("id")));
        return p;
    }
}
