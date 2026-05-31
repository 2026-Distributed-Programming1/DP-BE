package org.dpbe.domain.contract.repository;

import java.util.List;
import java.util.Optional;
import org.dpbe.domain.common.enums.CustomerResponse;
import org.dpbe.domain.contract.entity.ExpiringContractManagement;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class ExpiringContractManagementRepository {

    private static final String COLS =
            "id, contract_id, contractor_name, expiry_date, phone, email,"
            + " is_renewable, expected_premium, notice_date, notice_memo,"
            + " customer_response, renewal_premium, premium_diff";

    private final SqlExecutor sql;

    public ExpiringContractManagementRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** 안내 기록 신규 저장 — id 파생으로 notice_no 생성 */
    public void save(ExpiringContractManagement m) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO expiring_contract_notices"
                + " (contract_id, contractor_name, expiry_date, phone, email,"
                + "  is_renewable, expected_premium, notice_date, notice_memo)"
                + " VALUES (?,?,?,?,?,?,?,?,?)",
                parseId(m.getContractNo()), m.getContractorName(), m.getExpiryDate(),
                m.getPhone(), m.getEmail(), m.getIsRenewable(),
                m.getExpectedPremium(), m.getNoticeDate(), m.getNoticeMemo());
        m.setId(id);
        m.setNoticeNo("EXP" + String.format("%05d", id));
    }

    /** 고객 응답 업데이트 */
    public void updateResponse(Long id, String customerResponse,
                               Long renewalPremium, Long premiumDiff) {
        sql.executeUpdate(
                "UPDATE expiring_contract_notices"
                + " SET customer_response=?, renewal_premium=?, premium_diff=?"
                + " WHERE id=?",
                customerResponse,
                renewalPremium != null ? renewalPremium : 0L,
                premiumDiff != null ? premiumDiff : 0L,
                id);
    }

    public Optional<ExpiringContractManagement> findById(Long id) {
        List<ExpiringContractManagement> list = sql.executeQuery(
                "SELECT " + COLS + " FROM expiring_contract_notices WHERE id=?",
                rowMapper(), id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    public List<ExpiringContractManagement> findAll() {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM expiring_contract_notices ORDER BY id DESC",
                rowMapper());
    }

    public List<ExpiringContractManagement> findByContractNo(String contractNo) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM expiring_contract_notices WHERE contract_id=? ORDER BY id DESC",
                rowMapper(), parseId(contractNo));
    }

    private SqlExecutor.RowMapper<ExpiringContractManagement> rowMapper() {
        return rs -> {
            ExpiringContractManagement m = new ExpiringContractManagement();
            m.setId(rs.getLong("id"));
            m.setNoticeNo("EXP" + String.format("%05d", rs.getLong("id")));
            long contractId = rs.getLong("contract_id");
            if (!rs.wasNull()) m.setContractNo("CON" + String.format("%05d", contractId));
            m.setContractorName(rs.getString("contractor_name"));
            java.sql.Date ed = rs.getDate("expiry_date");
            if (ed != null) m.setExpiryDate(ed.toLocalDate());
            m.setPhone(rs.getString("phone"));
            m.setEmail(rs.getString("email"));
            m.setIsRenewable(rs.getBoolean("is_renewable"));
            m.setExpectedPremium(rs.getLong("expected_premium"));
            java.sql.Timestamp nd = rs.getTimestamp("notice_date");
            if (nd != null) m.setNoticeDate(nd.toLocalDateTime());
            m.setNoticeMemo(rs.getString("notice_memo"));
            String cr = rs.getString("customer_response");
            if (cr != null) {
                try { m.setCustomerResponse(CustomerResponse.valueOf(cr)); }
                catch (IllegalArgumentException ignored) {}
            }
            long rp = rs.getLong("renewal_premium");
            m.setRenewalPremium(rp == 0 ? null : rp);
            long pd = rs.getLong("premium_diff");
            m.setPremiumDiff(pd == 0 ? null : pd);
            return m;
        };
    }

    private Long parseId(String businessNo) {
        return Long.parseLong(businessNo.replaceAll("\\D", ""));
    }
}
