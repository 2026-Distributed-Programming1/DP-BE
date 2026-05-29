package org.dpbe.dao;

import org.dpbe.contract.ContractStatistics;
import org.dpbe.db.DBA;

public class ContractStatisticsDAO {

    public static void save(ContractStatistics s) {
        String statsNo = s.getContractNo() != null ? s.getContractNo() : "STATS-1";
        DBA.executeUpdate(
            "INSERT INTO contract_statistics (stats_no, total_count, active_count,"
            + " expired_count, cancelled_count)"
            + " SELECT ?, COUNT(*),"
            + "   SUM(status = 'NORMAL'),"
            + "   SUM(status = 'EXPIRED'),"
            + "   SUM(status = 'CANCELLED')"
            + " FROM contracts"
            + " ON DUPLICATE KEY UPDATE"
            + " total_count=VALUES(total_count), active_count=VALUES(active_count),"
            + " expired_count=VALUES(expired_count), cancelled_count=VALUES(cancelled_count)",
            statsNo);
    }
}