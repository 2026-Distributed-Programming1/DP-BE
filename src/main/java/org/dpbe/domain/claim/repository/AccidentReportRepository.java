package org.dpbe.domain.claim.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.claim.entity.AccidentReport;
import org.dpbe.domain.common.enums.AccidentType;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

/**
 * 사고 접수 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id. accident_no는 저장하지 않고 id에서 파생한다.
 */
@Repository
public class AccidentReportRepository {

    private static final String COLS =
            "id, customer_id, customer_name, accident_type, vehicle_no,"
            + " owner_name, phone_no, damage_type, location, needs_dispatch,"
            + " casualty_count, injury_severity, emergency_reported, reported_at, status";

    private final SqlExecutor sql;

    public AccidentReportRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 신규 사고 접수 저장 — INSERT 후 생성 id에서 accident_no 파생. */
    public void save(AccidentReport r) {
        String customerId   = r.getCustomer() != null ? r.getCustomer().getCustomerId() : null;
        String customerName = r.getCustomer() != null ? r.getCustomer().getName() : null;
        String accidentType = r.getAccidentType() != null ? r.getAccidentType().name() : null;
        String status       = r.getStatus() != null ? r.getStatus().name() : null;

        long id = sql.executeInsertReturningKey(
                "INSERT INTO accident_reports (customer_id, customer_name, accident_type,"
                + " vehicle_no, owner_name, phone_no, damage_type, location, needs_dispatch,"
                + " casualty_count, injury_severity, emergency_reported, reported_at, status)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                customerId, customerName, accidentType,
                r.getVehicleNo(), r.getOwnerName(), r.getPhoneNo(),
                r.getDamageType(), r.getLocation(), r.isNeedsDispatch(),
                r.getCasualtyCount(), r.getInjurySeverity(), r.isEmergencyReported(),
                r.getReportedAt(), status);
        r.setId(id);
        r.setReportNo("ACC" + String.format("%05d", id));
    }

    public List<AccidentReport> findAll() {
        return sql.executeQuery("SELECT " + COLS + " FROM accident_reports", this::mapRow);
    }

    public AccidentReport findById(Long id) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM accident_reports WHERE id=?", this::mapRow, id);
    }

    private AccidentReport mapRow(ResultSet rs) throws SQLException {
        AccidentReport r = new AccidentReport("ACC" + String.format("%05d", rs.getLong("id")));
        r.setId(rs.getLong("id"));
        String cid = rs.getString("customer_id");
        String cname = rs.getString("customer_name");
        if (cid != null || cname != null) {
            r.setCustomer(new Customer(
                    cid != null ? cid : "?", cname != null ? cname : "", null, null, null));
        }
        r.enterVehicleInfo(rs.getString("vehicle_no"), rs.getString("owner_name"), rs.getString("phone_no"));
        String at = rs.getString("accident_type");
        if (at != null) {
            try { r.selectAccidentType(AccidentType.valueOf(at), rs.getString("damage_type")); }
            catch (IllegalArgumentException ignored) {}
        }
        r.enterLocation(rs.getString("location"));
        r.setDispatchOption(rs.getBoolean("needs_dispatch"));
        r.enterCasualtyInfo(rs.getInt("casualty_count"),
                rs.getString("injury_severity"), rs.getBoolean("emergency_reported"));
        return r;
    }
}
