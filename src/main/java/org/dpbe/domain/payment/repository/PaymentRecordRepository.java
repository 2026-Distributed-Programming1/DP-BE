package org.dpbe.domain.payment.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.common.enums.PaymentRecordStatus;
import org.dpbe.domain.common.enums.RejectCategory;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.payment.entity.PaymentRecord;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

/**
 * 납부 내역 저장 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id. record_no는 INSERT 후 id에서 파생(저장형).
 */
@Repository
public class PaymentRecordRepository {

    private static final String COLS =
            "id, record_no, contract_no, customer_name, amount, method, payment_date, status,"
            + " confirmed_at, rejected_at, reject_category, reject_reason";

    private final SqlExecutor sql;

    public PaymentRecordRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public void save(PaymentRecord r) {
        String contractNo     = r.getContract() != null ? r.getContract().getContractNo() : null;
        String customerName   = r.getContract() != null && r.getContract().getCustomer() != null
                ? r.getContract().getCustomer().getName() : null;
        String status         = r.getStatus() != null ? r.getStatus().name() : null;
        String rejectCategory = r.getRejectCategory() != null ? r.getRejectCategory().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO payment_records (contract_no, customer_name, amount, method, payment_date,"
                + " status, confirmed_at, rejected_at, reject_category, reject_reason)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?)",
                contractNo, customerName, r.getAmount(), r.getMethod(), r.getPaymentDate(),
                status, r.getConfirmedAt(), r.getRejectedAt(), rejectCategory, r.getRejectReason());
        r.setId(id);
        r.setRecordNo("PRC" + String.format("%05d", id));
        sql.executeUpdate("UPDATE payment_records SET record_no=? WHERE id=?", r.getRecordNo(), id);
    }

    /** 확정/반려 후 상태 업데이트 */
    public void update(PaymentRecord r) {
        String status         = r.getStatus() != null ? r.getStatus().name() : null;
        String rejectCategory = r.getRejectCategory() != null ? r.getRejectCategory().name() : null;
        sql.executeUpdate(
                "UPDATE payment_records"
                + " SET status=?, confirmed_at=?, rejected_at=?, reject_category=?, reject_reason=?"
                + " WHERE record_no=?",
                status, r.getConfirmedAt(), r.getRejectedAt(), rejectCategory, r.getRejectReason(),
                r.getRecordNo());
    }

    public Optional<PaymentRecord> findByRecordNo(String recordNo) {
        List<PaymentRecord> list = sql.executeQuery(
                "SELECT " + COLS + " FROM payment_records WHERE record_no=?",
                rowMapper(), recordNo);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<PaymentRecord> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM payment_records ORDER BY id DESC",
                rowMapper());
    }

    public List<PaymentRecord> findByContractNo(String contractNo) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM payment_records WHERE contract_no=? ORDER BY id DESC",
                rowMapper(), contractNo);
    }

    public List<PaymentRecord> findByStatus(PaymentRecordStatus status) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM payment_records WHERE status=? ORDER BY id DESC",
                rowMapper(), status.name());
    }

    private SqlExecutor.RowMapper<PaymentRecord> rowMapper() {
        return rs -> {
            String cno  = rs.getString("contract_no");
            String cname = rs.getString("customer_name");
            Customer custShell = cname != null ? new Customer("?", cname, null, null, null) : null;
            Contract contractShell = Contract.shellOf(cno, custShell, 0L);

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
                    rs.getLong("amount"), rs.getString("method"), paymentDate, status);
            r.setId(rs.getLong("id"));
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
        };
    }
}