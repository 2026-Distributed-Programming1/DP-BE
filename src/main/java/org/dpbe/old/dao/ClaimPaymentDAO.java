package org.dpbe.old.dao;

import java.util.List;
import org.dpbe.domain.claim.entity.ClaimCalculation;
import org.dpbe.domain.claim.entity.ClaimPayment;
import org.dpbe.domain.common.enums.PaymentType;
import org.dpbe.old.db.DBA;
import org.dpbe.domain.common.enums.ClaimPaymentStatus;

public class ClaimPaymentDAO {

    public static void save(ClaimPayment p) {
        String calcNo = p.getCalculation() != null ? p.getCalculation().getCalculationNo() : null;
        String status = p.getStatus() != null ? p.getStatus().name() : null;
        String paymentType = p.getPaymentType() != null ? p.getPaymentType().name() : null;
        String recipientName = p.getRecipient() != null ? p.getRecipient().getName() : null;
        String accountNo = p.getAccount() != null ? p.getAccount().getAccountNo() : null;
        DBA.executeUpdate(
            "INSERT INTO claim_payments (payment_no, calculation_no, final_amount,"
            + " paid_at, scheduled_at, payment_type, recipient_name, account_no, failure_reason, status)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status),"
            + " paid_at=VALUES(paid_at), failure_reason=VALUES(failure_reason),"
            + " scheduled_at=VALUES(scheduled_at), payment_type=VALUES(payment_type),"
            + " recipient_name=VALUES(recipient_name), account_no=VALUES(account_no)",
            p.getPaymentNo(), calcNo, p.getFinalAmount(),
            p.getPaidAt(), p.getScheduledAt(), paymentType,
            recipientName, accountNo, p.getFailureReason(), status);
    }

    public static List<ClaimPayment> findAll() {
        return DBA.executeQuery(
            "SELECT payment_no, calculation_no, final_amount,"
            + " paid_at, scheduled_at, payment_type, recipient_name, account_no, failure_reason, status FROM claim_payments",
            rs -> {
                String cno = rs.getString("calculation_no");
                ClaimCalculation calcShell = new ClaimCalculation(
                    cno != null ? cno : "?", null, 0, 0, 0,
                    false, false, null);
                String st = rs.getString("status");
                ClaimPaymentStatus status = ClaimPaymentStatus.WAITING;
                if (st != null) {
                    try { status = ClaimPaymentStatus.valueOf(st); }
                    catch (IllegalArgumentException ignored) {}
                }
                ClaimPayment cp = new ClaimPayment(
                    rs.getString("payment_no"), calcShell,
                    rs.getLong("final_amount"), status);
                cp.setRecipientFromName(rs.getString("recipient_name"));
                cp.setAccountFromNo(rs.getString("account_no"));
                java.sql.Timestamp pat = rs.getTimestamp("paid_at");
                if (pat != null) cp.setPaidAt(pat.toLocalDateTime());
                java.sql.Timestamp sat = rs.getTimestamp("scheduled_at");
                if (sat != null) cp.setScheduledAt(sat.toLocalDateTime());
                String pt = rs.getString("payment_type");
                if (pt != null) {
                    try { cp.setPaymentType(PaymentType.valueOf(pt)); }
                    catch (IllegalArgumentException ignored) {}
                }
                cp.setFailureReason(rs.getString("failure_reason"));
                return cp;
            });
    }
}