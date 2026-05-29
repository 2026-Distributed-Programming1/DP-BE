package org.dpbe.old.dao;

import java.util.List;
import org.dpbe.old.db.DBA;
import org.dpbe.domain.common.enums.InsuranceType;
import org.dpbe.domain.sales.entity.CustomerRegistration;

public class CustomerRegistrationDAO {

    public static void save(CustomerRegistration r) {
        String insuranceType = r.getInsuranceType() != null ? r.getInsuranceType().name() : null;
        DBA.executeUpdate(
            "INSERT INTO customer_registrations (customer_id, name, ssn, ssn_masked, phone,"
            + " address, insurance_type, contract_date, expiry_date, monthly_premium)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE name=VALUES(name), ssn=VALUES(ssn),"
            + " ssn_masked=VALUES(ssn_masked), phone=VALUES(phone),"
            + " address=VALUES(address),"
            + " insurance_type=VALUES(insurance_type)",
            r.getCustomerId(),
            r.getName(),
            r.getSsn(),
            r.getMaskedSsn(),
            r.getPhone(),
            r.getAddress(),
            insuranceType,
            r.getContractDate(),
            r.getExpiryDate(),
            r.getMonthlyPremium());
    }

    public static List<CustomerRegistration> findAll() {
        return DBA.executeQuery(
            "SELECT customer_id, name, ssn, ssn_masked, phone, address, insurance_type,"
            + " contract_date, expiry_date, monthly_premium FROM customer_registrations",
            rs -> {
                String it = rs.getString("insurance_type");
                InsuranceType insuranceType = null;
                if (it != null) {
                    try { insuranceType = InsuranceType.valueOf(it); }
                    catch (IllegalArgumentException ignored) {}
                }
                java.sql.Date cd = rs.getDate("contract_date");
                java.sql.Date ed = rs.getDate("expiry_date");
                String ssn = rs.getString("ssn");
                if (ssn == null) ssn = rs.getString("ssn_masked");
                CustomerRegistration r = new CustomerRegistration(
                        rs.getString("customer_id"),
                        null,
                        rs.getString("name"),
                        ssn,
                        rs.getString("phone"),
                        insuranceType,
                        cd != null ? cd.toLocalDate() : null,
                        ed != null ? ed.toLocalDate() : null,
                        rs.getLong("monthly_premium"));
                r.setAddress(rs.getString("address"));
                return r;
            });
    }
}