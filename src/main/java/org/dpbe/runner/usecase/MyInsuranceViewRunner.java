package org.dpbe.runner.usecase;

import org.dpbe.actor.Customer;
import org.dpbe.contract.Contract;
import org.dpbe.dao.ContractDAO;
import org.dpbe.dao.CustomerDAO;
import org.dpbe.dao.PaymentRecordDAO;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import org.dpbe.payment.PaymentRecord;
import org.dpbe.runner.ConsoleHelper;

/**
 * 「가입 보험을 조회한다」 시나리오 진행자
 *
 * 정상 흐름:
 * 1. 고객 선택 (앱 로그인 시뮬레이션)
 * 2. 전체 가입 보험 목록 출력
 * 3. 보험 계약 상세 조회 (기본정보·보장내용·납입내역 탭)
 *
 * 분기:
 * - A1: 가입 계약 없음
 * - A2: 보험료 납입하기 → [8-UC7] 보험료를 납입한다
 * - A3: 취소/해약 → 보험을 해지한다
 * - A4: 납입방법 변경 (1원 인증)
 * - A5: 가입 증명서 PDF 확인
 * - A6: 약관 PDF 확인
 * - A7: 납입내역 기간 필터
 *
 * 예외:
 * - E1: 상세 조회 불가
 */
