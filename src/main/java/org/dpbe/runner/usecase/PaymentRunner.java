package org.dpbe.runner.usecase;

import org.dpbe.actor.Customer;
import org.dpbe.common.BankAccount;
import org.dpbe.contract.Contract;
import org.dpbe.dao.ContractDAO;
import org.dpbe.dao.CustomerDAO;
import org.dpbe.dao.PaymentDAO;
import org.dpbe.dao.PaymentRecordDAO;
import org.dpbe.db.DBA;
import org.dpbe.enums.PaymentMethod;
import org.dpbe.payment.Payment;
import org.dpbe.payment.PaymentItem;
import org.dpbe.payment.PaymentRecord;
import org.dpbe.runner.ConsoleHelper;
import java.util.List;
import java.util.stream.Collectors;

/**
 * UC7: 「보험료를 납입한다」 시나리오 진행자
 *
 * 정상 흐름:
 * 1. 계약 선택(N:M) → 횟수 입력 → 납입 방법 선택 → 계좌 인증 → 신청
 * 분기:
 * - A2: 납입 방법 변경 (즉시이체/가상계좌)
 * - A3: 새 계좌로 납입
 * - E1: 납입 처리 오류
 */
public class PaymentRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC7: 보험료를 납입한다");
        ConsoleHelper.printDoubleDivider();

        // 1) 고객 선택
        Customer customer = selectCustomer();
        if (customer == null) return;

        Payment payment = new Payment(customer);

        // 2) 계약 선택 (1건 이상)
        List<Contract> contracts = selectContracts(customer);
        if (contracts == null || contracts.isEmpty()) return;
        payment.selectContracts(contracts);

        // 3) 계약별 납입 횟수 입력
        ConsoleHelper.printStage("고객", "계약별 납입 횟수를 입력합니다.");
        for (PaymentItem item : payment.getItems()) {
            int count = ConsoleHelper.readPositiveInt(
                    "  " + item.getContract().getContractNo()
                            + " (월 " + item.getPremiumPerCount() + "원) 납입 횟수: ");
            payment.enterPaymentCount(item, count);
        }

        if (!payment.validatePaymentCount()) {
            ConsoleHelper.printError("[E1] 납입 횟수 검증에 실패했습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 4) 납입 방법 선택 (A2 분기)
        int methodChoice = ConsoleHelper.readMenuChoice("[고객] 납입 방법을 선택하세요:",
                "즉시이체", "가상계좌 (A2)");
        PaymentMethod method = (methodChoice == 1)
                ? PaymentMethod.IMMEDIATE_TRANSFER
                : PaymentMethod.VIRTUAL_ACCOUNT;
        payment.selectPaymentMethod(method);

        // 5) 계좌 처리 (A3 분기 + 인증)
        if (!handleAccount(payment, customer)) return;

        // 6) 총액 산출
        payment.calculateTotal();
        ConsoleHelper.printDivider();
        ConsoleHelper.printInfo("총 신청 금액: " + payment.getTotalAmount() + "원");
        ConsoleHelper.printInfo("선납 할인액: " + payment.getEarlyDiscount() + "원");
        ConsoleHelper.printInfo("최종 결제액: " + payment.getDiscountedAmount() + "원");
        ConsoleHelper.printDivider();

        if (!ConsoleHelper.readYesNo("[고객] 위 내용으로 납입 신청을 진행하시겠습니까?")) {
            payment.cancel();
            ConsoleHelper.waitEnter();
            return;
        }

        // 7) 신청
        payment.submit();
        List<PaymentRecord> savedRecords = new java.util.ArrayList<>();
        DBA.beginTransaction();
        try {
            PaymentDAO.save(payment);
            for (PaymentItem item : payment.getItems()) {
                PaymentRecord record = new PaymentRecord(
                        item.getContract(),
                        item.getSubtotal(),
                        method.name());
                PaymentRecordDAO.save(record);
                savedRecords.add(record);
            }
            DBA.commit();
        } catch (Exception e) {
            DBA.rollback();
            ConsoleHelper.printError("납입 저장 중 오류가 발생했습니다. 변경사항이 취소되었습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        ConsoleHelper.printSuccess("납입 완료");
        ConsoleHelper.printInfo("납입번호: " + payment.getPaymentNo()
                + " | 납입일시: " + payment.getRequestedAt()
                + " | 최종결제액: " + payment.getDiscountedAmount() + "원"
                + " | 납입방법: " + payment.getPaymentMethod());

        // 8) 결제가 발생했으므로 PaymentRecord(납부 내역)를 생성하여 시스템에 등록
        ConsoleHelper.printStage("시스템", "결제가 발생하여 납부 내역을 시스템에 등록합니다.");
        for (PaymentRecord record : savedRecords) {
            ConsoleHelper.printInfo("  납부 내역 생성: " + record.getRecordNo()
                    + " (계약 " + record.getContract().getContractNo()
                    + ", 금액 " + record.getAmount() + "원)");
        }

        ConsoleHelper.waitEnter();
    }

    /** MyInsuranceViewRunner에서 이관받아 실행 — 고객 선택 없이 직접 처리 */
    static void run(Customer customer) {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC7: 보험료를 납입한다");
        ConsoleHelper.printDoubleDivider();

        Payment payment = new Payment(customer);

        List<Contract> contracts = selectContracts(customer);
        if (contracts == null || contracts.isEmpty()) return;
        payment.selectContracts(contracts);

        ConsoleHelper.printStage("고객", "계약별 납입 횟수를 입력합니다.");
        for (PaymentItem item : payment.getItems()) {
            int count = ConsoleHelper.readPositiveInt(
                    "  " + item.getContract().getContractNo()
                            + " (월 " + item.getPremiumPerCount() + "원) 납입 횟수: ");
            payment.enterPaymentCount(item, count);
        }

        if (!payment.validatePaymentCount()) {
            ConsoleHelper.printError("[E1] 납입 횟수 검증에 실패했습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        int methodChoice = ConsoleHelper.readMenuChoice("[고객] 납입 방법을 선택하세요:",
                "즉시이체", "가상계좌 (A2)");
        PaymentMethod method = (methodChoice == 1)
                ? PaymentMethod.IMMEDIATE_TRANSFER
                : PaymentMethod.VIRTUAL_ACCOUNT;
        payment.selectPaymentMethod(method);

        if (!handleAccount(payment, customer)) return;

        payment.calculateTotal();
        ConsoleHelper.printDivider();
        ConsoleHelper.printInfo("총 신청 금액: " + payment.getTotalAmount() + "원");
        ConsoleHelper.printInfo("선납 할인액: " + payment.getEarlyDiscount() + "원");
        ConsoleHelper.printInfo("최종 결제액: " + payment.getDiscountedAmount() + "원");
        ConsoleHelper.printDivider();

        if (!ConsoleHelper.readYesNo("[고객] 위 내용으로 납입 신청을 진행하시겠습니까?")) {
            payment.cancel();
            ConsoleHelper.waitEnter();
            return;
        }

        payment.submit();
        List<PaymentRecord> savedRecords = new java.util.ArrayList<>();
        DBA.beginTransaction();
        try {
            PaymentDAO.save(payment);
            for (PaymentItem item : payment.getItems()) {
                PaymentRecord record = new PaymentRecord(
                        item.getContract(),
                        item.getSubtotal(),
                        method.name());
                PaymentRecordDAO.save(record);
                savedRecords.add(record);
            }
            DBA.commit();
        } catch (Exception e) {
            DBA.rollback();
            ConsoleHelper.printError("납입 저장 중 오류가 발생했습니다. 변경사항이 취소되었습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        ConsoleHelper.printSuccess("납입 완료");
        ConsoleHelper.printInfo("납입번호: " + payment.getPaymentNo()
                + " | 납입일시: " + payment.getRequestedAt()
                + " | 최종결제액: " + payment.getDiscountedAmount() + "원"
                + " | 납입방법: " + payment.getPaymentMethod());

        // 8) 결제가 발생했으므로 PaymentRecord(납부 내역)를 생성하여 시스템에 등록
        ConsoleHelper.printStage("시스템", "결제가 발생하여 납부 내역을 시스템에 등록합니다.");
        for (PaymentRecord record : savedRecords) {
            ConsoleHelper.printInfo("  납부 내역 생성: " + record.getRecordNo()
                    + " (계약 " + record.getContract().getContractNo()
                    + ", 금액 " + record.getAmount() + "원)");
        }

        ConsoleHelper.waitEnter();
    }

    /** A3: 새 계좌 등록 vs 등록 계좌 사용 */
    private static boolean handleAccount(Payment payment, Customer customer) {
        while (true) {
            int choice = ConsoleHelper.readMenuChoice("[고객] 납입 계좌를 어떻게 처리하시겠습니까?",
                    "등록된 계좌 사용", "새 계좌 등록 (A3)");
            if (choice == 1) {
                List<BankAccount> accounts = customer.getRegisteredAccounts();
                if (accounts.isEmpty()) {
                    ConsoleHelper.printWarning("등록된 계좌가 없습니다. 새 계좌를 등록해주세요.");
                    continue;
                }
                String[] options = accounts.stream()
                        .map(a -> a.getBankName() + " " + a.getAccountNo() + " (" + a.getAccountHolder() + ")")
                        .toArray(String[]::new);
                int idx = ConsoleHelper.readMenuChoice("등록된 계좌 중 선택:", options);
                payment.selectExistingAccount(accounts.get(idx - 1));
                ConsoleHelper.printSuccess("계좌 인증 완료");
                return true;
            } else {
                String bank = ConsoleHelper.readNonEmpty("  은행명: ");
                String accNo = ConsoleHelper.readNonEmpty("  계좌번호: ");
                String holder = ConsoleHelper.readNonEmpty("  예금주명: ");
                payment.registerNewAccount(bank, accNo, holder);
                if (payment.verifyAccount()) {
                    ConsoleHelper.printSuccess("새 계좌 인증 완료");
                    return true;
                }
                ConsoleHelper.printError("계좌 인증에 실패했습니다.");
                if (!ConsoleHelper.readYesNo("  계좌 정보를 다시 입력하시겠습니까?")) {
                    payment.cancel();
                    ConsoleHelper.waitEnter();
                    return false;
                }
            }
        }
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
        int choice = ConsoleHelper.readMenuChoice("[시스템] 납입할 고객을 선택하세요:", options);
        return customers.get(choice - 1);
    }

    private static List<Contract> selectContracts(Customer customer) {
        List<Contract> contracts = ContractDAO.findByCustomerId(customer.getCustomerId());
        if (contracts.isEmpty()) {
            ConsoleHelper.printError("해당 고객의 보험 계약이 없습니다.");
            ConsoleHelper.waitEnter();
            return null;
        }
        String[] options = contracts.stream()
                .map(c -> c.getContractNo() + " (월 " + c.getMonthlyPremium() + "원)")
                .toArray(String[]::new);
        List<Integer> selected = ConsoleHelper.readMultiChoice(
                "[고객] 납입할 계약을 선택하세요 (다중 선택 가능, N:M 매핑):", options);
        return selected.stream().map(i -> contracts.get(i - 1)).collect(Collectors.toList());
    }
}
