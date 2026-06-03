package org.dpbe.domain.payment.repository;

import java.time.LocalDate;
import java.util.ArrayList;
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
 * PK는 surrogate id. record_no는 저장하지 않고 id에서 파생한다.
 */
@Repository
public class PaymentRecordRepository {

    private static final String COLS =
            "id, contract_id, customer_name, amount, method, payment_date, status,"
            + " confirmed_at, rejected_at, reject_category, reject_reason";
    private static final String ALIASED_COLS =
            "pr.id, pr.contract_id, pr.customer_name, pr.amount, pr.method, pr.payment_date, pr.status,"
            + " pr.confirmed_at, pr.rejected_at, pr.reject_category, pr.reject_reason";

    private final SqlExecutor sql;

    public PaymentRecordRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public void save(PaymentRecord r) {
        Long contractId       = r.getContract() != null ? r.getContract().getId() : null;
        String customerName   = r.getContract() != null && r.getContract().getCustomer() != null
                ? r.getContract().getCustomer().getName() : null;
        String status         = r.getStatus() != null ? r.getStatus().name() : null;
        String rejectCategory = r.getRejectCategory() != null ? r.getRejectCategory().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO payment_records (contract_id, customer_name, amount, method, payment_date,"
                + " status, confirmed_at, rejected_at, reject_category, reject_reason)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?)",
                contractId, customerName, r.getAmount(), r.getMethod(), r.getPaymentDate(),
                status, r.getConfirmedAt(), r.getRejectedAt(), rejectCategory, r.getRejectReason());
        r.setId(id);
        r.setRecordNo("PRC" + String.format("%05d", id));
    }

    /** 확정/반려 후 상태 업데이트 */
    public void update(PaymentRecord r) {
        String status         = r.getStatus() != null ? r.getStatus().name() : null;
        String rejectCategory = r.getRejectCategory() != null ? r.getRejectCategory().name() : null;
        sql.executeUpdate(
                "UPDATE payment_records"
                + " SET status=?, confirmed_at=?, rejected_at=?, reject_category=?, reject_reason=?"
                + " WHERE id=?",
                status, r.getConfirmedAt(), r.getRejectedAt(), rejectCategory, r.getRejectReason(),
                r.getId());
    }

    public Optional<PaymentRecord> findById(Long id) {
        List<PaymentRecord> list = sql.executeQuery(
                "SELECT " + COLS + " FROM payment_records WHERE id=?",
                rowMapper(), id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<PaymentRecord> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM payment_records ORDER BY id DESC",
                rowMapper());
    }

    public List<PaymentRecord> findByContractNo(String contractNo) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM payment_records WHERE contract_id=? ORDER BY id DESC",
                rowMapper(), parseId(contractNo));
    }

    public List<PaymentRecord> findByStatus(PaymentRecordStatus status) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM payment_records WHERE status=? ORDER BY id DESC",
                rowMapper(), status.name());
    }

    public int countByFilters(String contractNo, PaymentRecordStatus status, String customerNo) {
        QueryParts query = buildFilterQuery(
                "SELECT COUNT(*) AS cnt FROM payment_records pr",
                contractNo,
                status,
                customerNo);
        return sql.queryOne(query.sql(), rs -> rs.getInt("cnt"), query.params().toArray());
    }

    public List<PaymentRecord> findPageByFilters(
            String contractNo, PaymentRecordStatus status, String customerNo, int limit, int offset) {
        QueryParts query = buildFilterQuery(
                "SELECT " + ALIASED_COLS + " FROM payment_records pr",
                contractNo,
                status,
                customerNo);
        String pageSql = query.sql() + " ORDER BY pr.id DESC LIMIT ? OFFSET ?";
        List<Object> params = new ArrayList<>(query.params());
        params.add(limit);
        params.add(offset);
        return sql.executeQuery(pageSql, rowMapper(), params.toArray());
    }

    private QueryParts buildFilterQuery(
            String selectSql, String contractNo, PaymentRecordStatus status, String customerNo) {
        StringBuilder query = new StringBuilder(selectSql);
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (customerNo != null) {
            query.append(" LEFT JOIN contracts c ON c.id = pr.contract_id");
            conditions.add("c.customer_id=?");
            params.add(customerNo);
        }
        if (contractNo != null && !contractNo.isBlank()) {
            conditions.add("pr.contract_id=?");
            params.add(parseId(contractNo));
        }
        if (status != null) {
            conditions.add("pr.status=?");
            params.add(status.name());
        }
        if (!conditions.isEmpty()) {
            query.append(" WHERE ").append(String.join(" AND ", conditions));
        }
        return new QueryParts(query.toString(), params);
    }

    private SqlExecutor.RowMapper<PaymentRecord> rowMapper() {
        return rs -> {
            long contractId = rs.getLong("contract_id");
            String cno = !rs.wasNull() ? "CON" + String.format("%05d", contractId) : null;
            String cname = rs.getString("customer_name");
            Customer custShell = cname != null ? new Customer("?", cname, null, null, null) : null;
            Contract contractShell = Contract.shellOf(cno, custShell, 0L);
            if (contractId > 0) contractShell.setId(contractId);

            String st = rs.getString("status");
            PaymentRecordStatus status = PaymentRecordStatus.WAITING;
            if (st != null) {
                try { status = PaymentRecordStatus.valueOf(st); }
                catch (IllegalArgumentException ignored) {}
            }
            java.sql.Date pd = rs.getDate("payment_date");
            LocalDate paymentDate = pd != null ? pd.toLocalDate() : LocalDate.now();

            PaymentRecord r = new PaymentRecord(
                    "PRC" + String.format("%05d", rs.getLong("id")), contractShell,
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

    private Long parseId(String businessNo) {
        return Long.parseLong(businessNo.replaceAll("\\D", ""));
    }

    private record QueryParts(String sql, List<Object> params) {
    }
}
