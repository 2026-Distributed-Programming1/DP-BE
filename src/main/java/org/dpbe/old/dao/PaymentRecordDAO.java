package org.dpbe.old.dao;

import java.time.LocalDate;
import java.util.List;
import org.dpbe.domain.common.enums.RejectCategory;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.old.db.DBA;
import org.dpbe.domain.common.enums.PaymentRecordStatus;
import org.dpbe.domain.payment.entity.PaymentRecord;

public class PaymentRecordDAO {

    public static void save(PaymentRecord r) {
        String contractNo   = r.getContract() != null ? r.getContract().getContractNo() : null;
        String customerName = r.getContract() != null && r.getContract().getCustomer() != null
                ? r.getContract().getCustomer().getName() : null;
        String status       = r.getStatus() != null ? r.getStatus().name() : null;
        String rejectCategory = r.getRejectCategory() != null ? r.getRejectCategory().name() : null;
        DBA.executeUpdate(
            "INSERT INTO payment_records (record_no, contract_no, customer_name, amount,"
            + " method, payment_date, status, confirmed_at, rejected_at, reject_category, reject_reason)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status),"
            + " confirmed_at=VALUES(confirmed_at), rejected_at=VALUES(rejected_at),"
            + " reject_category=VALUES(reject_category), reject_reason=VALUES(reject_reason)",
            r.getRecordNo(), contractNo, customerName,
            r.getAmount(), r.getMethod(), r.getPaymentDate(), status,
            r.getConfirmedAt(), r.getRejectedAt(), rejectCategory, r.getRejectReason());
    }

    private static final String SELECT_COLUMNS =
        "SELECT record_no, contract_no, customer_name, amount, method, payment_date, status,"
        + " confirmed_at, rejected_at, reject_category, reject_reason";

    private static PaymentRecord mapRow(java.sql.ResultSet rs, Contract contractShell) throws java.sql.SQLException {
        String st = rs.getString("status");
        PaymentRecordStatus status = PaymentRecordStatus.WAITING;
        if (st != null) {
            try { status = PaymentRecordStatus.valueOf(st); }
            catch (IllegalArgumentException ignored) {}
        }
        java.sql.Date pd = rs.getDate("payment_date");
        LocalDate paymentDate = pd != null ? pd.toLocalDate() : LocalDate.now();
        PaymentRecord r = new PaymentRecord(
            rs.getString("record_no"), contractShell,
            rs.getLong("amount"), rs.getString("method"),
            paymentDate, status);
        java.sql.Timestamp cat = rs.getTimestamp("confirmed_at");
        if (cat != null) r.setConfirmedAt(cat.toLocalDateTime());
        java.sql.Timestamp rat = rs.getTimestamp("rejected_at");
        if (rat != null) r.setRejectedAt(rat.toLocalDateTime());
        String rc = rs.getString("reject_category");
        if (rc != null) {
            try { r.setRejectCategory(RejectCategory.valueOf(rc)); }
            catch (IllegalArgumentException ignored) {}
        }
        r.setRejectReason(rs.getString("reject_reason"));
        return r;
    }

    public static List<PaymentRecord> findAll() {
        return DBA.executeQuery(
            SELECT_COLUMNS + " FROM payment_records",
            rs -> {
                String cno = rs.getString("contract_no");
                Contract contractShell = Contract.shellOf(cno, null, 0L);
                return mapRow(rs, contractShell);
            });
    }

    public static List<PaymentRecord> findByContractNo(String contractNo) {
        return DBA.executeQuery(
            SELECT_COLUMNS + " FROM payment_records WHERE contract_no=?",
            rs -> {
                Contract contractShell = new Contract();
                contractShell.setContractNo(rs.getString("contract_no"));
                return mapRow(rs, contractShell);
            }, contractNo);
    }
}