package org.dpbe.dao;

import org.dpbe.contract.ExpiringContractManagement;
import org.dpbe.db.DBA;

public class ExpiringContractManagementDAO {

    public static void save(ExpiringContractManagement m) {
        String noticeNo = "ECN-" + m.getContractNo()
                + (m.getNoticeDate() != null
                   ? "-" + m.getNoticeDate().toString().replaceAll("[^0-9]", "").substring(0, 14)
                   : "");
        String response = m.getCustomerResponse() != null ? m.getCustomerResponse().name() : null;
        DBA.executeUpdate(
            "INSERT INTO expiring_contract_notices (notice_no, contract_no, contractor_name,"
            + " expiry_date, phone, email, is_renewable, expected_premium,"
            + " notice_date, notice_memo, customer_response, renewal_premium, premium_diff)"
            + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE notice_memo=VALUES(notice_memo),"
            + " customer_response=VALUES(customer_response),"
            + " renewal_premium=VALUES(renewal_premium),"
            + " premium_diff=VALUES(premium_diff)",
            noticeNo,
            m.getContractNo(),
            m.getContractorName(),
            m.getExpiryDate(),
            m.getPhone(),
            m.getEmail(),
            m.getIsRenewable(),
            m.getExpectedPremium(),
            m.getNoticeDate(),
            m.getNoticeMemo(),
            response,
            m.getRenewalPremium(),
            m.getPremiumDiff());
    }
}