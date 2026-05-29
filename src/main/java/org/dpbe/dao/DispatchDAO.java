package org.dpbe.dao;

import org.dpbe.claim.AccidentReport;
import org.dpbe.claim.Dispatch;
import org.dpbe.db.DBA;
import org.dpbe.enums.DispatchStatus;
import java.util.List;

public class DispatchDAO {

    public static void save(Dispatch d) {
        DBA.executeUpdate(
            "INSERT INTO dispatches (dispatch_no, accident_no, status)"
            + " VALUES (?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status)",
            d.getDispatchNo(),
            d.getAccident() != null ? d.getAccident().getReportNo() : null,
            d.getStatus() != null ? d.getStatus().name() : null);
    }

    public static List<Dispatch> findAll() {
        return DBA.executeQuery(
            "SELECT dispatch_no, accident_no, status FROM dispatches",
            rs -> {
                String accNo = rs.getString("accident_no");
                AccidentReport shell = accNo != null ? new AccidentReport(accNo) : null;
                String st = rs.getString("status");
                DispatchStatus status = DispatchStatus.REQUESTED;
                if (st != null) {
                    try { status = DispatchStatus.valueOf(st); } catch (IllegalArgumentException ignored) {}
                }
                return new Dispatch(rs.getString("dispatch_no"), shell, status);
            });
    }
}