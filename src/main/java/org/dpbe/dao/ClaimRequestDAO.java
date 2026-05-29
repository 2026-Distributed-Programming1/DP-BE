package org.dpbe.dao;

import org.dpbe.actor.Customer;
import org.dpbe.claim.ClaimRequest;
import org.dpbe.contract.Contract;
import org.dpbe.db.DBA;
import org.dpbe.enums.ClaimRequestStatus;
import org.dpbe.enums.ClaimType;
import java.util.Arrays;
import java.util.List;

public class ClaimRequestDAO {

    public static void save(ClaimRequest r) {
        String customerId   = r.getCustomer() != null ? r.getCustomer().getCustomerId() : null;
        String customerName = r.getCustomer() != null ? r.getCustomer().getName() : null;
        String contractNo   = r.getContract() != null ? r.getContract().getContractNo() : null;
        String claimType    = r.getClaimType() != null ? r.getClaimType().name() : null;
        String reasons      = r.getClaimReasons() != null ? String.join(",", r.getClaimReasons()) : null;
        String bankName     = r.getBankAccount() != null ? r.getBankAccount().getBankName() : null;
        String accountNo    = r.getBankAccount() != null ? r.getBankAccount().getAccountNo() : null;
        String holder       = r.getBankAccount() != null ? r.getBankAccount().getAccountHolder() : null;
        String status       = r.getStatus() != null ? r.getStatus().name() : null;
        DBA.executeUpdate(
            "INSERT INTO claim_requests (claim_no, customer_id, customer_name, contract_no,"
            + " claim_type, diagnosis, claim_reasons, bank_name, account_no, account_holder,"
            + " requested_at, status)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status), requested_at=VALUES(requested_at)",
            r.getClaimNo(), customerId, customerName, contractNo,
            claimType, r.getDiagnosis(), reasons,
            bankName, accountNo, holder,
            r.getRequestedAt(), status);
    }

    public static List<ClaimRequest> findAll() {
        return DBA.executeQuery(
            "SELECT claim_no, customer_id, customer_name, contract_no, claim_type,"
            + " diagnosis, claim_reasons, bank_name, account_no, account_holder, requested_at, status FROM claim_requests",
            rs -> {
                String cid  = rs.getString("customer_id");
                String cname = rs.getString("customer_name");
                Customer customerShell = new Customer(
                    cid != null ? cid : "?",
                    cname != null ? cname : "",
                    null, null, null);
                String cno = rs.getString("contract_no");
                Contract contractShell = cno != null
                        ? Contract.shellOf(cno, customerShell, 0L)
                        : null;
                String st = rs.getString("status");
                ClaimRequestStatus status = ClaimRequestStatus.DRAFT;
                if (st != null) {
                    try { status = ClaimRequestStatus.valueOf(st); }
                    catch (IllegalArgumentException ignored) {}
                }
                ClaimRequest r = new ClaimRequest(
                    rs.getString("claim_no"), customerShell, contractShell, status);
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
                String accNo = rs.getString("account_no");
                String holder = rs.getString("account_holder");
                if (bank != null) {
                    org.dpbe.common.BankAccount bankAccount = new org.dpbe.common.BankAccount();
                    bankAccount.enter(bank, accNo, holder);
                    bankAccount.verify();
                    r.selectExistingAccount(bankAccount);
                }
                java.sql.Timestamp rat = rs.getTimestamp("requested_at");
                if (rat != null) r.setRequestedAt(rat.toLocalDateTime());
                return r;
            });
    }

    public static boolean existsByClaimNo(String claimNo) {
        return DBA.exists(
            "SELECT 1 FROM claim_requests WHERE claim_no=?", claimNo);
    }
}