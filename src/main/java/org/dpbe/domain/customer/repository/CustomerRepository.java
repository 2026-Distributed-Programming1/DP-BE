package org.dpbe.domain.customer.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import org.dpbe.domain.actor.Customer;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

/**
 * 고객 조회 리포지토리 (Spring 트랜잭션 통합 경로).
 * 기존 {@code CustomerDAO.findById}와 동일하나 SqlExecutor 경유.
 */
@Repository
public class CustomerRepository {

    private final SqlExecutor sql;

    public CustomerRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public Customer findById(String customerId) {
        return sql.queryOne(
                "SELECT customer_id, name, resident_no, phone, email, address, birth_date, registered_at"
                + " FROM customers WHERE customer_id=?",
                this::mapRow, customerId);
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer(
                rs.getString("customer_id"),
                rs.getString("name"),
                rs.getString("resident_no"),
                rs.getString("phone"),
                rs.getString("email"));
        String addr = rs.getString("address");
        if (addr != null) c.enterAddress(addr);
        java.sql.Date bd = rs.getDate("birth_date");
        if (bd != null) c.enterBirthDate(bd.toLocalDate());
        java.sql.Timestamp rat = rs.getTimestamp("registered_at");
        if (rat != null) c.setRegisteredAt(rat.toLocalDateTime());
        return c;
    }
}