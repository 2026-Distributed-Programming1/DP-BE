package org.dpbe.old.dao;

import java.util.List;
import org.dpbe.domain.actor.Customer;
import org.dpbe.old.db.DBA;

public class CustomerDAO {

    public static void save(Customer c) {
        DBA.executeUpdate(
            "INSERT INTO customers (customer_id, name, resident_no, phone, email, address, birth_date)"
            + " VALUES (?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE name=VALUES(name), phone=VALUES(phone), email=VALUES(email),"
            + " address=VALUES(address), birth_date=VALUES(birth_date)",
            c.getCustomerId(), c.getName(), c.getResidentNo(),
            c.getContact(), c.getEmail(), c.getAddress(), c.getBirthDate());
    }

    public static List<Customer> findAll() {
        return DBA.executeQuery(
            "SELECT customer_id, name, resident_no, phone, email, address, birth_date, registered_at FROM customers",
            rs -> {
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
            });
    }

    public static Customer findById(String customerId) {
        return DBA.queryOne(
            "SELECT customer_id, name, resident_no, phone, email, address, birth_date, registered_at"
            + " FROM customers WHERE customer_id=?",
            rs -> {
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
            }, customerId);
    }
}