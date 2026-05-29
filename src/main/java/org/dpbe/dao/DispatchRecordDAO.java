package org.dpbe.dao;

import org.dpbe.claim.Dispatch;
import org.dpbe.claim.DispatchRecord;
import org.dpbe.db.DBA;
import org.dpbe.enums.DispatchRecordStatus;
import java.util.List;

public class DispatchRecordDAO {

    public static void save(DispatchRecord r) {
        DBA.executeUpdate(
            "INSERT INTO dispatch_records (record_no, dispatch_no, agent_name,"
            + " police_required, towing_required, notes, transmitted_at, status)"
            + " VALUES (?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status),"
            + " notes=VALUES(notes), transmitted_at=VALUES(transmitted_at)",
            r.getRecordId(),
            r.getDispatch() != null ? r.getDispatch().getDispatchNo() : null,
            r.getDispatch() != null && r.getDispatch().getAgent() != null
                ? r.getDispatch().getAgent().getName() : null,
            r.isPoliceRequired(), r.isTowingRequired(),
            r.getNotes(), r.getTransmittedAt(),
            r.getStatus() != null ? r.getStatus().name() : null);
    }

    public static List<DispatchRecord> findAll() {
        return DBA.executeQuery(
            "SELECT record_no, dispatch_no, agent_name, police_required, towing_required,"
            + " notes, transmitted_at, status FROM dispatch_records",
            rs -> {
                String dno = rs.getString("dispatch_no");
                Dispatch dispatchShell = dno != null ? new Dispatch(dno, null, null) : null;
                DispatchRecord rec = new DispatchRecord(dispatchShell);
                rec.setPoliceRequired(rs.getBoolean("police_required"));
                rec.setTowingRequired(rs.getBoolean("towing_required"));
                String n = rs.getString("notes");
                if (n != null) rec.enterNotes(n);
                String st = rs.getString("status");
                if (st != null) {
                    try { rec.setStatus(DispatchRecordStatus.valueOf(st)); }
                    catch (IllegalArgumentException ignored) {}
                }
                return rec;
            });
    }
}