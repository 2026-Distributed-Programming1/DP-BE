package org.dpbe.domain.payment.repository;

import java.util.ArrayList;
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
        Long refundId = p.getRefund() != null ? p.getRefund().getId() : null;
        Long cancId   = p.getRefund() != null && p.getRefund().getCancellation() != null
                ? p.getRefund().getCancellation().getId() : null;
        String status   = p.getStatus() != null ? p.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO refund_payments"
                + " (refund_id, cancellation_id, final_amount,"
                + "  transferred_at, notice_sent, otp_fail_count, status)"
                + " VALUES (?,?,?,?,?,?,?)",
                refundId, cancId, p.getFinalAmount(),
                p.getTransferredAt(), p.isNoticeSent(), p.getOtpFailCount(), status);
        p.setId(id);
        p.setPaymentNo("RPY" + String.format("%05d", id));
    }

    public void update(RefundPayment p) {
        sql.executeUpdate(
                "UPDATE refund_payments SET status=?, transferred_at=?,"
                + " notice_sent=?, otp_fail_count=? WHERE id=?",
                p.getStatus().name(), p.getTransferredAt(),
                p.isNoticeSent(), p.getOtpFailCount(), p.getId());
    }

    private static final String COLS =
            "id, refund_id, cancellation_id, final_amount,"
            + " transferred_at, notice_sent, otp_fail_count, status";
    private static final String ALIASED_COLS =
            "rp.id, rp.refund_id, rp.cancellation_id, rp.final_amount,"
            + " rp.transferred_at, rp.notice_sent, rp.otp_fail_count, rp.status";

    public Optional<RefundPayment> findById(Long id) {
        List<RefundPayment> list = sql.executeQuery(
                "SELECT " + COLS + " FROM refund_payments WHERE id=?",
                rowMapper(), id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public Optional<RefundPayment> findByRefundNo(String refundNo) {
        List<RefundPayment> list = sql.executeQuery(
                "SELECT " + COLS + " FROM refund_payments WHERE refund_id=?",
                rowMapper(), parseId(refundNo));
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<RefundPayment> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM refund_payments ORDER BY id DESC",
                rowMapper());
    }

    public int countByCustomerNo(String customerNo) {
        QueryParts query = buildCustomerQuery(
                "SELECT COUNT(*) AS cnt FROM refund_payments rp",
                customerNo);
        return sql.queryOne(query.sql(), rs -> rs.getInt("cnt"), query.params().toArray());
    }

    public List<RefundPayment> findPageByCustomerNo(String customerNo, int limit, int offset) {
        QueryParts query = buildCustomerQuery(
                "SELECT " + ALIASED_COLS + " FROM refund_payments rp",
                customerNo);
        List<Object> params = new ArrayList<>(query.params());
        params.add(limit);
        params.add(offset);
        return sql.executeQuery(query.sql() + " ORDER BY rp.id DESC LIMIT ? OFFSET ?",
                rowMapper(), params.toArray());
    }

    private QueryParts buildCustomerQuery(String selectSql, String customerNo) {
        StringBuilder query = new StringBuilder(selectSql);
        List<Object> params = new ArrayList<>();
        if (customerNo != null) {
            query.append(" LEFT JOIN cancellations ca ON ca.id = rp.cancellation_id")
                    .append(" LEFT JOIN contracts c ON c.id = ca.contract_id")
                    .append(" WHERE c.customer_id=?");
            params.add(customerNo);
        }
        return new QueryParts(query.toString(), params);
    }

    private RowMapper<RefundPayment> rowMapper() {
        return rs -> {
            long refundId = rs.getLong("refund_id");
            String refundNo = !rs.wasNull() ? "RFC" + String.format("%05d", refundId) : "?";
            long cancellationId = rs.getLong("cancellation_id");
            String cancNo = !rs.wasNull() ? "CAN" + String.format("%05d", cancellationId) : "?";

            Customer custShell = new Customer("?", "", null, null, null);
            Contract contractShell = Contract.shellOf(null, custShell, 0L);
            Cancellation cancShell = new Cancellation(
                    cancNo != null ? cancNo : "?", contractShell, null, 0L, "완료");
            if (cancellationId > 0) cancShell.setId(cancellationId);
            RefundCalculation refundShell = new RefundCalculation(
                    refundNo != null ? refundNo : "?", cancShell,
                    0L, null, 0L, 0.0, 0L, 0L, 0L, RefundStatus.CALCULATED);
            if (refundId > 0) refundShell.setId(refundId);

            String st = rs.getString("status");
            RefundPaymentStatus status = RefundPaymentStatus.WAITING;
            if (st != null) {
                try { status = RefundPaymentStatus.valueOf(st); } catch (IllegalArgumentException ignored) {}
            }
            RefundPayment p = new RefundPayment(
                    "RPY" + String.format("%05d", rs.getLong("id")), refundShell,
                    rs.getLong("final_amount"), status);
            p.setId(rs.getLong("id"));
            java.sql.Timestamp tat = rs.getTimestamp("transferred_at");
            if (tat != null) p.setTransferredAt(tat.toLocalDateTime());
            p.setNoticeSent(rs.getBoolean("notice_sent"));
            p.setOtpFailCount(rs.getInt("otp_fail_count"));
            return p;
        };
    }

    private Long parseId(String businessNo) {
        return Long.parseLong(businessNo.replaceAll("\\D", ""));
    }

    private record QueryParts(String sql, List<Object> params) {
    }
}
