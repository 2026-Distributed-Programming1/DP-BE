package org.dpbe.dao;

import org.dpbe.actor.Customer;
import org.dpbe.claim.ClaimRequest;
import org.dpbe.claim.DamageInvestigation;
import org.dpbe.db.DBA;
import org.dpbe.enums.ClaimRequestStatus;
import org.dpbe.enums.InvestigationStatus;
import java.util.List;

public class DamageInvestigationDAO {

    public static void save(DamageInvestigation inv) {
        String claimNo  = inv.getClaim() != null ? inv.getClaim().getClaimNo() : null;
        String claimCus = inv.getClaim() != null && inv.getClaim().getCustomer() != null
                ? inv.getClaim().getCustomer().getName() : null;
        String customerId = inv.getClaim() != null && inv.getClaim().getCustomer() != null
                ? inv.getClaim().getCustomer().getCustomerId() : null;
        String handlerId = inv.getHandler() != null ? inv.getHandler().getEmployeeId() : null;
        String handlerName = inv.getHandler() != null ? inv.getHandler().getName() : null;
        String result    = inv.getResult() != null ? inv.getResult().name() : null;
        String status    = inv.getStatus() != null ? inv.getStatus().name() : null;
        DBA.executeUpdate(
            "INSERT INTO damage_investigations (investigation_no, claim_no, claim_customer,"
            + " customer_id, handler_emp_id, handler_name, our_fault_ratio, counter_ratio,"
            + " recognized_damage, opinion, result, reject_reason, investigated_at, status)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status), result=VALUES(result),"
            + " recognized_damage=VALUES(recognized_damage), investigated_at=VALUES(investigated_at),"
            + " opinion=VALUES(opinion), reject_reason=VALUES(reject_reason),"
            + " our_fault_ratio=VALUES(our_fault_ratio), counter_ratio=VALUES(counter_ratio)",
            inv.getInvestigationNo(), claimNo, claimCus,
            customerId, handlerId, handlerName,
            inv.getOurFaultRatio(), inv.getCounterFaultRatio(),
            inv.getRecognizedDamage(), inv.getOpinion(),
            result, inv.getRejectReason(), inv.getInvestigatedAt(), status);
    }

    public static List<DamageInvestigation> findAll() {
        return DBA.executeQuery(
            "SELECT investigation_no, claim_no, claim_customer, customer_id,"
            + " handler_emp_id, handler_name,"
            + " our_fault_ratio, counter_ratio, recognized_damage, opinion,"
            + " result, reject_reason, investigated_at, status FROM damage_investigations",
            rs -> {
                String cn    = rs.getString("claim_no");
                String cname = rs.getString("claim_customer");
                String cid   = rs.getString("customer_id");
                Customer custShell = new Customer(
                    cid != null ? cid : "?", cname != null ? cname : "", null, null, null);
                ClaimRequest claimShell = new ClaimRequest(
                    cn != null ? cn : "?", custShell, null, ClaimRequestStatus.RECEIVED);
                String st = rs.getString("status");
                InvestigationStatus status = InvestigationStatus.NEW_ASSIGNED;
                if (st != null) {
                    try { status = InvestigationStatus.valueOf(st); }
                    catch (IllegalArgumentException ignored) {}
                }
                DamageInvestigation inv = new DamageInvestigation(
                    rs.getString("investigation_no"),
                    claimShell,
                    rs.getString("handler_name"),
                    rs.getDouble("our_fault_ratio"),
                    rs.getDouble("counter_ratio"),
                    rs.getLong("recognized_damage"),
                    status);
                String hid = rs.getString("handler_emp_id");
                String hname = rs.getString("handler_name");
                if (hid != null || hname != null) {
                  org.dpbe.actor.ClaimsHandler handlerShell = new org.dpbe.actor.ClaimsHandler(
                        hid != null ? hid : "?", hname != null ? hname : "", "", "", 0L);
                    inv.setHandlerShell(handlerShell);
                }
                String op = rs.getString("opinion");
                if (op != null) inv.enterOpinion(op);
                String res = rs.getString("result");
                if (res != null) {
                    try { inv.selectResult(org.dpbe.enums.InvestigationResult.valueOf(res)); }
                    catch (IllegalArgumentException ignored) {}
                }
                String rr = rs.getString("reject_reason");
                if (rr != null) inv.setRejectReason(rr);
                java.sql.Timestamp iat = rs.getTimestamp("investigated_at");
                if (iat != null) inv.setInvestigatedAt(iat.toLocalDateTime());
                return inv;
            });
    }
}