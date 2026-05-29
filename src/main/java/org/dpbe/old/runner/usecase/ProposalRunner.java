package org.dpbe.old.runner.usecase;

import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.consultation.entity.InsuranceProduct;
import org.dpbe.domain.consultation.entity.InterviewRecord;
import org.dpbe.domain.consultation.entity.Proposal;
import org.dpbe.old.dao.CustomerDAO;
import org.dpbe.old.dao.InsuranceProductDAO;
import org.dpbe.old.dao.ProposalDAO;
import org.dpbe.old.runner.ConsoleHelper;
import java.util.List;

/**
 * UC: 보험상품을 제안한다 시나리오 진행자
 * (면담기록을 관리한다 유스케이스에 포함되어 실행)
 *
 * Basic Path:
 *   1. 판매채널은 보험상품 제안 화면에서 보험상품 목록을 조회한다.
 *   2. 시스템은 보험상품 제안 화면을 출력한다. (고객 기본 정보, 보험상품 목록)
 *   3. 판매채널은 제안할 상품을 선택하고 [상품 선택] 버튼을 클릭한다. (A1, A2)
 *   4. 시스템은 선택한 보험상품의 상세 정보를 출력한다.
 *   5. 판매채널은 [제안서 발송] 버튼을 클릭한다.
 *   6. 시스템은 고객에게 보험상품 제안서를 발송하고 발송 완료 결과를 출력한다. (E1)
 *   7. 판매채널은 [닫기] 버튼을 클릭한다.
 *   8. 시스템은 면담기록 관리 화면으로 돌아간다.
 *
 * Alternative:
 *   A1) 원하는 상품을 찾지 못한 경우 → 검색 조건 입력 후 재조회
 *   A2) [닫기] 버튼을 클릭한 경우 → 제안 화면으로 복귀
 *
 * Exception:
 *   E1) 고객에게 제안서 발송이 실패한 경우 → 재시도 안내
 */
public class ProposalRunner {

    public static void run(InterviewRecord record) {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC: 보험상품을 제안한다");
        ConsoleHelper.printDoubleDivider();

        Customer customer = selectCustomer();
        if (customer == null) return;

        // 2. 시스템은 보험상품 제안 화면을 출력한다. (고객 기본 정보 + 면담 내용 요약)
        ConsoleHelper.printStage("시스템", "보험상품 제안 화면을 출력합니다.");
        ConsoleHelper.printInfo("고객명: " + customer.getName() + " | 연락처: " + customer.getContact());
        ConsoleHelper.printInfo("[면담 내용 요약] 고객명: " + record.getCustomerName()
                + " | 면담일시: " + record.getInterviewedAt()
                + " | 면담 내용: " + record.getContent()
                + " | 고객 반응: " + record.getCustomerReaction()
                + " | 후속 조치: " + (record.getFollowUpAction() != null ? record.getFollowUpAction() : "-"));

        List<InsuranceProduct> products = InsuranceProductDAO.findAll();
        if (products.isEmpty()) {
            ConsoleHelper.printError("조회 가능한 보험상품이 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        ConsoleHelper.printInfo("보험상품 목록 (상품명 / 유형 / 월보험료 / 보장내용):");
        for (int i = 0; i < products.size(); i++) {
            InsuranceProduct p = products.get(i);
            ConsoleHelper.printInfo("[" + (i + 1) + "] " + p.getProductName()
                    + " | " + p.getType()
                    + " | 월 " + p.getMonthlyPremium() + "원"
                    + " | " + p.getCoverage());
        }

        int action = ConsoleHelper.readMenuChoice(
                "[판매채널] 작업을 선택하세요.",
                "상품 선택", "닫기 (A2)");

        if (action == 2) {
            ConsoleHelper.printStage("시스템", "보험상품 제안 화면으로 복귀합니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        int choice = ConsoleHelper.readMenuChoice(
                "[판매채널] 제안할 상품을 선택하세요.",
                products.stream().map(InsuranceProduct::getProductName).toArray(String[]::new));

        Proposal proposal = new Proposal();
        proposal.setCustomerName(customer.getName());
        proposal.selectProduct(products.get(choice - 1));

        ConsoleHelper.printStage("시스템", "선택한 보험상품 상세 정보를 출력합니다.");
        ConsoleHelper.printInfo("상품명: " + proposal.getInsuranceProduct().getProductName());
        ConsoleHelper.printInfo("보험유형: " + proposal.getInsuranceProduct().getType());
        ConsoleHelper.printInfo("월보험료: " + proposal.getInsuranceProduct().getMonthlyPremium() + "원");
        ConsoleHelper.printInfo("보장내용: " + proposal.getInsuranceProduct().getCoverage());
        ConsoleHelper.printInfo("특약사항: " + proposal.getInsuranceProduct().getSpecialTerms());

        boolean send = ConsoleHelper.readYesNo("[판매채널] 제안서를 발송하시겠습니까?");
        if (send) {
            proposal.send();
            ProposalDAO.save(proposal);
            ConsoleHelper.printStage("시스템", "제안서 발송 완료 결과를 출력합니다.");
            ConsoleHelper.printInfo("제안번호: " + proposal.getProposalId()
                    + " | 발송일시: " + proposal.getSentAt()
                    + " | 수신고객명: " + proposal.getCustomerName());
        }

        ConsoleHelper.printStage("시스템", "면담기록 관리 화면으로 돌아갑니다.");
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
