package org.dpbe.old.dao;

import org.dpbe.domain.consultation.entity.Revival;
import org.dpbe.old.db.DBA;

public class RevivalDAO {

    public static void save(Revival r) {
        DBA.executeUpdate(
            "INSERT INTO revivals (revival_no, contract_no, customer_name,"
            + " contact, unpaid_amount, payment_method, revived_at)"
            + " VALUES (?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE contact=VALUES(contact),"
            + " unpaid_amount=VALUES(unpaid_amount),"
            + " payment_method=VALUES(payment_method),"
            + " revived_at=VALUES(revived_at)",
            String.valueOf(r.getRevivalNumber()),
            r.getContractNo(),
            r.getCustomer() != null ? r.getCustomer().getName() : null,
            r.getContact(),
            r.getUnpaidAmount(),
            r.getPaymentMethod(),
            r.getAppliedAt());
    }
}