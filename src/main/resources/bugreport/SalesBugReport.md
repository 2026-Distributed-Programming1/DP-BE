# SalesBugReport.md
> sales 도메인 API 테스트 결과 (2026-06-01)
> POST /api/customer-registrations 등록·조회 흐름 수동 검증에서 발견

---

## BUG-SALES-01 ✅ 수정 완료
**CustomerRegistration 목록 조회 시 SSN 마스킹 이중 적용 (이중 대시)**

| 항목 | 내용 |
|---|---|
| 파일 | `domain/sales/repository/CustomerRegistrationRepository.java:64` |
| 심각도 | MEDIUM |
| 재현 경로 | `POST /api/customer-registrations` → `GET /api/customer-registrations` |
| 현상 | 등록 직후 단건 응답은 `"900101-1******"`(정상)이지만, 목록 조회(`GET`) 응답에서는 `"900101--******"`(이중 대시)로 출력된다. |

**원인**

`mapRow()`가 DB의 `ssn_masked` 컬럼값(이미 마스킹된 문자열)을 엔터티의 `ssn` 필드에 저장한다.

```java
// CustomerRegistrationRepository.java:60~65
CustomerRegistration r = new CustomerRegistration(
    rs.getString("customer_id"),
    null,
    rs.getString("name"),
    rs.getString("ssn_masked"),  // ← "900101-1******" 를 ssn 필드로 넣음
    ...
);
```

이후 `CustomerRegistrationResponse.from()`이 `r.getMaskedSsn()`을 호출하면,
`getMaskedSsn()`은 `ssn` 필드를 **원시 주민번호**로 가정하고 재마스킹한다.

```java
// CustomerRegistration.java
public String getMaskedSsn() {
    if (ssn == null || ssn.length() < 7) return ssn;
    return ssn.substring(0, 6) + "-" + ssn.charAt(6) + "******";
    //  "900101"          + "-" + "-"(6번째 문자) + "******"
    //  → "900101--******"  (이중 대시 발생)
}
```

**수정 방법**

`mapRow()`에서 `ssn_masked`를 엔터티의 `ssn` 필드가 아닌 별도 표시용 필드로 저장하거나,
`CustomerRegistrationResponse.from()`이 DB 로딩 시에는 `getMaskedSsn()` 대신 저장된 마스킹 값을 직접 사용하도록 변경한다.

가장 간단한 수정: `CustomerRegistration` 엔터티에 `ssnMasked` 필드를 추가하고, `mapRow()`에서 `setSsnMasked()`로 저장, `CustomerRegistrationResponse.from()`에서 우선 참조한다.

```java
// 수정안 — CustomerRegistration 엔터티
private String ssnMasked;
public String getSsnMasked() { return ssnMasked; }
public void setSsnMasked(String v) { this.ssnMasked = v; }

// mapRow() 수정
r.setSsnMasked(rs.getString("ssn_masked"));

// CustomerRegistrationResponse.from() 수정
r.getSsnMasked() != null ? r.getSsnMasked() : r.getMaskedSsn(),
```
