# Day 1-3 이관 대조·검증 결과

2026-10-07 · 에듀테크R&D팀(rnd) · 대상: 문항 검색 GET 하나.

## 검증 범위와 결과

- PHP `GET /search.php` → Java `GET /api/items/search`.
- 계획을 먼저 작성하고 40개 실제 응답 스냅샷을 기준점으로 커밋했다.
- 레거시 전체 테스트는 54개 두 번 연속 통과했고, 마지막에도 54개 통과했다.
- 새 API 전체 비교는 **53개 통과·1개 건너뜀**: 검색 40개는 모두 통과했다. 건너뜀 1개는 기존 예시 단원 화면 전용 테스트가 원래 TARGET_BASE_URL에서 제외하도록 설정된 것이다.
- Java: 최초69개 → 배열 처리 회귀 테스트 추가 후 **80개 통과**, 실패·오류·건너뜀0.
- 별도 읽기 전용 HTTP 입력16개: 최초5개 불일치 → 수정 후 **16개 모두 동일**. 기존 스냅샷을 바꾸지 않았다.
- 보호 대상 legacy/·db/·공용 정규화·기존 테스트·승인된 보안 잠금 파일66개의 SHA-256이 유지됐다. 기준 확립 후 characterization의 전체 관리 파일13개도 그대로다.

아래는 정상 로컬 DB에서 공용 정규화가 수집하는 조회 결과의 범위에 대한 정적 대조와 실행 결과다.
모든 HTTP·인코딩·데이터 분포에 대한 완전한 동등성 증명은 아니다.

## 규칙별 최종 대조

| 규칙 | 레거시 | 새 코드 | 판정 | 확인 내용 |
|---|---|---|---|---|
| BR-01 | `legacy/item-bank-php/search.php:522` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:16` | 동일 | 기존 공개 뷰를 그대로 읽는다. H2 실제 SQL 테스트에서 A/R/D 필터를 검증했다. |
| BR-02 | `legacy/item-bank-php/search.php:98` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:51` | 동일 | AND 조합과 제목/지문 OR 괄호를 보존한다. |
| BR-03 | `legacy/item-bank-php/search.php:53` | `modern/api/src/main/java/com/example/item/ItemSearchRequest.java:23` | 초기 다름 → 수정 후 동일 | 색인·혼용·반복·중첩 배열을 원문 출현 순서로 처리한다. PHP 9개 변환과 HTTP 5개 차이를 재현하고 수정했다. |
| BR-04 | `legacy/item-bank-php/search.php:98` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:55` | 동일 | 같은 DB에 제목/지문 LIKE 조건을 바인딩한다. |
| BR-05 | `legacy/item-bank-php/search.php:86` | `modern/api/src/main/java/com/example/item/ItemSearchService.java:53` | 동일 | PHP trim과 코드포인트 100자 제한. HTTP `%` 100개 뒤 문자를 붙인 케이스로 자르기를 구분했다. |
| BR-06 | `legacy/item-bank-php/search.php:97` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:56` | 동일 | %와 _를 LIKE 와일드카드로 유지한다. |
| BR-07 | `legacy/item-bank-php/search.php:118` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:59` | 동일 | 단원 입력을 변경하지 않고 같은 MariaDB에 바인딩한다. |
| BR-08 | `legacy/item-bank-php/search.php:111` | `modern/api/src/main/java/com/example/item/ItemSearchService.java:54` | 조회 동일 / 경고 범위 제외 | 없는 단원도 200·0건. 소문자는 같은 DB 콜레이션으로 비교한다. |
| BR-09 | `legacy/item-bank-php/search.php:215` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:64` | 동일 | 난이도 미지정은 level < 5다. |
| BR-10 | `legacy/item-bank-php/search.php:217` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:66` | 동일 | 지정한 난이도는 정수 비교한다. |
| BR-11 | `legacy/item-bank-php/search.php:226` | `modern/api/src/main/java/com/example/item/ItemSearchService.java:107` | 조회 동일 | 05/3abc/3.7/1e0 및 추가 +3abc/큰 정수 입력을 검증했다. TINYINT 범위 밖 숫자의 내부 double 정밀도는 PHP와 다를 수 있지만 현재 조회는 둘 다 0건이다. |
| BR-12 | `legacy/item-bank-php/search.php:244` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:70` | 동일 | 태그 이름 정확 비교와 EXISTS를 보존한다. |
| BR-13 | `legacy/item-bank-php/search.php:240` | `modern/api/src/main/java/com/example/item/ItemSearchService.java:56` | 조회 동일 / 경고 범위 제외 | 50자 제한. 태그 %는 부분 일치가 아니다. |
| BR-14 | `legacy/item-bank-php/search.php:253` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:70` | 조회 동일 / 경고 범위 제외 | 미등록 이름도 거부하지 않고 조회한다. |
| BR-15 | `legacy/item-bank-php/search.php:320` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:86` | 동일 | 기본 level DESC,id ASC. |
| BR-16 | `legacy/item-bank-php/search.php:322` | `modern/api/src/main/java/com/example/item/ItemSearchService.java:80` | 조회 동일 / 경고 범위 제외 | 미등록 기준은 기본 정렬로 처리한다. |
| BR-17 | `legacy/item-bank-php/search.php:277` | `modern/api/src/main/java/com/example/item/ItemSearchRepository.java:79` | 동일 | 정렬 10종과 unit의 level DESC,id ASC 보조 키를 보존한다. |
| BR-18 | `legacy/item-bank-php/search.php:266` | `modern/api/src/main/java/com/example/item/ItemSearchService.java:69` | 조회 동일 / 경고 범위 제외 | 잘못된 방향은 각 기준의 기본 방향. ASCII 소문자 변환과 공백 처리가 같다. |
| BR-19 | `legacy/item-bank-php/search.php:349` | `modern/api/src/main/java/com/example/item/ItemSearchService.java:22` | 동일 | 페이지 크기20, offset=(page-1)*20. |
| BR-20 | `legacy/item-bank-php/search.php:336` | `modern/api/src/main/java/com/example/item/ItemSearchService.java:88` | 동일 | page 원문 미trim, 숫자/선행0 처리와 1~999 경계. PHP $의 끝 개행 처리도 유지한다. |
| BR-21 | `legacy/item-bank-php/search.php:526` | `modern/api/src/main/java/com/example/item/ItemSearchService.java:44` | 동일 | 전체 count와 현재 페이지를 별도 조회. 범위 밖 빈 행에서도 count와 message null 유지. |
| BR-22 | `legacy/item-bank-php/search.php:657` | `modern/api/src/main/java/com/example/item/ItemSearchResponse.java:17` | 동일 | 총0일 때만 검색 결과가 없습니다. |
| BR-23 | `legacy/item-bank-php/search.php:685` | `modern/api/src/main/java/com/example/item/ItemSearchController.java:23` | 범위 제외 | HTML 페이지·정렬 링크는 JSON 검색 결과 API의 범위가 아니다. |
| BR-24 | `legacy/item-bank-php/search.php:678` | `modern/api/src/main/java/com/example/item/ItemSearchService.java:181` | 정규화된 조회 결과 동일 | 뷰에서 태그id 순 CSV를 읽고 기존 HTML 정규화와 같은 배열로 반환한다. |
| BR-25 | `legacy/item-bank-php/search.php:712` | `modern/api/src/main/java/com/example/item/ItemSearchController.java:23` | 범위 제외 / 차이 명시 | DB 장애는 PHP 200 HTML, 새 API는 기존 GlobalExceptionHandler를 따라 500 JSON. 장애를 유도하지 않았다. |

