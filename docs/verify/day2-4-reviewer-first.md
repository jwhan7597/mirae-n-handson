# Day 2-4 독립 reviewer 첫 호출 원문

**판정: 반려 (확인 필요 포함)**

일부 도구가 막혀서 `git diff`는 하나도 실행하지 못했습니다. 이 때문에 diff 본문 대신 working tree의 파일을 직접 읽었고, 아래 판단은 모두 파일 본문 기준입니다. 이번 비교 범위의 파일 목록은 처음 실행한 `git diff --stat`으로만 확인했습니다. 테스트는 실행하지 않았습니다.

## 기준 표

| 기준 | 결과 | 근거 |
|---|---|---|
| 테스트 통과 | 확인 불가 | 실행 증빙이 전달되지 않았습니다. 새 동작에 대응하는 테스트는 대부분 있습니다(아래 W1·W2 제외). 레거시와 새 API 양쪽 실행 결과 비교도 확인 불가입니다. |
| 치명 이슈 0건 | 충족 | 하드코딩된 인증정보는 없습니다. 값은 모두 바인딩합니다(`ItemSearchRepository.java:55-72`). ORDER BY는 enum별 고정 문자열입니다(`ItemSearchRepository.java:78-91`). |
| 요청 범위 이탈 없음 | 충족 | 변경은 `modern/api`의 `ItemSearch*` 파일, `characterization` 테스트와 스냅샷, 승인된 lock 파일뿐입니다. 스키마·시드·의존성 변경은 확인되지 않았습니다. |
| 컨벤션 준수 | 미충족 | 계층 구조·DTO·생성자 주입·SLF4J는 준수했습니다. 다만 "잘못된 입력도 400으로 거부하지 않는다"는 호환 조건을 지키지 못하는 경로가 있습니다(W1). |

## 이슈

**경고 W1 (새 이슈, 코드 근거 확인, 실행 미확인)**
- 위치: `modern/api/src/main/java/com/example/item/ItemSearchRequest.java:57-59`
- 근거:
  - `URLDecoder.decode`는 `q=%`나 `q=%zz` 같은 잘못된 percent 인코딩에서 `IllegalArgumentException`을 던집니다.
  - 이 예외는 `GlobalExceptionHandler.java:36-44`에서 400으로 변환됩니다.
  - 레거시 PHP는 같은 입력을 거부하지 않고 조회합니다.
  - 컨트롤러 주석(`ItemSearchController.java:21`)과 `MIGRATION-PLAN.md:102-109`는 비정상 입력도 거부하지 않는다고 적고 있습니다.
  - 이 경로를 검증하는 테스트가 없습니다. `ItemSearchRequestTest.java:13-38`에는 이 입력이 없습니다.
- 수정 방향: 잘못된 `%`는 PHP처럼 문자 그대로 두도록 디코더를 직접 구현하거나 예외를 처리하고, 해당 테스트를 추가합니다.
- 제약: PHP 쪽 실제 실행 결과는 확인하지 못했습니다. 실행으로 확정이 필요합니다.

**경고 W2 (새 이슈, 테스트 누락)**
- 위치: `ItemSearchServiceTest.java:56-70`, `ItemSearchService.java:92-105`
- 근거:
  - 페이지의 후행 개행 하나 허용(`"2\n"`)은 PHP `$`를 따르려고 추가한 분기입니다. 이 분기를 검증하는 케이스가 없습니다.
  - 선행 0과 4자리 이상 숫자는 테스트되는지 확인하기 어렵습니다. `0002`, 아주 긴 숫자열 케이스가 없습니다.
- 수정 방향: 해당 입력들을 `@CsvSource`에 추가합니다.

**확인 필요 C1 (추정, 확정 이슈로 세지 않음)**
- 위치: `ItemSearchService.java:111-122`
- 내용: 난이도로 `1e30`, `1e1000` 같은 범위 밖 지수값이 오면 Java는 `(long)double`로 최댓값에 머뭅니다. PHP `(int)`의 실제 결과와 같은지 실행으로 확인한 근거가 없습니다. 이 입력의 테스트도 없습니다.

**확인 필요 C2**
- 위치: `characterization/lib/target.mjs`, `characterization/tests/item-bank.test.js:5-46`
- 내용: 스냅샷 기대값은 레거시 응답 기준입니다. 이 38개 케이스를 새 API(`TARGET_BASE_URL`)에 실제로 돌린 결과는 전달되지 않았습니다. CLAUDE.md §5의 완료 기준(`characterization` `npm test` 전체 통과)과 체크리스트 1번의 양쪽 실행 비교가 확인 불가입니다.

**제안**
- `ItemSearchService.java:128-143` `phpTrim`(NUL·수직 탭)과 `truncate`(4바이트 문자)를 개별 테스트로 보강합니다.
- `ItemSearchService.java:36-37` `DISPLAY_WHITESPACE`는 JS `\s`와 같은 집합을 의도한 것으로 보입니다. NBSP·전각 공백 케이스를 하나 추가하면 좋습니다.
- `ItemSearchRepositoryTest.java:27-47`의 `v_item_public` 정의는 테스트 안의 복사본입니다. 운영 DB 뷰 정의와 어긋나지 않는지 주기적으로 대조하는 것을 권합니다. 이번 diff에서 뷰는 변경되지 않았습니다.

**기존 문제**
- 레거시 전용 제외 1건은 변경 전부터 있던 기존 테스트로, 새 이슈로 세지 않았습니다.
- 레거시 화면의 경고·요약·페이지 링크는 정규화 대상이 아닙니다. 응답에서 빠진 것은 `MIGRATION-PLAN.md:97`의 계약대로이며 범위 이탈이 아닙니다.

## 승인 조건
- W1을 수정하거나 PHP 실제 동작을 확인한 근거를 제시합니다.
- W2의 테스트를 보완합니다.
- 테스터 실행 증빙(API `./gradlew test`, `characterization` 양쪽 대상 `npm test`)을 전달합니다.

관련 파일은 모두 `/Users/jwhan/work/mirae-n-handson/` 아래입니다.
- `modern/api/src/main/java/com/example/item/ItemSearchRequest.java`
- `modern/api/src/main/java/com/example/item/ItemSearchService.java`
- `modern/api/src/test/java/com/example/item/ItemSearchRequestTest.java`
- `modern/api/src/test/java/com/example/item/ItemSearchServiceTest.java`
- `characterization/tests/item-bank.test.js`

커밋·푸시·코드 수정은 하지 않았습니다.

---
독립 Claude reviewer 반환 원문을 Codex가 저장했다. 수용·반박·숫자 정정은 day2-4-review-response.md에 따로 기록한다.
