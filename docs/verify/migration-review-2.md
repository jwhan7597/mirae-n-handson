판정: 승인

검증 대상: `upstream/main...HEAD` (변경 31개 파일)
요청 범위(한 문장): 레거시 문항 검색 GET 하나를 현재 동작을 바꾸지 않고 Java API로 이관하고 테스트·분석·회고를 기록한다. CLAUDE.md·Day1-1/2 문서/Skill 및 승인된 source-map-js 보안 수정은 승인된 준비 작업이고, Day2-1 준비·리뷰 기록과 응답 계약 주석 보강도 요청에 포함된다.

## 기준별 결과

| 기준 | 결과 | 근거 |
|---|---|---|
| 테스트 통과 | 충족 | 아래 테스트 실행 결과 참조. 3개 명령 모두 실패 0건, 이번 실행으로 확인 |
| 치명 이슈 0건 | 충족 | 치명 0건. SQL 값 전부 바인딩(`ItemSearchRepository.java:55-72`), ORDER BY는 enum별 고정 문자열(`:78-91`), 비밀값·삭제/덮어쓰기 코드 없음 |
| 요청 범위 이탈 없음 | 충족 | 코드 변경은 `com.example.item.ItemSearch*` 신규 9개와 테스트 4개, `characterization/tests/item-bank.test.js`·스냅샷뿐. 기존 소스·설정·`build.gradle` 변경 없음 |
| 컨벤션 준수 | 충족 | 위반 0건. 아래 컨벤션 대조 참조 |

## 테스트 실행 (이번 세션에서 실제 실행)

| 명령 | 종료 | 통과 | 실패 | 건너뜀 |
|---|---|---|---|---|
| `cd modern/api && ./gradlew test --rerun-tasks` | BUILD SUCCESSFUL (5 tasks executed, 캐시 아님) | 80 | 0 | 0 |
| `cd characterization && npm test` | 성공 | 54 | 0 | 0 |
| `cd characterization && TARGET_BASE_URL=http://localhost:8080 npm test` | 성공 | 53 | 0 | 1 |

- API 합계는 `modern/api/build/test-results/test/*.xml` 13개 파일의 `tests` 속성을 합산했다. `failures`·`errors`·`skipped`는 모두 0이다. 이번 변경분은 ItemSearch* 4개 클래스 56건(Controller 6, Repository 7, Request 10, Service 33)이다.
- 건너뜀 1건은 `characterization/tests/example-units.test.js:21`의 `it.skipIf(legacyOnly)`로, 레거시 전용 케이스다. 이번 diff가 추가한 것이 아니고 `item-bank.test.js`에는 skip이 없다. 의도된 건너뜀으로 판단한다.
- 첫 시도에서 `cd characterization`이 현재 디렉터리가 `modern/api`로 남아 있어 `no such file or directory`로 실패했다. 테스트가 실행되지 않은 경로 오류였고, 저장소 루트 절대경로로 같은 명령을 다시 실행해 위 결과를 얻었다.
- 두 번째 `TARGET_BASE_URL` 실행은 `localhost:8080`의 Java API(이번 HEAD의 bootRun)를 대상으로 한다. 스냅샷 대조가 통과했으므로 동작 보존 근거가 된다.
- 기존 테스트를 지우거나 `@Disabled`로 바꾼 곳은 없다(`ItemSearch*.java` 전체 Grep 결과 `@Disabled`, `catch`, `System.out/err`, `printStackTrace` 0건).

## 컨벤션 대조 (CLAUDE.md §2, convention-check 7항목)

| 항목 | 결과 | 근거 |
|---|---|---|
| 컨트롤러가 Repository/SQL 직접 사용 | 없음 | `ItemSearchController.java:17-25`는 서비스만 주입 |
| 빈 catch / 예외 무시 | 없음 | `catch` 0건 |
| 임시 출력 | 없음 | 로깅은 SLF4J `log.debug` (`ItemSearchService.java:52`) |
| record DTO, 엔티티 미노출 | 충족 | `ItemSearchResponse`, `ItemSearchRowResponse`는 record, 엔티티 미사용 |
| 의존성·스키마·설정 변경 | 없음 | 변경 목록에 `build.gradle`, `application*.yml`, 시드 없음. `package-lock.json`은 아래 확인 필요 참조 |
| 새 동작에 대응하는 테스트 | 충족 | Controller·Request·Service·Repository 각각 테스트 존재 |
| 화면(`modern/web`) 항목 | 해당 없음 | 변경 없음 |

## 이슈 목록 (근거 확인됨)

치명 0건, 경고 0건. 제안만 남긴다. 제안은 승인 여부에 영향을 주지 않는다.

