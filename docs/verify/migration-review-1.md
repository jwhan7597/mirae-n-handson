# 마이그레이션 리뷰 1 (Day2-1 독립 첫 리뷰)

판정: 반려

- 반려 사유는 코드 결함이 아니다. 필수 기준 1(테스트 통과)에서 `modern/api`의 `./gradlew test --rerun-tasks` 실행이 도구 권한 거부(don't ask 모드)로 막혀 `실행하지 않음`이다. 실행하지 않은 테스트를 통과로 쓸 수 없고, 확인 필요인 필수 기준이 남으면 승인하지 않는다.
- 비교 범위 `upstream/main...HEAD`. 요청 범위: 레거시 문항 검색 GET(`legacy/item-bank-php/search.php`) 하나를 현재 동작을 바꾸지 않고 `modern/api`로 이관하고 검증·분석·회고를 기록한다.
- 확인한 근거: diff, `modern/api` 신규 소스·테스트, `characterization/tests/item-bank.test.js`, 루트 `CLAUDE.md`, `templates/approval-checklist.md`, `templates/verification-loop.md`, `convention-check` Skill, 비교용 `search.php`. 이전 이관 결과·회고 보고서(`docs/item-bank/MIGRATION-REVIEW.md` 등)는 읽지 않았다.

## 기준표

| 기준 | 결과 | 근거 |
|---|---|---|
| 테스트 통과 | 확인 필요(미충족) | API: 실행하지 않음(권한 거부). characterization 두 명령은 실행해 모두 실패 0건(아래). |
| 치명 이슈 0건 | 충족 | 치명 0건. 값은 모두 바인딩, ORDER BY는 enum별 고정 문자열(`ItemSearchRepository.java:78-90`). |
| 요청 범위 이탈 없음 | 충족 | 변경 28개 파일이 요청(검색 GET 이관·테스트·분석/회고 문서)과 승인된 준비 작업으로 설명됨. 확인 필요 1건(아래). |
| 컨벤션 준수 | 충족 | 위반 0건. 경고 1건·제안은 아래에 따로 적음. |

## 테스트 실행 결과

| 명령 | 결과 |
|---|---|
| `cd modern/api && ./gradlew test --rerun-tasks` | **실행하지 않음** — Bash 도구 권한 거부(don't ask 모드). 통과·실패 수 없음. `build/test-results`의 기존 XML은 이번 실행 결과가 아니라서 합산하지 않았다. |
| `cd characterization && npm test` | 파일 3개 통과, 테스트 54개 통과, 실패 0, 건너뜀 0 |
| `cd characterization && TARGET_BASE_URL=http://localhost:8080 npm test` | 파일 2개 통과·1개 건너뜀, 테스트 53개 통과, 실패 0, 건너뜀 1 |

- 건너뜀 1건은 `characterization/tests/example-units.test.js:18,21`의 `skipIf(legacyOnly)`이다. `TARGET_BASE_URL`이 있으면 레거시 `units.php` 전용 예시를 건너뛰는 설계이며(같은 파일 11행 주석), 이번 변경 파일이 아니고 item-bank 케이스도 아니다. item-bank 검색 39개 케이스는 두 실행 모두 통과했다.
- 8080의 API가 현재 HEAD 빌드인지는 확인하지 못했다. 관리자가 켠 프로세스라서, 위 통과는 "지금 떠 있는 서버"에 대한 결과다.

## 이슈 목록

치명: 0건.

| # | 심각도 | 파일:줄번호 | 근거 | 수정 방향 |
|---|---|---|---|---|
| 1 | 경고 | `modern/api/src/main/java/com/example/item/ItemSearchService.java:31-33,175-178` | 운영 코드가 응답 문자열(`title`, `unit`, 태그)의 공백을 정규화 도구(JS `\s`) 기준으로 접고 trim한다(`DISPLAY_WHITESPACE`). 근거는 주석 한 줄뿐이다. DB 값의 연속 공백·NBSP가 API 응답에서 바뀌므로, 정규화 도구의 정의에 운영 응답이 묶여 있다. 정규화 규칙이 바뀌면 응답이 같이 바뀌거나 테스트만 맞는 상태가 될 수 있다. 값이 실제로 깨지는 사례는 확인하지 못했다. | 응답을 레거시 HTML 화면 기준으로 의도적으로 접는 것인지 사람이 결정하고 `docs`에 근거를 남긴다. 아니면 원문을 반환하고 정규화는 characterization 쪽에서만 한다. 판단은 사람이 한다. |
| 2 | 제안 | `modern/api/src/test/java/com/example/item/ItemSearchRepositoryTest.java:134-150` | 저장소 테스트가 `ID_*`, `LEVEL_*`, `UNIT_*`만 검증하고 `TITLE_ASC/DESC`, `CREATED_ASC/DESC`(`ItemSearchRepository.java:82-83,88-89`)는 단위 수준 검증이 없다. characterization(`item-bank.test.js` 정렬 케이스)이 서버 대상으로만 덮는다. | H2에서 확인 가능한 범위(제목 정렬, 같은 제목 id 보조 키)로 테스트를 추가한다. `created_at`은 시각이 같은 시드면 변별력이 없으니 시각을 달리 심는다. |
| 3 | 제안 | `modern/api/src/test/java/com/example/item/ItemSearchRepositoryTest.java:27-47` | H2 테스트가 `db/mariadb/init/01-schema.sql:52`의 `v_item_public` 정의를 복사해 쓴다. 원본 뷰가 바뀌어도 이 테스트는 알아채지 못한다. | 복사본임을 주석에 남긴 현재 상태를 유지하되, 뷰를 바꾸는 변경의 체크 항목에 이 테스트를 넣는다. |
| 4 | 제안 | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:34,42-45,52-74` | 체크리스트 v0 "SQL을 문자열로 이어 붙여 만드는 곳이 없다"를 글자 그대로 읽으면 SQL 조립(`StringBuilder`, `+`)이 해당한다. 그러나 이어 붙이는 조각은 고정 리터럴 또는 enum 상수별 고정 문자열이고, 외부 입력은 모두 `:keyword` 등 바인딩이다(`:56,60,67,72,46-47`). 외부 입력이 SQL에 들어가는 경로는 확인하지 못했으므로 치명이 아니다. | 이 항목은 이슈가 아니라 **기준 v0의 모호함**이다. v1에서 "외부 입력의 직접 삽입 금지, 고정 조각·allowlist 정렬은 허용"으로 문구를 구분한다. 코드 수정은 필요 없다. |

## 필수 확인 필요 (이번 변경의 확정 이슈 아님)

1. **API 테스트 실행.** `modern/api`에서 `./gradlew test --rerun-tasks`를 권한이 있는 세션에서 실행하고 `build/test-results/test/TEST-*.xml`을 합산해 통과·실패·건너뜀 수를 기록해야 한다. 이 결과가 있어야 기준 1을 판정할 수 있다. 새 테스트 4개 클래스(`ItemSearchController/Repository/Request/ServiceTest`)와 기존 테스트의 `@Disabled`·삭제 여부는 diff상 변경된 기존 테스트 파일이 없어 문제는 보이지 않으나(변경 목록에 기존 테스트 없음), 실행으로 확인되지 않았다.
2. **8080 서버의 빌드.** characterization 통과가 HEAD 빌드의 결과인지 확인해야 한다(위 설명).
3. **`docs/assignment/*` 3개 파일(ARCHITECTURE·BUSINESS-RULES·ERD).** 검색 GET이 아닌 assignment 모듈 분석 문서다. 요청의 "Day1-1/2 … 분석 문서" 승인 범위로 보고 이탈로 세지 않았지만, 범위에 들어가는지는 요청자가 확인해야 한다. 코드·설정 변경은 없다.

## 이번 변경 범위 밖 / 기존 사실

- 요청에서 제외한 항목(원시 HTML 폼·경고·링크, POST, DB 장애, 잘못된 UTF-8, 비표준 파라미터 키의 서버 차이)은 판단하지 않았다. 참고로 `ItemSearchRequest.java:58`의 `URLDecoder.decode`는 잘못된 퍼센트 인코딩에서 예외를 던질 수 있으나 이는 제외 범위의 입력이다.
- `characterization` 스냅샷(`item-bank.test.js.snap`, 4380줄)은 이번 변경에서 새로 추가된 파일이며 내용은 필요한 부분 외에는 읽지 않았다. 스냅샷을 고쳐 통과시킨 흔적(기존 파일 변경)은 diff에 없다.
- `characterization/package-lock.json`의 `source-map-js` 1.2.1→1.2.2는 사전 승인된 보안 잠금 수정이며 되돌리지 않았다.

## 컨벤션 대조 요약 (`CLAUDE.md` 2절)

- Controller→Service→Repository 순서, 생성자 주입: `ItemSearchController.java:17-19`, `ItemSearchService.java:37-39`, `ItemSearchRepository.java:27-29`. 컨트롤러는 Repository·SQL을 쓰지 않는다.
- record DTO 응답이며 엔티티를 반환하지 않는다(`ItemSearchResponse`, `ItemSearchRowResponse`). 변환은 `@Transactional(readOnly = true)` 서비스 안(`ItemSearchService.java:17,45-47`).
- 빈 `catch` 없음, `System.out`·`printStackTrace` 없음, 로그는 SLF4J(`:20,48`). 로그에 검색어 원문을 남기지 않는다(건수·offset만).
- `ACTIVE` 공개 필터는 `v_item_public`(`status='A'`)에 맡기며 기존 `ItemController` 경로는 변경하지 않았다. 의존성·스키마·시드·접속 설정 변경 없음(`modern/api/build.gradle`·`resources` diff 없음).
- 새 엔드포인트·서비스·저장소·요청 파서에 대응하는 테스트가 있다(`ItemSearch*Test` 4개). 실행 결과는 위 "필수 확인 필요 1".


---
저장 방식: 리뷰 세션의 보고서 Write가 권한 거부되어, Codex가 해당 도구 요청의 본문을 수정 없이 파일로 저장했다. 판정·수치·이슈는 독립 리뷰 원문이다. 숫자 오기는 대응 보고서에서 바로잡는다.
