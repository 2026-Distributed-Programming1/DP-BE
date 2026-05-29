package org.dpbe.dao;

import org.dpbe.actor.Customer;
import org.dpbe.db.DBA;
import org.dpbe.enums.PaymentStatus;
import org.dpbe.payment.Payment;
import org.dpbe.payment.PaymentItem;
import java.util.List;

public class PaymentDAO {

    public static void save(Payment p) {
        String customerId   = p.getCustomer() != null ? p.getCustomer().getCustomerId() : null;
        String customerName = p.getCustomer() != null ? p.getCustomer().getName() : null;
        String method       = p.getPaymentMethod() != null ? p.getPaymentMethod().name() : null;
        String status       = p.getStatus() != null ? p.getStatus().name() : null;
        DBA.executeUpdate(
            "INSERT INTO payments (payment_no, customer_id, customer_name, total_amount,"
            + " payment_method, requested_at, status)"
            + " VALUES (?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE status=VALUES(status)",
            p.getPaymentNo(), customerId, customerName,
            p.getDiscountedAmount(), method, p.getRequestedAt(), status);

        for (PaymentItem item : p.getItems()) {
            String contractNo = item.getContract() != null ? item.getContract().getContractNo() : null;
            DBA.executeUpdate(
                "INSERT INTO payment_items (payment_no, contract_no, count, subtotal)"
                + " VALUES (?,?,?,?)",
                p.getPaymentNo(), contractNo, item.getCount(), item.getSubtotal());
        }
    }

    public static List<Payment> findAll() {
        return DBA.executeQuery(
            "SELECT payment_no, customer_id, customer_name, total_amount,"
            + " payment_method, requested_at, status FROM payments",
            rs -> {
                String cid  = rs.getString("customer_id");
                String cname = rs.getString("customer_name");
                Customer custShell = new Customer(
                    cid != null ? cid : "?", cname != null ? cname : "", null, null, null);
                Payment pay = new Payment(custShell);
                pay.setPaymentNo(rs.getString("payment_no"));
                String method = rs.getString("payment_method");
                if (method != null) {
                    try { pay.setPaymentMethod(org.dpbe.enums.PaymentMethod.valueOf(method)); }
                    catch (IllegalArgumentException ignored) {}
                }
                java.sql.Timestamp rat = rs.getTimestamp("requested_at");
                if (rat != null) pay.setRequestedAt(rat.toLocalDateTime());
                pay.setDiscountedAmount(rs.getLong("total_amount"));
                String st = rs.getString("status");
                if (st != null) {
                    try { pay.setStatus(PaymentStatus.valueOf(st)); }
                    catch (IllegalArgumentException ignored) {}
                }
                return pay;
            });
    }
}