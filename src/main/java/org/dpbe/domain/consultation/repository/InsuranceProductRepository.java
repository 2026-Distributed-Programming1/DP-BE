package org.dpbe.domain.consultation.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.dpbe.domain.consultation.entity.InsuranceProduct;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class InsuranceProductRepository {

    private static final String COLS =
            "id, product_name, category, monthly_premium, coverage_summary, exclusion_summary";

    private final SqlExecutor sql;

    public InsuranceProductRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<InsuranceProduct> findAll() {
        return sql.executeQuery("SELECT " + COLS + " FROM insurance_products ORDER BY product_name", this::mapRow);
    }

    public InsuranceProduct findByProductName(String productName) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM insurance_products WHERE product_name=?",
                this::mapRow, productName);
    }

    /** 보험상품 저장 — 자연키(product_name) upsert. */
    public void save(InsuranceProduct p) {
        sql.executeUpdate(
                "INSERT INTO insurance_products"
                + " (product_name, category, monthly_premium, coverage_summary, exclusion_summary)"
                + " VALUES (?,?,?,?,?)"
                + " ON DUPLICATE KEY UPDATE"
                + " category=VALUES(category), monthly_premium=VALUES(monthly_premium),"
                + " coverage_summary=VALUES(coverage_summary), exclusion_summary=VALUES(exclusion_summary)",
                p.getProductName(), p.getType(), p.getMonthlyPremium(),
                p.getCoverage(), p.getSpecialTerms());
    }

    private InsuranceProduct mapRow(ResultSet rs) throws SQLException {
        InsuranceProduct p = new InsuranceProduct(
                rs.getString("product_name"),
                rs.getString("category"),
                rs.getLong("monthly_premium"),
                rs.getString("coverage_summary"),
                rs.getString("exclusion_summary"));
        long id = rs.getLong("id");
        if (!rs.wasNull()) p.setId(id);
        return p;
    }
}