package org.dpbe.domain.contract.repository;

import java.util.List;
import org.dpbe.domain.contract.entity.ContractStatistics;
import org.dpbe.global.jdbc.SqlExecutor;
import org.springframework.stereotype.Repository;

@Repository
public class ContractStatisticsRepository {

    private final SqlExecutor sql;

    public ContractStatisticsRepository(SqlExecutor sql) {
        this.sql = sql;
    }

    /** contracts 테이블에서 상태별 집계 — 저장 없음 */
    public ContractStatistics aggregate() {
        ContractStatistics s = sql.queryOne(
                "SELECT COUNT(*) AS total_count,"
                + " SUM(status = 'NORMAL') AS active_count,"
                + " SUM(status = 'EXPIRED') AS expired_count,"
                + " SUM(status = 'CANCELLED') AS cancelled_count"
                + " FROM contracts",
                rs -> {
                    ContractStatistics r = new ContractStatistics();
                    r.setTotalCount(rs.getInt("total_count"));
                    r.setActiveCount(rs.getInt("active_count"));
                    r.setExpiredCount(rs.getInt("expired_count"));
                    r.setCancelledCount(rs.getInt("cancelled_count"));
                    return r;
                });
        return s != null ? s : new ContractStatistics();
    }

    /** 집계 결과를 스냅샷으로 저장 — id 파생으로 stats_no 생성 */
    public void save(ContractStatistics s) {
        long id = sql.executeInsertReturningKey(
                "INSERT INTO contract_statistics (total_count, active_count, expired_count, cancelled_count)"
                + " VALUES (?,?,?,?)",
                s.getTotalCount(), s.getActiveCount(), s.getExpiredCount(), s.getCancelledCount());
        s.setId(id);
        s.setStatsNo("STA" + String.format("%05d", id));
    }

    public List<ContractStatistics> findAll() {
        return sql.executeQuery(
                "SELECT id, total_count, active_count, expired_count,"
                + " cancelled_count, created_at"
                + " FROM contract_statistics ORDER BY id DESC",
                rs -> {
                    ContractStatistics s = new ContractStatistics();
                    s.setId(rs.getLong("id"));
                    s.setStatsNo("STA" + String.format("%05d", rs.getLong("id")));
                    s.setTotalCount(rs.getInt("total_count"));
                    s.setActiveCount(rs.getInt("active_count"));
                    s.setExpiredCount(rs.getInt("expired_count"));
                    s.setCancelledCount(rs.getInt("cancelled_count"));
                    java.sql.Timestamp ca = rs.getTimestamp("created_at");
                    if (ca != null) s.setCreatedAt(ca.toLocalDateTime());
                    return s;
                });
    }
}