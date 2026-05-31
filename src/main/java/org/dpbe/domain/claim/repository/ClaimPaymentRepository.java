package org.dpbe.domain.claim.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.claim.entity.ClaimCalculation;
import org.dpbe.domain.claim.entity.ClaimPayment;
import org.dpbe.domain.common.enums.ClaimPaymentStatus;
import org.dpbe.domain.common.enums.PaymentType;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

/**
 * 보험금 지급 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id. payment_no는 INSERT 후 id에서 파생(CPY+%05d).
 * 단일 테이블 매핑은 기존 {@code ClaimPaymentDAO}를 정답지로 따르고,
 * 지급 생성 시 필요한 산출액·청구 계좌는 별도 조인 1쿼리로 로드한다(PayoutSource).
 */
@Repository
public class ClaimPaymentRepository {

    private static final String COLS =
            "id, payment_no, calculation_no, final_amount, paid_at, scheduled_at,"
            + " payment_type, recipient_name, account_no, failure_reason, status";

    private final SqlExecutor sql;

    public ClaimPaymentRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /**
     * 지급 생성에 필요한 데이터를 산출→조사→청구 조인으로 한 번에 로드한다.
     * (claim_payments 자체엔 bank_name·account_holder가 없으므로 청구 계좌를 승계)
     */
    public record PayoutSource(
            long finalAmount,
            String recipientName,
            String bankName,
            String accountNo,
            String accountHolder) {
    }

    public PayoutSource loadPayoutSource(String calculationNo) {
        return sql.queryOne(
                "SELECT cal.final_amount, cr.customer_name, cr.bank_name, cr.account_no, cr.account_holder"
                + " FROM claim_calculations cal"
                + " JOIN damage_investigations di ON di.investigation_no = cal.investigation_no"
                + " JOIN claim_requests cr ON cr.claim_no = di.claim_no"
                + " WHERE cal.calculation_no = ?",
                rs -> new PayoutSource(
                        rs.getLong("final_amount"),
                        rs.getString("customer_name"),
                        rs.getString("bank_name"),
                        rs.getString("account_no"),
                        rs.getString("account_holder")),
                calculationNo);
    }

    /** 신규 지급 저장 — INSERT 후 생성 id에서 payment_no 파생. */
    public void save(ClaimPayment p) {
        String calcNo = p.getCalculation() != null ? p.getCalculation().getCalculationNo() : null;
        String status = p.getStatus() != null ? p.getStatus().name() : null;
        String paymentType = p.getPaymentType() != null ? p.getPaymentType().name() : null;
        String recipientName = p.getRecipient() != null ? p.getRecipient().getName() : null;
        String accountNo = p.getAccount() != null ? p.getAccount().getAccountNo() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO claim_payments (calculation_no, final_amount, paid_at, scheduled_at,"
                + " payment_type, recipient_name, account_no, failure_reason, status)"
                + " VALUES (?,?,?,?,?,?,?,?,?)",
                calcNo, p.getFinalAmount(), p.getPaidAt(), p.getScheduledAt(),
                paymentType, recipientName, accountNo, p.getFailureReason(), status);
        p.setId(id);
        p.setPaymentNo("CPY" + String.format("%05d", id));
        sql.executeUpdate("UPDATE claim_payments SET payment_no=? WHERE id=?", p.getPaymentNo(), id);
    }

    /** 지급 상태 갱신 (실행/실패 결과 반영). */
    public void update(ClaimPayment p) {
        String status = p.getStatus() != null ? p.getStatus().name() : null;
        String paymentType = p.getPaymentType() != null ? p.getPaymentType().name() : null;
        sql.executeUpdate(
                "UPDATE claim_payments SET final_amount=?, paid_at=?, scheduled_at=?,"
                + " payment_type=?, failure_reason=?, status=? WHERE payment_no=?",
                p.getFinalAmount(), p.getPaidAt(), p.getScheduledAt(),
                paymentType, p.getFailureReason(), status, p.getPaymentNo());
    }

    public ClaimPayment findByCalculationNo(String calculationNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM claim_payments WHERE calculation_no=?",
                this::mapRow, calculationNo);
    }

    public ClaimPayment findByPaymentNo(String paymentNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM claim_payments WHERE payment_no=?",
                this::mapRow, paymentNo);
    }

    public List<ClaimPayment> findAll() {
        return sql.executeQuery("SELECT " + COLS + " FROM claim_payments", this::mapRow);
    }

    private ClaimPayment mapRow(ResultSet rs) throws SQLException {
        String cno = rs.getString("calculation_no");
        ClaimCalculation calcShell = new ClaimCalculation(
                cno != null ? cno : "?", null, 0, 0, 0, false, false, null);

        String st = rs.getString("status");
        ClaimPaymentStatus status = ClaimPaymentStatus.WAITING;
        if (st != null) {
            try { status = ClaimPaymentStatus.valueOf(st); }
            catch (IllegalArgumentException ignored) {}
        }
        ClaimPayment cp = new ClaimPayment(
                rs.getString("payment_no"), calcShell, rs.getLong("final_amount"), status);
        cp.setId(rs.getLong("id"));
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
    }
}