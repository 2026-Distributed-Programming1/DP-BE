package org.dpbe.domain.contract.repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.dpbe.domain.actor.Customer;
import org.dpbe.global.jdbc.SqlExecutor;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.common.enums.ContractStatus;
import org.springframework.stereotype.Repository;

/**
 * 계약 리포지토리 (Spring 트랜잭션 통합 경로).
 * PK는 surrogate id(AUTO_INCREMENT). 업무번호(contract_no·policy_no)는 INSERT 후 id에서 파생(저장형).
 */
@Repository
public class ContractRepository {

    private static final String COLS =
            "id, contract_no, policy_no, customer_id, customer_name, contract_date, expiry_date,"
            + " monthly_premium, insurance_type, status, is_expiring_soon, is_overdue,"
            + " overdue_count, total_pay_count, paid_count, last_payment_date";

    private final SqlExecutor sql;

    public ContractRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    public List<Contract> findAll() {
        return sql.executeQuery("SELECT " + COLS + " FROM contracts", this::mapRow);
    }

    public List<Contract> findByCustomerId(String customerId) {
        return sql.executeQuery(
                "SELECT " + COLS + " FROM contracts WHERE customer_id=?", this::mapRow, customerId);
    }

    public Contract findByContractNo(String contractNo) {
        return sql.queryOne(
                "SELECT " + COLS + " FROM contracts WHERE contract_no=?", this::mapRow, contractNo);
    }

    /** 신규 계약 저장 — INSERT 후 생성 id에서 contract_no/policy_no 파생. */
    public void save(Contract c) {
        String customerId   = c.getCustomer() != null ? c.getCustomer().getCustomerId() : null;
        String customerName = c.getCustomer() != null ? c.getCustomer().getName() : null;
        String status       = c.getStatus() != null ? c.getStatus().name() : "NORMAL";
        long id = sql.executeInsertReturningKey(
                "INSERT INTO contracts (customer_id, customer_name, contract_date, expiry_date,"
                + " monthly_premium, insurance_type, status, is_expiring_soon, is_overdue,"
                + " overdue_count, total_pay_count, paid_count, last_payment_date)"
                + " VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",
                customerId, customerName, c.getContractDate(), c.getExpiryDate(),
                c.getMonthlyPremium(), c.getInsuranceType(), status,
                c.getIsExpiringSoon(), c.getIsOverdue(),
                c.getOverdueCount() != null ? c.getOverdueCount() : 0,
                c.getTotalPayCount() != null ? c.getTotalPayCount() : 0,
                c.getPaidCount() != null ? c.getPaidCount() : 0,
                c.getLastPaymentDate());
        c.setId(id);
        c.setContractNo("CON" + String.format("%05d", id));
        c.setPolicyNo("POL" + String.format("%05d", id));
        sql.executeUpdate("UPDATE contracts SET contract_no=?, policy_no=? WHERE id=?",
                c.getContractNo(), c.getPolicyNo(), id);
    }

    private Contract mapRow(ResultSet rs) throws SQLException {
        String st = rs.getString("status");
        ContractStatus status = ContractStatus.NORMAL;
        if (st != null) {
            try { status = ContractStatus.valueOf(st); }
            catch (IllegalArgumentException ignored) {}
        }
        String cid  = rs.getString("customer_id");
        String name = rs.getString("customer_name");
        Customer customer = cid != null
                ? new Customer(cid, name != null ? name : "", null, null, null)
                : null;
        Contract c = new Contract(
                rs.getString("contract_no"),
                rs.getString("policy_no"),
                customer,
                toLocalDate(rs.getDate("contract_date")),
                toLocalDate(rs.getDate("expiry_date")),
                rs.getLong("monthly_premium"),
                rs.getString("insurance_type"),
                status,
                rs.getBoolean("is_expiring_soon"),
                rs.getBoolean("is_overdue"),
                rs.getInt("overdue_count"));
        c.setId(rs.getLong("id"));
        c.setTotalPayCount(rs.getInt("total_pay_count"));
        c.setPaidCount(rs.getInt("paid_count"));
        c.setLastPaymentDate(toLocalDate(rs.getDate("last_payment_date")));
        return c;
    }

    private LocalDate toLocalDate(java.sql.Date d) {
        return d != null ? d.toLocalDate() : null;
    }
}