판정: 승인 (검증루프 기준 충족. 사람의 머지 승인·팀 합의는 아직 받지 않았다)

# 이관 변경 3차 검증 — GET /api/items/search

- 비교 범위: `upstream/main...HEAD -- modern characterization`
- 요청 범위: Day1의 `legacy/item-bank-php/search.php` 문항 검색 한 엔드포인트를 `modern/api` `GET /api/items/search`로 이관하고, `characterization/lib/normalize.mjs` 기준으로 정규화한 조회 동작을 보존한다. 원시 HTML 동일성은 요청하지 않았다.
- 계약 제외: HTML 폼·경고·요약·링크, POST, DB 장애, 잘못된 percent/UTF-8 인코딩·비표준 키·서버별 요청 제한. 계약을 확대하지 않았다.
- 승인된 준비 작업(범위 이탈로 세지 않음): `characterization/package-lock.json` source-map-js 1.2.1→1.2.2 보안 패치, Day1 모듈 문서화.
- 수행 방식: verify Skill → `reviewer` Sub-agent → `tester` Sub-agent 순서로 위임했고 완료 보고를 받은 뒤 메인이 판정했다. 메인은 코드·테스트를 직접 수정하거나 실행하지 않았다.

## 네 기준

| 기준 | 결과 | 근거 |
|---|---|---|
| 테스트 통과 | 충족 | tester 실측: API 84건 통과·실패 0·오류 0·건너뜀 0, characterization 54건 통과, 새 API 대상 53건 통과·1건 건너뜀(기존 레거시 전용)·실패 0 |
| 치명 이슈 0건 | 충족 | reviewer: 값 바인딩(`ItemSearchRepository.java:55-72`), `ORDER BY`는 enum 고정 문자열(`:78-91`), 하드코딩 인증정보·삭제·권한 변경 없음 |
| 요청 범위 이탈 없음 | 충족 | 소스는 `modern/api`의 `ItemSearch*`와 테스트, `characterization`은 신규 테스트·스냅샷·승인된 lock 패치. 새 의존성·스키마·시드·접속 설정 변경 없음 |
| 컨벤션 준수 | 충족(읽기 기준) | Controller → Service → Repository, 생성자 주입, record DTO, `System.out`·빈 `catch` 없음 |

## 이슈

확정 이슈: 치명 0건, 경고 0건.

W2(해소): 페이지 후행 개행 1개 허용, 개행 2개 거부, 선행 0, 긴 숫자 클램프 분기(`ItemSearchService.java:92-105`)에 대응하는 테스트가 없다는 첫 리뷰의 경고다. tester가 아래 4개 테스트를 추가했고 `--rerun-tasks` 새 실행에서 모두 통과했다. 이 분기의 PHP 대응(`search.php:337-347`)은 reviewer가 입력별로 대조해 동일하다고 확인했다.

제안:
- `ItemSearchService.java:111-122` `phpIntValue`는 지수 표기를 `double`로 변환한다. 범위 밖 난이도 `1e30`/`1e1000`은 더미 DB에서 두 서버 모두 정규화 응답 200·0건으로 동일하게 실측됐다. 내부 숫자 변환의 모든 값이 같다고는 주장하지 않는다. `level=9007199254740993`처럼 정밀도가 문제 되는 큰 정수 접두사 테스트는 후속 과제로 고려할 수 있다.

기존부터 존재한 문제(이번 변경의 확정 이슈 아님):
- W1(잘못된 percent 인코딩): 기존 이관 계약의 제외 범위다. `ItemSearchRequest.java:58`의 `URLDecoder.decode`가 예외를 던질 수 있으나 계약을 확대하지 않는다.

확인 필요: 없음. reviewer의 `확인 불가(테스트 미실행)`는 tester 실측으로 채워졌고 두 보고 사이에 모순되는 관찰은 없었다.

## tester 실행 결과

| # | 명령 | 종료 상태 | 통과 | 실패 | 오류 | 건너뜀 |
|---|---|---|---|---|---|---|
| 1 | `cd /Users/jwhan/work/mirae-n-handson/modern/api && ./gradlew test --rerun-tasks` | `BUILD SUCCESSFUL in 4s` (숫자 종료 코드는 도구 응답에 없음) | 84 | 0 | 0 | 0 |
| 2 | `cd /Users/jwhan/work/mirae-n-handson/characterization && npm test` | 오류 없이 정상 종료 (숫자 종료 코드 없음) | 54 | 0 | 0 | 0 |
| 3 | `cd /Users/jwhan/work/mirae-n-handson/characterization && TARGET_BASE_URL=http://localhost:8080 npm test` | 오류 없이 정상 종료 (숫자 종료 코드 없음) | 53 | 0 | 0 | 1 |

