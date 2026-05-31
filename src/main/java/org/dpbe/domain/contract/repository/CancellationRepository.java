package org.dpbe.domain.contract.repository;

import java.util.List;
import java.util.Optional;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class CancellationRepository {

    private final SqlExecutor sql;

    public CancellationRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public void save(Cancellation c) {
        String contractNo   = c.getContract() != null ? c.getContract().getContractNo() : null;
        String customerName = c.getContract() != null && c.getContract().getCustomer() != null
                ? c.getContract().getCustomer().getName() : null;
        long monthlyPremium = c.getContract() != null ? c.getContract().getMonthlyPremium() : 0L;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO cancellations"
                + " (contract_no, customer_name, monthly_premium, reason, detail_reason,"
                + "  expected_refund, status, cancelled_at)"
                + " VALUES (?,?,?,?,?,?,?,?)",
                contractNo, customerName, monthlyPremium,
                c.getReason(), c.getDetailReason(),
                c.getExpectedRefund(), c.getStatus(), c.getCanceledAt());
        c.setId(id);
        c.setCancellationNo("CAN" + String.format("%05d", id));
        sql.executeUpdate("UPDATE cancellations SET cancellation_no=? WHERE id=?",
                c.getCancellationNo(), id);
    }

    public Optional<Cancellation> findByCancellationNo(String cancellationNo) {
        List<Cancellation> list = sql.executeQuery(
                "SELECT id, cancellation_no, contract_no, customer_name, monthly_premium,"
                + " reason, detail_reason, expected_refund, status, cancelled_at"
                + " FROM cancellations WHERE cancellation_no=?",
                rowMapper(), cancellationNo);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<Cancellation> findAll() {
        return sql.executeQuery(
                "SELECT id, cancellation_no, contract_no, customer_name, monthly_premium,"
                + " reason, detail_reason, expected_refund, status, cancelled_at"
                + " FROM cancellations ORDER BY id DESC",
                rowMapper());
    }

    private SqlExecutor.RowMapper<Cancellation> rowMapper() {
        return rs -> {
            String cno   = rs.getString("contract_no");
            String cname = rs.getString("customer_name");
            long premium = rs.getLong("monthly_premium");
            Customer custShell = new Customer("?", cname != null ? cname : "", null, null, null);
            Contract contractShell = Contract.shellOf(cno, custShell, premium);

            Cancellation c = new Cancellation(
                    rs.getString("cancellation_no"),
                    contractShell,
                    rs.getString("reason"),
                    rs.getLong("expected_refund"),
                    rs.getString("status"));
            c.setId(rs.getLong("id"));
            String detail = rs.getString("detail_reason");
            if (detail != null) c.enterDetailReason(detail);
            java.sql.Timestamp cat = rs.getTimestamp("cancelled_at");
            if (cat != null) c.setCanceledAt(cat.toLocalDateTime());
            return c;
        };
    }
}