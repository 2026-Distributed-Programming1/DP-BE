package org.dpbe.old.runner.usecase;

import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.claim.entity.AccidentReport;
import org.dpbe.domain.claim.entity.Dispatch;
import org.dpbe.old.dao.AccidentReportDAO;
import org.dpbe.old.dao.CustomerDAO;
import org.dpbe.old.dao.DispatchDAO;
import java.util.List;
import org.dpbe.domain.common.enums.AccidentType;
import org.dpbe.old.runner.ConsoleHelper;

/**
 * UC1: 「사고를 접수한다」 시나리오 진행자
 *
 * 정상 흐름과 함께 다음 분기를 시뮬레이션한다.
 * - A2: 사고 접수만 진행하고 현장출동을 신청하지 않음
 * - E1: 당사 가입 내역 대조 실패 → 안내 후 종료
 */
public class AccidentReportRunner {

    public static void run() {
        ConsoleHelper.printDoubleDivider();
        System.out.println("UC1: 사고를 접수한다");
        ConsoleHelper.printDoubleDivider();

        // 1) 고객 선택 (실제 시스템에서는 로그인된 고객)
        Customer customer = selectCustomer();
        if (customer == null) return;

        ConsoleHelper.printStage("고객", "사고 접수 화면을 엽니다.");
        AccidentReport report = new AccidentReport(customer);

        // 2) 차량 정보 입력
        ConsoleHelper.printStage("고객", "차량 정보를 입력합니다.");
        String vehicleNo = ConsoleHelper.readNonEmpty("  차량번호: ");
        String ownerName = ConsoleHelper.readNonEmpty("  자동차 소유자명: ");
        String contact = ConsoleHelper.readNonEmpty("  휴대폰 번호: ");
        report.enterVehicleInfo(vehicleNo, ownerName, contact);

        // 3) 사고 유형 선택
        int typeChoice = ConsoleHelper.readMenuChoice("[고객] 사고 유형을 선택하세요:",
                "사물 (차량 등의 물적 사고)",
                "사람 (인적 사고)");
        AccidentType accidentType = (typeChoice == 1) ? AccidentType.OBJECT : AccidentType.PERSON;
        String damageType = ConsoleHelper.readNonEmpty("  피해 유형 상세: ");
        report.selectAccidentType(accidentType, damageType);

        // A1) 인명사고인 경우 추가 입력
        if (accidentType == AccidentType.PERSON) {
            ConsoleHelper.printInfo("[A1] 인명사고이므로 추가 정보를 입력합니다.");
            int casualtyCount = ConsoleHelper.readPositiveInt("  부상자 수: ");
            int severityChoice = ConsoleHelper.readMenuChoice("  부상 정도를 선택하세요:",
                    "경상", "중상", "사망");
            String injurySeverity = severityChoice == 1 ? "경상" : severityChoice == 2 ? "중상" : "사망";
            boolean emergency = ConsoleHelper.readYesNo("  119에 신고하셨습니까?");
            report.enterCasualtyInfo(casualtyCount, injurySeverity, emergency);
            ConsoleHelper.printInfo("[A1] 부상자 수: " + casualtyCount
                    + " | 부상 정도: " + injurySeverity
                    + " | 119 신고: " + (emergency ? "예" : "아니오"));
        }

        // 4) 사고 위치 입력
        ConsoleHelper.printStage("고객", "사고 위치를 입력합니다. (실제 시스템에서는 GPS 자동 인식)");
        String location = ConsoleHelper.readNonEmpty("  사고 위치: ");
        report.enterLocation(location);

        // 5) 약관 동의
        boolean agreed = ConsoleHelper.readYesNo("[고객] 위치기반 서비스 약관에 동의하시겠습니까?");
        if (!agreed) {
            ConsoleHelper.printError("약관 미동의로 사고 접수를 진행할 수 없습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        report.agreeTerms();

        // 6) 현장출동 옵션
        boolean needsDispatch = ConsoleHelper.readYesNo("[고객] 현장출동을 신청하시겠습니까?");
        report.setDispatchOption(needsDispatch);

        // 7) 필수 항목 검증
        if (!report.validateRequiredFields()) {
            ConsoleHelper.printError("필수 항목이 누락되었습니다.");
            ConsoleHelper.waitEnter();
            return;
        }

        // 8) 당사 가입 내역 대조 (E1)
        ConsoleHelper.printStage("시스템", "당사 가입 내역을 조회합니다.");
        if (!report.verifyContract()) {
            ConsoleHelper.printError("[E1] 당사 가입 내역이 확인되지 않습니다.");
            ConsoleHelper.waitEnter();
            return;
        }
        ConsoleHelper.printSuccess("당사 가입 고객으로 확인되었습니다.");

        // 9) 접수 처리
        report.receive();
        AccidentReportDAO.save(report);
        ConsoleHelper.printSuccess("사고 접수번호: " + report.getReportNo());

        // 10) A2 분기 처리 - 현장출동 신청 여부에 따라
        if (needsDispatch) {
            Dispatch dispatch = report.requestDispatch();
            if (dispatch != null) {
                DispatchDAO.save(dispatch);
                ConsoleHelper.printSuccess("현장출동 신청 완료");
                ConsoleHelper.printInfo("출동번호: " + dispatch.getDispatchNo()
                        + " | 신청시간: " + report.getReportedAt()
                        + " | 사고위치: " + report.getLocation()
                        + " | 사고상황: " + report.getDamageType()
                        + " | 휴대폰: " + report.getPhoneNo()
                        + " | 차량번호: " + report.getVehicleNo());
                ConsoleHelper.printInfo("→ 현장출동 서비스 부서로 신청 내역이 전달되었습니다.");
            }
        } else {
            // A2: 접수만 진행하고 종료
            ConsoleHelper.printInfo("[A2] 현장출동 없이 사고 접수만 완료되었습니다.");
        }

        ConsoleHelper.waitEnter();
    }

    /** 샘플 고객 중 선택 */
    private static Customer selectCustomer() {
        List<Customer> customers = CustomerDAO.findAll();
        if (customers.isEmpty()) {
            ConsoleHelper.printError("등록된 고객이 없습니다.");
            return null;
        }
        String[] options = customers.stream()
                .map(c -> c.getName() + " (" + c.getCustomerNo() + ")")
                .toArray(String[]::new);
        int choice = ConsoleHelper.readMenuChoice("[시스템] 사고를 접수할 고객을 선택하세요:", options);
        return customers.get(choice - 1);
    }
}
