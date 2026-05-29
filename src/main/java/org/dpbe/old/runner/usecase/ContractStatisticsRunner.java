package org.dpbe.old.runner.usecase;

import org.dpbe.domain.contract.entity.Contract;
import java.time.YearMonth;
import org.dpbe.domain.contract.entity.ContractStatistics;
import org.dpbe.old.dao.ContractStatisticsDAO;
import org.dpbe.old.runner.ConsoleHelper;

/**
 * UC: 계약 통계 정보를 관리한다 시나리오 진행자
 *
 * Basic Path:
 *   1. [계약 정보를 조회한다] A4 2번에서 넘어온다.
 *   2. 시스템은 계약 통계 페이지를 출력한다.
 *   3. 계약관리 담당자는 기간 필터에 시작 연월과 종료 연월을 입력하고
 *      [조회] 버튼을 클릭한다. (E1)
 *   4. 시스템은 입력한 기간에 해당하는 납부 이력만 필터링하여 테이블을 갱신한다.
 *   5. 계약관리 담당자는 [엑셀 다운로드] 버튼을 클릭한다.
 *   6. 시스템은 현재 화면에 표시된 통계 데이터를 엑셀 파일로 생성하여 다운로드한다.
 *      파일명: "계약통계_[계약번호]_[다운로드일자].xlsx"
 *
 * Alternative:
 *   A1) 전체 계약 통계를 조회하는 경우
 *       → [전체 계약 통계] 버튼 클릭
 *       → 필터 조건 설정 후 [조회] 클릭 (A2, E1)
 *       → 계약 목록에서 특정 계약 행 클릭 → Basic Path 2번으로 복귀
 *   A2) 필터 조건에 해당하는 계약이 존재하지 않는 경우
 *       → "해당 조건에 일치하는 계약이 존재하지 않습니다." 메시지 출력
 *
 * Exception:
 *   E1) 기간 오류 (종료 연월이 시작 연월보다 앞인 경우)
 *       → 기간 오류 메시지 출력
 */
