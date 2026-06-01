package org.dpbe.global.seed;

import java.time.LocalDate;
import lombok.extern.slf4j.Slf4j;
import org.dpbe.domain.actor.Customer;
import org.dpbe.domain.consultation.entity.InsuranceProduct;
import org.dpbe.domain.consultation.repository.InsuranceProductRepository;
import org.dpbe.domain.contract.entity.Contract;
import org.dpbe.domain.contract.repository.ContractRepository;
import org.dpbe.domain.customer.repository.CustomerRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 웹(Spring) 기동 시 초기 데이터 시더.
 *
 * 개발·검증 환경에서 Spring 기동 시 필요한 기본 데이터를 적재한다.
 * Repository + @Transactional + DataSource 경로로 실제 MySQL에 적재한다.
 *
 * - 멱등: 고객 데이터가 이미 있으면 건너뛴다(재기동 시 중복 방지).
 * - 토글: {@code app.seed.enabled=false} 로 비활성화(실 DB 보호).
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
public class DataSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final InsuranceProductRepository insuranceProductRepository;

    public DataSeeder(CustomerRepository customerRepository,
                      ContractRepository contractRepository,
                      InsuranceProductRepository insuranceProductRepository) {
        this.customerRepository = customerRepository;
        this.contractRepository = contractRepository;
        this.insuranceProductRepository = insuranceProductRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!customerRepository.findAll().isEmpty()) {
            log.info("[seed] 기존 고객 데이터 존재 — 시드 건너뜀");
            return;
        }
        log.info("[seed] 초기 데이터 적재 시작");

        Customer c1 = new Customer("CUS00001", "김고객", "900101-1234567", "010-1111-2222", "kim@test.com");
        c1.enterAddress("서울시 강남구 테헤란로 123");
        c1.enterBirthDate(LocalDate.of(1990, 1, 1));
        customerRepository.save(c1);

        Customer c2 = new Customer("CUS00002", "이고객", "850515-2345678", "010-3333-4444", "lee@test.com");
        c2.enterAddress("서울시 서초구 반포대로 45");
        c2.enterBirthDate(LocalDate.of(1985, 5, 15));
        customerRepository.save(c2);

        Customer c3 = new Customer("CUS00003", "최고객", "950820-1456789", "010-5555-6666", "choi@test.com");
        c3.enterAddress("경기도 성남시 분당구");
        c3.enterBirthDate(LocalDate.of(1995, 8, 20));
        customerRepository.save(c3);

        saveContract(c1, LocalDate.of(2023, 1, 1), LocalDate.of(2033, 1, 1), 500_000L, "생명보험");
        saveContract(c1, LocalDate.of(2022, 6, 1), LocalDate.of(2032, 6, 1), 100_000L, "실손의료보험");
        saveContract(c2, LocalDate.of(2024, 3, 1), LocalDate.of(2054, 3, 1), 300_000L, "종신보험");
        saveContract(c3, LocalDate.of(2024, 1, 1), LocalDate.of(2034, 1, 1), 200_000L, "자동차보험");

        insuranceProductRepository.save(new InsuranceProduct("실손의료보험", "건강",  50_000L, "의료비 전액 보장", "치과 제외"));
        insuranceProductRepository.save(new InsuranceProduct("종신보험",    "생명", 150_000L, "사망 시 1억 지급", "없음"));
        insuranceProductRepository.save(new InsuranceProduct("자동차보험",  "손해",  80_000L, "대인/대물 무제한", "음주운전 제외"));

        log.info("[seed] 완료 — 고객 {}명, 계약 {}건, 보험상품 {}개", 3, 4, 3);
    }

    private void saveContract(Customer customer, LocalDate start, LocalDate end, long premium, String type) {
        Contract contract = new Contract(customer, start, end, premium);
        contract.setInsuranceType(type);
        contractRepository.save(contract);
    }
}
