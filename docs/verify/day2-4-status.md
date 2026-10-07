# Day 2-4 로컬 수행 결과

작성: 2026-10-07, 에듀테크R&D팀(rnd), GitHub `jwhan7597`.
사용자 요청: 초대 없이 진행할 수 있는 Day2-4 작업을 먼저 수행한다.

| 문서 단계 | 결과 | 근거 |
|---|---|---|
| 1. reviewer 생성·등록 | 완료 | `.claude/agents/reviewer.md`, tools=Read/Grep/Glob/Bash, model=inherit. 새 CLI의 agents 목록과 실제 reviewer 위임에서 확인 |
| 2. tester·테스트 허용 설정 | 완료 | `.claude/agents/tester.md`, `.claude/settings.json` 두 allow 규칙. 등록 확인 후 테스트 추가 없이 API 80건 새 실행 통과 |
| 3. 리뷰 대응·verify 연결·재검증 | 완료 | reviewer의 W2를 수용해 tester가 페이지 경계 테스트 4건 추가. `/verify`에서 두 역할이 실제 호출되고 `migration-review-3.md` 최종 승인 |
| 4. 공용 자산 준비 | 완료 | 외부 준비 폴더에 자산 11개·동일한 ZIP·SHA-256 목록 준비 |
| 4. 팀 개인 브랜치 업로드 | 대기 | 현재 팀 초대 목록 `[]`, `tristan-kkim/mirae-n-claude-harness-rnd` 조회 404. 팀 저장소 접근·초대 수락 필요 |
| 팀 main 통합·README | 팀 작업 대기 | 팀에서 반영 담당·자산 기준 브랜치를 정한 뒤 문서의 담당 절차 수행. 담당·팀 버전을 임의로 정하지 않음 |
| 마지막 서비스 정리 | 완료 | bootRun 종료, Compose php/modern down 성공, compose ps 빈 목록, 8080·8081·3306 리스닝 없음 |

## 실제 검증

| 대상 | 이번 실행 | 통과 | 실패·오류 | 건너뜀 |
|---|---|---:|---:|---:|
| API | `./gradlew test --rerun-tasks`, 5 task 모두 executed | 84 | 0 | 0 |
| PHP 동작 보존 | `npm test` | 54 | 0 | 0 |
| 새 API 동작 보존 | `TARGET_BASE_URL=http://localhost:8080 npm test` | 53 | 0 | 1 |

기존 레거시 단원 화면 전용 제외 1건은 `characterization/tests/example-units.test.js:18,21`이다.
숫자 종료 코드는 Claude Bash 응답에 노출되지 않아 성공 출력·도구 오류 여부로 종료 상태를 기록했다.
API 수치는 Codex가 이번 JUnit XML 13개를 별도로 합산해 동일함을 확인했다.
큰 지수 난이도 2개와 새 페이지 경계 4개에 대한 추가 HTTP 비교도 두 서버에서 정규화 응답이 동일했다.
전체 HTTP·인코딩·데이터 분포의 동등성을 증명한 결과는 아니다.

## 변경 범위 확인

- 시작 HEAD: `aaace2110ef87fb3f108989d47a8d81cd2a1c628`. 비교 기준 upstream/main: `1bb4f2f77a0ff0111ae571461327343186195487`.
- 최종 검증은 위 HEAD의 이관 코드와 tester가 작업 트리에 추가한 테스트 4건을 실행했다. 소스 수정이 없으므로 기동 중인 API의 재시작은 필요하지 않았다.
- 시작 tracked 파일 285개 중 바뀐 파일은 verify Skill, 검증루프 템플릿, `ItemSearchServiceTest.java`뿐이다. 나머지 282개 SHA-256은 동일하다.
- tester의 Edit 호출은 `ItemSearchServiceTest.java` 1개에만 있었다. 기존 테스트 기대값·skip·스냅샷, characterization 전체, 보안 패치 잠금 파일, 앱 소스·DB 스키마·시드는 그대로다.
- reviewer는 파일 수정·테스트 실행이 없었다. 최종 메인의 유일한 Write는 지정된 `migration-review-3.md`였다. 최종 호출의 권한 거부는 0건이다.
- 테스트 Bash 허용은 프로젝트 settings에서 읽혔다. CLI에 Gradle/npm 일반 테스트 allow를 중복 등록하지 않았고 전역 설정에도 일치하는 테스트 allow는 없다.
- 폴더 이동·읽기 전용 git·테스트 파일/보고서 쓰기·환경 변수 접두사 테스트는 이번 호출에서만 범위를 제한해 허용했다. 전역 model=opus, effortLevel=xhigh는 그대로다.
- 원문 첫 리뷰와 최종 보고는 보존했다. 오판·제외 범위 정정은 `day2-4-review-response.md`에서 처리했다. W2는 계획만으로 해소된 것이 아니라 새 테스트 4건의 실행으로 해소했다.

## 공유 준비물·대기 작업

위치: `/Users/jwhan/work/mirae-n-preparation/day2-team-upload/`.
`assets/`, `day2-rnd-jwhan7597-assets.zip`, `file-hashes.json`, `UPLOAD-GUIDE.md`와 최종 커밋을 기록하는 manifest를 사용한다.
자산은 CLAUDE.md, 프로젝트 settings, Skill 4개·RCA masking-rules, reviewer/tester, 템플릿 2개다.
실행 보고서·앱 소스·MCP 설정·실제 토큰·settings.local은 공유 묶음에 포함하지 않았다.
팀 표준·합의 완료 문서가 아니라 개인 실습 초안이다. 개인 포크·팀 저장소 모두 이번 작업에서 푸시하지 않았다.
초대 수락 후 팀 개인 브랜치 업로드, 담당의 main 통합이 남았다. 메신저 전송도 하지 않았다.

## 개인 회고

R&D팀의 검색 API 변경·외주 PR 검수에서 리뷰와 테스트를 한 번의 `/verify`로 반복하는 데 적용할 수 있다.
실제 프로젝트에 가져갈 때는 경로·테스트 명령·승인 기준을 맞춰야 한다.
리뷰는 별도 컨텍스트에, 테스트 보완은 테스트 파일만 고치는 역할에 맡긴다. 한 줄 수정 같은 작은 작업은 메인이 직접 처리한다.
실제 팀원과 회고·기준 합의를 했다고 기록하지 않는다.

증빙: `/Users/jwhan/work/mirae-n-preparation/day2-4-evidence/`의 호출 원문·도구 결과·JUnit 합산·파일 해시·정리 결과.
현재 공식 설정 형식은 Context7의 `/websites/code_claude`에서 확인했다:
[Sub-agents](https://code.claude.com/docs/en/sub-agents), [Permissions](https://code.claude.com/docs/en/permissions), [Skills](https://code.claude.com/docs/en/slash-commands).
