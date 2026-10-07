판정: 반려

요청 범위(한 문장): 단원별 문항 수 통계 API 추가 (`GET /api/units/item-counts`).
검토 대상: `HEAD~1..HEAD` (커밋 메시지 `review: vendor pr-1-missing-tests 적용`). 변경 3개 파일, +31/-1, 모두 `modern/api/src/main`.

## 기준별 결과

| 기준 | 결과 | 근거 |
|---|---|---|
| 테스트 통과 | 미충족 | `cd modern/api && ./gradlew test --rerun-tasks` 이번 실행 BUILD SUCCESSFUL, XML 13개 합계 80건 통과 / 실패 0 / 오류 0 / 건너뜀 0. 그러나 새 동작(`countActiveItemsByUnit`, `GET /api/units/item-counts`)을 검증하는 테스트가 없다(이슈 1). 체크리스트 1장 "테스트가 충분하다" 미충족. 기존 테스트 삭제·`@Disabled` 없음 |
| 치명 이슈 0건 | 충족 | 치명 이슈 없음. 인증값·토큰 리터럴 없음, 문자열 SQL 없음(파생 쿼리 `countByUnitIdAndStatus`), 삭제·덮어쓰기 코드 없음, 외부 요청 코드 없음 |
| 요청 범위 이탈 없음 | 충족 | 3개 파일(Controller·Response record·Service) 모두 요청 문장으로 설명됨. 의존성·스키마·설정 변경 없음. `modern/web`, `legacy`, `characterization` 변경 없음 |
| 컨벤션 준수 | 미충족 | CLAUDE.md §2 "새 엔드포인트와 서비스의 새 동작에는 … 테스트를 추가한다" 위반(이슈 1). 그 외 항목은 준수(아래) |

컨벤션 대조(CLAUDE.md §2 API): 컨트롤러는 서비스만 호출(`UnitController.java:28`), record DTO 사용, 엔티티 직접 노출 없음, 서비스 `@Transactional(readOnly = true)` 안에서 변환(`UnitService.java:10,37-40`), `ItemStatus.ACTIVE` 사용(`UnitService.java:39`), SLF4J 사용, 빈 catch·`System.out` 없음.

## 이슈 목록

| # | 심각도 | 파일:줄번호 | 근거 | 수정 방향 |
|---|---|---|---|---|
| 1 | 경고 (기준 1·4 미충족 사유 → 반려) | `modern/api/src/main/java/com/example/item/UnitController.java:25-29`, `UnitService.java:32-43` | 새 엔드포인트·서비스 메서드가 추가됐으나 `src/test` 어디에도 `item-counts`·`countActiveItemsByUnit`·`UnitItemCountResponse` 참조가 없다(Grep 확인). 기존 `UnitServiceTest.java`는 `listUnits`만, `ItemRepositoryTest.java:77-80`은 저장소 쿼리만 검증 | ① `UnitServiceTest`에 `ItemRepository` `@Mock` 추가 후 `countActiveItemsByUnit` 테스트: 학년·코드 순서 유지, 문항 0건 단원이 0으로 포함, `ItemStatus.ACTIVE`로만 조회, 단원 없음→빈 목록. ② `UnitController` `@WebMvcTest`(기존 `ItemControllerTest` 형식)로 `GET /api/units/item-counts` 200과 JSON 필드(`code,name,grade,activeItemCount`) 검증. 기존 테스트 기대값은 바꾸지 말 것 |
| 2 | 경고 | `UnitService.java:37-40` | 단원 수만큼 `countByUnitIdAndStatus`를 반복 호출(단원당 쿼리 1회, N+1). 단원 수가 적은 현재는 동작에 문제 없으나 단원이 늘면 비용 증가 | `ItemRepository`에 `unitId`·`status`로 `GROUP BY` 하는 집계 쿼리 1개로 바꾸고 서비스에서 Map 조회. 변경 시 이슈 1의 테스트로 0건 단원 포함 동작을 고정 |
| 3 | 제안 | `modern/api/src/test/java/com/example/item/UnitServiceTest.java:19-23` | `@InjectMocks`가 생성자 인자를 모두 채우지 못하면 `itemRepository`가 null로 주입됨. 지금은 `listUnits`만 쓰므로 통과하지만, 새 테스트를 같은 클래스에 추가할 때 NPE 원인이 될 수 있음 | `ItemRepository` `@Mock` 명시 추가(이슈 1과 함께 처리) |
| 4 | 제안 | `UnitItemCountResponse.java:4` | `grade`는 `Integer`, `activeItemCount`는 `long` 으로 타입이 섞임. `UnitResponse`의 grade 타입과 맞는지 확인 필요(문제라는 근거는 없음) | 기존 `UnitResponse`와 필드 타입을 맞추는지 팀 판단 |

## 확인 필요 / 기존 문제 구분

- 필수 확인 필요: 없음.
- 이번 변경 확정 이슈: 이슈 1(반려 사유), 2~4(경고·제안).
- 기존부터 있던 문제: 확인된 것 없음.
- 참고: 지시문의 "루트 approval-checklist.md"는 루트에 없다. `templates/approval-checklist.md`를 기준으로 썼고 `docs/approval-checklist.md`는 내용이 같다.
- 참고: 커밋 메시지(`review: vendor pr-1-missing-tests 적용`)는 변경 내용을 설명하지 않는다. 요청 범위는 지시문 기준으로 판단했다.
- 이 변경에는 인증정보 생성·외부 요청 패턴이 없다. 보고서에 인증값을 옮기지 않았다.
- characterization HTTP 비교·`modern/web` 검사는 요청에 따라 해당 없음.

## 테스트 실행 기록

- 단위테스트의 외부 호출 확인: `application-test.yml`은 H2 인메모리(`jdbc:h2:mem`)만 사용. `src/test`에서 HTTP 클라이언트·외부 URL 사용 없음(`WebConfigTest`의 `http://evil.example`은 CORS Origin 헤더 문자열일 뿐 호출 아님).
- 실행: `cd modern/api && ./gradlew test --rerun-tasks` → BUILD SUCCESSFUL, `--rerun-tasks`로 캐시 없이 실행(5 tasks executed).
- 결과 수는 `build/test-results/test/*.xml`의 `tests/skipped/failures/errors` 속성으로 확인: 합계 80 / 0 / 0 / 0.

## 외주사 수정 요청

1. 새 엔드포인트와 `countActiveItemsByUnit`에 대한 테스트 추가(이슈 1의 ①·②). 정상·경계(문항 0건 단원, 순서, ACTIVE 필터)를 포함할 것.
2. 가능하면 단원별 반복 count를 집계 쿼리 1회로 바꾸고(이슈 2), `UnitServiceTest`의 `ItemRepository` mock을 명시할 것(이슈 3).
3. 수정 후 `modern/api`에서 `./gradlew test` 결과(통과·실패 수)와 함께 다시 제출. 재검토는 새 리뷰 세션에서 진행.


---
저장 방식: 리뷰 도구의 Write 권한 거부로 Codex가 도구 요청 본문을 그대로 저장했다. 독립 판정 원문이다.