- 1번: 5개 task가 모두 새로 실행됐고 JUnit XML 13개를 집계했다. 타임스탬프가 모두 이번 실행 시각(2026-10-07T04:33 이후)이라 캐시가 아니다. `ItemSearchServiceTest`는 37건이다.
- 건너뜀 1건: `characterization/tests/example-units.test.js:21` `it.skipIf(legacyOnly)('단원 목록 전체 …')`. 조건은 18줄 `legacyOnly = Boolean(process.env.TARGET_BASE_URL)`이고, 새 API에 해당 화면 응답이 없어 건너뛴다(12줄 부근 주석). 이번에 만든 skip이 아니라 기존 제외다.
- `modern/web`은 프런트엔드 변경이 없어 실행하지 않음.

## 추가 테스트

파일: `modern/api/src/test/java/com/example/item/ItemSearchServiceTest.java` (기존 `pageIsDigitsOnlyAndClamped` 56-70줄과 기존 기대값은 그대로. 메인이 Read로 줄번호를 확인함)

| 줄 | 메서드 | 입력 → 기대 |
|---|---|---|
| 72-76 | `pageAllowsSingleTrailingNewline` | `"2\n"` → offset 20 |
| 78-82 | `pageRejectsTwoTrailingNewlines` | `"2\n\n"` → offset 0 |
| 84-88 | `pageIgnoresLeadingZeros` | `"0002"` → offset 20 |
| 90-95 | `pageClampsVeryLongNumber` | `"9"` 100자리 → offset 19960 |

다른 테스트는 새 필수 누락이 확인되지 않아 추가하지 않았다. tester가 바꾼 파일은 `git status` 기준으로 이 파일 하나뿐이고 `characterization`은 수정되지 않았다(tester 보고).

## 위임 결과

- reviewer: 첫 판정은 `반려`였다. 사유는 (1) 테스트 실행 증빙 없음(읽기 전용이라 실행하지 않음), (2) 페이지 분기 테스트 없음(W2)이다. 치명·경고 확정 이슈는 없었다. 두 사유는 tester 실측과 추가 테스트로 채워졌다.
- tester: 4개 테스트 추가 후 세 명령을 모두 실행했고 실패·오류 0건이다. 추가 테스트는 소스 수정 없이 통과했다.
- 메인: 두 보고가 엇갈리는 지점이 없어 승인으로 판정했다.

## 증빙의 범위와 한계

- reviewer가 본문을 읽은 파일: `ItemSearchService`, `ItemSearchRepository`, `ItemSearchController`, `ItemSearchRequest`, `ItemSearchServiceTest`, `search.php:300-379`, `characterization/tests/item-bank.test.js`, `package-lock.json` diff.
- 읽지 않았거나 Grep으로만 본 것: `ItemSearchControllerTest`·`ItemSearchRepositoryTest`·`ItemSearchRequestTest` 본문, 4380줄 스냅샷, DTO 클래스, `review-boundary-probes.json`(전달받은 내용을 근거로 함).
- 새 페이지 테스트 4건은 Mockito 기반 서비스 단위 테스트다. 실제 PHP와의 개행·선행 0 일치는 reviewer의 코드 대조(`search.php:337-347` vs `ItemSearchService.java:92-105`)에 근거하며, characterization 스냅샷이 이 입력을 담는지는 읽지 않아 확인하지 못했다.
- tester는 서비스(PHP 8081 / Java 8080) 기동 상태를 직접 확인하지 않았고, 3번 실행에서 건너뜀 외 실패가 없었다는 사실만 확인했다.
- 숫자 종료 코드는 도구 응답에 없어 관찰한 종료 상태로 적었다. 위 결과는 더미 DB 기준이다.
- tester 보고에 `git diff`로 변경 범위를 확인한 기록은 없다(`git status`만 사용).

## 이전 보고 대비 실제 개선

- Day2-1 `migration-review-1.md`: 당시 API 테스트 권한 거부로 테스트 실행 증빙이 없었고 표시 문자열 계약 경고로 반려됐다. 이번에는 `--rerun-tasks` 새 실행 84건 통과와 characterization 양쪽 실행 증빙이 있다. 표시 문자열 계약은 Day2-1 주석·계약으로 해소된 것으로 전달받았으며, 이번 reviewer는 이를 다시 이슈로 제기하지 않았다. 과거 보고서 원문은 수정하지 않았다.
- 첫 리뷰의 W2는 테스트 4건 추가와 통과로 해소됐다. W1은 기존 제외 범위로 분류돼 그대로 남는다.
- 커밋·푸시·머지는 하지 않았고, 서비스도 내리지 않았다.
