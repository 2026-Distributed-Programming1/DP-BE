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
        String cancNo  = r.getCancellation() != null ? r.getCancellation().getCancellationNo() : null;
        String status  = r.getStatus() != null ? r.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO refund_calculations"
                + " (cancellation_no, total_paid_premium, payment_period, reserve_amount,"
                + "  applied_rate, base_refund, unpaid_premium, final_refund, status)"
                + " VALUES (?,?,?,?,?,?,?,?,?)",
                cancNo, r.getTotalPaidPremium(), r.getPaymentPeriod(),
                r.getReserveAmount(), r.getAppliedRate(), r.getBaseRefund(),
                r.getUnpaidPremium(), r.getFinalRefund(), status);
        r.setId(id);
        r.setRefundNo("RFC" + String.format("%05d", id));
        sql.executeUpdate("UPDATE refund_calculations SET refund_no=? WHERE id=?", r.getRefundNo(), id);
    }

    public void updateStatus(RefundCalculation r) {
        sql.executeUpdate("UPDATE refund_calculations SET status=? WHERE id=?",
                r.getStatus().name(), r.getId());
    }

    public Optional<RefundCalculation> findByRefundNo(String refundNo) {
        List<RefundCalculation> list = sql.executeQuery(
                "SELECT id, refund_no, cancellation_no, total_paid_premium, payment_period,"
                + " reserve_amount, applied_rate, base_refund, unpaid_premium, final_refund, status"
                + " FROM refund_calculations WHERE refund_no=?",
                rowMapper(), refundNo);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<RefundCalculation> findByCancellationNo(String cancellationNo) {
        List<RefundCalculation> list = sql.executeQuery(
                "SELECT id, refund_no, cancellation_no, total_paid_premium, payment_period,"
                + " reserve_amount, applied_rate, base_refund, unpaid_premium, final_refund, status"
                + " FROM refund_calculations WHERE cancellation_no=?",
                rowMapper(), cancellationNo);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<RefundCalculation> findAll() {
        return sql.executeQuery(
                "SELECT id, refund_no, cancellation_no, total_paid_premium, payment_period,"
                + " reserve_amount, applied_rate, base_refund, unpaid_premium, final_refund, status"
                + " FROM refund_calculations ORDER BY id DESC",
                rowMapper());
    }

    private SqlExecutor.RowMapper<RefundCalculation> rowMapper() {
        return rs -> {
            String cancNo = rs.getString("cancellation_no");
            Customer custShell = new Customer("?", "", null, null, null);
            Contract contractShell = Contract.shellOf(null, custShell, 0L);
            Cancellation cancShell = new Cancellation(
                    cancNo != null ? cancNo : "?", contractShell, null, 0L, "완료");

            String st = rs.getString("status");
            RefundStatus status = RefundStatus.CALCULATED;
            if (st != null) {
                try { status = RefundStatus.valueOf(st); } catch (IllegalArgumentException ignored) {}
            }
            RefundCalculation r = new RefundCalculation(
                    rs.getString("refund_no"), cancShell,
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
}