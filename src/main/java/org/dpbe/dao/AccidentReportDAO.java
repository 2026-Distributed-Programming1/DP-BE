package org.dpbe.dao;

import org.dpbe.actor.Customer;
import org.dpbe.claim.AccidentReport;
import org.dpbe.db.DBA;
import org.dpbe.enums.AccidentType;
import java.util.List;

public class AccidentReportDAO {

    public static void save(AccidentReport r) {
        DBA.executeUpdate(
            "INSERT INTO accident_reports (accident_no, customer_id, customer_name,"
            + " accident_type, vehicle_no, owner_name, phone_no, damage_type, location,"
            + " needs_dispatch, casualty_count, injury_severity, emergency_reported,"
            + " reported_at, status)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status),"
            + " location=VALUES(location), damage_type=VALUES(damage_type)",
            r.getReportNo(),
            r.getCustomer() != null ? r.getCustomer().getCustomerId() : null,
            r.getCustomer() != null ? r.getCustomer().getName() : null,
            r.getAccidentType() != null ? r.getAccidentType().name() : null,
            r.getVehicleNo(), r.getOwnerName(), r.getPhoneNo(),
            r.getDamageType(), r.getLocation(),
            r.isNeedsDispatch(),
            r.getCasualtyCount(), r.getInjurySeverity(), r.isEmergencyReported(),
            r.getReportedAt(),
            r.getStatus() != null ? r.getStatus().name() : null);
    }

    public static List<AccidentReport> findAll() {
        return DBA.executeQuery(
            "SELECT accident_no, customer_id, customer_name, accident_type,"
            + " vehicle_no, owner_name, phone_no, damage_type, location, needs_dispatch"
            + " FROM accident_reports",
            rs -> {
                AccidentReport r = new AccidentReport(rs.getString("accident_no"));
                String cid = rs.getString("customer_id");
                String cname = rs.getString("customer_name");
                Customer custShell = new Customer(
                    cid != null ? cid : "?", cname != null ? cname : "", null, null, null);
                r.enterVehicleInfo(
                    rs.getString("vehicle_no"),
                    rs.getString("owner_name"),
                    rs.getString("phone_no"));
                String at = rs.getString("accident_type");
                if (at != null) {
                    try { r.selectAccidentType(AccidentType.valueOf(at), rs.getString("damage_type")); }
                    catch (IllegalArgumentException ignored) {}
                }
                r.enterLocation(rs.getString("location"));
                r.setDispatchOption(rs.getBoolean("needs_dispatch"));
                return r;
            });
    }
}