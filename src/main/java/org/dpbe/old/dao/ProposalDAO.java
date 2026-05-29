package org.dpbe.old.dao;

import org.dpbe.domain.consultation.entity.Proposal;
import org.dpbe.old.db.DBA;
import java.util.List;

public class ProposalDAO {

    public static void save(Proposal p) {
        String productName = p.getInsuranceProduct() != null
                ? p.getInsuranceProduct().getProductName() : null;
        long premium = p.getInsuranceProduct() != null
                ? p.getInsuranceProduct().getMonthlyPremium() : 0L;
        DBA.executeUpdate(
            "INSERT INTO proposals (proposal_no, customer_name, product_name, monthly_premium, created_at)"
            + " VALUES (?,?,?,?,?)"
            + " ON DUPLICATE KEY UPDATE product_name=VALUES(product_name),"
            + " monthly_premium=VALUES(monthly_premium)",
            String.valueOf(p.getProposalId()),
            p.getCustomerName(),
            productName,
            premium,
            p.getSentAt());
    }

    public static List<Proposal> findAll() {
        return DBA.executeQuery(
            "SELECT proposal_no, customer_name, product_name, monthly_premium, created_at FROM proposals",
            rs -> {
                int proposalId = 0;
                try { proposalId = Integer.parseInt(rs.getString("proposal_no")); }
                catch (NumberFormatException ignored) {}
                java.sql.Timestamp ts = rs.getTimestamp("created_at");
                java.time.LocalDateTime sentAt = ts != null ? ts.toLocalDateTime() : null;
                return new Proposal(proposalId, sentAt, rs.getString("customer_name"), null);
            });
    }
}