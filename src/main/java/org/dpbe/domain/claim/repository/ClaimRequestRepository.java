package org.dpbe.domain.claim.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.claim.entity.ClaimRequest;
import org.dpbe.domain.common.entity.BankAccount;
import org.dpbe.domain.common.enums.ClaimRequestStatus;
import org.dpbe.domain.common.enums.ClaimType;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

/**
 * 보험금 청구 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id(AUTO_INCREMENT). claim_no는 저장하지 않고 id에서 format-on-read로 파생한다.
 */
@Repository
public class ClaimRequestRepository {

    private static final String COLS =
            "id, customer_id, customer_name, contract_id, claim_type, diagnosis,"
            + " claim_reasons, bank_name, account_no, account_holder, personal_info_agreed,"
            + " requested_at, status";

    private final SqlExecutor sql;

    public ClaimRequestRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 신규 청구 저장 — INSERT 후 생성 id에서 claim_no 파생. */
    public void save(ClaimRequest r) {
        String customerId   = r.getCustomer() != null ? r.getCustomer().getCustomerId() : null;
        String customerName = r.getCustomer() != null ? r.getCustomer().getName() : null;
        Long contractId     = r.getContract() != null ? r.getContract().getId() : null;
        String claimType    = r.getClaimType() != null ? r.getClaimType().name() : null;
        String reasons      = r.getClaimReasons() != null ? String.join(",", r.getClaimReasons()) : null;
        String bankName     = r.getBankAccount() != null ? r.getBankAccount().getBankName() : null;
        String accountNo    = r.getBankAccount() != null ? r.getBankAccount().getAccountNo() : null;
        String holder       = r.getBankAccount() != null ? r.getBankAccount().getAccountHolder() : null;
        String status       = r.getStatus() != null ? r.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO claim_requests (customer_id, customer_name, contract_id, claim_type,"
                + " diagnosis, claim_reasons, bank_name, account_no, account_holder,"
                + " personal_info_agreed, requested_at, status)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                customerId, customerName, contractId, claimType,
                r.getDiagnosis(), reasons, bankName, accountNo, holder,
                r.isPersonalInfoAgreed(), r.getRequestedAt(), status);
        r.setId(id);
        r.setClaimNo("CLM" + String.format("%05d", id));
    }

    public List<ClaimRequest> findAll() {
        return sql.executeQuery("SELECT " + COLS + " FROM claim_requests", this::mapRow);
    }

    public ClaimRequest findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM claim_requests WHERE id=?", this::mapRow, id);
    }

    private ClaimRequest mapRow(ResultSet rs) throws SQLException {
        String cid   = rs.getString("customer_id");
        String cname = rs.getString("customer_name");
        Customer customerShell = new Customer(
                cid != null ? cid : "?", cname != null ? cname : "", null, null, null);
        long contractId = rs.getLong("contract_id");
        Contract contractShell = !rs.wasNull()
                ? Contract.shellOf("CON" + String.format("%05d", contractId), customerShell, 0L)
                : null;
        if (contractShell != null) contractShell.setId(contractId);

        String st = rs.getString("status");
        ClaimRequestStatus status = ClaimRequestStatus.DRAFT;
        if (st != null) {
            try { status = ClaimRequestStatus.valueOf(st); }
            catch (IllegalArgumentException ignored) {}
        }
        ClaimRequest r = new ClaimRequest(
                "CLM" + String.format("%05d", rs.getLong("id")), customerShell, contractShell, status);
        r.setId(rs.getLong("id"));

        String ct = rs.getString("claim_type");
        if (ct != null) {
            try { r.selectClaimType(ClaimType.valueOf(ct)); }
            catch (IllegalArgumentException ignored) {}
        }
        String diag = rs.getString("diagnosis");
        if (diag != null) r.enterDiagnosis(diag);
        String reasons = rs.getString("claim_reasons");
        if (reasons != null && !reasons.isEmpty()) {
            r.selectClaimReasons(Arrays.asList(reasons.split(",")));
        }
        String bank = rs.getString("bank_name");
        if (bank != null) {
            BankAccount account = new BankAccount();
            account.enter(bank, rs.getString("account_no"), rs.getString("account_holder"));
            account.verify();
            r.selectExistingAccount(account);
        }
        if (rs.getBoolean("personal_info_agreed")) {
            r.agreePersonalInfoTerms();
        }
        java.sql.Timestamp rat = rs.getTimestamp("requested_at");
        if (rat != null) r.setRequestedAt(rat.toLocalDateTime());
        return r;
    }
}
