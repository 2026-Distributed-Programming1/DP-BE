package org.dpbe.runner.usecase;

import org.dpbe.contract.Contract;
import org.dpbe.dao.ContractDAO;
import org.dpbe.enums.ContractStatus;
import org.dpbe.runner.ConsoleHelper;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * UC: 계약 정보를 조회한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 계약관리 담당자는 [계약] 메뉴 > [계약관리] 항목을 클릭한다.
 *   2. 시스템은 필터링한 조건에 맞는 계약 목록 테이블을 출력한다. (A1, A2, E1)
 *   3. 계약관리 담당자는 계약 목록 테이블에서 해당 계약의 행을 클릭한다.
 *   4. 시스템은 계약 상세정보 패널을 출력한다. (A3, A5)
 *   5. 계약관리 담당자는 [만기 계약 관리] 또는 [계약 통계] 버튼을 클릭한다. (A3, A4)
 *
 * Alternative:
 *   A1) 필터링 없이 [조회] 버튼을 누를 경우
 *       → 전체 계약 목록을 최근 등록순으로 페이지 당 20건 출력한다.
 *   A2) 필터링과 일치하는 계약이 존재하지 않는 경우
 *       → "조회 결과가 없습니다." 메시지 출력
 *   A3) 만료일 기준 30일 이내인 계약이 확인된 경우
 *       → "이 계약은 [날짜]에 만료됩니다. 만기까지 [D-XX] 남았습니다." 메시지 출력
 *       → [만기 계약 관리] 버튼 클릭 시 [만기 계약을 관리한다] 유스케이스로 이동
 *   A4) 계약 통계 분석이 필요한 경우
 *       → [계약 통계 정보를 관리한다] 유스케이스로 이동
 *   A5) 특약 미가입 시
 *       → 특약 정보란에 "가입된 특약 없음" 출력
 *
 * Exception:
 *   E1) 페이지 출력 중 오류가 발생한 경우
 *       → "데이터를 불러오는 중에 오류가 발생하였습니다. 잠시 후 다시 시도하거나 담당 부서에 문의해 주세요." 출력
 *       → [다시 시도] 버튼 클릭 시 Basic Path 2번으로 이동
 */
