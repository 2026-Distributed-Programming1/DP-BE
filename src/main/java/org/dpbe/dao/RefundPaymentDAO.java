package org.dpbe.dao;

import java.util.List;
import org.dpbe.db.DBA;
import org.dpbe.enums.RefundPaymentStatus;
import org.dpbe.payment.RefundCalculation;
import org.dpbe.payment.RefundPayment;

public class RefundPaymentDAO {

    public static void save(RefundPayment p) {
        String refundNo = p.getRefund() != null ? p.getRefund().getRefundNo() : null;
        String cancNo   = p.getRefund() != null && p.getRefund().getCancellation() != null
                ? p.getRefund().getCancellation().getCancellationNo() : null;
        String status   = p.getStatus() != null ? p.getStatus().name() : null;
        DBA.executeUpdate(
            "INSERT INTO refund_payments (payment_no, refund_no, cancellation_no, final_amount,"
            + " transferred_at, notice_sent, otp_fail_count, status)"
            + " VALUES (?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status), transferred_at=VALUES(transferred_at),"
            + " notice_sent=VALUES(notice_sent)",
            p.getPaymentNo(), refundNo, cancNo, p.getFinalAmount(),
            p.getTransferredAt(), p.isNoticeSent(), p.getOtpFailCount(), status);
    }

    public static List<RefundPayment> findAll() {
        return DBA.executeQuery(
            "SELECT payment_no, refund_no, cancellation_no, final_amount, status,"
            + " transferred_at, notice_sent, otp_fail_count FROM refund_payments",
            rs -> {
                String rno   = rs.getString("refund_no");
                String cancNo = rs.getString("cancellation_no");
                // Cancellation 체인 복원: cancellation_no로 DB 조회
                org.dpbe.contract.Cancellation cancellation = null;
                if (cancNo != null) {
                    cancellation = CancellationDAO.findAll().stream()
                            .filter(c -> cancNo.equals(c.getCancellationNo()))
                            .findFirst().orElse(null);
                }
                RefundCalculation refundShell = new RefundCalculation(
                    rno != null ? rno : "?", cancellation, 0, null, 0, 0, 0, 0, 0, null);
                String st = rs.getString("status");
                RefundPaymentStatus status = RefundPaymentStatus.WAITING;
                if (st != null) {
                    try { status = RefundPaymentStatus.valueOf(st); }
                    catch (IllegalArgumentException ignored) {}
                }
                RefundPayment p = new RefundPayment(
                    rs.getString("payment_no"), refundShell,
                    rs.getLong("final_amount"), status);
                java.sql.Timestamp tat = rs.getTimestamp("transferred_at");
                if (tat != null) p.setTransferredAt(tat.toLocalDateTime());
                p.setNoticeSent(rs.getBoolean("notice_sent"));
                p.setOtpFailCount(rs.getInt("otp_fail_count"));
                return p;
            });
    }
}