판정: 반려

# 외주사 PR 3 독립 검토 (HEAD~1..HEAD)

- 요청 범위(한 문장): 문항 검색에 난이도 필터를 추가한다.
- 대상 커밋: `1f978b5 review: vendor pr-3-out-of-scope 적용` (메시지에 요청 설명 없음)
- 기준 문서: `CLAUDE.md`, `templates/approval-checklist.md`, `templates/verification-loop.md`. 루트에 `approval-checklist.md`는 없어서 `templates/` 것을 썼다.
- 이전 PR·마이그레이션 리뷰 판정 파일은 읽지 않았다.

## 기준별 결과

| 기준 | 결과 | 근거 |
|---|---|---|
| 테스트 통과 | 충족 | `cd modern/api && ./gradlew test --rerun-tasks` 이번 실행, BUILD SUCCESSFUL. XML 13개 합계 82건, 실패 0 · 오류 0 · 건너뜀 0. 단위테스트에 외부 호출 없음(URL 문자열은 CORS 헤더 값뿐). 새 서비스 동작 테스트 2건 있음. 단, 새 엔드포인트의 컨트롤러 테스트는 없음(이슈 4) |
| 치명 이슈 0건 | 충족 | 인증값·SQL 문자열 결합·데이터 삭제 코드 없음. 치명 0건 |
| 요청 범위 이탈 없음 | 미충족 | 이슈 1~3. 5개 파일 중 `assignment/` 2개는 요청과 무관하고, 나머지 `item/` 변경도 "검색"이 아닌 다른 엔드포인트다 |
| 컨벤션 준수 | 미충족 | `CLAUDE.md` §3 "요청 범위 밖의 파일을 포맷팅 · 리팩토링 · 정리하지 않는다" 위반(이슈 2, 3). 그 외 항목(컨트롤러→서비스 호출, DTO, SLF4J, 예외 처리)은 위반 없음 |

## 이슈 목록

| # | 심각도 | 파일:줄번호 | 근거 | 수정 방향 |
|---|---|---|---|---|
| 1 | 경고 | `modern/api/src/main/java/com/example/item/UnitController.java:27-34`, `UnitService.java:32-47` | 요청은 "문항 검색"이다. 검색 API `/api/items/search`(`ItemSearchController.java:21-25`)는 이미 `level` 파라미터를 받고, `ItemSearchRepositoryTest`에 난이도 지정 테스트도 있다. 외주사는 검색이 아닌 `GET /api/units/{code}/items?level=` 를 새로 만들었다. 요청이 "단원별 목록 필터"를 뜻한 것인지 확인되지 않았다 | 요청자에게 대상 API를 확인한다. 검색이 목표라면 `/api/items/search`의 기존 level 동작을 확인하고 필요한 부분만 수정한다. 단원 목록이 목표라면 요청 문장을 정정한 뒤 다시 제출한다 |
| 2 | 경고 | `modern/api/src/main/java/com/example/assignment/DistributionService.java:52, 62-66` | 재배포 검사를 `assertNotClosed`로 추출한 리팩토링이다. 난이도 필터와 무관하다. 예외 메시지 조립도 `id`에서 `distribution.getId()`로 바뀌었다 | 이 파일 변경을 PR에서 뺀다. 필요하면 별도 PR로 올린다 |
| 3 | 경고 | `modern/api/src/main/java/com/example/assignment/ReportService.java:65, 71-72, 82-88` | 평균 계산을 `averageOf`로 추출했고, 보고서 생성 로그에 `average` 를 추가했다. 요청과 무관한 리팩토링과 로그 형식 변경이다 | 이 파일 변경을 PR에서 뺀다. 로그 필드 추가가 필요하면 별도 요청으로 한다 |
| 4 | 경고 | `modern/api/src/test/java/com/example/item/` (`UnitControllerTest` 없음) | 새 엔드포인트 `UnitController.java:31`에 컨트롤러 테스트가 없다. `params = "level"` 분기와 기존 `ItemController.java:29`의 `/units/{code}/items` 와의 매핑 충돌 여부를 검증하는 테스트가 없다. 서비스 테스트(`UnitServiceTest.java:52-72`)만 있다. `CLAUDE.md` §2 "새 엔드포인트에는 테스트" 대상 | 이슈 1이 해소돼 엔드포인트를 유지하게 되면 `@WebMvcTest`로 level 있음/없음 두 경로와 404를 검증한다 |
| 5 | 제안 | `modern/api/src/main/java/com/example/item/UnitService.java:33, 39-44` | Javadoc은 난이도 1~5라고 쓰지만 범위 검증이 없다. 범위 밖 값은 조용히 빈 목록이 된다. 검색 API는 "잘못된 값도 400으로 거부하지 않는" 규약이라 일관성은 있다 | 의도라면 Javadoc 문구를 정리하고, 아니라면 검증을 추가한다 |

## 확인 필요 (이번 변경의 확정 이슈 아님)

- 이슈 1의 요청 의도: 요청 원문이 한 문장뿐이라 "검색"이 `/api/items/search`를 뜻하는지 단원별 목록을 뜻하는지 확정할 수 없다. 사람이 확정해야 한다. 이 항목이 남아 있어 승인하지 않는다.
- 기존부터 존재한 문제: `ReportService.java`의 `// TODO: 트랜잭션 밖으로` 주석(서명 계산 중 커넥션 점유)은 이번 diff 밖이다. 이번 변경의 이슈로 세지 않는다.
- characterization HTTP 비교, `modern/web` 테스트는 요청 지시대로 적용하지 않았다(실행하지 않음).

## 외주사 수정 요청

1. `DistributionService.java`, `ReportService.java` 변경을 모두 제거한다(이슈 2, 3).
2. 요청 대상 API를 확인해 달라. 검색(`/api/items/search`)이 대상이면 기존 `level` 동작과 차이를 PR 설명에 적고, 단원별 목록이 대상이면 요청 문장을 정정한다(이슈 1).
3. 엔드포인트를 유지한다면 `@WebMvcTest` 컨트롤러 테스트를 추가한다(이슈 4).
4. 커밋 메시지에 요청 내용을 적고, 변경 파일이 요청 문장으로 설명되게 PR을 다시 제출한다.


---
저장 방식: 리뷰 도구의 Write 권한 거부로 Codex가 도구 요청 본문을 그대로 저장했다. 독립 판정 원문이다.