public class ContractInfoRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 계약 정보를 조회한다");
        ConsoleHelper.printDoubleDivider();

        // 1. 계약관리 담당자는 [계약관리] 항목을 클릭한다.
        ConsoleHelper.printStage("계약관리담당자", "[계약] 메뉴 > [계약관리] 항목을 클릭합니다.");

        Contract contract = new Contract();

        // 2. 시스템은 필터링한 조건에 맞는 계약 목록 테이블을 출력한다. (A1, A2, E1)
        contract.search();

        // E1) 페이지 출력 오류 시뮬레이션
        boolean pageError = ConsoleHelper.readYesNo("  [E1] 페이지 출력 오류를 시뮬레이션하시겠습니까?");
        if (pageError) {
            ConsoleHelper.printError("[E1] 데이터를 불러오는 중에 오류가 발생하였습니다. 잠시 후 다시 시도하거나 담당 부서에 문의해 주세요.");
            int retryAction = ConsoleHelper.readMenuChoice(
                    "[계약관리담당자] 처리를 선택하세요.",
                    "다시 시도", "목록으로 돌아가기");
            if (retryAction == 1) {
                contract.retry();
                ConsoleHelper.printInfo("[E1] 다시 시도합니다. Basic Path 2번으로 이동합니다.");
            }
            ConsoleHelper.waitEnter();
            return;
        }

        // 필터링 조건 입력
        ConsoleHelper.printStage("계약관리담당자", "필터링 조건을 입력합니다.");
        ConsoleHelper.printInfo("  보험 종류 예시: 생명보험, 실손의료보험, 종신보험, 자동차보험");
        String filterType = ConsoleHelper.readLine("  보험 종류 필터 (없으면 엔터 - A1 전체 조회): ");

        // DB에서 계약 목록 로드 후 필터 적용
        List<Contract> allList = ContractDAO.findAll();
        List<Contract> contractList = allList.stream()
                .filter(c -> filterType.isEmpty()
                        || (c.getInsuranceType() != null && c.getInsuranceType().contains(filterType)))
                .collect(java.util.stream.Collectors.toList());

        // A1) 필터링 없이 조회 시
        if (filterType.isEmpty()) {
            ConsoleHelper.printStage("시스템", "[A1] 전체 계약 목록을 출력합니다.");
        }

        // A2) 조회 결과 없는 경우
        if (contractList.isEmpty()) {
            contract.showNotFoundMessage();
            ConsoleHelper.printStage("시스템", "[A2] 조회 결과가 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 계약 목록 출력
        ConsoleHelper.printStage("시스템", "계약 목록 테이블을 출력합니다.");
        ConsoleHelper.printInfo("  번호 | 계약번호 | 고객명 | 보험종류 | 기간 | 보험료 | 납입현황 | 상태");
        for (int i = 0; i < contractList.size(); i++) {
            Contract c = contractList.get(i);
            String customerName = c.getCustomer() != null ? c.getCustomer().getName() : "-";
            String status;
            switch (c.getStatus() != null ? c.getStatus() : ContractStatus.NORMAL) {
                case EXPIRED:   status = "만기"; break;
                case CANCELLED: status = "해지"; break;
                case LAPSED:    status = "실효"; break;
                default:        status = "정상"; break;
            }
            String payStatus = (c.getPaidCount() != null ? c.getPaidCount() : 0)
                    + "/" + (c.getTotalPayCount() != null ? c.getTotalPayCount() : 0);
            ConsoleHelper.printInfo("  " + (i + 1) + " | " + c.getContractNo()
                    + " | " + customerName
                    + " | " + c.getInsuranceType()
                    + " | " + c.getStartDate() + " ~ " + c.getEndDate()
                    + " | " + c.getMonthlyPremium() + "원"
                    + " | " + payStatus
                    + " | " + status);
        }

        // 3. 계약관리 담당자는 계약 목록에서 해당 계약의 행을 클릭한다.
        ConsoleHelper.printStage("계약관리담당자", "조회할 계약 번호를 선택합니다.");
        int selected = ConsoleHelper.readMenuChoice("  계약을 선택하세요.",
                contractList.stream().map(Contract::getContractNo).toArray(String[]::new));
        contract = contractList.get(selected - 1);

        // 만기 임박 여부 재계산
        LocalDate endDate = contract.getEndDate();
        long daysUntilExpiry = endDate != null ? ChronoUnit.DAYS.between(LocalDate.now(), endDate) : -1;
        contract.setIsExpiringSoon(daysUntilExpiry >= 0 && daysUntilExpiry <= 30);

        // 4. 시스템은 계약 상세정보 패널을 출력한다.
        contract.getDetail();
        ConsoleHelper.printStage("시스템", "계약 상세정보 패널을 출력합니다.");
        String customerName = contract.getCustomer() != null ? contract.getCustomer().getName() : "-";
        String customerContact = contract.getCustomer() != null ? contract.getCustomer().getContact() : "-";
        String detailStatus;
        switch (contract.getStatus() != null ? contract.getStatus() : ContractStatus.NORMAL) {
            case EXPIRED:   detailStatus = "만기"; break;
            case CANCELLED: detailStatus = "해지"; break;
            case LAPSED:    detailStatus = "실효"; break;
            default:        detailStatus = "정상"; break;
        }
        String payInfo = (contract.getPaidCount() != null ? contract.getPaidCount() : 0)
                + "/" + (contract.getTotalPayCount() != null ? contract.getTotalPayCount() : 0)
                + " | 최근납입일: " + (contract.getLastPaymentDate() != null ? contract.getLastPaymentDate() : "-");
        ConsoleHelper.printInfo("계약번호: " + contract.getContractNo()
                + " | 고객명: " + customerName
                + " | 연락처: " + customerContact
                + " | 보험종류: " + contract.getInsuranceType()
                + " | 기간: " + contract.getStartDate() + " ~ " + contract.getEndDate()
                + " | 월보험료: " + contract.getMonthlyPremium() + "원"
                + " | 상태: " + detailStatus
                + " | 납입현황: " + payInfo
                + " | 연체: " + (contract.getIsOverdue() ? contract.getOverdueCount() + "회" : "없음"));

        // A3) 만료일 기준 30일 이내인 경우
        if (contract.getIsExpiringSoon()) {
            ConsoleHelper.printStage("시스템", "[A3] 이 계약은 [" + endDate + "]에 만료됩니다. 만기까지 [D-" + daysUntilExpiry + "] 남았습니다.");
        }

        // A5) 특약 여부
        if (contract.getSpecialClauses().isEmpty()) {
            contract.showNoSpecialClause();
            ConsoleHelper.printInfo("[A5] 가입된 특약 없음");
        } else {
            ConsoleHelper.printInfo("특약: " + String.join(", ", contract.getSpecialClauses()));
        }

        ContractDAO.save(contract);

        // 5. 계약관리 담당자는 처리를 선택한다. (A3, A4)
        int action = ConsoleHelper.readMenuChoice(
                "[계약관리담당자] 처리를 선택하세요.",
                "만기 계약 관리", "계약 통계", "종료");

        if (action == 1) {
            // A3) [만기 계약 관리] → [만기 계약을 관리한다] 유스케이스로 이동
            contract.navigateToExpiry();
            ConsoleHelper.printInfo("[A3] [만기 계약을 관리한다] 유스케이스 1번으로 이동합니다.");
            ExpiringContractManagementRunner.run();
        } else if (action == 2) {
            // A4) [계약 통계] → [계약 통계 정보를 관리한다] 유스케이스로 이동
            contract.navigateToStats();
            ConsoleHelper.printInfo("[A4] [계약 통계 정보를 관리한다] 유스케이스 1번으로 이동합니다.");
            ContractStatisticsRunner.run(contract);
        }

        ConsoleHelper.waitEnter();
    }
}