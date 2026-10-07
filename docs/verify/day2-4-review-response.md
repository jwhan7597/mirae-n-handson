# Day 2-4 첫 reviewer 보고 대응

작성 역할: Codex. reviewer 원문은 `day2-4-reviewer-first.md`에 보존했다.
애플리케이션 비교 범위: `upstream/main...HEAD -- modern characterization`.
Day1의 표준 UTF-8 GET 문항 검색 이관 계약과 기존 제외 범위를 유지한다.

| 지적 | 분류 | 근거와 처리 |
|---|---|---|
| W1: 깨진 percent 인코딩의 400 처리 | 반박(기존 제외 범위) | `docs/item-bank/MIGRATION-REVIEW.md:64,69`와 `docs/verify/migration-review-2.md:56`에서 잘못된 인코딩·비표준 키를 이미 이번 이관의 검증 범위 밖으로 기록했다. URLDecoder가 예외를 내는 코드 사실은 맞지만, 새 요구로 호환 범위를 넓히지 않는다. 이번 호출의 요청 문장에 이 제외 조건을 빠뜨린 것이므로 `/verify`에는 명시해 전달한다. 수정하지 않는다. |
| W2: 페이지 후행 개행·선행 0·긴 숫자 경계 테스트 누락 | 수용(테스터에 위임) | `ItemSearchService.java:92-105`의 분기에 대응하는 `2\n`, `2\n\n`, `0002`, 100자리 숫자 케이스가 기존 `ItemSearchServiceTest.java:56-70`에 없다. 4자리 `1000` 자체는 기존 케이스에 있으므로 그 부분은 정정한다. 실제 누락된 네 경계만 테스터가 별도 테스트로 추가한다. 소스는 수정하지 않는다. |
| C1: 범위 밖 지수 난이도 동등성 미확인 | 확인 필요 → 현재 더미 DB의 응답 동일 확인 | 두 서버에 `level=1e30`, `level=1e1000`을 실제 GET 요청했다. 각각 정규화 응답은 200·0건·검색 결과 없음으로 동일했다. 내부 정수 변환의 모든 값이 같다는 증명은 아니며 범용 숫자 파서 변경은 하지 않는다. |
| C2: 양쪽 characterization 실행 미확인 | 수용 | reviewer는 설계상 테스트를 실행하지 않았다. `/verify`의 tester가 API와 양쪽 대상 테스트를 새로 실행해 이 칸을 채운다. |
| phpTrim·4바이트 문자·표시 공백 추가 테스트 | 제안으로 기록 | 이번 필수 지적 W2부터 보완한다. 제안 때문에 소스나 원래 스냅샷을 바꾸지 않는다. |
| H2 뷰 복사본 동기화 | 기존 제안 유지 | 이번 diff에는 DB 뷰 변경이 없다. 체크리스트의 뷰·fixture 대응 항목으로 관리한다. |
| 검색 characterization이 38개라는 수치 | 정정 | `characterization/tests/item-bank.test.js:5-46`의 cases는 40개다. 원문은 수정하지 않는다. 실제 전체 실행 수는 tester 결과로 기록한다. |

## 도구 권한과 재호출 준비

- 첫 tester는 테스트 뒤 `echo`를 붙여 허용 범위를 벗어났고 실행이 차단됐다. 원문을 증빙에 보존하고 tester 지침을 보완했다.
- 두 번째 tester는 정확한 테스트 명령으로 API 80개를 새로 실행했다. 권한 거부 0건, `5 actionable tasks: 5 executed`, JUnit 실패·오류·제외 0건. 기존 tracked 파일 285개의 SHA-256은 모두 동일했다.
- 첫 reviewer의 diff stat은 실행됐으나 `cat -n`이 붙은 복합 명령, `git -C` 명령은 거부됐다. 본문 diff를 읽지 못했다는 원문 제한을 유지한다. 파일은 Read로 확인했고 파일 수정·테스트 실행은 없었다.
- reviewer 지침을 저장소 루트의 단일 `git diff/log/show/status` 명령과 Read·Grep으로 분명히 했다. 최종 `/verify`에서는 diff 본문을 실제로 읽었는지도 확인한다.
- `.claude/settings.json`의 두 테스트 allow 규칙을 유지한다. 새 API 대상의 `TARGET_BASE_URL=... npm test`와 실행 폴더의 `cd`, 읽기 전용 git, 지정 보고서/테스트 파일 편집은 이번 검증 호출에서만 허용한다. 테스트 명령 자체를 CLI allow에 중복 등록하지 않는다.

## 실측 증빙

`/Users/jwhan/work/mirae-n-preparation/day2-4-evidence/`:
`reviewer-first-stream.jsonl`, `tester-smoke-stream.jsonl`, `tester-smoke-2-stream.jsonl`,
`tester-smoke-2-independent-check.json`, `review-boundary-probes.json`.
이 기록은 실제 사내 표준·팀 합의·사람의 머지 승인으로 간주하지 않는다.