BR-26~39(등록), BR-40~41(단원 목록), BR-42(화면 선택 목록)는 이번 기능 이관 범위에서 제외했다.

## 독립 검토와 실제 판정

Opus 5.5가 최초 구현을 작성했고 Sonnet 5.5가 읽기 전용으로 규칙 대조를 수행했다.
Codex가 PHP 실행 결과와 실제 HTTP 응답으로 지적 사항을 판정한 뒤 새 Java 코드만 수정했다.
원본 CLI 출력과 최초 코드는 별도 로컬 증빙 폴더에 보존했다.

| 지적 | 판정 · 조치 |
|---|---|
| q[0]·q[a]·혼용 순서 불일치 | 실제 오류. 최초 구현은 색인 키를 무시하고 MultiValueMap에서 키 사이 순서를 잃었다. 원문 파싱으로 수정하고 Java 회귀 테스트를 추가했다. |
| 모든 스냅샷이 통과해도 입력 변환 누락 가능 | 실제 확인. 초기 검색40개는 모두 통과했지만 추가16개 중5개가 달랐다. 기준을 고치는 대신 이관 코드를 수정했다. |
| 키워드101자 입력만으로 자르기 검증 부족 | 타당. `%`100개+문자 입력의 PHP/API 응답20건 일치로 보완했다. 기존 테스트·스냅샷은 유지했다. |
| 공백 접기·태그 CSV 분할은 부당한 추가 규칙 | 이 실습에서는 채택하지 않았다. JSON 정규화는 행 문자열을 접거나 CSV를 분할하지 않으므로, 계획에서 요구한 정규화 결과와 같은 모양을 API가 반환해야 한다. 원시 DB 값을 반환하는 범용 API 설계와는 구분한다. |
| “오류는 정상 빈 검색과 정규화 후 구분 불가” | 부정확. PHP 오류 화면은 message null, 정상0건은 검색 결과가 없습니다이므로 구분된다. 실제 장애 발생은 검증하지 않았다. |
| 기존 입력 케이스41개 | 숫자 오기. 테스트 파일의 cases는40개이고 베이스라인40개와 실행 결과로 확인했다. |
| 원시 대괄호·잘못된 UTF-8·비표준 키·요청 제한 | 이번 표준 UTF-8 인코딩 GET 조회 범위에서 검증하지 않았다. Tomcat/설정 변경을 임의로 추가하지 않았다. |

## 남은 차이·검증 한계

- HTML 폼·경고·요약·링크, POST 요청, DB 장애의 오류 응답은 이관 범위 밖이다.
- PHP의 비표준 키 이름 보정, 잘못된 인코딩, 서버별 요청/중첩/변수 개수 제한까지 동일하다고 주장하지 않는다.
- TINYINT 범위 밖 정수 변환의 내부 정밀도는 다를 수 있다. 현재 검색 결과가0건인 범위에서는 영향이 없다.
- 공백이 많은 임의 제목·쉼표를 포함한 새 태그 등 현재 시드에 없는 모든 데이터를 검증하지 않았다.
- 팀 저장소 `tristan-kkim/mirae-n-claude-harness-rnd`는 접근404, 초대 목록0건이다. rnd 신청 댓글은 이미 등록되어 있어 추가 댓글을 보내지 않았다.

## 증빙

로컬: `/Users/jwhan/work/mirae-n-preparation/day1-3-evidence/`

- `baseline-summary.json`, `legacy-verification.txt`, `legacy-final.log`
- `modern-initial.log`, `modern-final.log`, `java-initial-summary.json`, `java-final-summary.json`
- `review-legacy.json`, `review-modern-initial.json`, `review-modern.json`, `probes-initial.log`, `probes-final.log`
- `php-request-casts.json`, `protected-before.json`, `baseline-hashes.json`, `protected-final.json`
- `implementation-stream.jsonl`, `review-stream.jsonl`, `initial-implementation/`
