package org.dpbe.domain.claim.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.actor.ClaimsHandler;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.claim.entity.ClaimRequest;
import org.dpbe.domain.claim.entity.DamageInvestigation;
import org.dpbe.domain.common.enums.ClaimRequestStatus;
import org.dpbe.domain.common.enums.InvestigationResult;
import org.dpbe.domain.common.enums.InvestigationStatus;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

/**
 * 손해 조사 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id. investigation_no는 INSERT 후 id에서 파생(INV+%05d).
 * 매핑은 기존 {@code DamageInvestigationDAO}를 정답지로 따르되 id를 포함한다.
 */
@Repository
public class DamageInvestigationRepository {

    private static final String COLS =
            "id, investigation_no, claim_no, claim_customer, customer_id, handler_emp_id,"
            + " handler_name, our_fault_ratio, counter_ratio, recognized_damage, opinion,"
            + " result, reject_reason, investigated_at, status";

    private final SqlExecutor sql;

    public DamageInvestigationRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 신규 조사 저장 — INSERT 후 생성 id에서 investigation_no 파생. */
    public void save(DamageInvestigation inv) {
        String claimNo  = inv.getClaim() != null ? inv.getClaim().getClaimNo() : null;
        String claimCus = inv.getClaim() != null && inv.getClaim().getCustomer() != null
                ? inv.getClaim().getCustomer().getName() : null;
        String customerId = inv.getClaim() != null && inv.getClaim().getCustomer() != null
                ? inv.getClaim().getCustomer().getCustomerId() : null;
        String handlerId = inv.getHandler() != null ? inv.getHandler().getEmployeeId() : null;
        String handlerName = inv.getHandler() != null ? inv.getHandler().getName() : null;
        String result    = inv.getResult() != null ? inv.getResult().name() : null;
        String status    = inv.getStatus() != null ? inv.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO damage_investigations (claim_no, claim_customer, customer_id,"
                + " handler_emp_id, handler_name, our_fault_ratio, counter_ratio,"
                + " recognized_damage, opinion, result, reject_reason, investigated_at, status)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                claimNo, claimCus, customerId, handlerId, handlerName,
                inv.getOurFaultRatio(), inv.getCounterFaultRatio(),
                inv.getRecognizedDamage(), inv.getOpinion(),
                result, inv.getRejectReason(), inv.getInvestigatedAt(), status);
        inv.setId(id);
        inv.setInvestigationNo("INV" + String.format("%05d", id));
        sql.executeUpdate("UPDATE damage_investigations SET investigation_no=? WHERE id=?",
                inv.getInvestigationNo(), id);
    }

    public DamageInvestigation findByClaimNo(String claimNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM damage_investigations WHERE claim_no=?",
                this::mapRow, claimNo);
    }

    public DamageInvestigation findByInvestigationNo(String investigationNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM damage_investigations WHERE investigation_no=?",
                this::mapRow, investigationNo);
    }

    public List<DamageInvestigation> findAll() {
        return sql.executeQuery("SELECT " + COLS + " FROM damage_investigations", this::mapRow);
    }

    private DamageInvestigation mapRow(ResultSet rs) throws SQLException {
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
        inv.setId(rs.getLong("id"));

        String hid = rs.getString("handler_emp_id");
        String hname = rs.getString("handler_name");
        if (hid != null || hname != null) {
            ClaimsHandler handlerShell = new ClaimsHandler(
                    hid != null ? hid : "?", hname != null ? hname : "", "", "", 0L);
            inv.setHandlerShell(handlerShell);
        }
        String op = rs.getString("opinion");
        if (op != null) inv.enterOpinion(op);
        String res = rs.getString("result");
        if (res != null) {
            try { inv.selectResult(InvestigationResult.valueOf(res)); }
            catch (IllegalArgumentException ignored) {}
        }
        String rr = rs.getString("reject_reason");
        if (rr != null) inv.setRejectReason(rr);
        java.sql.Timestamp iat = rs.getTimestamp("investigated_at");
        if (iat != null) inv.setInvestigatedAt(iat.toLocalDateTime());
        return inv;
    }
}