public class ContractStatisticsRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 계약 통계 정보를 관리한다");
        ConsoleHelper.printDoubleDivider();

        // 1. [계약 정보를 조회한다] A4 2번에서 넘어온다.
        ConsoleHelper.printInfo("[계약 정보를 조회한다] A4) [계약 통계]로부터 넘어옵니다.");

        ContractStatistics statistics = new ContractStatistics();

        // 2. 시스템은 계약 통계 페이지를 출력한다.
        statistics.loadStatisticsPage();
        ConsoleHelper.printStage("시스템", "계약 통계 페이지를 출력합니다.");

        // A1) 전체 계약 통계 조회 여부 선택
        int topAction = ConsoleHelper.readMenuChoice(
                "[계약관리담당자] 처리를 선택하세요.",
                "특정 계약 납부 이력 조회", "전체 계약 통계 조회 (A1)");

        if (topAction == 2) {
            // A1) 전체 계약 통계를 조회하는 경우
            statistics.loadGlobalStats();
            ConsoleHelper.printStage("계약관리담당자", "[A1] [전체 계약 통계] 버튼을 클릭합니다.");

            // 필터 조건 설정
            ConsoleHelper.printStage("계약관리담당자", "필터 조건을 설정하고 [조회] 버튼을 클릭합니다.");
            String insuranceTypeFilter = ConsoleHelper.readLine("  보험 종류 필터 (없으면 엔터): ");
            statistics.setGlobalInsuranceType(insuranceTypeFilter);
            String statusFilter = ConsoleHelper.readLine("  계약 상태 필터 (없으면 엔터): ");
            statistics.setGlobalContractStatus(statusFilter);
            statistics.setGlobalDateRange();
            statistics.applyGlobalStats();

            // A2) 필터 조건에 해당하는 계약이 없는 경우
            boolean hasGlobalResult = ConsoleHelper.readYesNo("  조회된 계약이 있습니까?");
            if (!hasGlobalResult) {
                statistics.showNoResultMessage();
                ConsoleHelper.printStage("시스템", "[A2] 해당 조건에 일치하는 계약이 존재하지 않습니다.");
                ConsoleHelper.printInfo("필터 조건을 수정한 후 [조회] 버튼을 클릭해주세요.");
                ConsoleHelper.waitEnter();
                return;
            }

            // 전체 통계 결과 출력
            statistics.showGlobalSummary();
            ConsoleHelper.printStage("시스템", "전체 계약 통계 결과를 출력합니다.");

            // 계약 목록에서 특정 계약 행 클릭 → Basic Path 2번으로 복귀
            statistics.selectContract();
            ConsoleHelper.printStage("계약관리담당자", "계약 목록에서 특정 계약 행을 클릭합니다.");
            ConsoleHelper.printInfo("[A1] Basic Path 2번으로 돌아갑니다.");
        }

        // 계약번호 및 계약자명 입력 (수동)
        ConsoleHelper.printStage("계약관리담당자", "계약 정보를 입력합니다.");
        String contractNo = ConsoleHelper.readNonEmpty("  계약번호: ");
        statistics.setContractNo(contractNo);
        String contractorName = ConsoleHelper.readNonEmpty("  계약자명: ");
        statistics.setContractorName(contractorName);

        // 3. 계약관리 담당자는 기간 필터에 시작 연월과 종료 연월을 입력하고 [조회]를 클릭한다. (E1)
        ConsoleHelper.printStage("계약관리담당자", "기간 필터를 입력하고 [조회] 버튼을 클릭합니다.");

        YearMonth startMonth;
        YearMonth endMonth;
        while (true) {
            int startYear = ConsoleHelper.readPositiveInt("  시작 연도 (예: 2024): ");
            int startMon = ConsoleHelper.readRangeInt("  시작 월 (1~12): ", 1, 12);
            int endYear = ConsoleHelper.readPositiveInt("  종료 연도 (예: 2024): ");
            int endMon = ConsoleHelper.readRangeInt("  종료 월 (1~12): ", 1, 12);
            startMonth = YearMonth.of(startYear, startMon);
            endMonth = YearMonth.of(endYear, endMon);
            statistics.setFilterStartMonth(startMonth);
            statistics.setFilterEndMonth(endMonth);

            if (!statistics.validateDateRange()) {
                // E1) 기간 오류
                statistics.showDataRangeError();
                ConsoleHelper.printStage("시스템", "[E1] 종료 연월은 시작 연월보다 이전일 수 없습니다. 다시 입력해주세요.");
            } else {
                break;
            }
        }

        // 4. 시스템은 기간에 해당하는 납부 이력만 필터링하여 테이블을 갱신한다.
        statistics.filterPaymentHistory();
        ConsoleHelper.printStage("시스템", "납부 이력을 필터링하여 테이블을 갱신합니다.");
        ConsoleHelper.printInfo("조회 기간: " + statistics.getFilterStartMonth()
                + " ~ " + statistics.getFilterEndMonth()
                + " | 계약번호: " + statistics.getContractNo()
                + " | 계약자: " + statistics.getContractorName());

        // 5. 계약관리 담당자는 [엑셀 다운로드] 버튼을 클릭한다.
        ConsoleHelper.printStage("계약관리담당자", "[엑셀 다운로드] 버튼을 클릭합니다.");

        // 6. 시스템은 통계 데이터를 엑셀 파일로 생성하여 다운로드한다.
        statistics.exportToExcel();
        ContractStatisticsDAO.save(statistics);
        ConsoleHelper.printStage("시스템", "통계 데이터를 엑셀 파일로 생성하여 다운로드합니다.");
        ConsoleHelper.printInfo("파일명: " + statistics.getFileName());

        ConsoleHelper.waitEnter();
    }

    public static void run(Contract contract) {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 계약 통계 정보를 관리한다");
        ConsoleHelper.printDoubleDivider();

        ConsoleHelper.printInfo("[계약 정보를 조회한다] A4) [계약 통계]로부터 넘어옵니다.");

        ContractStatistics statistics = new ContractStatistics();

        statistics.loadStatisticsPage();
        ConsoleHelper.printStage("시스템", "계약 통계 페이지를 출력합니다.");

        // A1) 전체 계약 통계 조회 여부 선택
        int topAction = ConsoleHelper.readMenuChoice(
                "[계약관리담당자] 처리를 선택하세요.",
                "특정 계약 납부 이력 조회", "전체 계약 통계 조회 (A1)");

        if (topAction == 2) {
            statistics.loadGlobalStats();
            ConsoleHelper.printStage("계약관리담당자", "[A1] [전체 계약 통계] 버튼을 클릭합니다.");
            ConsoleHelper.printStage("계약관리담당자", "필터 조건을 설정하고 [조회] 버튼을 클릭합니다.");
            statistics.setGlobalInsuranceType(ConsoleHelper.readLine("  보험 종류 필터 (없으면 엔터): "));
            statistics.setGlobalContractStatus(ConsoleHelper.readLine("  계약 상태 필터 (없으면 엔터): "));
            statistics.setGlobalDateRange();
            statistics.applyGlobalStats();

            boolean hasGlobalResult = ConsoleHelper.readYesNo("  조회된 계약이 있습니까?");
            if (!hasGlobalResult) {
                statistics.showNoResultMessage();
                ConsoleHelper.printStage("시스템", "[A2] 해당 조건에 일치하는 계약이 존재하지 않습니다.");
                ConsoleHelper.printInfo("필터 조건을 수정한 후 [조회] 버튼을 클릭해주세요.");
                ConsoleHelper.waitEnter();
                return;
            }
            statistics.showGlobalSummary();
            ConsoleHelper.printStage("시스템", "전체 계약 통계 결과를 출력합니다.");
            statistics.selectContract();
            ConsoleHelper.printStage("계약관리담당자", "계약 목록에서 특정 계약 행을 클릭합니다.");
            ConsoleHelper.printInfo("[A1] Basic Path 2번으로 돌아갑니다.");
            return;
        }

        // 계약번호 및 계약자명 자동 표시 (ContractInfoRunner에서 넘어온 컨텍스트 활용)
        String contractNo = contract.getContractNo();
        String contractorName = contract.getCustomer() != null ? contract.getCustomer().getName() : "-";
        statistics.setContractNo(contractNo);
        statistics.setContractorName(contractorName);
        ConsoleHelper.printStage("시스템", "계약 정보를 자동으로 표시합니다.");
        ConsoleHelper.printInfo("계약번호: " + contractNo + " | 계약자: " + contractorName);

        // 3. 기간 필터 입력 (E1)
        ConsoleHelper.printStage("계약관리담당자", "기간 필터를 입력하고 [조회] 버튼을 클릭합니다.");
        YearMonth startMonth;
        YearMonth endMonth;
        while (true) {
            int startYear = ConsoleHelper.readPositiveInt("  시작 연도 (예: 2024): ");
            int startMon = ConsoleHelper.readRangeInt("  시작 월 (1~12): ", 1, 12);
            int endYear = ConsoleHelper.readPositiveInt("  종료 연도 (예: 2024): ");
            int endMon = ConsoleHelper.readRangeInt("  종료 월 (1~12): ", 1, 12);
            startMonth = YearMonth.of(startYear, startMon);
            endMonth = YearMonth.of(endYear, endMon);
            statistics.setFilterStartMonth(startMonth);
            statistics.setFilterEndMonth(endMonth);
            if (!statistics.validateDateRange()) {
                statistics.showDataRangeError();
                ConsoleHelper.printStage("시스템", "[E1] 종료 연월은 시작 연월보다 이전일 수 없습니다. 다시 입력해주세요.");
            } else {
                break;
            }
        }

        // 4. 납부 이력 필터링
        statistics.filterPaymentHistory();
        ConsoleHelper.printStage("시스템", "납부 이력을 필터링하여 테이블을 갱신합니다.");
        ConsoleHelper.printInfo("조회 기간: " + statistics.getFilterStartMonth()
                + " ~ " + statistics.getFilterEndMonth()
                + " | 계약번호: " + statistics.getContractNo()
                + " | 계약자: " + statistics.getContractorName());

        // 5~6. 엑셀 다운로드
        ConsoleHelper.printStage("계약관리담당자", "[엑셀 다운로드] 버튼을 클릭합니다.");
        statistics.exportToExcel();
        ContractStatisticsDAO.save(statistics);
        ConsoleHelper.printStage("시스템", "통계 데이터를 엑셀 파일로 생성하여 다운로드합니다.");
        ConsoleHelper.printInfo("파일명: " + statistics.getFileName());

        ConsoleHelper.waitEnter();
    }
}