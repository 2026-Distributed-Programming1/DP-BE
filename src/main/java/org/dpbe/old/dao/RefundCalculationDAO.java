package org.dpbe.old.dao;

import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.contract.entity.Cancellation;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.old.db.DBA;
import org.dpbe.domain.common.enums.RefundStatus;
import org.dpbe.domain.payment.entity.RefundCalculation;
import java.util.List;

public class RefundCalculationDAO {

    public static void save(RefundCalculation r) {
        String cancNo = r.getCancellation() != null ? r.getCancellation().getCancellationNo() : null;
        String status = r.getStatus() != null ? r.getStatus().name() : null;
        DBA.executeUpdate(
            "INSERT INTO refund_calculations (refund_no, cancellation_no, total_paid_premium,"
            + " payment_period, reserve_amount, applied_rate, base_refund,"
            + " unpaid_premium, final_refund, status)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE final_refund=VALUES(final_refund), status=VALUES(status)",
            r.getRefundNo(), cancNo, r.getTotalPaidPremium(),
            r.getPaymentPeriod(), r.getReserveAmount(), r.getAppliedRate(),
            r.getBaseRefund(), r.getUnpaidPremium(), r.getFinalRefund(), status);
    }

    public static List<RefundCalculation> findAll() {
        return DBA.executeQuery(
            "SELECT refund_no, cancellation_no, total_paid_premium, payment_period,"
            + " reserve_amount, applied_rate, base_refund, unpaid_premium, final_refund, status"
            + " FROM refund_calculations",
            rs -> {
                String cno = rs.getString("cancellation_no");
                Customer custShell = new Customer("?", "", null, null, null);
                Contract contractShell = Contract.shellOf(
                        null, custShell, rs.getLong("total_paid_premium") / 24);
                Cancellation cancShell = new Cancellation(
                    cno != null ? cno : "?", contractShell, null, 0L, "완료");
                String st = rs.getString("status");
                RefundStatus status = RefundStatus.CALCULATED;
                if (st != null) {
                    try { status = RefundStatus.valueOf(st); }
                    catch (IllegalArgumentException ignored) {}
                }
                return new RefundCalculation(
                    rs.getString("refund_no"), cancShell,
                    rs.getLong("total_paid_premium"),
                    rs.getString("payment_period"),
                    rs.getLong("reserve_amount"),
                    rs.getDouble("applied_rate"),
                    rs.getLong("base_refund"),
                    rs.getLong("unpaid_premium"),
                    rs.getLong("final_refund"),
                    status);
            });
    }

    public static boolean existsByCancellationNo(String cancellationNo) {
        return DBA.exists(
            "SELECT 1 FROM refund_calculations WHERE cancellation_no=?", cancellationNo);
    }
}