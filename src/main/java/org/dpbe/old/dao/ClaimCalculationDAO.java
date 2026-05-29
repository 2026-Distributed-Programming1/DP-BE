package org.dpbe.old.dao;

import org.dpbe.domain.claim.entity.ClaimCalculation;
import org.dpbe.domain.claim.entity.DamageInvestigation;
import org.dpbe.old.db.DBA;
import org.dpbe.domain.common.enums.CalculationStatus;
import java.util.List;

public class ClaimCalculationDAO {

    public static void save(ClaimCalculation c) {
        String invNo  = c.getInvestigation() != null ? c.getInvestigation().getInvestigationNo() : null;
        String status = c.getStatus() != null ? c.getStatus().name() : null;
        DBA.executeUpdate(
            "INSERT INTO claim_calculations (calculation_no, investigation_no, recognized_damage,"
            + " fault_ratio, final_amount, exceeded_deductible, adjusted, status)"
            + " VALUES (?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status), final_amount=VALUES(final_amount),"
            + " recognized_damage=VALUES(recognized_damage), fault_ratio=VALUES(fault_ratio),"
            + " exceeded_deductible=VALUES(exceeded_deductible), adjusted=VALUES(adjusted)",
            c.getCalculationNo(), invNo, c.getRecognizedDamage(),
            c.getFaultRatio(), c.getFinalAmount(),
            c.isExceededDeductible(), c.isAdjusted(), status);
    }

    public static List<ClaimCalculation> findAll() {
        return DBA.executeQuery(
            "SELECT calculation_no, investigation_no, recognized_damage, fault_ratio,"
            + " final_amount, exceeded_deductible, adjusted, status FROM claim_calculations",
            rs -> {
                String invNo = rs.getString("investigation_no");
                DamageInvestigation invShell = new DamageInvestigation(
                    invNo != null ? invNo : "?", null, null,
                    0, 0, 0, null);
                String st = rs.getString("status");
                CalculationStatus status = CalculationStatus.CALCULATED;
                if (st != null) {
                    try { status = CalculationStatus.valueOf(st); }
                    catch (IllegalArgumentException ignored) {}
                }
                return new ClaimCalculation(
                    rs.getString("calculation_no"),
                    invShell,
                    rs.getLong("recognized_damage"),
                    rs.getDouble("fault_ratio"),
                    rs.getLong("final_amount"),
                    rs.getBoolean("exceeded_deductible"),
                    rs.getBoolean("adjusted"),
                    status);
            });
    }
}