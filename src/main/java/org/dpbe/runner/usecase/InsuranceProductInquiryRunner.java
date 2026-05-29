package org.dpbe.runner.usecase;

import java.util.List;
import org.dpbe.actor.Customer;
import org.dpbe.consultation.InsuranceProduct;
import org.dpbe.dao.CustomerDAO;
import org.dpbe.dao.InsuranceProductDAO;
import org.dpbe.runner.ConsoleHelper;

/**
 * UC: 보험상품을 조회한다 시나리오 진행자
 *
 * Basic Path:
 *   1. 고객은 메인 화면에서 [보험상품 조회] 메뉴를 클릭한다.
 *   2. 시스템은 보험상품 조회 화면을 출력한다.
 *      - 조회 조건: 보험 유형 / 월보험료 범위
 *      - 보험상품 목록 테이블: 상품명 / 유형 / 월보험료 / 보장내용
 *   3. 고객은 조회 조건을 입력하고 [조회] 버튼을 클릭한다. (A1)
 *   4. 시스템은 조회 조건에 맞는 보험상품 목록을 출력한다. (A2)
 *   5. 고객은 상품 항목을 클릭한다.
 *   6. 시스템은 선택한 보험상품의 상세 정보를 출력한다.
 *      - 상품명, 보험유형, 월보험료, 보장내용, 특약사항
 *      - [보험 신청] 버튼, [부활 신청] 버튼, [닫기] 버튼
 *   7. 고객은 [닫기] 버튼을 클릭한다. (A3, A4)
 *   8. 시스템은 보험상품 목록 화면으로 돌아간다.
 *
 * Alternative:
 *   A1) 조회 조건 없이 [조회] 버튼을 클릭한 경우 → 전체 상품 목록 출력
 *   A2) 조회 조건에 해당하는 상품이 없는 경우 → 안내 메시지 출력
 *   A3) [보험 신청] 버튼을 클릭한 경우
 *       → 보험을 신청한다 유스케이스로 이동 (extend)
 *   A4) [부활 신청] 버튼을 클릭한 경우
 *       → 부활을 요청한다 유스케이스로 이동 (extend)
 *
 * Exception:
 *   E1) 조회 가능한 보험상품이 없는 경우 → 안내 메시지 출력
 */
public class InsuranceProductInquiryRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 보험상품을 조회한다");
        ConsoleHelper.printDoubleDivider();

        Customer customer = selectCustomer();
        if (customer == null) return;

        // 2. 시스템은 보험상품 조회 화면을 출력한다.
        ConsoleHelper.printStage("시스템", "보험상품 조회 화면을 출력합니다.");
        ConsoleHelper.printInfo("조회 조건: 보험유형 (전체/건강/생명/손해) | 월보험료 범위");

        List<InsuranceProduct> products = InsuranceProductDAO.findAll();

        // E1) 조회 가능한 보험상품이 없는 경우
        if (products.isEmpty()) {
            ConsoleHelper.printError("[E1] 조회 가능한 보험상품이 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 3. 고객은 조회 조건을 입력한다. (A1)
        ConsoleHelper.printStage("고객", "조회 조건을 입력합니다. (없으면 엔터 - 전체 조회)");
        String typeFilter = ConsoleHelper.readLine("  보험유형 (건강/생명/손해, 없으면 엔터): ");

        // 4. 시스템은 조회 조건에 맞는 보험상품 목록을 출력한다.
        List<InsuranceProduct> filtered = products.stream()
                .filter(p -> typeFilter.isEmpty() || p.getType().equals(typeFilter))
                .collect(java.util.stream.Collectors.toList());

        // A2) 조회 조건에 해당하는 상품이 없는 경우
        if (filtered.isEmpty()) {
            ConsoleHelper.printError("[A2] 조회 조건에 해당하는 보험상품이 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        ConsoleHelper.printStage("시스템", "보험상품 목록을 출력합니다.");
        for (int i = 0; i < filtered.size(); i++) {
            InsuranceProduct p = filtered.get(i);
            ConsoleHelper.printInfo("[" + (i + 1) + "] " + p.getProductName()
                    + " | " + p.getType()
                    + " | 월 " + p.getMonthlyPremium() + "원"
                    + " | " + p.getCoverage());
        }

        // 5. 고객은 상품 항목을 클릭한다.
        int choice = ConsoleHelper.readMenuChoice(
                "[고객] 상세 조회할 상품을 선택하세요.",
                filtered.stream().map(InsuranceProduct::getProductName).toArray(String[]::new));
        InsuranceProduct selected = filtered.get(choice - 1);

        // 6. 시스템은 선택한 보험상품의 상세 정보를 출력한다.
        ConsoleHelper.printStage("시스템", "보험상품 상세 정보를 출력합니다.");
        ConsoleHelper.printInfo("상품명: " + selected.getProductName());
        ConsoleHelper.printInfo("보험유형: " + selected.getType());
        ConsoleHelper.printInfo("월보험료: " + selected.getMonthlyPremium() + "원");
        ConsoleHelper.printInfo("보장내용: " + selected.getCoverage());
        ConsoleHelper.printInfo("특약사항: " + selected.getSpecialTerms());
        ConsoleHelper.printInfo("버튼: [보험 신청] [부활 신청] [닫기]");

        // 7. 고객은 버튼을 선택한다. (A3, A4)
        int action = ConsoleHelper.readMenuChoice(
                "[고객] 작업을 선택하세요.",
                "보험 신청 (A3 - extend)", "부활 신청 (A4 - extend)", "닫기");

        if (action == 1) {
            // A3) 보험을 신청한다 유스케이스로 이동 (extend)
            ConsoleHelper.printStage("시스템", "[A3] 보험을 신청한다 유스케이스로 이동합니다.");
            InsuranceApplicationRunner.run();
        } else if (action == 2) {
            // A4) 부활을 요청한다 유스케이스로 이동 (extend)
            ConsoleHelper.printStage("시스템", "[A4] 부활을 요청한다 유스케이스로 이동합니다.");
            RevivalRunner.run();
        } else {
            // 8. 시스템은 보험상품 목록 화면으로 돌아간다.
            ConsoleHelper.printStage("시스템", "보험상품 목록 화면으로 돌아갑니다.");
        }

        ConsoleHelper.waitEnter();
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
        int choice = ConsoleHelper.readMenuChoice("[시스템] 고객을 선택하세요:", options);
        return customers.get(choice - 1);
    }
}