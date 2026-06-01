# 배치 6 상세 계획 — 해지·환급·계약통계·만기관리 (전환 누락분)

> **배경**: 배치 1~5에서 contract·payment·claim·consultation·sales·education·inquiry 도메인은 전환됐으나, **해지(cancellation)·환급(refund)·계약통계(statistics)·만기관리(expiring)** UC 4종이 누락됐다. 이들은 전환 대상이며 본 문서가 그 전환 가이드다.
> **현재 기준 (2026-06-01)**: 배치 6 완료 및 최종 수렴 완료. `old/` 제거, 업무번호 저장 컬럼 제거, FK `id(BIGINT)` 전환, format-on-read 적용 완료.
> **문서 성격**: 배치 6 전환 당시의 상세 기록이다. 레거시/저장형 표현은 당시 이력이며 현재 구현 기준은 `ApiMigrationPlan.md`와 `Convergence_Progress.md`를 따른다.
> **API 설계**: 단계 순서를 그대로 복제하지 않고 비즈니스 규칙·검증·분기만 Service에 반영했다. 연쇄는 단계마다 1 엔드포인트·자기 테이블 1행 저장, 부모는 URL 업무번호를 파싱해 id 기반으로 조회한다.

---

## 0. 대상 정리

| UC | 엔터티 | 테이블 | 도메인(feature) |
|---|---|---|---|
| 보험 해지 | `contract/entity/Cancellation` | cancellations | contract |
| 해약 환급금 산출 | `payment/entity/RefundCalculation` | refund_calculations | payment |
| 해약 환급금 지급 | `payment/entity/RefundPayment` | refund_payments | payment |
| 해약 환급 내역 조회 | (위 둘 조회) | refund_calculations/payments | payment |
| 계약 통계 관리 | `contract/entity/ContractStatistics` | contract_statistics | contract |
| 만기 계약 관리 | `contract/entity/ExpiringContractManagement` | expiring_contract_notices | contract |

- `OverdueNoticeSetting`(overdue_notice_settings, id=1 싱글톤)·`RefundProcess`(테이블 없음, 값 객체로 추정)는 **PK 전환 대상 아님** — 구현 시 역할만 확인.

## 1. 파이프라인 / 연쇄

```
해지                  환급 산출               환급 지급
Cancellation  →  RefundCalculation  →  RefundPayment
cancellation_no    refund_no             payment_no
   (CAN)            (REF)                 (RFP)
        ↘ 둘 다 cancellation_no 보유
```
- 현재 FK: `refund_calculations.cancellation_id → cancellations.id`, `refund_payments.refund_id → refund_calculations.id` (+ 필요한 부모 id 보유).
- 계약통계·만기관리는 **독립**(연쇄 아님). 통계는 contracts 집계 스냅샷, 만기는 만기임박 계약 안내.
- 해지·환급·만기는 `contracts.id`를 부모로 참조한다.

## 2. PK 파운데이션 ✅ (2026-05-31 완료)

전환 당시 VARCHAR 업무키 PK를 **`id BIGINT AUTO_INCREMENT PRIMARY KEY` + 업무키 `UNIQUE`**로 변경했다. 최종 수렴 후 업무키 저장 컬럼은 제거됐고 업무번호는 id에서 파생한다.

| 테이블 | 업무키 | 접두 | 비고 | 완료 |
|---|---|---|---|:---:|
| cancellations | cancellation_no | `CAN` | id-파생으로 전환 | ✅ |
| refund_calculations | refund_no | `RFC` | 동일 | ✅ |
| refund_payments | payment_no | `RPY` | 동일 | ✅ |
| contract_statistics | stats_no | `STA` | 엔터티 전면 재정의(스냅샷용 필드로 교체) | ✅ |
| expiring_contract_notices | notice_no | `EXP` | 엔터티에 id·noticeNo 필드 추가, 기존 필드 유지 | ✅ |

**추가 결정사항**:
- `ContractStatistics` 엔터티: 집계 스냅샷용 DB 필드(statsNo·카운트류)를 추가하는 방식으로 재정의.
- `ExpiringContractManagement`: 계약당 N건 허용(contract_no UNIQUE 없음), id-파생 notice_no 신규 도입.

## 3. 도메인별 구현 (메인 §8 + claim 패턴 복제)

각 UC: Repository(`SqlExecutor` 주입, finder + `save()`=INSERT→`executeInsertReturningKey`→엔터티 업무번호 주입) → Service(`@Transactional`, 조회 readOnly, 규칙·검증·E1 처리) → DTO(record) → Controller(REST). **참고 구현**: `domain/claim/**`(연쇄·상태가드), `domain/contract/**`(조회), `domain/payment/PaymentRepository`(id-파생 save).

### 3-A. contract 도메인 추가 (해지·통계·만기)
이미 `domain/contract/`에 controller/service/repository/dto/entity가 있으므로 **그 안에 추가**한다(새 도메인 폴더 만들지 말 것).

- **해지(Cancellation)**: `CancellationRepository`, `CancellationService`, `CancellationController`.
  - 엔드포인트 초안: `POST /api/contracts/{contractNo}/cancellation`(해지 신청), `GET /api/cancellations`, `GET /api/cancellations/{cancellationNo}`.
  - 규칙: 해지 사유·예상환급액 산정은 엔터티 메서드로. 상태 전이(신청→처리)는 엔터티 행위 메서드 사용. 환급 산출은 **별도 엔드포인트**로 분리(무상태).
