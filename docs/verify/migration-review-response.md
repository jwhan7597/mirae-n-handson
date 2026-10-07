# 1차 리뷰 대응 — 작성 역할의 판정

2026-10-07. 독립 Sonnet 리뷰 원문은 `migration-review-1.md`에 보존했다.
리뷰어가 코드를 수정한 것이 아니며, 아래 판정·근거 보강은 작성 역할인 Codex가 수행했다.

| 지적 | 분류 | 근거·조치 |
|---|---|---|
| API 테스트 실행 권한 거부 | 수용 | 실제로 미실행이었다. Codex가 JDK21로 `./gradlew test --rerun-tasks`를 실행하여80개 통과·실패0·오류0을 확인했다. 리뷰 도구의 허용 명령을 수정했고2차 리뷰도 실제 실행한다. |
| 리뷰 보고서 쓰기 권한 거부 | 수용 | 원문 Write 도구 요청을 Codex가 그대로 저장했다. 이후 보고서 쓰기는 현재 Claude 문서대로 `Edit(보고서 경로)` 권한으로 제한한다. |
| 응답 공백 접기가 테스트 도구에 종속됨 | 부분 수용·반박 | 이 실습 API는 정규화된 표시 결과를 반환하는 계약이다. `docs/item-bank/MIGRATION-PLAN.md:119`와 `:120`, `characterization/lib/normalize.mjs:83`(HTML 공백 처리), `:189`(JSON rows 원문 유지)가 근거다. 원문 반환 제안은 계약을 바꾸므로 반박한다. 해당 `ItemSearchService.java` 주석에 계약과 근거 경로를 추가했고 실행 로직은 바꾸지 않았다. |
| 제목·등록 시각 정렬의 추가 저장소 단위 테스트 | 제안으로 보류 | 해당 정렬은 `characterization/tests/item-bank.test.js:33`부터의 실제 MariaDB HTTP 케이스로 검증한다. 추가 H2 테스트는 이번 수용한 치명·경고 수정 범위에 넣지 않는다. |
| H2 뷰 정의 복사의 추후 불일치 위험 | 제안으로 기록 | `modern/api/src/test/java/com/example/item/ItemSearchRepositoryTest.java:27`에 테스트 뷰가 정의되어 있다. 운영 DDL이 바뀌면 함께 점검할 항목으로 v1 체크리스트에 반영한다. 이번 요청은 DDL 변경이 아니다. |
| v0 SQL 문자열 조립 금지 표현 | 기준 수정 수용·코드 변경 반박 | `ItemSearchRepository.java:56`, `:60`, `:67`, `:72`는 외부 값을 바인딩하고 `:79`부터 정렬은 enum 고정 문자열이다. SQL 인젝션 근거가 없고 코드 변경은 불필요하다. v1에서는 외부 값을 SQL 구문에 직접 삽입하는 것과 고정 조각 조립을 구분한다. |
| assignment 분석 문서의 범위 | 기존 승인 확인 | Day1-2 Skill 재사용 실습에서 생성한 문서3개다. 현재 요청과 STARTUP.md에 승인된 준비 작업으로 명시했다. 반복 승인을 요구하지 않는다. |
| 검색 케이스39개라는 원문 기록 | 오기 정정 | `characterization/tests/item-bank.test.js:6`의 cases는40개이며 스냅샷40개와 Vitest 결과도 확인했다. 원문을 바꾸지 않고 여기에서 정정한다. |
| 서버가 현재 HEAD 빌드인지 확인 필요 | 수용 | Codex가 직접 현재 작업의 bootRun을 기동했다. 주석 보강 커밋 후 API를 다시 기동하고 동일 테스트를2차 독립 리뷰에서 실행한다. |

## 수정 범위

- API 파일은 리뷰가 지적한 `ItemSearchService.java`의 설명 주석만 보강했다.
- 기능·원본 PHP·DB·기존 테스트·스냅샷·보안 잠금 파일을 변경하지 않았다.
- 확정된 기능 결함이 없어 실패를 만들거나 제안 사항을 임의 구현하지 않았다.
- 필수 확인 필요인 API 실행을 해결한 후 새 Sonnet 세션으로 재리뷰한다.
