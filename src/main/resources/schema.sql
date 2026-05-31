-- ============================================================
-- DP Insurance System  —  DDL  (MySQL 8.0)
-- 생성 기준: dp/dao 패키지 전체 분석 (44 테이블)
-- 실행 방법: docker-compose up -d
--            또는  mysql -u admin -p1234 insurance_db < schema.sql
--
-- 테이블 생성 순서는 외래키 의존성 계층을 고려하였습니다.
--   Tier 1 : 독립 엔터티 (참조 없음)
--   Tier 2 : customers / education_plans / accident_reports 참조
--   Tier 3 : contracts / claim_requests / dispatches 참조
--   Tier 4 : cancellations / damage_investigations / education_preparations 참조
--   Tier 5 : refund_calculations / claim_calculations 참조
--
-- ============================================================

CREATE DATABASE IF NOT EXISTS insurance_db
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;
    USE insurance_db;

-- ============================================================
-- Tier 1-A : 고객
-- ============================================================

CREATE TABLE IF NOT EXISTS customers (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    customer_id   VARCHAR(20)   UNIQUE NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    resident_no   VARCHAR(20),
    phone         VARCHAR(20),
    email         VARCHAR(100),
    address       VARCHAR(200),
    birth_date    DATE,
    registered_at TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Tier 1-B : 임직원 계열
-- ============================================================

CREATE TABLE IF NOT EXISTS education_trainers (
    employee_id  VARCHAR(20)   PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    department   VARCHAR(100),
    position     VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS sales_managers (
    manager_id   VARCHAR(50)   PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    department   VARCHAR(100)
);

CREATE TABLE IF NOT EXISTS insurance_reviewers (
    employee_id  VARCHAR(20)   PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    department   VARCHAR(100),
    position     VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS claims_handlers (
    employee_id    VARCHAR(20)  PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    department     VARCHAR(100),
    position       VARCHAR(50),
    transfer_limit BIGINT       DEFAULT 0
);

CREATE TABLE IF NOT EXISTS dispatch_agents (
    employee_id  VARCHAR(20)   PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    department   VARCHAR(100),
    position     VARCHAR(50),
    region       VARCHAR(100),
    vehicle_no   VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS finance_managers (
    employee_id  VARCHAR(20)   PRIMARY KEY,
    name         VARCHAR(100)  NOT NULL,
    department   VARCHAR(100),
    position     VARCHAR(50)
);

-- ============================================================
-- Tier 1-C : 판매채널 (설계사 / 대리점)
-- ============================================================

CREATE TABLE IF NOT EXISTS designers (
    channel_id     VARCHAR(20)  PRIMARY KEY,
    name           VARCHAR(100) NOT NULL,
    location       VARCHAR(200),
    license_number VARCHAR(50)
);

CREATE TABLE IF NOT EXISTS agencies (
    channel_id    VARCHAR(20)  PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    location      VARCHAR(200),
    agency_number VARCHAR(50)
);

-- ============================================================
-- Tier 1-D : 상품 / 시스템 설정
-- ============================================================

-- product_name 을 PK 로 사용해야 ON DUPLICATE KEY UPDATE 가 동작함
-- (InsuranceProductDAO.save 의 ON DUPLICATE KEY UPDATE 기준이 product_name)
CREATE TABLE IF NOT EXISTS insurance_products (
    id                BIGINT       AUTO_INCREMENT UNIQUE KEY,
    product_name      VARCHAR(100) PRIMARY KEY,
    category          VARCHAR(50),
    monthly_premium   BIGINT       DEFAULT 0,
    coverage_summary  TEXT,
    exclusion_summary TEXT
);

-- 시스템 단일 레코드 (id = 1 고정)
CREATE TABLE IF NOT EXISTS overdue_notice_settings (
    id                  INT      PRIMARY KEY DEFAULT 1,
    max_overdue_count   INT      DEFAULT 3,
    notice_method       VARCHAR(50),
    auto_cancel_enabled BOOLEAN  DEFAULT FALSE,
    saved_at            TIMESTAMP NULL
);

-- ============================================================
-- Tier 2 : customers 참조
-- ============================================================

-- 계약 (contracts.customer_id → customers.customer_id)
CREATE TABLE IF NOT EXISTS contracts (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    contract_no      VARCHAR(20)  UNIQUE,
    policy_no        VARCHAR(20),
    customer_id      VARCHAR(20),           -- → customers.customer_id
    customer_name    VARCHAR(100),
    contract_date    DATE,
    expiry_date      DATE,
    monthly_premium  BIGINT       DEFAULT 0,
    insurance_type   VARCHAR(50),
    status           VARCHAR(20)  DEFAULT 'NORMAL',
    is_expiring_soon BOOLEAN      DEFAULT FALSE,
    is_overdue       BOOLEAN      DEFAULT FALSE,
    overdue_count    INT          DEFAULT 0,
    total_pay_count  INT          DEFAULT 0,
    paid_count       INT          DEFAULT 0,
    last_payment_date DATE
);

-- 고객 정보 등록 이력 (판매채널이 등록한 원본 양식)
CREATE TABLE IF NOT EXISTS customer_registrations (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    customer_id     VARCHAR(20),            -- → customers.customer_id
    name            VARCHAR(100),
    ssn             VARCHAR(20),
    ssn_masked      VARCHAR(20),
    phone           VARCHAR(20),
    address         VARCHAR(255),
    insurance_type  VARCHAR(50),
    contract_date   DATE,
    expiry_date     DATE,
    monthly_premium BIGINT       DEFAULT 0,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 보험료 납부 요청
CREATE TABLE IF NOT EXISTS payments (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    payment_no     VARCHAR(20)  UNIQUE,
    customer_id    VARCHAR(20),             -- → customers.customer_id
    customer_name  VARCHAR(100),
    total_amount   BIGINT       DEFAULT 0,
    payment_method VARCHAR(50),
    requested_at   TIMESTAMP,
    status         VARCHAR(20)
);

-- 사고 접수
CREATE TABLE IF NOT EXISTS accident_reports (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    accident_no      VARCHAR(20)  UNIQUE,
    customer_id      VARCHAR(20),              -- → customers.customer_id
    customer_name    VARCHAR(100),
    accident_type    VARCHAR(50),
    vehicle_no       VARCHAR(50),
    owner_name       VARCHAR(100),
    phone_no         VARCHAR(20),
    damage_type      VARCHAR(200),
    location         VARCHAR(200),
    needs_dispatch   BOOLEAN      DEFAULT FALSE,
    casualty_count   INT          DEFAULT 0,
    injury_severity  VARCHAR(50),
    emergency_reported BOOLEAN    DEFAULT FALSE,
    reported_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    status           VARCHAR(20)
);

-- 보험 청구 접수
CREATE TABLE IF NOT EXISTS claim_requests (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    claim_no       VARCHAR(20)  UNIQUE,
    customer_id    VARCHAR(20),             -- → customers.customer_id
    customer_name  VARCHAR(100),
    contract_no    VARCHAR(20),             -- → contracts.contract_no
    claim_type     VARCHAR(20),
    diagnosis      VARCHAR(200),
    claim_reasons  VARCHAR(500),
    bank_name      VARCHAR(100),
    account_no     VARCHAR(50),
    account_holder VARCHAR(100),
    personal_info_agreed BOOLEAN DEFAULT FALSE,
    requested_at   TIMESTAMP,
    status         VARCHAR(20)
);

-- 보험 가입 신청
CREATE TABLE IF NOT EXISTS insurance_applications (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    application_no  VARCHAR(20)  UNIQUE,
    customer_id     VARCHAR(20),            -- → customers.customer_id
    customer_name   VARCHAR(100),
    product_name    VARCHAR(100),
    monthly_premium BIGINT       DEFAULT 0,
    payment_method  VARCHAR(50),
    applied_at      TIMESTAMP,
    status          VARCHAR(20)  DEFAULT '신청'
);

-- 청약
CREATE TABLE IF NOT EXISTS policy_applications (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    application_no VARCHAR(20)  UNIQUE,
    customer_id    VARCHAR(20),             -- → customers.customer_id
    customer_name  VARCHAR(100),
    product_name   VARCHAR(100),
    period         INT          DEFAULT 1,
    payment_method VARCHAR(50),
    submitted_at   TIMESTAMP,
    uploaded_at    TIMESTAMP    NULL,
    status         VARCHAR(20)  DEFAULT '신청'
);

-- ============================================================
-- Tier 2 : 교육 도메인 (독립)
-- ============================================================

-- 교육 계획안
CREATE TABLE IF NOT EXISTS education_plans (
    plan_no           VARCHAR(20)  PRIMARY KEY,
    trainer_name      VARCHAR(100),
    title             VARCHAR(200),
    target_audience   VARCHAR(200),
    scheduled_date    DATE,
    end_date          DATE,
    target_count      INT          DEFAULT 0,
    budget            BIGINT       DEFAULT 0,
    education_goal    TEXT,
    education_content TEXT,
    textbook_list     TEXT,
    reject_reason     TEXT,
    approved_at       TIMESTAMP    NULL,
    status            VARCHAR(20)
);

-- ============================================================
-- Tier 2 : 상담 도메인
-- ============================================================

-- 상담 요청 (consultation_type: 방문/전화/온라인)
CREATE TABLE IF NOT EXISTS consultation_requests (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    consult_no        VARCHAR(20)  UNIQUE,
    consultation_type VARCHAR(50),
    location          VARCHAR(200),
    contact           VARCHAR(100),
    content           TEXT,
    status            VARCHAR(20),
    scheduled_at      TIMESTAMP    NULL,
    received_at       TIMESTAMP    NULL,
    accepted_at       TIMESTAMP    NULL
);

-- 면담 일정
CREATE TABLE IF NOT EXISTS interview_schedules (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    schedule_no   VARCHAR(20)  UNIQUE,
    customer_name VARCHAR(100),
    designer_name VARCHAR(100),
    interview_type VARCHAR(20),
    scheduled_at  TIMESTAMP,
    location      VARCHAR(200),
    preparation   TEXT,
    status        VARCHAR(20),
    registered_at TIMESTAMP    NULL,
    modified_at   TIMESTAMP    NULL,
    cancelled_at  TIMESTAMP    NULL
);

-- 면담 기록
CREATE TABLE IF NOT EXISTS interview_records (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    record_no         VARCHAR(20)  UNIQUE,
    customer_name     VARCHAR(100),
    content           TEXT,
    customer_reaction TEXT,
    follow_up_action  TEXT,
    interviewed_at    TIMESTAMP    NULL,
    recorded_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    modified_at       TIMESTAMP    NULL
);

-- 제안서
CREATE TABLE IF NOT EXISTS proposals (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    proposal_no     VARCHAR(20)  UNIQUE,
    customer_name   VARCHAR(100),
    product_name    VARCHAR(100),
    monthly_premium BIGINT       DEFAULT 0,
    sent_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 인수심사
CREATE TABLE IF NOT EXISTS underwritings (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    underwriting_no  VARCHAR(20)  UNIQUE,
    review_type      VARCHAR(20),
    app_no           VARCHAR(20),
    customer_name    VARCHAR(100),
    risk_grade       VARCHAR(50),
    review_opinion   TEXT,
    result           VARCHAR(20),
    result_condition TEXT         NULL,
    rejection_reason TEXT         NULL,
    reviewed_at      TIMESTAMP
);

-- 부활
CREATE TABLE IF NOT EXISTS revivals (
    id             BIGINT       AUTO_INCREMENT PRIMARY KEY,
    revival_no     VARCHAR(20)  UNIQUE,
    contract_no    VARCHAR(20),              -- → contracts.contract_no
    customer_name  VARCHAR(100),
    contact        VARCHAR(100),
    unpaid_amount  BIGINT       DEFAULT 0,
    payment_method VARCHAR(50),
    revived_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Tier 2 : 영업 도메인
-- ============================================================

-- 채널 모집
CREATE TABLE IF NOT EXISTS channel_recruitments (
    recruitment_no VARCHAR(20)  PRIMARY KEY,
    manager_name   VARCHAR(100),
    channel_type   VARCHAR(50),
    recruit_count  INT          DEFAULT 0,
    start_date     DATE,
    end_date       DATE,
    condition_text VARCHAR(200),
    status         VARCHAR(20),
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 채널 심사
CREATE TABLE IF NOT EXISTS channel_screenings (
    screening_no     VARCHAR(20)  PRIMARY KEY,
    candidate_name   VARCHAR(100),
    channel_type     VARCHAR(50),
    qualification    VARCHAR(200),
    certifications   TEXT,
    application_date DATE,
    rejection_reason TEXT,
    status           VARCHAR(20),
    reviewed_at      TIMESTAMP
);

-- 활동 계획
CREATE TABLE IF NOT EXISTS activity_plans (
    plan_no                  VARCHAR(20)  PRIMARY KEY,
    plan_name                VARCHAR(200),
    author_name              VARCHAR(100),
    start_date               DATE,
    end_date                 DATE,
    target_contract_count    INT          DEFAULT 0,
    target_contract_amount   BIGINT       DEFAULT 0,
    target_new_customer      INT          DEFAULT 0,
    proposed_customer_id     VARCHAR(20),
    proposed_insurance_type  VARCHAR(50),
    proposal_reason          TEXT,
    memo                     TEXT,
    status                   VARCHAR(20)
);

CREATE TABLE IF NOT EXISTS activity_schedule_items (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    plan_no         VARCHAR(20),
    customer_id     VARCHAR(20),
    activity_type   VARCHAR(50),
    activity_datetime TIMESTAMP,
    location        VARCHAR(200),
    memo            TEXT
);

-- 성과급 요청
CREATE TABLE IF NOT EXISTS bonus_requests (
    request_no       VARCHAR(20)  PRIMARY KEY,
    requester        VARCHAR(100),
    evaluation_no    VARCHAR(20),
    channel_type     VARCHAR(50),
    evaluation_grade VARCHAR(20),
    amount           BIGINT       DEFAULT 0,
    reason           TEXT,
    status           VARCHAR(20),
    created_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 영업 활동 관리
CREATE TABLE IF NOT EXISTS sales_activity_managements (
    activity_no         VARCHAR(20)  PRIMARY KEY,
    manager_name        VARCHAR(100),
    channel_name        VARCHAR(100),
    activity_type       VARCHAR(50),
    start_date          DATE,
    end_date            DATE,
    visit_count         INT          DEFAULT 0,
    contract_count      INT          DEFAULT 0,
    achievement_rate    DOUBLE       DEFAULT 0,
    improvement_content TEXT,
    revised_target      INT          DEFAULT 0,
    created_at          TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 영업 조직 평가
CREATE TABLE IF NOT EXISTS sales_org_evaluations (
    evaluation_no      VARCHAR(20)  PRIMARY KEY,
    org_name           VARCHAR(100),
    channel_type       VARCHAR(50),
    grade              VARCHAR(20),
    score              DOUBLE       DEFAULT 0,
    sales_result       BIGINT       DEFAULT 0,
    contract_count     INT          DEFAULT 0,
    evaluation_comment TEXT,
    evaluated_at       TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Tier 2 : 민원 도메인
-- ============================================================

CREATE TABLE IF NOT EXISTS inquiries (
    inquiry_no            VARCHAR(20)  PRIMARY KEY,
    customer_name         VARCHAR(100),
    inquiry_type          VARCHAR(50),
    title                 VARCHAR(50),
    content               TEXT,
    attachment_file_name  VARCHAR(200),
    attachment_file_size  BIGINT,
    answer_content        TEXT,
    answered_at           TIMESTAMP    NULL,
    status                VARCHAR(20),
    created_at            TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Tier 2 : 만기 계약 안내 (contracts 참조)
-- ============================================================

CREATE TABLE IF NOT EXISTS expiring_contract_notices (
    notice_no         VARCHAR(50)  PRIMARY KEY,
    contract_no       VARCHAR(20),            -- → contracts.contract_no
    contractor_name   VARCHAR(100),
    expiry_date       DATE,
    phone             VARCHAR(20),
    email             VARCHAR(100),
    is_renewable      BOOLEAN      DEFAULT FALSE,
    expected_premium  BIGINT       DEFAULT 0,
    notice_date       TIMESTAMP    NULL,
    notice_memo       TEXT,
    customer_response VARCHAR(50),
    renewal_premium   BIGINT       DEFAULT 0,
    premium_diff      BIGINT       DEFAULT 0
);

-- ============================================================
-- Tier 3 : contracts 참조
-- ============================================================

-- 납부 기록
CREATE TABLE IF NOT EXISTS payment_records (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    record_no       VARCHAR(20)  UNIQUE,
    contract_no     VARCHAR(20),            -- → contracts.contract_no
    customer_name   VARCHAR(100),
    amount          BIGINT       DEFAULT 0,
    method          VARCHAR(50),
    payment_date    DATE,
    status          VARCHAR(20),
    confirmed_at    TIMESTAMP    NULL,
    rejected_at     TIMESTAMP    NULL,
    reject_category VARCHAR(50),
    reject_reason   VARCHAR(500)
);

-- 해지
CREATE TABLE IF NOT EXISTS cancellations (
    cancellation_no VARCHAR(20)  PRIMARY KEY,
    contract_no     VARCHAR(20),            -- → contracts.contract_no
    customer_name   VARCHAR(100),
    monthly_premium BIGINT       DEFAULT 0,
    reason          VARCHAR(500),
    detail_reason   TEXT,
    expected_refund BIGINT       DEFAULT 0,
    status          VARCHAR(20),
    cancelled_at    TIMESTAMP
);

-- 계약 통계 (스냅샷)
CREATE TABLE IF NOT EXISTS contract_statistics (
    stats_no        VARCHAR(20)  PRIMARY KEY,
    total_count     INT          DEFAULT 0,
    active_count    INT          DEFAULT 0,
    expired_count   INT          DEFAULT 0,
    cancelled_count INT          DEFAULT 0,
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- Tier 3 : accident_reports 참조
-- ============================================================

-- 현장 출동
CREATE TABLE IF NOT EXISTS dispatches (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    dispatch_no VARCHAR(20)  UNIQUE,
    accident_no VARCHAR(20),                -- → accident_reports.accident_no
    status      VARCHAR(20)
);

-- ============================================================
-- Tier 3 : claim_requests 참조
-- ============================================================

-- 피해 조사
CREATE TABLE IF NOT EXISTS damage_investigations (
    id                BIGINT       AUTO_INCREMENT PRIMARY KEY,
    investigation_no  VARCHAR(20)  UNIQUE,
    claim_no          VARCHAR(20),          -- → claim_requests.claim_no
    claim_customer    VARCHAR(100),
    customer_id       VARCHAR(20),          -- → customers.customer_id
    handler_emp_id    VARCHAR(20),
    handler_name      VARCHAR(100),
    our_fault_ratio   DOUBLE       DEFAULT 0,
    counter_ratio     DOUBLE       DEFAULT 0,
    recognized_damage BIGINT       DEFAULT 0,
    opinion           TEXT,
    result            VARCHAR(20),
    reject_reason     TEXT,
    investigated_at   TIMESTAMP,
    status            VARCHAR(20)
);

-- ============================================================
-- Tier 3 : education_plans 참조
-- ============================================================

-- 교육 제반 준비
-- (plan_no 는 앱 코드에서 현재 null 로 삽입됨 — 향후 연결 가능)
CREATE TABLE IF NOT EXISTS education_preparations (
    prep_no         VARCHAR(20)  PRIMARY KEY,
    plan_no         VARCHAR(20),            -- → education_plans.plan_no
    trainer_name    VARCHAR(100),
    venue           VARCHAR(200),
    material_ready  BOOLEAN      DEFAULT FALSE,
    textbook_status VARCHAR(200),
    attendance_list TEXT,
    status          VARCHAR(20),
    registered_at   TIMESTAMP    NULL
);

-- ============================================================
-- Tier 4 : cancellations 참조
-- ============================================================

-- 환급금 계산
CREATE TABLE IF NOT EXISTS refund_calculations (
    refund_no          VARCHAR(20)  PRIMARY KEY,
    cancellation_no    VARCHAR(20),         -- → cancellations.cancellation_no
    total_paid_premium BIGINT       DEFAULT 0,
    payment_period     VARCHAR(50),
    reserve_amount     BIGINT       DEFAULT 0,
    applied_rate       DOUBLE       DEFAULT 0,
    base_refund        BIGINT       DEFAULT 0,
    unpaid_premium     BIGINT       DEFAULT 0,
    final_refund       BIGINT       DEFAULT 0,
    status             VARCHAR(20)
);

-- ============================================================
-- Tier 4 : damage_investigations 참조
-- ============================================================

-- 보상금 산출
CREATE TABLE IF NOT EXISTS claim_calculations (
    id                  BIGINT       AUTO_INCREMENT PRIMARY KEY,
    calculation_no      VARCHAR(20)  UNIQUE,
    investigation_no    VARCHAR(20),        -- → damage_investigations.investigation_no
    recognized_damage   BIGINT       DEFAULT 0,
    fault_ratio         DOUBLE       DEFAULT 0,
    deductible          BIGINT       DEFAULT 0,
    coverage_limit      BIGINT       DEFAULT 0,
    final_amount        BIGINT       DEFAULT 0,
    exceeded_deductible BOOLEAN      DEFAULT FALSE,
    adjusted            BOOLEAN      DEFAULT FALSE,
    status              VARCHAR(20)
);

-- ============================================================
-- Tier 4 : dispatches 참조
-- ============================================================

-- 출동 기록
CREATE TABLE IF NOT EXISTS dispatch_records (
    id               BIGINT       AUTO_INCREMENT PRIMARY KEY,
    record_no        VARCHAR(20)  UNIQUE,
    dispatch_no      VARCHAR(20),                -- → dispatches.dispatch_no
    agent_name       VARCHAR(100),
    police_required  BOOLEAN      DEFAULT FALSE,
    towing_required  BOOLEAN      DEFAULT FALSE,
    notes            TEXT,
    transmitted_at   TIMESTAMP    NULL,
    status           VARCHAR(20)
);

-- 출동 기록 사진 (dispatch_records 1:N) — 실제 파일은 로컬 파일시스템, 여기엔 메타만
CREATE TABLE IF NOT EXISTS dispatch_photos (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    record_no   VARCHAR(20),                -- → dispatch_records.record_no
    file_name   VARCHAR(255),
    file_path   VARCHAR(500),
    file_size   BIGINT       DEFAULT 0,
    mime_type   VARCHAR(100),
    uploaded_at TIMESTAMP    NULL
);

-- ============================================================
-- Tier 4 : education_preparations 참조
-- ============================================================

-- 교육 실시
-- (prep_no 는 앱 코드에서 현재 null 로 삽입됨 — 향후 연결 가능)
CREATE TABLE IF NOT EXISTS education_executions (
    execution_no   VARCHAR(20)  PRIMARY KEY,
    prep_no        VARCHAR(20),             -- → education_preparations.prep_no
    trainer_name   VARCHAR(100),
    executed_at    TIMESTAMP,
    attendee_count INT          DEFAULT 0,
    memo           TEXT,
    status         VARCHAR(20)
);

-- 개별 출석 이력 (BUG-EDU-06)
CREATE TABLE IF NOT EXISTS education_attendances (
    id            BIGINT       AUTO_INCREMENT PRIMARY KEY,
    execution_no  VARCHAR(20),              -- → education_executions.execution_no
    attendee_name VARCHAR(100),
    is_attended   BOOLEAN      DEFAULT FALSE
);

-- ============================================================
-- Tier 5 : refund_calculations 참조
-- ============================================================

-- 환급금 지급
CREATE TABLE IF NOT EXISTS refund_payments (
    payment_no      VARCHAR(20)  PRIMARY KEY,
    refund_no       VARCHAR(20),            -- → refund_calculations.refund_no
    cancellation_no VARCHAR(20),
    final_amount    BIGINT       DEFAULT 0,
    transferred_at  DATETIME     NULL,
    notice_sent     BOOLEAN      DEFAULT FALSE,
    otp_fail_count  INT          DEFAULT 0,
    status          VARCHAR(20)
);

-- 계약별 납입 항목 (BUG-FIN-02)
CREATE TABLE IF NOT EXISTS payment_items (
    id          BIGINT       AUTO_INCREMENT PRIMARY KEY,
    payment_no  VARCHAR(20),                -- → payments.payment_no
    contract_no VARCHAR(20),
    count       INT          DEFAULT 0,
    subtotal    BIGINT       DEFAULT 0
);

-- ============================================================
-- Tier 5 : claim_calculations 참조
-- ============================================================

-- 보험금 지급
CREATE TABLE IF NOT EXISTS claim_payments (
    id              BIGINT       AUTO_INCREMENT PRIMARY KEY,
    payment_no      VARCHAR(20)  UNIQUE,
    calculation_no  VARCHAR(20),             -- → claim_calculations.calculation_no
    final_amount    BIGINT       DEFAULT 0,
    paid_at         DATETIME     NULL,
    scheduled_at    DATETIME     NULL,
    payment_type    VARCHAR(20),
    recipient_name  VARCHAR(100),
    account_no      VARCHAR(50),
    failure_reason  TEXT,
    status          VARCHAR(20)
);

-- ============================================================
-- FK 제약 (모두 NULLABLE — NULL 삽입 허용, 비-NULL 값만 참조 무결성 검사)
-- 적용 순서: Tier 2 → 3 → 4 → 5 (부모 테이블이 먼저 생성된 순서)
-- ※ customer_registrations.customer_id 제외
--    (판매채널이 고객 DB 등록 전에 registration 먼저 저장하는 흐름 존재)
-- ============================================================

-- Tier 2: customers 참조
ALTER TABLE contracts              ADD CONSTRAINT fk_contracts_customer              FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);
ALTER TABLE payments               ADD CONSTRAINT fk_payments_customer               FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);
ALTER TABLE accident_reports       ADD CONSTRAINT fk_accident_reports_customer       FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);
ALTER TABLE claim_requests         ADD CONSTRAINT fk_claim_requests_customer         FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);
ALTER TABLE insurance_applications ADD CONSTRAINT fk_insurance_applications_customer FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);
ALTER TABLE policy_applications    ADD CONSTRAINT fk_policy_applications_customer    FOREIGN KEY (customer_id)      REFERENCES customers(customer_id);

-- Tier 2/3: contracts 참조
ALTER TABLE claim_requests             ADD CONSTRAINT fk_claim_requests_contract           FOREIGN KEY (contract_no) REFERENCES contracts(contract_no);
ALTER TABLE revivals                   ADD CONSTRAINT fk_revivals_contract                 FOREIGN KEY (contract_no) REFERENCES contracts(contract_no);
ALTER TABLE payment_records            ADD CONSTRAINT fk_payment_records_contract           FOREIGN KEY (contract_no) REFERENCES contracts(contract_no);
ALTER TABLE cancellations              ADD CONSTRAINT fk_cancellations_contract             FOREIGN KEY (contract_no) REFERENCES contracts(contract_no);
ALTER TABLE expiring_contract_notices  ADD CONSTRAINT fk_expiring_contract_notices_contract FOREIGN KEY (contract_no) REFERENCES contracts(contract_no);

-- Tier 3: 중간 엔터티 참조
ALTER TABLE dispatches             ADD CONSTRAINT fk_dispatches_accident            FOREIGN KEY (accident_no)      REFERENCES accident_reports(accident_no);
ALTER TABLE damage_investigations  ADD CONSTRAINT fk_damage_investigations_claim    FOREIGN KEY (claim_no)         REFERENCES claim_requests(claim_no);
ALTER TABLE education_preparations ADD CONSTRAINT fk_education_preparations_plan    FOREIGN KEY (plan_no)          REFERENCES education_plans(plan_no);

-- Tier 4: 처리 레코드 참조
ALTER TABLE dispatch_records       ADD CONSTRAINT fk_dispatch_records_dispatch      FOREIGN KEY (dispatch_no)      REFERENCES dispatches(dispatch_no);
ALTER TABLE dispatch_photos        ADD CONSTRAINT fk_dispatch_photos_record         FOREIGN KEY (record_no)        REFERENCES dispatch_records(record_no);
ALTER TABLE claim_calculations     ADD CONSTRAINT fk_claim_calculations_investigation FOREIGN KEY (investigation_no) REFERENCES damage_investigations(investigation_no);
ALTER TABLE education_executions   ADD CONSTRAINT fk_education_executions_prep      FOREIGN KEY (prep_no)          REFERENCES education_preparations(prep_no);
ALTER TABLE education_attendances  ADD CONSTRAINT fk_education_attendances_execution FOREIGN KEY (execution_no)    REFERENCES education_executions(execution_no);
ALTER TABLE refund_calculations    ADD CONSTRAINT fk_refund_calculations_cancellation FOREIGN KEY (cancellation_no) REFERENCES cancellations(cancellation_no);
ALTER TABLE activity_schedule_items ADD CONSTRAINT fk_activity_schedule_items_plan  FOREIGN KEY (plan_no)          REFERENCES activity_plans(plan_no);

-- Tier 5: 최종 처리 참조
ALTER TABLE claim_payments  ADD CONSTRAINT fk_claim_payments_calculation  FOREIGN KEY (calculation_no) REFERENCES claim_calculations(calculation_no);
ALTER TABLE refund_payments ADD CONSTRAINT fk_refund_payments_refund      FOREIGN KEY (refund_no)      REFERENCES refund_calculations(refund_no);
ALTER TABLE payment_items   ADD CONSTRAINT fk_payment_items_payment       FOREIGN KEY (payment_no)     REFERENCES payments(payment_no);