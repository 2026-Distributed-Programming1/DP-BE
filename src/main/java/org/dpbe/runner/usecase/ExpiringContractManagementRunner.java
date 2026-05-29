package org.dpbe.runner.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.contract.Contract;
import org.dpbe.contract.ExpiringContractManagement;
import org.dpbe.dao.ContractDAO;
import org.dpbe.dao.ExpiringContractManagementDAO;
import org.dpbe.db.DBA;
import org.dpbe.enums.CustomerResponse;
import org.dpbe.runner.ConsoleHelper;

/**
 * UC: 만기 계약을 관리한다 시나리오 진행자
 *
 * Basic Path:
 *   1. [계약 정보를 조회한다] A3) 2번에서 넘어온다.
 *   2. 시스템은 만기 계약 관리 화면으로 전환한다.
 *      - 화면 상단: 대상 계약 정보(계약번호, 계약자명, 보험종류, 만료일, 잔여일수) (E1)
 *      - 탭: [만기 안내] [갱신 처리] [처리 이력] / 하단: [해지 처리로 전환]
 *   3. 계약관리 담당자는 [만기 안내] 탭을 클릭한다.
 *   4. 시스템은 [만기 안내] 탭 화면을 출력한다.
 *      - 고객 연락처(계약자명, 전화번호, 이메일)
 *      - 안내 내용 미리보기(만료일, 갱신 가능 여부, 갱신 시 예상 보험료, 고객 응답 링크)
 *   5. 계약관리 담당자는 [안내 문자 발송] 버튼을 클릭한다.
 *   6. 시스템은 고객의 등록된 전화번호로 만기 안내 문자를 발송한다. (E2)
 *   7. 계약관리 담당자는 [안내 기록] 버튼을 클릭한다.
 *   8. 시스템은 안내 기록 입력 폼(안내 일시 자동 입력, 메모)을 출력한다.
 *   9. 계약관리 담당자는 메모를 입력한 후 [저장] 버튼을 클릭한다.
 *  10. 시스템은 안내 기록을 저장하고 [처리 이력] 탭에 반영한다.
 *  11. 고객은 응답 링크를 통해 의사를 선택한다.
 *  12. 시스템은 계약관리 담당자에게 고객 응답 알림을 발송한다. (A1, A2, A3)
 *
 * Alternative:
 *   A1) 고객이 갱신을 희망하는 경우
 *       → [갱신 처리] 탭 클릭 → "갱신 처리할까요?" 팝업 (A4)
 *       → [예] → 갱신 후 월 보험료/변동액 입력 → [갱신 확정]
 *       → "정상적으로 갱신처리되었습니다." 팝업 출력 → [처리 이력] 반영
 *   A2) 고객이 해지를 희망하는 경우
 *       → [해지 처리로 전환] 클릭 → "전환하시겠습니까?" 팝업 (A4)
 *       → [예] → 해지 처리 유스케이스로 전환
 *   A3) 고객이 추후 결정을 원하는 경우
 *       → [처리 이력] 탭에 추후 결정 상태 기록
 *       → 만료일 7일 전 D-7 알림 자동 발송
 *   A4) [아니오] 버튼 클릭 시
 *       → 팝업을 닫고 만기 계약 관리 화면으로 돌아간다.
 *
 * Exception:
 *   E1) 만료일이 이미 경과한 계약인 경우
 *       → 만료 경과 배너 출력 → [갱신 처리] 탭 비활성화
 *       → [해지 처리로 전환] 버튼은 활성 유지
 *   E2) 문자 발송에 실패한 경우
 *       → "문자 발송에 실패했습니다. 고객 연락처를 확인하거나 직접 안내해주세요." 출력
 *       → Basic Path 8번으로 복귀
 */
