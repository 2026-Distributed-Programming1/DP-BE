package org.dpbe.domain.payment.repository;

import java.util.List;
import java.util.Optional;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.common.enums.RefundStatus;
import org.dpbe.domain.payment.entity.RefundCalculation;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class RefundCalculationRepository {

    private final SqlExecutor sql;

    public RefundCalculationRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public void save(RefundCalculation r) {
        Long cancId    = r.getCancellation() != null ? r.getCancellation().getId() : null;
        String status  = r.getStatus() != null ? r.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO refund_calculations"
                + " (cancellation_id, total_paid_premium, payment_period, reserve_amount,"
                + "  applied_rate, base_refund, unpaid_premium, final_refund, status)"
                + " VALUES (?,?,?,?,?,?,?,?,?)",
                cancId, r.getTotalPaidPremium(), r.getPaymentPeriod(),
                r.getReserveAmount(), r.getAppliedRate(), r.getBaseRefund(),
                r.getUnpaidPremium(), r.getFinalRefund(), status);
        r.setId(id);
        r.setRefundNo("RFC" + String.format("%05d", id));
    }

    public void updateStatus(RefundCalculation r) {
        sql.executeUpdate("UPDATE refund_calculations SET status=? WHERE id=?",
                r.getStatus().name(), r.getId());
    }

    private static final String COLS =
            "id, cancellation_id, total_paid_premium, payment_period,"
            + " reserve_amount, applied_rate, base_refund, unpaid_premium, final_refund, status";

    public Optional<RefundCalculation> findById(Long id) {
        List<RefundCalculation> list = sql.executeQuery(
                "SELECT " + COLS + " FROM refund_calculations WHERE id=?",
                rowMapper(), id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<RefundCalculation> findByCancellationNo(String cancellationNo) {
        List<RefundCalculation> list = sql.executeQuery(
                "SELECT " + COLS + " FROM refund_calculations WHERE cancellation_id=?",
                rowMapper(), parseId(cancellationNo));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<RefundCalculation> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM refund_calculations ORDER BY id DESC",
                rowMapper());
    }

    private SqlExecutor.RowMapper<RefundCalculation> rowMapper() {
        return rs -> {
            long cancellationId = rs.getLong("cancellation_id");
            String cancNo = !rs.wasNull() ? "CAN" + String.format("%05d", cancellationId) : "?";
            Customer custShell = new Customer("?", "", null, null, null);
            Contract contractShell = Contract.shellOf(null, custShell, 0L);
            Cancellation cancShell = new Cancellation(
                    cancNo != null ? cancNo : "?", contractShell, null, 0L, "완료");
            if (cancellationId > 0) cancShell.setId(cancellationId);

            String st = rs.getString("status");
            RefundStatus status = RefundStatus.CALCULATED;
            if (st != null) {
                try { status = RefundStatus.valueOf(st); } catch (IllegalArgumentException ignored) {}
            }
            RefundCalculation r = new RefundCalculation(
                    "RFC" + String.format("%05d", rs.getLong("id")), cancShell,
                    rs.getLong("total_paid_premium"),
                    rs.getString("payment_period"),
                    rs.getLong("reserve_amount"),
                    rs.getDouble("applied_rate"),
                    rs.getLong("base_refund"),
                    rs.getLong("unpaid_premium"),
                    rs.getLong("final_refund"),
                    status);
            r.setId(rs.getLong("id"));
            return r;
        };
    }

    private Long parseId(String businessNo) {
        return Long.parseLong(businessNo.replaceAll("\\D", ""));
    }
}
