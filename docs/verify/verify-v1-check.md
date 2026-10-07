# verify v1 체크리스트 읽기 확인 · pr-1-missing-tests

판정: **반려** (미충족 기준: 테스트 통과)

- 대상: `vendor-prs/pr-1-missing-tests.patch` (적용하지 않음, `git apply --stat`만 실행: 3 files, +31/-1)
- 요청 범위(patch 제목 `:4`, 본문 `:9-:10`): 단원별 문항 수 통계 API `GET /api/units/item-counts` 추가
- 기준 파일: `templates/approval-checklist.md` ("승인 체크리스트 v1 · 2회차", `:1`). `docs/approval-checklist.md` v0 복사본은 사용하지 않음.

## 기준별 결과

| 기준 | 결과 | 근거 |
|---|---|---|
| 1. 테스트 통과 | 미충족 | 새 동작 테스트 없음(아래). 이번 테스트 실행 기준은 **확인 필요**(미적용 patch, 실행하지 않음) |
| 2. 치명 이슈 0건 | 충족 | 인증정보·SQL 조합·삭제/권한 변경 없음. 상태 값은 `ItemStatus.ACTIVE`로 바인딩(patch `:81`) |
| 3. 요청 범위 이탈 없음 | 충족 | 변경 3파일(`:16-:18`) 모두 통계 API 구현에 해당. 의존성·스키마·설정 변경 없음 |
| 4. 컨벤션 준수 | 충족 | Controller→Service 호출(`:34`), record DTO(`:46`), 생성자 주입(`:63-:65`), SLF4J `log.debug`(`:83`), 빈 catch·System.out 없음 |

## 현재 v1의 새 동작 테스트 항목 (원문 인용)

- `templates/approval-checklist.md:29`: "- [ ] 새 public 동작·추가 분기·경계에 대응하는 테스트가 있는가? 단순 getter/setter/생성자와 DTO 접근자는 제외한다."
- `templates/approval-checklist.md:30`: "- [ ] 새 동작 테스트가 요구한 결과를 검증하는가? 기존 테스트 통과만으로 새 동작의 검증을 대신하지 않았는가?"
- 테스트 실행 항목: `templates/approval-checklist.md:27`: "- [ ] 변경된 폴더의 테스트 명령을 실제로 실행하고 이번 실행의 종료 상태·통과/실패/건너뜀 수를 기록했는가?"

## 새 동작 ↔ 누락된 테스트 대조

patch가 바꾼 파일은 `modern/api/src/main/java` 하위 3개뿐이며(`:16-:18`) 테스트 파일 변경은 없다.

| 새 동작 (patch 파일:줄) | 대응 테스트 | 항목 29/30 |
|---|---|---|
| `GET /api/units/item-counts` 매핑·`itemCounts()` (`:32-:34`) | 없음 (200·응답 필드 `code/name/grade/activeItemCount` 미검증) | 아니오 |
| `UnitService.countActiveItemsByUnit()` ACTIVE 문항만 집계 (`:78-:81`) | 없음 | 아니오 |
| 문항 0개 단원 0 포함, 단원 목록과 같은 순서(학년·코드), 빈 단원 목록 (`:75-:76`, `:79`) | 없음 | 아니오 |
| 생성자에 `ItemRepository` 추가 (`:63-:65`) | 기존 `UnitService` 생성 테스트 영향은 확인 필요(미실행) | 확인 필요 |

- 본문 `:14` "기존 테스트 전부 통과 확인"은 항목 30이 금지한 "기존 테스트 통과만으로 대신"하는 근거다. 새 동작 검증으로 인정하지 않는다.
- 참고(저장소 현재 코드): `ItemRepository.java:25`의 `countByUnitIdAndStatus`와 `ItemRepositoryTest.java:77-80` 테스트는 patch 이전부터 있으며, 새 통계 API·서비스 동작의 검증이 아니다.

## 이슈

| 심각도 | 파일:줄 | 근거 | 수정 방향 |
|---|---|---|---|
| 경고 | `UnitController.java:25`(patch `:32-:34`) | 새 엔드포인트 테스트 없음 | `@WebMvcTest`로 200·응답 필드 검증 |
| 경고 | `UnitService.java:36`(patch `:78-:85`) | ACTIVE 필터·0건 단원·순서·빈 목록 테스트 없음 | Mockito로 서비스 테스트 추가 |
| 제안 | patch `:79-:82` | 단원마다 `countByUnitIdAndStatus`를 호출하는 구조(단원 수만큼 쿼리). 요구 충족에는 영향 없음 | 필요 시 집계 쿼리 1회로 개선 검토 |

## 테스트 실행 상태

- 이번 실행: **실행하지 않음 · 확인 필요** (patch 미적용, 사용자 지시). 승인 근거로 쓰지 않는다.
- 이전 실행 결과(이번 실행 아님): `docs/verify/vendor-pr-verdicts.md:18` — review/pr-1, 커밋 447f7d7, `./gradlew test --rerun-tasks`, 통과 80 / 실패 0 / 오류 0 / 건너뜀 0. 같은 문서 `:8`은 이 PR을 "새 동작 테스트 누락"으로 반려했다.

## 확인 사항

verify Skill은 이번에 `templates/approval-checklist.md` v1(`:1`의 "v1 · 2회차")을 읽어 위 기준·항목·출력 형식(`:68-:72`)을 적용했다. 판정은 리뷰 결과이며 머지 결정은 사용자가 한다.