- **계약통계(ContractStatistics)**: `ContractStatisticsRepository`, Service, Controller.
  - 엔드포인트: `GET /api/contract-statistics`(집계 결과) + 필요 시 `POST`(스냅샷 저장).
  - ⚠️ **엔터티와 스키마가 거의 안 맞는다**: `ContractStatistics` 엔터티는 contractNo·paymentStatus·monthlyRetentionData 등 *행 단위 통계 화면용 필드*만 있고 **stats_no·total_count 같은 스키마 컬럼 필드가 없다**(생성자도 빈 `()`뿐, 행위 메서드는 대부분 빈 스텁). 반면 테이블 컬럼은 stats_no·total_count·active_count·expired_count·cancelled_count·created_at.
    → 권장: 통계는 **집계 스냅샷**으로 재정의. Service에서 `ContractRepository.findAll()`로 상태별 카운트를 계산해 응답. 저장이 필요하면 스키마 5컬럼에 맞는 **새 DTO/매핑**을 쓰고 엔터티의 무관한 필드는 쓰지 않는다(엔터티에 stats_no/카운트 필드를 추가하거나, 엔터티를 거치지 않고 Repository가 직접 컬럼을 다루는 방식 중 택1 — 구현 시 결정).
- **만기관리(ExpiringContractManagement)**: Repository, Service, Controller.
  - 엔드포인트: `GET /api/expiring-contracts`(만기임박 목록), `POST /api/expiring-contracts/{contractNo}/notice`(안내 발송 기록), 갱신 응답 반영 `PUT`/`POST`.
  - 만기임박 판정은 contracts의 expiry_date 기준(배치1 ContractService의 D-day 로직 참고).

### 3-B. payment 도메인 추가 (환급 산출·지급·조회) ✅ (2026-05-31 완료)

구현 파일: `RefundCalculationRepository`, `RefundPaymentRepository`, `RefundService`, `RefundController`, DTO(`RefundCalculationResponse`, `RefundPaymentResponse`, `RefundPaymentExecuteRequest`).
`CancellationRepository`도 이 단계에서 선행 생성(RefundService 의존).

| HTTP | 경로 | 설명 | 완료 |
|---|---|---|:---:|
| POST | `/api/cancellations/{cancellationNo}/refund-calculation` | 환급금 산출 (중복 방지) | ✅ |
| GET | `/api/refund-calculations` | 산출 목록 | ✅ |
| GET | `/api/refund-calculations/{refundNo}` | 산출 단건 | ✅ |
| POST | `/api/refund-calculations/{refundNo}/confirm` | 확정 + 지급 이관 (중복 방지) | ✅ |
| POST | `/api/refund-payments/{paymentNo}/execute` | OTP 인증 후 이체 실행 | ✅ |
| GET | `/api/refund-payments` | 지급 목록 | ✅ |
| GET | `/api/refund-payments/{paymentNo}` | 지급 단건 | ✅ |

**검증 완료**: 정상 흐름(산출→확정→이체), 중복 산출 400, 없는 해지 404, OTP 실패 카운트 반환, 이미 완료 건 재시도 400.

## 4. 작업 순서

1. ✅ **PK 파운데이션 일괄**: schema.sql 5테이블 id PK+UNIQUE, 엔터티 5종 수정. docker 재생성.
2. ✅ **payment 환급 3종**(산출→지급→조회) — `CancellationRepository` 선행 생성 포함. 검증 완료.
3. ✅ **contract 해지** — `CancellationService`, `CancellationController`. 해지 시 contract.status도 CANCELLED로 트랜잭션 처리.
4. ✅ **contract 통계·만기** — 독립, 조회 위주.
5. 각 단계: `./gradlew compileJava` → `docker compose down -v && up -d` → 흐름 API 호출 검증(해지→산출→지급 연쇄, 통계 집계, 만기 목록) → 메인 부록 A.6 / 본 문서에 ✅.

## 5. 미완료 / 후속 수정 필요 항목

| 항목 | 위치 | 내용 |
|---|---|---|
| 환급금 지급 계좌 | `RefundService.execute()` | OTP 검증 후 이체 실행 시 실제 계좌(BankAccount)가 없어 stub 계좌(`enter("시스템","000-0000","자동이체")`)를 주입해 `execute()` 통과. 실제 서비스에서는 고객 등록 계좌를 조회하거나 요청으로 받아야 함. |

## 6. 완료 기준 / 주의

- **추측 금지(claim 1차 실패 교훈)**: 작성 전 ① 대상 엔터티의 실제 게터·생성자, ② schema.sql 실제 컬럼명, ③ 현재 Repository/Service/DTO를 확인한다. 특히 ContractStatistics 필드↔컬럼 불일치, stats/expiring 번호 생성 방식.
- 현재 런타임 경로는 Spring REST API 단일 경로다.
- FK는 부모 `id(BIGINT)` 참조 기준이다.
- 검증 시드: 해지/환급/만기는 contracts·customers 시드(DataSeeder)에 의존. 통계는 기존 계약 4건으로 집계 확인 가능. 환급 연쇄는 해지부터 API로 생성.
- 완료 후 **메인 `ApiMigrationPlan.md`**, **`Convergence_Progress.md`**, **CLAUDE.md 현재 상태/PK 방식 표**를 갱신했다.