public class ExpiringContractManagementRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 만기 계약을 관리한다");
        ConsoleHelper.printDoubleDivider();

        // 1. [계약 정보를 조회한다] A3에서 넘어온다.
        ConsoleHelper.printInfo("[계약 정보를 조회한다] A3) [만기 계약 관리]로부터 넘어옵니다.");

        ExpiringContractManagement mgmt = new ExpiringContractManagement();

        // 2. 시스템은 만기 계약 관리 화면으로 전환한다.
        ConsoleHelper.printStage("시스템", "만기 계약 관리 화면으로 전환합니다.");
        List<Contract> expiringList = ContractDAO.findAll().stream()
                .filter(Contract::isMaturityNear)
                .collect(Collectors.toList());

        if (expiringList.isEmpty()) {
            ConsoleHelper.printInfo("  (만기 임박 계약이 없습니다.)");
            ConsoleHelper.waitEnter();
            return;
        }

        ConsoleHelper.printInfo("  번호 | 계약번호 | 고객명 | 보험종류 | 만료일 | 잔여일수");
        for (int i = 0; i < expiringList.size(); i++) {
            Contract c = expiringList.get(i);
            String name = c.getCustomer() != null ? c.getCustomer().getName() : "-";
            long days = ChronoUnit.DAYS.between(LocalDate.now(), c.getEndDate());
            ConsoleHelper.printInfo("  " + (i + 1) + " | " + c.getContractNo()
                    + " | " + name
                    + " | " + c.getInsuranceType()
                    + " | " + c.getEndDate()
                    + " | D-" + days);
        }

        int selected = ConsoleHelper.readMenuChoice("  계약을 선택하세요.",
                expiringList.stream().map(Contract::getContractNo).toArray(String[]::new));
        Contract selectedContract = expiringList.get(selected - 1);

        // 계약 정보 상단 고정 표시
        mgmt.setContractNo(selectedContract.getContractNo());
        mgmt.setContractorName(selectedContract.getCustomer() != null ? selectedContract.getCustomer().getName() : "-");
        mgmt.setInsuranceType(selectedContract.getInsuranceType());
        mgmt.setExpiryDate(selectedContract.getEndDate());
        long remainingDays = ChronoUnit.DAYS.between(LocalDate.now(), selectedContract.getEndDate());
        mgmt.setRemainingDays((int) remainingDays);

        ConsoleHelper.printInfo("계약번호: " + mgmt.getContractNo()
                + " | 계약자명: " + mgmt.getContractorName()
                + " | 보험종류: " + mgmt.getInsuranceType()
                + " | 만료일: " + mgmt.getExpiryDate()
                + " | 잔여일수: D-" + mgmt.getRemainingDays());
        ConsoleHelper.printInfo("탭: [만기 안내] [갱신 처리] [처리 이력]  |  하단: [해지 처리로 전환]");

        // E1) 만료일이 이미 경과한 경우
        if (remainingDays < 0) {
            mgmt.showExpiredBanner();
            ConsoleHelper.printStage("시스템", "[E1] 이 계약은 [" + mgmt.getExpiryDate()
                    + "]에 이미 만료되었습니다. 갱신처리는 만료일 이전에만 가능합니다.");
            mgmt.disableRenewalTab();
            ConsoleHelper.printInfo("[E1] [갱신 처리] 탭이 비활성화됩니다. [해지 처리로 전환] 버튼은 활성화 상태를 유지합니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 3. 계약관리 담당자는 [만기 안내] 탭을 클릭한다.
        ConsoleHelper.printStage("계약관리담당자", "[만기 안내] 탭을 클릭합니다.");
        mgmt.switchTab();

        // 4. 시스템은 [만기 안내] 탭 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "[만기 안내] 탭 화면을 출력합니다.");
        String phone = ConsoleHelper.readNonEmpty("  전화번호: ");
        mgmt.setPhone(phone);
        String email = ConsoleHelper.readLine("  이메일 (없으면 엔터): ");
        if (!email.isEmpty()) mgmt.setEmail(email);
        boolean isRenewable = ConsoleHelper.readYesNo("  갱신 가능 여부");
        mgmt.setIsRenewable(isRenewable);
        long expectedPremium = ConsoleHelper.readLong("  갱신 시 예상 보험료 (원): ");
        mgmt.setExpectedPremium(expectedPremium);

        ConsoleHelper.printInfo("안내 내용 미리보기 — 만료일: " + mgmt.getExpiryDate()
                + " | 갱신가능: " + (mgmt.getIsRenewable() ? "예" : "아니오")
                + " | 예상보험료: " + mgmt.getExpectedPremium() + "원");

        // 5. 계약관리 담당자는 [안내 문자 발송] 버튼을 클릭한다.
        ConsoleHelper.printStage("계약관리담당자", "[안내 문자 발송] 버튼을 클릭합니다.");

        // 6. 시스템은 고객의 전화번호로 만기 안내 문자를 발송한다. (E2)
        mgmt.sendNoticeSms();

        // E2) 문자 발송 실패 시 (시뮬레이션: 항상 성공)
        boolean smsSuccess = true;
        if (!smsSuccess) {
            mgmt.showSmsError();
            ConsoleHelper.printStage("시스템", "[E2] 문자 발송에 실패했습니다. 고객 연락처를 확인하거나 직접 안내해주세요.");
            // Basic Path 8번으로 복귀
        } else {
            ConsoleHelper.printStage("시스템", "만기 안내 문자를 발송했습니다. | 전화번호: " + mgmt.getPhone());
        }

        // 7. 계약관리 담당자는 [안내 기록] 버튼을 클릭한다.
        ConsoleHelper.printStage("계약관리담당자", "[안내 기록] 버튼을 클릭합니다.");

        // 8. 시스템은 안내 기록 입력 폼을 출력한다.
        ConsoleHelper.printStage("시스템", "안내 기록 입력 폼을 출력합니다.");
        ConsoleHelper.printInfo("  안내 일시: 자동 입력");

        // 9. 계약관리 담당자는 메모를 입력하고 [저장]을 클릭한다.
        ConsoleHelper.printStage("계약관리담당자", "메모를 입력하고 [저장] 버튼을 클릭합니다.");
        String memo = ConsoleHelper.readLine("  메모 (없으면 엔터): ");
        if (!memo.isEmpty()) mgmt.setNoticeMemo(memo);
        mgmt.setNoticeDate(LocalDateTime.now());

        // 10. 시스템은 안내 기록을 저장하고 [처리 이력] 탭에 반영한다.
        mgmt.saveNoticeRecord();
        ExpiringContractManagementDAO.save(mgmt);
        mgmt.updateHistoryTab();
        ConsoleHelper.printStage("시스템", "안내 기록을 저장하고 [처리 이력] 탭에 반영합니다.");
        ConsoleHelper.printInfo("안내 일시: " + mgmt.getNoticeDate());

        // 11-12. 고객 응답 시뮬레이션 + 담당자 알림 발송
        ConsoleHelper.printStage("시스템", "고객이 응답 링크를 통해 의사를 선택합니다. (시뮬레이션)");
        mgmt.notifyManager();

        int responseChoice = ConsoleHelper.readMenuChoice(
                "[시뮬레이션] 고객 응답을 선택하세요.",
                "갱신 희망 (A1)", "해지 희망 (A2)", "추후 결정 (A3)");

        ConsoleHelper.printStage("시스템", "계약관리 담당자에게 고객 응답 알림을 발송합니다.");

        if (responseChoice == 1) {
            // A1) 고객이 갱신을 희망하는 경우
            mgmt.setCustomerResponse(CustomerResponse.RENEWAL);
            ConsoleHelper.printInfo("고객 [" + mgmt.getContractorName() + "]님이 [갱신 희망]을 선택하였습니다.");

            ConsoleHelper.printStage("계약관리담당자", "[갱신 처리] 탭을 클릭합니다.");

            // A1-2) "갱신 처리할까요?" 확인 팝업
            ConsoleHelper.printStage("시스템", "[" + mgmt.getContractorName() + "]님의 계약 ["
                    + mgmt.getContractNo() + "]을 갱신 처리할까요?");
            boolean confirmRenewal = ConsoleHelper.readYesNo("  [예/아니오]");

            if (!confirmRenewal) {
                // A4) [아니오] → 팝업 닫고 만기 계약 관리 화면으로 돌아간다.
                mgmt.closePopup();
                ConsoleHelper.printStage("시스템", "[A4] 팝업을 닫고 만기 계약 관리 화면으로 돌아갑니다.");
                ConsoleHelper.waitEnter();
                return;
            }

            // A1-4) 갱신 처리 화면 출력
            ConsoleHelper.printStage("시스템", "[갱신 처리] 화면을 출력합니다.");
            ConsoleHelper.printInfo("현재 월 보험료: " + selectedContract.getMonthlyPremium() + "원");
            long renewalPremium = ConsoleHelper.readLong("  갱신 후 월 보험료 (원): ");
            mgmt.setRenewalPremium(renewalPremium);
            mgmt.setPremiumDiff(renewalPremium - selectedContract.getMonthlyPremium());
            ConsoleHelper.printInfo("변동액: " + mgmt.getPremiumDiff() + "원");

            // A1-5) [갱신 확정] 클릭
            ConsoleHelper.printStage("계약관리담당자", "[갱신 확정] 버튼을 클릭합니다.");
            mgmt.confirmRenewal();
            mgmt.saveRenewalContract();
            DBA.beginTransaction();
            try {
                ExpiringContractManagementDAO.save(mgmt);
                selectedContract.setMonthlyPremium(mgmt.getRenewalPremium());
                ContractDAO.save(selectedContract);
                DBA.commit();
            } catch (Exception e) {
                DBA.rollback();
                ConsoleHelper.printError("갱신 처리 중 오류가 발생했습니다. 변경사항이 취소되었습니다.");
                ConsoleHelper.waitEnter();
                return;
            }

            // A1-6~7) 완료 팝업 + [처리 이력] 반영
            ConsoleHelper.printStage("시스템", "[" + mgmt.getContractorName() + "]님의 계약 ["
                    + mgmt.getContractNo() + "]이 정상적으로 갱신처리되었습니다.");
            mgmt.updateHistoryTab();
            mgmt.switchTab();

        } else if (responseChoice == 2) {
            // A2) 고객이 해지를 희망하는 경우
            mgmt.setCustomerResponse(CustomerResponse.TERMINATION);
            ConsoleHelper.printInfo("고객 [" + mgmt.getContractorName() + "]님이 [해지 희망]을 선택하였습니다.");

            ConsoleHelper.printStage("계약관리담당자", "[해지 처리로 전환] 버튼을 클릭합니다.");

            // A2-2) "해지 처리로 전환하시겠습니까?" 확인 팝업
            ConsoleHelper.printStage("시스템", "해지 처리로 전환하시겠습니까?");
            boolean confirmTermination = ConsoleHelper.readYesNo("  [예/아니오]");

            if (!confirmTermination) {
                // A4) [아니오] → 팝업 닫고 만기 계약 관리 화면으로 돌아간다.
                mgmt.closePopup();
                ConsoleHelper.printStage("시스템", "[A4] 팝업을 닫고 만기 계약 관리 화면으로 돌아갑니다.");
                ConsoleHelper.waitEnter();
                return;
            }

            // A2-3) 해지 처리 유스케이스로 전환
            mgmt.switchToTermination();
            mgmt.updateHistoryTab();
            ExpiringContractManagementDAO.save(mgmt);
            ConsoleHelper.printStage("시스템", "[A2] 해지 처리 유스케이스로 전환합니다.");
            InsuranceCancellationRunner.run(selectedContract);

        } else {
            // A3) 고객이 추후 결정을 원하는 경우
            mgmt.setCustomerResponse(CustomerResponse.PENDING);
            ConsoleHelper.printInfo("고객 [" + mgmt.getContractorName() + "]님이 [추후 결정]을 선택하였습니다.");
            mgmt.updateHistoryTab();
            ConsoleHelper.printStage("시스템", "[처리 이력] 탭에 추후 결정 상태로 기록됩니다.");
            mgmt.sendPendingAlert();
            ExpiringContractManagementDAO.save(mgmt);
            ConsoleHelper.printStage("시스템", "[A3] 만료일 7일 전, D-7 알림이 자동 발송됩니다: 계약 ["
                    + mgmt.getContractNo() + "], 만료까지 D-7");
        }

        mgmt.closePopup();
        ConsoleHelper.waitEnter();
    }
}
