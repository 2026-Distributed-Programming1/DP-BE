package org.dpbe.domain.contract.entity;

import java.io.File;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import org.dpbe.domain.common.enums.ClaimStatus;
import org.dpbe.domain.common.enums.PaymentStatus;

/**
 * 계약 통계 (ContractStatistics)
 * 계약 통계 정보를 관리하는 유스케이스의 주요 도메인 클래스이다.
 */
public class ContractStatistics {
    private String contractNo;                  // 계약번호
    private String contractorName;             // 계약자명
    private Integer paySequence;               // 납부 회차
    private LocalDate paymentDate;             // 납부일
    private Long paymentAmount;                // 납부 금액
    private PaymentStatus paymentStatus;       // 납부 상태 - 정상/연체/미납 (enum)
    private LocalDateTime claimDate;           // 청구일시
    private Long claimAmount;                  // 청구금액
    private Long paidAmount;                   // 지급 금액
    private ClaimStatus claimStatus;           // 처리 상태 - 지급완료/심사중/반려 (enum)
    private List<Object> monthlyRetentionData; // 월별 유지 데이터
    private YearMonth filterStartMonth;        // 필터 시작 연월
    private YearMonth filterEndMonth;          // 필터 종료 연월
    private String fileName;                   // 엑셀 파일명
    private String globalInsuranceType;        // 전체 통계 보험종류 필터
    private String globalContractStatus;       // 전체 통계 계약상태 필터

    public ContractStatistics() {
        this.monthlyRetentionData = new ArrayList<>();
    }

    public void loadStatisticsPage() {}

    public void filterPaymentHistory() {}

    public Boolean validateDateRange() {
        if (filterStartMonth == null || filterEndMonth == null) return false;
        return !filterEndMonth.isBefore(filterStartMonth);
    }

    public File exportToExcel() {
        this.fileName = generateFileName();
        return new File(fileName);
    }

    public void loadGlobalStats() {}

    public void applyGlobalStats() {}

    public void showGlobalSummary() {}

    public void showNoResultMessage() {}

    public void selectContract() {}

    public void showDataRangeError() {}

    public String generateFileName() {
        return "계약통계_" + contractNo + "_" + LocalDate.now() + ".xlsx";
    }

    public void setGlobalInsuranceType(String type) {
        this.globalInsuranceType = (type != null && !type.isEmpty()) ? type : null;
    }

    public void setGlobalContractStatus(String status) {
        this.globalContractStatus = (status != null && !status.isEmpty()) ? status : null;
    }

    public void setGlobalDateRange() {}

    public String getGlobalInsuranceType() { return globalInsuranceType; }
    public String getGlobalContractStatus() { return globalContractStatus; }

    // Runner에서 실제 사용하는 getter/setter만 유지
    public String getContractNo() { return contractNo; }
    public void setContractNo(String contractNo) { this.contractNo = contractNo; }
    public String getContractorName() { return contractorName; }
    public void setContractorName(String contractorName) { this.contractorName = contractorName; }
    public YearMonth getFilterStartMonth() { return filterStartMonth; }
    public void setFilterStartMonth(YearMonth filterStartMonth) { this.filterStartMonth = filterStartMonth; }
    public YearMonth getFilterEndMonth() { return filterEndMonth; }
    public void setFilterEndMonth(YearMonth filterEndMonth) { this.filterEndMonth = filterEndMonth; }
    public String getFileName() { return fileName; }
}