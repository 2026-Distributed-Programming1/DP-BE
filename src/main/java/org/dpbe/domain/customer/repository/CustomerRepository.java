package org.dpbe.domain.customer.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.dpbe.domain.actor.Customer;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

/**
 * 고객 조회 리포지토리 (Spring 트랜잭션 통합 경로).
 */
@Repository
public class CustomerRepository {

    private final SqlExecutor sql;

    public CustomerRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public Customer findById(String customerId) {
        return sql.queryOne(
                "SELECT id, customer_id, name, resident_no, phone, email, address, birth_date, registered_at"
                + " FROM customers WHERE customer_id=?",
                this::mapRow, customerId);
    }

    public List<Customer> findAll() {
        return sql.executeQuery(
                "SELECT id, customer_id, name, resident_no, phone, email, address, birth_date, registered_at"
                + " FROM customers",
                this::mapRow);
    }

    public int countByKeyword(String keyword) {
        if (keyword == null) {
            Integer count = sql.queryOne(
                    "SELECT COUNT(*) AS cnt FROM customers",
                    rs -> rs.getInt("cnt"));
            return count != null ? count : 0;
        }

        String like = "%" + keyword + "%";
        Integer count = sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM customers"
                + " WHERE name LIKE ? OR customer_id LIKE ? OR phone LIKE ?",
                rs -> rs.getInt("cnt"),
                like, like, like);
        return count != null ? count : 0;
    }

    public List<Customer> findByKeyword(String keyword, int limit, int offset) {
        if (keyword == null) {
            return sql.executeQuery(
                    "SELECT id, customer_id, name, resident_no, phone, email, address, birth_date, registered_at"
                    + " FROM customers ORDER BY id DESC LIMIT ? OFFSET ?",
                    this::mapRow, limit, offset);
        }

        String like = "%" + keyword + "%";
        return sql.executeQuery(
                "SELECT id, customer_id, name, resident_no, phone, email, address, birth_date, registered_at"
                + " FROM customers"
                + " WHERE name LIKE ? OR customer_id LIKE ? OR phone LIKE ?"
                + " ORDER BY id DESC LIMIT ? OFFSET ?",
                this::mapRow, like, like, like, limit, offset);
    }

    public void save(Customer c) {
        sql.executeUpdate(
                "INSERT INTO customers (customer_id, name, resident_no, phone, email, address, birth_date)"
                + " VALUES (?,?,?,?,?,?,?)"
                + " ON DUPLICATE KEY UPDATE name=VALUES(name), phone=VALUES(phone), email=VALUES(email),"
                + " address=VALUES(address), birth_date=VALUES(birth_date)",
                c.getCustomerId(), c.getName(), c.getResidentNo(),
                c.getContact(), c.getEmail(), c.getAddress(), c.getBirthDate());
    }

    public Customer saveNew(Customer c) {
        String temporaryCustomerId = "TMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 17);
        long id = sql.executeInsertReturningKey(
                "INSERT INTO customers (customer_id, name, resident_no, phone, email, address, birth_date)"
                + " VALUES (?,?,?,?,?,?,?)",
                temporaryCustomerId, c.getName(), c.getResidentNo(), c.getContact(),
                c.getEmail(), c.getAddress(), c.getBirthDate());

        String customerId = formatCustomerId(id);
        sql.executeUpdate("UPDATE customers SET customer_id=? WHERE id=?", customerId, id);
        c.setId(id);
        c.setCustomerId(customerId);
        return c;
    }

    public boolean existsByResidentNo(String residentNo) {
        Integer count = sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM customers WHERE resident_no=?",
                rs -> rs.getInt("cnt"),
                residentNo);
        return count != null && count > 0;
    }

    public boolean existsByPhone(String phone) {
        Integer count = sql.queryOne(
                "SELECT COUNT(*) AS cnt FROM customers WHERE phone=?",
                rs -> rs.getInt("cnt"),
                phone);
        return count != null && count > 0;
    }

    private String formatCustomerId(long id) {
        return String.format("CUS%05d", id);
    }

    private Customer mapRow(ResultSet rs) throws SQLException {
        Customer c = new Customer(
                rs.getString("customer_id"),
                rs.getString("name"),
                rs.getString("resident_no"),
                rs.getString("phone"),
                rs.getString("email"));
        c.setId(rs.getLong("id"));
        String addr = rs.getString("address");
        if (addr != null) c.enterAddress(addr);
        java.sql.Date bd = rs.getDate("birth_date");
        if (bd != null) c.enterBirthDate(bd.toLocalDate());
        java.sql.Timestamp rat = rs.getTimestamp("registered_at");
        if (rat != null) c.setRegisteredAt(rat.toLocalDateTime());
        return c;
    }
}
