package org.dpbe.old.dao;

import java.util.List;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.old.db.DBA;

public class CancellationDAO {

    public static void save(Cancellation c) {
        String contractNo   = c.getContract() != null ? c.getContract().getContractNo() : null;
        String customerName = c.getContract() != null && c.getContract().getCustomer() != null
                ? c.getContract().getCustomer().getName() : null;
        long monthlyPremium = c.getContract() != null ? c.getContract().getMonthlyPremium() : 0L;
        DBA.executeUpdate(
            "INSERT INTO cancellations (cancellation_no, contract_no, customer_name,"
            + " monthly_premium, reason, detail_reason, expected_refund, status, cancelled_at)"
            + " VALUES (?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status), cancelled_at=VALUES(cancelled_at),"
            + " reason=VALUES(reason), detail_reason=VALUES(detail_reason),"
            + " expected_refund=VALUES(expected_refund)",
            c.getCancellationNo(), contractNo, customerName,
            monthlyPremium, c.getReason(), c.getDetailReason(), c.getExpectedRefund(),
            c.getStatus(), c.getCanceledAt());
    }

    public static List<Cancellation> findAll() {
        return DBA.executeQuery(
            "SELECT cancellation_no, contract_no, customer_name, monthly_premium,"
            + " reason, detail_reason, expected_refund, status, cancelled_at FROM cancellations",
            rs -> {
                String cno   = rs.getString("contract_no");
                String cname = rs.getString("customer_name");
                long premium = rs.getLong("monthly_premium");
                Customer custShell = new Customer(
                    cno != null ? cno : "?", cname != null ? cname : "", null, null, null);
                Contract contractShell = Contract.shellOf(cno, custShell, premium);
                Cancellation c = new Cancellation(
                    rs.getString("cancellation_no"),
                    contractShell,
                    rs.getString("reason"),
                    rs.getLong("expected_refund"),
                    rs.getString("status"));
                String detail = rs.getString("detail_reason");
                if (detail != null) c.enterDetailReason(detail);
                java.sql.Timestamp cat = rs.getTimestamp("cancelled_at");
                if (cat != null) c.setCanceledAt(cat.toLocalDateTime());
                return c;
            });
    }
}