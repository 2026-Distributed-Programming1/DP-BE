package org.dpbe.domain.claim.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.claim.entity.ClaimCalculation;
import org.dpbe.domain.claim.entity.DamageInvestigation;
import org.dpbe.domain.common.enums.CalculationStatus;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

/**
 * 보험금 산출 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id. calculation_no는 INSERT 후 id에서 파생(CAL+%05d).
 * 매핑은 기존 {@code ClaimCalculationDAO}를 정답지로 따르되 id와 산출 파라미터
 * (deductible·coverage_limit)를 포함한다 — 행이 자기 산출 결과를 설명하도록 영속화한다.
 */
@Repository
public class ClaimCalculationRepository {

    private static final String COLS =
            "id, calculation_no, investigation_no, recognized_damage, fault_ratio,"
            + " deductible, coverage_limit, final_amount, exceeded_deductible, adjusted, status";

    private final SqlExecutor sql;

    public ClaimCalculationRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 신규 산출 저장 — INSERT 후 생성 id에서 calculation_no 파생. */
    public void save(ClaimCalculation c) {
        String invNo  = c.getInvestigation() != null ? c.getInvestigation().getInvestigationNo() : null;
        String status = c.getStatus() != null ? c.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO claim_calculations (investigation_no, recognized_damage, fault_ratio,"
                + " deductible, coverage_limit, final_amount, exceeded_deductible, adjusted, status)"
                + " VALUES (?,?,?,?,?,?,?,?,?)",
                invNo, c.getRecognizedDamage(), c.getFaultRatio(),
                c.getDeductible(), c.getCoverageLimit(), c.getFinalAmount(),
                c.isExceededDeductible(), c.isAdjusted(), status);
        c.setId(id);
        c.setCalculationNo("CAL" + String.format("%05d", id));
        sql.executeUpdate("UPDATE claim_calculations SET calculation_no=? WHERE id=?",
                c.getCalculationNo(), id);
    }

    /** 상태 갱신 (승인 등 전이 반영). */
    public void updateStatus(ClaimCalculation c) {
        String status = c.getStatus() != null ? c.getStatus().name() : null;
        sql.executeUpdate("UPDATE claim_calculations SET status=? WHERE calculation_no=?",
                status, c.getCalculationNo());
    }

    public ClaimCalculation findByInvestigationNo(String investigationNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM claim_calculations WHERE investigation_no=?",
                this::mapRow, investigationNo);
    }

    public ClaimCalculation findByCalculationNo(String calculationNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM claim_calculations WHERE calculation_no=?",
                this::mapRow, calculationNo);
    }

    public List<ClaimCalculation> findAll() {
        return sql.executeQuery("SELECT " + COLS + " FROM claim_calculations", this::mapRow);
    }

    private ClaimCalculation mapRow(ResultSet rs) throws SQLException {
        String invNo = rs.getString("investigation_no");
        DamageInvestigation invShell = new DamageInvestigation(
                invNo != null ? invNo : "?", null, null, 0, 0, 0, null);

        String st = rs.getString("status");
        CalculationStatus status = CalculationStatus.CALCULATED;
        if (st != null) {
            try { status = CalculationStatus.valueOf(st); }
            catch (IllegalArgumentException ignored) {}
        }
        ClaimCalculation c = new ClaimCalculation(
                rs.getString("calculation_no"),
                invShell,
                rs.getLong("recognized_damage"),
                rs.getDouble("fault_ratio"),
                rs.getLong("final_amount"),
                rs.getBoolean("exceeded_deductible"),
                rs.getBoolean("adjusted"),
                status);
        c.setId(rs.getLong("id"));
        c.setDeductible(rs.getLong("deductible"));
        c.setCoverageLimit(rs.getLong("coverage_limit"));
        return c;
    }
}
