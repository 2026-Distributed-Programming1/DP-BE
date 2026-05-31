package org.dpbe.domain.sales.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.common.enums.InsuranceType;
import org.dpbe.domain.sales.entity.CustomerRegistration;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class CustomerRegistrationRepository {

    private static final String COLS =
            "id, customer_id, name, ssn_masked, phone, address,"
            + " insurance_type, contract_date, expiry_date, monthly_premium";

    private final SqlExecutor sql;

    public CustomerRegistrationRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<CustomerRegistration> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM customer_registrations ORDER BY id DESC",
                this::mapRow);
    }

    public void save(CustomerRegistration r) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO customer_registrations"
                + " (name, ssn, ssn_masked, phone, address,"
                + "  insurance_type, contract_date, expiry_date, monthly_premium)"
                + " VALUES (?,?,?,?,?,?,?,?,?)",
                r.getName(),
                r.getSsn(),
                r.getMaskedSsn(),
                r.getPhone(),
                r.getAddress(),
                r.getInsuranceType() != null ? r.getInsuranceType().name() : null,
                r.getContractDate(),
                r.getExpiryDate(),
                r.getMonthlyPremium());
        r.setId(id);
        r.setCustomerId("CRG" + String.format("%05d", id));
        sql.executeUpdate("UPDATE customer_registrations SET customer_id=? WHERE id=?",
                r.getCustomerId(), id);
    }

    private CustomerRegistration mapRow(ResultSet rs) throws SQLException {
        String it = rs.getString("insurance_type");
        InsuranceType insuranceType = null;
        if (it != null) {
            try { insuranceType = InsuranceType.valueOf(it); }
            catch (IllegalArgumentException ignored) {}
        }
        java.sql.Date cd = rs.getDate("contract_date");
        java.sql.Date ed = rs.getDate("expiry_date");
        CustomerRegistration r = new CustomerRegistration(
                rs.getString("customer_id"),
                null,
                rs.getString("name"),
                rs.getString("ssn_masked"),
                rs.getString("phone"),
                insuranceType,
                cd != null ? cd.toLocalDate() : null,
                ed != null ? ed.toLocalDate() : null,
                rs.getLong("monthly_premium"));
        r.setId(rs.getLong("id"));
        r.setAddress(rs.getString("address"));
        return r;
    }
}