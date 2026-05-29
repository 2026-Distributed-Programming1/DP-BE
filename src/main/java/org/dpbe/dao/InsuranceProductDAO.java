package org.dpbe.dao;

import java.util.List;
import org.dpbe.consultation.InsuranceProduct;
import org.dpbe.db.DBA;

public class InsuranceProductDAO {

    public static void save(InsuranceProduct p) {
        DBA.executeUpdate(
            "INSERT INTO insurance_products (product_name, category, monthly_premium,"
            + " coverage_summary, exclusion_summary)"
            + " VALUES (?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE monthly_premium=VALUES(monthly_premium),"
            + " coverage_summary=VALUES(coverage_summary)",
            p.getProductName(), p.getType(), p.getMonthlyPremium(),
            p.getCoverage(), p.getSpecialTerms());
    }

    public static List<InsuranceProduct> findAll() {
        return DBA.executeQuery(
            "SELECT product_name, category, monthly_premium, coverage_summary, exclusion_summary"
            + " FROM insurance_products",
            rs -> new InsuranceProduct(
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getLong("monthly_premium"),
                rs.getString("coverage_summary"),
                rs.getString("exclusion_summary")));
    }
}