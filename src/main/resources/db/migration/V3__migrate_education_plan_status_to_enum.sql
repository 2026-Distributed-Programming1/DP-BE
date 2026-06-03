-- education_plans.status 컬럼 값을 한글 문자열에서 PlanStatus enum name으로 변경
UPDATE education_plans SET status = 'TEMP_SAVE'    WHERE status IN ('임시저장', '작성중');
UPDATE education_plans SET status = 'UNDER_REVIEW' WHERE status = '승인요청';
UPDATE education_plans SET status = 'APPROVED'     WHERE status = '승인';
UPDATE education_plans SET status = 'REJECTED'     WHERE status = '반려';