public class MyInsuranceViewRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("가입 보험을 조회한다");
        ConsoleHelper.printDoubleDivider();

        Customer customer = selectCustomer();
        if (customer == null) return;

        run(customer);
    }

    /** MyInsuranceViewRunner에서 A2→PaymentRunner 복귀 후 재사용 가능하도록 분리 */
    static void run(Customer customer) {
        while (true) {
            // Basic Path 2: 전체 보험 목록
            List<Contract> contracts = ContractDAO.findByCustomerId(customer.getCustomerId());

            if (contracts.isEmpty()) {
                // A1: 가입된 보험 계약 없음
                ConsoleHelper.printError("[A1] 가입된 보험 계약 내역이 없습니다.");
                ConsoleHelper.waitEnter();
                return;
            }

            // 목록 출력
            ConsoleHelper.printDivider();
            System.out.printf("  [%s 님의 가입 보험 목록]%n", customer.getName());
            for (int i = 0; i < contracts.size(); i++) {
                Contract c = contracts.get(i);
                System.out.printf("  %d. %-20s | 증권: %-12s | 상태: %-6s | 월 보험료: %,d원%n",
                        i + 1,
                        c.getInsuranceType() != null ? c.getInsuranceType() : "(보험명 미설정)",
                        c.getPolicyNo(),
                        statusLabel(c),
                        c.getMonthlyPremium());
            }
            ConsoleHelper.printDivider();

            // 메뉴 구성: 상세 조회(N건) + 보험료 납입 + 뒤로가기
            String[] options = new String[contracts.size() + 2];
            for (int i = 0; i < contracts.size(); i++) {
                options[i] = contracts.get(i).getPolicyNo() + " 상세 조회";
            }
            options[contracts.size()]     = "보험료 납입하기 (A2)";
            options[contracts.size() + 1] = "뒤로가기";

            int choice = ConsoleHelper.readMenuChoice("[고객] 메뉴를 선택하세요:", options);

            if (choice == contracts.size() + 1) {
                // A2: 보험료 납입 → UC7
                PaymentRunner.run(customer);
                // 납입 후 Basic Path 2번(목록)으로 자동 복귀
                continue;
            }
            if (choice == contracts.size() + 2) {
                return; // 뒤로가기
            }

            // Basic Path 3: 상세 조회
            showDetail(contracts.get(choice - 1));
        }
    }

    /** 계약 상세 화면 — 3개 탭 + 부가 기능 */
    private static void showDetail(Contract contract) {
        // E1 시뮬레이션
        if (ConsoleHelper.readYesNo("  [E1 시뮬레이션] 상세 조회 오류를 시뮬레이션하시겠습니까?")) {
            ConsoleHelper.printError("[E1] 해당 보험 계약 정보를 현재 조회할 수 없습니다. 잠시 후 다시 시도하거나 고객센터에 문의해 주세요.");
            int retryChoice = ConsoleHelper.readMenuChoice("[고객] 처리를 선택하세요:", "다시 시도", "목록으로 돌아가기");
            if (retryChoice == 1) {
                ConsoleHelper.printInfo("[E1] 다시 시도합니다.");
            }
            return; // 어느 경우든 목록으로
        }

        while (true) {
            // Basic Path 4: 상세 화면 (기본정보 탭 기본 선택)
            ConsoleHelper.printDivider();
            System.out.println("  [보험 계약 상세] 상태: " + statusLabel(contract));
            showBasicInfoTab(contract);

            int tabChoice = ConsoleHelper.readMenuChoice("[고객] 탭 / 메뉴를 선택하세요:",
                    "보장내용 탭 조회",
                    "납입내역 탭 조회",
                    "취소/해약 (A3)",
                    "납입방법 변경 (A4)",
                    "가입 증명서 확인 (A5)",
                    "약관 확인 (A6)",
                    "목록으로 (Basic Path 9)");

            switch (tabChoice) {
                case 1:
                    // Basic Path 5-6: 보장내용 탭
                    showCoverageTab(contract);
                    break;
                case 2:
                    // Basic Path 7-8: 납입내역 탭
                    showPaymentHistoryTab(contract);
                    break;
                case 3:
                    // A3: 보험 해지
                    boolean cancelled = InsuranceCancellationRunner.run(contract);
                    if (cancelled) return; // 해약 완료 → 목록으로
                    break;
                case 4:
                    // A4: 납입방법 변경
                    handlePaymentMethodChange(contract);
                    break;
                case 5:
                    // A5: 가입 증명서 확인
                    contract.getCertificatePDF();
                    ConsoleHelper.printInfo("[A5] 가입 증명서 PDF를 출력합니다. (다운로드·공유 버튼 포함)");
                    ConsoleHelper.waitEnter();
                    break;
                case 6:
                    // A6: 약관 확인
                    contract.getPolicyPDF();
                    ConsoleHelper.printInfo("[A6] 약관 PDF를 출력합니다. (다운로드·공유 버튼 포함)");
                    ConsoleHelper.waitEnter();
                    break;
                case 7:
                    return; // 목록으로 → outer loop 재실행
            }
        }
    }

    // ── 탭별 출력 ────────────────────────────────────────────────────────────────

    private static void showBasicInfoTab(Contract contract) {
        ConsoleHelper.printDivider();
        System.out.println("  [기본정보 탭]");
        System.out.println("  상품명     : " + nvl(contract.getInsuranceType(), "(미설정)"));
        System.out.println("  계약일자   : " + contract.getContractDate());
        System.out.println("  증권번호   : " + contract.getPolicyNo());
        System.out.println("  만기일     : " + contract.getExpiryDate());
        System.out.println("  계약자     : " + contract.getCustomer().getName());
        System.out.println("  월 보험료  : " + String.format("%,d", contract.getMonthlyPremium()) + "원");
        if (contract.getPaidCount() != null) {
            System.out.println("  납입 횟수  : " + contract.getPaidCount() + "회");
        }
        ConsoleHelper.printDivider();
    }

    private static void showCoverageTab(Contract contract) {
        ConsoleHelper.printDivider();
        System.out.println("  [보장내용 탭]");
        System.out.println("  계약자: " + contract.getCustomer().getName());
        List<String> clauses = contract.getSpecialClauses();
        if (clauses == null || clauses.isEmpty()) {
            System.out.println("  (가입된 특약 없음)");
        } else {
            for (String clause : clauses) {
                System.out.println("  특약: " + clause);
            }
        }
        ConsoleHelper.printDivider();
        ConsoleHelper.waitEnter();
    }

    private static void showPaymentHistoryTab(Contract contract) {
        ConsoleHelper.printDivider();
        System.out.println("  [납입내역 탭]");

        List<PaymentRecord> records = PaymentRecordDAO.findByContractNo(contract.getContractNo());

        // A7: 납입 내역 기간 선택
        int periodChoice = ConsoleHelper.readMenuChoice("[고객] 조회 기간을 선택하세요:",
                "전체", "최근 1년", "최근 2년");

        if (periodChoice == 2) {
            LocalDate cutoff = LocalDate.now().minusYears(1);
            records = records.stream()
                    .filter(r -> r.getPaymentDate() != null && !r.getPaymentDate().isBefore(cutoff))
                    .collect(Collectors.toList());
        } else if (periodChoice == 3) {
            LocalDate cutoff = LocalDate.now().minusYears(2);
            records = records.stream()
                    .filter(r -> r.getPaymentDate() != null && !r.getPaymentDate().isBefore(cutoff))
                    .collect(Collectors.toList());
        }

        long totalAmount = records.stream().mapToLong(PaymentRecord::getAmount).sum();
        System.out.println("  총 납입 금액: " + String.format("%,d", totalAmount) + "원");
        System.out.println("  총 납입 횟수: " + records.size() + "회");

        if (records.isEmpty()) {
            System.out.println("  (납입 내역 없음)");
        } else {
            System.out.printf("  %-12s | %13s | %s%n", "납입 날짜", "납입 보험료", "납입 방법");
            for (PaymentRecord r : records) {
                System.out.printf("  %-12s | %,12d원 | %s%n",
                        r.getPaymentDate(), r.getAmount(), r.getMethod());
            }
        }
        ConsoleHelper.printDivider();
        ConsoleHelper.waitEnter();
    }

    // ── 부가 기능 ─────────────────────────────────────────────────────────────────

    private static void handlePaymentMethodChange(Contract contract) {
        ConsoleHelper.printStage("고객", "[A4] 납입방법 변경 화면을 출력합니다.");
        ConsoleHelper.readMenuChoice("[고객] 납입방법을 선택하세요:", "계좌이체 (기본)", "카드결제");
        String bank   = ConsoleHelper.readNonEmpty("  은행명(카드사명): ");
        String accNo  = ConsoleHelper.readNonEmpty("  계좌번호(카드번호): ");

        ConsoleHelper.printInfo("[시스템] 1원 인증번호를 해당 계좌로 전송합니다.");
        String code = ConsoleHelper.readNonEmpty("[고객] 인증번호 입력: ");

        org.dpbe.common.BankAccount account = new org.dpbe.common.BankAccount();
        account.enter(bank, accNo, contract.getCustomer().getName());

        if (contract.verifyAccount(1, code)) {
            account.verify();
            contract.changePaymentMethod("계좌이체", account);
            ContractDAO.save(contract);
            ConsoleHelper.printSuccess("납입 방법이 정상적으로 변경되었습니다.");
        } else {
            ConsoleHelper.printError("인증 실패. 납입방법 변경이 취소되었습니다.");
        }
        ConsoleHelper.waitEnter();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────────

    private static String statusLabel(Contract contract) {
        if (contract.getStatus() == null) return "정상유지";
        return switch (contract.getStatus()) {
            case EXPIRED   -> "만기";
            case CANCELLED -> "해지";
            default        -> "정상유지";
        };
    }

    private static String nvl(String value, String fallback) {
        return value != null ? value : fallback;
    }

    private static Customer selectCustomer() {
        List<Customer> customers = CustomerDAO.findAll();
        if (customers.isEmpty()) {
            ConsoleHelper.printError("등록된 고객이 없습니다.");
            return null;
        }
        String[] options = customers.stream()
                .map(c -> c.getName() + " (" + c.getCustomerNo() + ")")
                .toArray(String[]::new);
        int choice = ConsoleHelper.readMenuChoice("[시스템] 고객을 선택하세요 (로그인 시뮬레이션):", options);
        return customers.get(choice - 1);
    }
}