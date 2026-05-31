package org.dpbe.domain.payment.repository;

import java.util.List;
import java.util.Optional;
import org.dpbe.domain.common.enums.RefundPaymentStatus;
import org.dpbe.domain.common.enums.RefundStatus;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.payment.entity.RefundCalculation;
import org.dpbe.domain.payment.entity.RefundPayment;
import org.dpbe.global.jdbc.SqlExecutor;
import org.dpbe.global.jdbc.SqlExecutor.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RefundPaymentRepository {

    private final SqlExecutor sql;

    public RefundPaymentRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public void save(RefundPayment p) {
        String refundNo = p.getRefund() != null ? p.getRefund().getRefundNo() : null;
        String cancNo   = p.getRefund() != null && p.getRefund().getCancellation() != null
                ? p.getRefund().getCancellation().getCancellationNo() : null;
        String status   = p.getStatus() != null ? p.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO refund_payments"
                + " (refund_no, cancellation_no, final_amount,"
                + "  transferred_at, notice_sent, otp_fail_count, status)"
                + " VALUES (?,?,?,?,?,?,?)",
                refundNo, cancNo, p.getFinalAmount(),
                p.getTransferredAt(), p.isNoticeSent(), p.getOtpFailCount(), status);
        p.setId(id);
        p.setPaymentNo("RPY" + String.format("%05d", id));
        sql.executeUpdate("UPDATE refund_payments SET payment_no=? WHERE id=?", p.getPaymentNo(), id);
    }

    public void update(RefundPayment p) {
        sql.executeUpdate(
                "UPDATE refund_payments SET status=?, transferred_at=?,"
                + " notice_sent=?, otp_fail_count=? WHERE id=?",
                p.getStatus().name(), p.getTransferredAt(),
                p.isNoticeSent(), p.getOtpFailCount(), p.getId());
    }

    public Optional<RefundPayment> findByPaymentNo(String paymentNo) {
        List<RefundPayment> list = sql.executeQuery(
                "SELECT id, payment_no, refund_no, cancellation_no, final_amount,"
                + " transferred_at, notice_sent, otp_fail_count, status"
                + " FROM refund_payments WHERE payment_no=?",
                rowMapper(), paymentNo);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<RefundPayment> findByRefundNo(String refundNo) {
        List<RefundPayment> list = sql.executeQuery(
                "SELECT id, payment_no, refund_no, cancellation_no, final_amount,"
                + " transferred_at, notice_sent, otp_fail_count, status"
                + " FROM refund_payments WHERE refund_no=?",
                rowMapper(), refundNo);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<RefundPayment> findAll() {
        return sql.executeQuery(
                "SELECT id, payment_no, refund_no, cancellation_no, final_amount,"
                + " transferred_at, notice_sent, otp_fail_count, status"
                + " FROM refund_payments ORDER BY id DESC",
                rowMapper());
    }

    private RowMapper<RefundPayment> rowMapper() {
        return rs -> {
            String refundNo = rs.getString("refund_no");
            String cancNo   = rs.getString("cancellation_no");

            Customer custShell = new Customer("?", "", null, null, null);
            Contract contractShell = Contract.shellOf(null, custShell, 0L);
            Cancellation cancShell = new Cancellation(
                    cancNo != null ? cancNo : "?", contractShell, null, 0L, "완료");
            RefundCalculation refundShell = new RefundCalculation(
                    refundNo != null ? refundNo : "?", cancShell,
                    0L, null, 0L, 0.0, 0L, 0L, 0L, RefundStatus.CALCULATED);

            String st = rs.getString("status");
            RefundPaymentStatus status = RefundPaymentStatus.WAITING;
            if (st != null) {
                try { status = RefundPaymentStatus.valueOf(st); } catch (IllegalArgumentException ignored) {}
            }
            RefundPayment p = new RefundPayment(
                    rs.getString("payment_no"), refundShell,
                    rs.getLong("final_amount"), status);
            p.setId(rs.getLong("id"));
            java.sql.Timestamp tat = rs.getTimestamp("transferred_at");
            if (tat != null) p.setTransferredAt(tat.toLocalDateTime());
            p.setNoticeSent(rs.getBoolean("notice_sent"));
            p.setOtpFailCount(rs.getInt("otp_fail_count"));
            return p;
        };
    }
}