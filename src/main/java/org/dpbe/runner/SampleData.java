package org.dpbe.runner;

import java.time.LocalDate;
import java.util.List;
import org.dpbe.actor.Agency;
import org.dpbe.actor.ClaimsHandler;
import org.dpbe.actor.Customer;
import org.dpbe.actor.Designer;
import org.dpbe.actor.DispatchAgent;
import org.dpbe.actor.EducationTrainer;
import org.dpbe.actor.FinanceManager;
import org.dpbe.actor.InsuranceReviewer;
import org.dpbe.actor.SalesManager;
import org.dpbe.common.BankAccount;
import org.dpbe.consultation.InsuranceProduct;
import org.dpbe.contract.Contract;
import org.dpbe.dao.AgencyDAO;
import org.dpbe.dao.ClaimsHandlerDAO;
import org.dpbe.dao.ContractDAO;
import org.dpbe.dao.CustomerDAO;
import org.dpbe.dao.DesignerDAO;
import org.dpbe.dao.DispatchAgentDAO;
import org.dpbe.dao.EducationTrainerDAO;
import org.dpbe.dao.FinanceManagerDAO;
import org.dpbe.dao.InsuranceProductDAO;
import org.dpbe.dao.InsuranceReviewerDAO;
import org.dpbe.dao.OverdueNoticeSettingDAO;
import org.dpbe.dao.SalesManagerDAO;
import org.dpbe.db.SequenceSync;
import org.dpbe.payment.OverdueNoticeSetting;

/**
 * 샘플 데이터 생성 (유스케이스 외부의 구동 코드)
 *
 * 프로그램 시작 시 시연을 위해 기본 객체들을 DB에 등록한다.
 * 이미 데이터가 존재하면 추가 삽입 없이 건너뛴다 (ON DUPLICATE KEY UPDATE).
 */
public class SampleData {

    private SampleData() {}

    public static void initialize() {
        SequenceSync.sync();
        if (!InsuranceProductDAO.findAll().isEmpty()) {
            System.out.println("[시스템] 샘플 데이터 초기화 완료 (기존 데이터 사용)");
            return;
        }
        createActors();
        createInsuranceProducts();
        createCustomers();
        createEmployees();
        createContracts();
        createSettings();
        System.out.println("[시스템] 샘플 데이터 초기화 완료");
    }

    private static void createActors() {
        EducationTrainerDAO.save(new EducationTrainer("김영교", "010-1111-2222", "trainer@ins.com"));
        SalesManagerDAO.save(new SalesManager("이영관", "010-3333-4444", "manager@ins.com"));
        InsuranceReviewerDAO.save(new InsuranceReviewer("박심사", "010-5555-6666", "reviewer@ins.com"));
        DesignerDAO.save(new Designer("1", "최설계", "서울", "L-2024-001"));
        AgencyDAO.save(new Agency("2", "한국대리점", "부산", "A-2024-001"));
    }

    private static void createInsuranceProducts() {
        InsuranceProductDAO.save(new InsuranceProduct(
                "실손의료보험", "건강", 50000, "의료비 전액 보장", "치과 제외"));
        InsuranceProductDAO.save(new InsuranceProduct(
                "종신보험", "생명", 150000, "사망 시 1억 지급", "없음"));
        InsuranceProductDAO.save(new InsuranceProduct(
                "자동차보험", "손해", 80000, "대인/대물 무제한", "음주운전 제외"));
    }

    private static void createCustomers() {
        Customer c1 = new Customer("김고객", "900101-1234567", "010-1111-2222", "kim@test.com");
        c1.enterAddress("서울시 강남구 테헤란로 123");
        c1.enterBirthDate(LocalDate.of(1990, 1, 1));
        BankAccount a1 = new BankAccount();
        a1.enter("국민은행", "123-456-789012", "김고객");
        a1.verify();
        c1.registerAccount(a1);
        CustomerDAO.save(c1);

        Customer c2 = new Customer("이고객", "850515-2345678", "010-3333-4444", "lee@test.com");
        c2.enterAddress("서울시 서초구 반포대로 45");
        c2.enterBirthDate(LocalDate.of(1985, 5, 15));
        BankAccount a2 = new BankAccount();
        a2.enter("신한은행", "987-654-321098", "이고객");
        a2.verify();
        c2.registerAccount(a2);
        CustomerDAO.save(c2);

        Customer c3 = new Customer("최고객", "950820-1456789", "010-5555-6666", "choi@test.com");
        c3.enterAddress("경기도 성남시 분당구");
        c3.enterBirthDate(LocalDate.of(1995, 8, 20));
        BankAccount a3 = new BankAccount();
        a3.enter("우리은행", "111-222-333444", "최고객");
        a3.verify();
        c3.registerAccount(a3);
        CustomerDAO.save(c3);
    }

    private static void createEmployees() {
        ClaimsHandlerDAO.save(new ClaimsHandler("박보상", "보상팀", "대리", 5_000_000L));
        ClaimsHandlerDAO.save(new ClaimsHandler("정보상", "보상팀", "과장", 20_000_000L));

        DispatchAgentDAO.save(new DispatchAgent("이출동", "현장출동팀", "사원", "강남", "12가3456"));
        DispatchAgentDAO.save(new DispatchAgent("강출동", "현장출동팀", "사원", "분당", "34나5678"));

        FinanceManagerDAO.save(new FinanceManager("정재무", "재무팀", "과장"));
    }

    private static void createContracts() {
        List<Customer> customers = CustomerDAO.findAll();
        if (customers.size() < 3) return;

        Customer c1 = customers.get(0);
        Customer c2 = customers.get(1);
        Customer c3 = customers.get(2);

        Contract con1 = new Contract(c1, LocalDate.of(2023, 1, 1), LocalDate.of(2033, 1, 1), 500_000L);
        con1.setInsuranceType("생명보험");
        ContractDAO.save(con1);

        Contract con2 = new Contract(c1, LocalDate.of(2022, 6, 1), LocalDate.of(2032, 6, 1), 100_000L);
        con2.setInsuranceType("실손의료보험");
        ContractDAO.save(con2);

        Contract con3 = new Contract(c2, LocalDate.of(2024, 3, 1), LocalDate.of(2054, 3, 1), 300_000L);
        con3.setInsuranceType("종신보험");
        ContractDAO.save(con3);

        Contract con4 = new Contract(c3, LocalDate.of(2024, 1, 1), LocalDate.of(2034, 1, 1), 200_000L);
        con4.setInsuranceType("자동차보험");
        ContractDAO.save(con4);
    }

    private static void createSettings() {
        OverdueNoticeSettingDAO.save(new OverdueNoticeSetting());
    }
}