package org.dpbe.domain.contract.entity;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

/**
 * 계약 통계 스냅샷 (ContractStatistics)
 *
 * DB 저장 필드: id, statsNo, totalCount, activeCount, expiredCount, cancelledCount, createdAt
 * 하단 레거시 필드/메서드는 ContractStatisticsRunner(old/) 컴파일 유지용 스텁이다.
 */
public class ContractStatistics {

    // ── 신규 스냅샷 필드 ──────────────────────────────
    private Long id;
    private String statsNo;
    private int totalCount;
    private int activeCount;
    private int expiredCount;
    private int cancelledCount;
    private LocalDateTime createdAt;

    // ── 레거시 Runner 호환 필드 (DB 저장 안 함) ──────
    private String contractNo;
    private String contractorName;
    private YearMonth filterStartMonth;
    private YearMonth filterEndMonth;
    private String fileName;
    private String globalInsuranceType;
    private String globalContractStatus;

    public ContractStatistics() {}

    public ContractStatistics(String statsNo, int totalCount, int activeCount,
                               int expiredCount, int cancelledCount, LocalDateTime createdAt) {
        this.statsNo = statsNo;
        this.totalCount = totalCount;
        this.activeCount = activeCount;
        this.expiredCount = expiredCount;
        this.cancelledCount = cancelledCount;
        this.createdAt = createdAt;
    }

    // ── 신규 getter/setter ────────────────────────────
    public Long getId()                   { return id; }
    public void setId(Long id)            { this.id = id; }
    public String getStatsNo()            { return statsNo; }
    public void setStatsNo(String statsNo){ this.statsNo = statsNo; }
    public int getTotalCount()            { return totalCount; }
    public void setTotalCount(int v)      { this.totalCount = v; }
    public int getActiveCount()           { return activeCount; }
    public void setActiveCount(int v)     { this.activeCount = v; }
    public int getExpiredCount()          { return expiredCount; }
    public void setExpiredCount(int v)    { this.expiredCount = v; }
    public int getCancelledCount()        { return cancelledCount; }
    public void setCancelledCount(int v)  { this.cancelledCount = v; }
    public LocalDateTime getCreatedAt()   { return createdAt; }
    public void setCreatedAt(LocalDateTime v) { this.createdAt = v; }

    // ── 레거시 Runner 호환 메서드 (스텁) ─────────────
    public void loadStatisticsPage()  {}
    public void filterPaymentHistory() {}
    public void loadGlobalStats()     {}
    public void setGlobalDateRange()  {}
    public void applyGlobalStats()    {}
    public void showGlobalSummary()   {}
    public void showNoResultMessage() {}
    public void selectContract()      {}
    public void showDataRangeError()  {}

    public Boolean validateDateRange() {
        if (filterStartMonth == null || filterEndMonth == null) return false;
        return !filterEndMonth.isBefore(filterStartMonth);
    }

    public File exportToExcel() {
        this.fileName = "계약통계_" + contractNo + "_" + LocalDate.now() + ".xlsx";
        return new File(fileName);
    }

    public String generateFileName() {
        return "계약통계_" + contractNo + "_" + LocalDate.now() + ".xlsx";
    }

    public void setGlobalInsuranceType(String type) {
        this.globalInsuranceType = (type != null && !type.isEmpty()) ? type : null;
    }
    public void setGlobalContractStatus(String status) {
        this.globalContractStatus = (status != null && !status.isEmpty()) ? status : null;
    }
    public String getGlobalInsuranceType()  { return globalInsuranceType; }
    public String getGlobalContractStatus() { return globalContractStatus; }
    public String getContractNo()           { return contractNo; }
    public void setContractNo(String v)     { this.contractNo = v; }
    public String getContractorName()       { return contractorName; }
    public void setContractorName(String v) { this.contractorName = v; }
    public YearMonth getFilterStartMonth()  { return filterStartMonth; }
    public void setFilterStartMonth(YearMonth v) { this.filterStartMonth = v; }
    public YearMonth getFilterEndMonth()    { return filterEndMonth; }
    public void setFilterEndMonth(YearMonth v)   { this.filterEndMonth = v; }
    public String getFileName()             { return fileName; }
}