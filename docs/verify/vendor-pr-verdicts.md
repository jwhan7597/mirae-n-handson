# 외주 PR 판정표

2026-10-07. 교육 자료의 patch 3개를 각각 분리된 브랜치/worktree에 적용하고 새 Sonnet 컨텍스트에서 `/verify HEAD~1..HEAD`로 검토했다. 전부 테스트를 실제 실행했다.
독립 리뷰의 근거를 Codex가 원본 patch와 대조하여 아래 표로 정리했다. 실제 페어의 판정·팀 합의·사람의 머지 승인을 받은 기록은 아니다. 수정 요청은 연습용 초안이며 외부로 보내지 않았다.

| PR | 판정 | 미충족 기준 | 근거(파일:줄번호) | 외주사에 돌려보낼 수정 요청 |
|---|---|---|---|---|
| pr-1-missing-tests | 반려 | 테스트 통과(새 동작 검증 누락) / 컨벤션 준수 | `modern/api/src/main/java/com/example/item/UnitController.java:25`, `modern/api/src/main/java/com/example/item/UnitService.java:36`에 새 통계 동작이 있으나 patch는 main 파일 3개만 바꾸고 해당 동작 테스트를 추가하지 않음 | 새 API의 200·응답 필드와 서비스의 ACTIVE 필터·0건 단원·빈 목록·순서를 검증하는 테스트를 추가하고 결과와 함께 재제출한다. |
| pr-2-hardcoded-secret | 반려 | 치명 이슈 0건 / 새 동작 테스트 / 컨벤션 준수 | `modern/api/src/main/java/com/example/assignment/GradingServerClient.java:28`·`:29` 인증정보 하드코딩, `:60`·`:66` 외부 요청 헤더에 사용. `:38` 호출 메서드 테스트 없음. `:56` JSON에 studentId를 직접 연결 | 인증정보를 외부 설정으로 주입한다. JSON을 안전하게 직렬화하고 가짜 HTTP 클라이언트로 호출의 정상·실패 경로를 테스트한다. 외부 배포 시 평문 인증 전송을 허용하지 않는 설정을 함께 제시한다. |
| pr-3-out-of-scope | 반려 | 요청 범위 이탈 없음 / 컨벤션 준수 / 새 엔드포인트 테스트 | `modern/api/src/main/java/com/example/assignment/DistributionService.java:52`·`:62`, `modern/api/src/main/java/com/example/assignment/ReportService.java:65`·`:71`·`:83`은 난이도 필터와 무관한 리팩토링·로그 변경. `modern/api/src/main/java/com/example/item/UnitController.java:31`의 새 매핑을 검증하는 컨트롤러 테스트 없음 | assignment 파일 2개의 변경을 분리한다. 단원별 목록의 level 있음/없음·없는 단원 응답을 컨트롤러 테스트로 확인하여 재제출한다. 대상 API는 원본 patch 본문에 명시되어 있어 다시 확인을 요구하지 않는다. |

## 실제 테스트 결과

모두 JDK21의 `modern/api`에서 `./gradlew test --rerun-tasks`를 실행했다. 이 PR들은 레거시 검색 이관이 아니므로 HTTP characterization과 화면 테스트는 적용하지 않는다.

| PR | 브랜치 | 검토 커밋 | 통과 | 실패/오류/건너뜀 | 독립 리뷰 원문 |
|---|---|---|---:|---|---|
| pr-1 | review/pr-1 | 447f7d7 | 80 | 0 / 0 / 0 | [vendor-pr-1-review.md](vendor-pr-1-review.md) |
| pr-2 | review/pr-2-hardcoded-secret | 9ebf040 | 81 | 0 / 0 / 0 | [vendor-pr-2-review.md](vendor-pr-2-review.md) |
| pr-3 | review/pr-3-out-of-scope | 1f978b5 | 82 | 0 / 0 / 0 | [vendor-pr-3-review.md](vendor-pr-3-review.md) |

기존 테스트 통과와 새 동작 검증 유무를 함께 판단했다. 인증값은 교육용 더미다. 실제 자격증명 유출을 확인했다는 뜻이 아니며 원문·인코딩값은 기록하지 않는다.
판정에 사용한 소스 줄번호는 각 표의 **검토 브랜치 HEAD** 기준이다. 반려한 patch는 day1에 머지하지 않았다.

원문의 잘못된 추정과 추가 제안의 처리는 [vendor-review-adjudication.md](vendor-review-adjudication.md)에 남겼다. v1 기준으로 재판정한 결과는 [criteria-v1-validation.md](criteria-v1-validation.md)에 기록한다.
