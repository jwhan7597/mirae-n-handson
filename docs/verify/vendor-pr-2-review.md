판정: 반려

대상: `HEAD~1..HEAD` (9ebf040) / 요청 범위: 외부 채점 서버 연동 클라이언트 추가
근거 문서: `CLAUDE.md`, `templates/approval-checklist.md` (루트에는 없고 `docs/`와 `templates/`에 동일 내용 존재), `templates/verification-loop.md`
비밀값 표기: 인증값과 그 Base64는 마스킹했다(`****`). 교육용 더미이므로 실제 유출이 있었다고 단정하지 않는다.

## 기준별 결과

| 기준 | 결과 | 근거 |
|---|---|---|
| 테스트 통과 | 충족 (경고 W2) | `cd modern/api && ./gradlew test --rerun-tasks` 이번 실행. BUILD SUCCESSFUL, XML 14개 합계 tests 81 / failures 0 / errors 0 / skipped 0. 신규 `GradingServerClientTest` 1건 통과. 기존 테스트 삭제·`@Disabled` 없음 |
| 치명 이슈 0건 | **미충족** | 이슈 #1 (인증정보 리터럴) |
| 요청 범위 이탈 없음 | 충족 | 변경 2개 파일(`GradingServerClient.java`, `GradingServerClientTest.java`) 모두 요청 문장으로 설명됨. 의존성·스키마·설정 파일 변경 없음 |
| 컨벤션 준수 | 확인 필요 (C1) | Controller/Repository/SQL 직접 호출 없음, 빈 catch 없음, `System.out` 없음, SLF4J 사용. 기본 URL의 내부 주소 여부는 확인 불가 |

## 이슈 목록

| # | 심각도 | 파일:줄번호 | 근거 | 수정 방향 |
|---|---|---|---|---|
| 1 | 치명 | `modern/api/src/main/java/com/example/assignment/GradingServerClient.java:28-29` (사용처 `:66`, 헤더 `:60`) | `username`, `password`가 필드에 문자열 리터럴(`"****"`)로 들어 있고, `basicAuth()`(`:65-68`)가 이를 합쳐 Base64로 만들어 외부 요청 `Authorization` 헤더에 붙인다. 체크리스트 2의 "비밀번호·토큰·키가 리터럴로 들어 있지 않다" 위반. 소스와 git 이력에 남는다 | 리터럴 제거. `@Value("${grading.username}")`/`${grading.password}`처럼 환경변수·외부 설정으로 주입하고 기본값을 두지 않는다(미설정 시 기동 실패). 설정 키 추가는 CLAUDE.md §3에 따라 이유를 설명하고 승인받는다. 이력에 남은 값은 폐기·교체한다 |
| 2 | 경고 (W1) | `GradingServerClient.java:56` | JSON 본문을 문자열 연결로 만든다. `studentId`에 `"`·`\`가 있으면 JSON이 깨지거나 필드가 주입된다. CLAUDE.md §2 "요청·응답에는 record DTO" 취지와도 어긋난다 | record DTO를 만들어 `ObjectMapper`로 직렬화한다 |
| 3 | 경고 (W2) | `GradingServerClient.java:38-52` / 테스트 `GradingServerClientTest.java:13-23` | 새 동작 `requestGrading`(200 정상, 200 아님, `IOException`, `InterruptedException`)에 테스트가 없다. 테스트는 `buildRequest`만 검증하고, 본문은 단언하지 않으며, 인증 헤더는 `startsWith("Basic ")`만 본다. 테스트가 실제 외부 서비스를 호출하지 않는 것은 확인했다(`:11`, `:16`은 요청 객체만 생성). CLAUDE.md §2 "서비스의 새 동작에는 정상 또는 경계 조건 테스트" | `HttpClient`를 생성자 주입으로 바꿔 가짜 객체로 `requestGrading`의 정상·오류 경로를 검증한다. 본문 JSON 단언을 추가한다. 테스트에는 인증값 리터럴을 쓰지 않는다 |
| 4 | 경고 (W3) | `GradingServerClient.java:32` | 기본값 `http://grading.internal:9000`이 평문 HTTP다. Basic 인증 헤더가 암호화 없이 전송된다 | 기본값을 제거하고 설정으로 받으며 HTTPS를 사용한다 |
| 5 | 제안 | `GradingServerClient.java:38,47` | 응답 본문(점수 문자열)을 그대로 반환한다. 채점 서버 오류(`:42`)는 `IllegalStateException`이라 `GlobalExceptionHandler`에서 500으로 처리된다 | 응답을 record DTO로 파싱하고, 호출 지점이 생길 때 상태·오류 매핑(예: 504/502)을 정한다 |

## 확인 필요 (확정 이슈 아님)

- C1: `GradingServerClient.java:32`의 `grading.internal`이 실제 사내 호스트명인지 알 수 없다. 실제 내부 주소라면 CLAUDE.md §3 "내부 주소를 공개 저장소에 넣지 않는다" 위반이다. 외주사에 확인한다.
- C2: `requestGrading`의 호출처가 이번 변경에는 없다(사용되지 않는 빈). 요청 범위 안이라 이탈로 보지 않지만, 후속 연결 PR에서 위 이슈가 같이 확인돼야 한다.

기존부터 존재한 문제: 해당 없음(이번 변경은 신규 파일 2개).

## 외주사 수정 요청

1. (필수) 인증정보 리터럴을 제거하고 외부 설정으로 주입한다. 이력에 남은 값은 폐기한다.
2. (필수) `requestGrading`의 정상·오류 경로 테스트를 추가한다(`HttpClient` 주입 후 가짜 객체 사용, 실제 호출 금지).
3. JSON 본문을 DTO와 직렬화기로 만든다.
4. 기본 URL의 내부 주소 여부를 답하고, 평문 HTTP 기본값을 제거한다.

위 1번이 해소되고 C1이 확인되기 전에는 승인하지 않는다. 수정 후 재리뷰한다.


---
저장 방식: 리뷰 도구의 Write 권한 거부로 Codex가 도구 요청 본문을 그대로 저장했다. 독립 판정 원문이다.