| # | 심각도 | 파일:줄번호 | 근거 | 수정 방향 |
|---|---|---|---|---|
| 1 | 제안 | `modern/api/src/main/java/com/example/item/ItemSearchService.java:121` | `(long) Double.parseDouble(...)`는 2^53을 넘는 정수 문자열에서 정밀도를 잃는다. PHP `(int)`는 정수 문자열을 정확히 읽는다. 다만 난이도 컬럼 범위를 벗어난 값은 어느 쪽이든 0건이라 현재 응답이 달라지는 입력은 확인하지 못했다 | 정수형 접두사만 `Long.parseLong`(범위 초과 시 포화)로 읽고 소수·지수만 double로 처리. 이번 이관에서 꼭 할 필요는 없음 |
| 2 | 제안 | `modern/api/src/test/java/com/example/item/ItemSearchServiceTest.java` (page/level 파라미터 테스트) | `level=[9223372036854775808]`처럼 long 범위 밖 값의 포화 동작을 고정하는 케이스가 없다. 코드 주석(`ItemSearchService.java:120`)은 포화를 명시한다 | 해당 주석을 뒷받침하는 케이스 1건 추가 |

## 확인한 내용 (이슈 아님)

- 응답 계약 주석(`ItemSearchService.java:31-34`)이 인용한 `docs/item-bank/MIGRATION-PLAN.md` §5가 `:111`에 실제로 있다. `characterization/lib/normalize.mjs:83`의 `text()`가 `\s+`를 한 칸으로 접고 `trim()`하는 것도 확인했다. `DISPLAY_WHITESPACE` 정규식(`:36-37`)과 `stripSpaces`(`:192-201`)가 이를 JSON 행에 맞추는 구현이다. 계약과 코드가 일치한다.
- 바인딩된 SQL: 키워드는 `LIKE :keyword`로 `%`·`_`를 이스케이프하지 않아 레거시 와일드카드 동작을 보존한다(`ItemSearchRepository.java:54-56`). 난이도 미지정은 `level < 5`(`:62-64`)로 `search.php`의 공개 뷰 기준 동작과 맞고, 스냅샷(`level=5` 명시, 기본 조회)이 통과했다.
- `count`는 전체 건수이고 `message`는 전체 0건일 때만 채운다(`ItemSearchResponse.java:18`). 페이지가 범위를 넘으면 `message=null`이며 `ItemSearchServiceTest`가 이를 고정한다.
- PHP 배열 색인·중복 키·스칼라/배열 혼용은 `ItemSearchRequest.java:23-55`가 원본 쿼리 문자열의 출현 순서로 처리한다. CLAUDE.md §5의 "Map만으로 순서가 보존된다고 가정하지 않는다"를 지킨다. `ItemSearchRequestTest` 10건(파라미터화 포함)과 스냅샷 2건(`q[]`, 단일 키 반복)이 통과했다.
- 범위 외로 제외한 항목: HTML 폼·경고·링크, POST, DB 장애, 잘못된 인코딩·비표준 파라미터 키. 예를 들어 `ItemSearchRequest.java:58`의 `URLDecoder`는 깨진 `%` 인코딩에서 예외를 던지는데, 요청이 범위 외로 명시한 입력이라 이슈로 올리지 않는다.

## 확인 필요 (필수 기준을 막지 않음)

1. `characterization/package-lock.json` 상세 diff: 이 세션에서 `git diff` 재실행이 권한 거부되어 줄 단위 변경은 읽지 못했다. `--stat`은 6줄 변경(+3/-3)이고, 현재 파일의 `source-map-js`는 `1.2.2`(`package-lock.json:1227-1228`)다. 요청이 승인한 보안 수정과 일치하는 것으로 보이지만 줄 단위 diff는 확인하지 못했다.
2. `docs/assignment/*` 3개(ARCHITECTURE, BUSINESS-RULES, ERD): 요청 문장은 문항 검색 하나를 다루는데, 이 문서들은 과제(assignment) 모듈 분석이다. 승인된 Day1 문서 준비 작업으로 보이지만 요청 문장만으로는 확정할 수 없다. 코드·설정 변경이 아니라서 판정에는 반영하지 않았다.
3. 스냅샷 `characterization/tests/__snapshots__/item-bank.test.js.snap`(4380줄)은 이번 diff에서 신규 추가된 파일이라 기준 대비 변경분이 없고, 내용을 줄 단위로 읽지 않았다. 두 대상(레거시, Java API)에서 모두 통과했다는 사실로만 확인했다.
4. 읽지 않은 파일: 요청 지시에 따라 `docs/verify/migration-review-1.md`, `migration-review-response.md`와 기존 이관 결과 보고서는 읽지 않았다.

## 기존부터 존재한 문제

- 없음(이번 세션에서 확인한 범위 기준).

## 요약

기능 결함은 0건이다. 경고·제안 단계까지 다시 살펴 제안 2건만 남겼다. 이 제안들은 입력이 DB 컬럼 범위를 벗어나는 극단값에 한정되고 응답 차이를 확인하지 못했다. 네 기준이 모두 충족돼 승인한다.


---
저장 방식: 리뷰 도구의 Write 권한 거부로 Codex가 도구 요청 본문을 그대로 저장했다. 독립 판정 원문이다.
