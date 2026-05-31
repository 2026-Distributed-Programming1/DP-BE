package org.dpbe.domain.contract.entity;

import java.time.LocalDateTime;

/**
 * 계약 통계 스냅샷 (ContractStatistics)
 *
 * DB 저장 필드: id, statsNo, totalCount, activeCount, expiredCount, cancelledCount, createdAt
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
}