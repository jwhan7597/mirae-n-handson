# Day 2-1 AI 검증루프 실습 결과

2026-10-07 · 에듀테크R&D팀(rnd). 작업 저장소 `mirae-n-handson`, 브랜치 `day1`.

## 진행 결과

| 실습 | 결과 | 기록 |
|---|---|---|
| 시작 준비 | upstream 최신화 완료, PHP·MariaDB·현재 Java API 실행 | [STARTUP.md](STARTUP.md) |
| verify Skill | 필수 frontmatter 3항목, 인자/기본 범위, 네 기준, 실제 검사와 읽기 전용 리뷰 절차 작성·호출 | `.claude/skills/verify/SKILL.md` |
| 기존 이관 결과 검증 | 새 Sonnet 세션의 1차 검토 → 지적 대응 → 로컬 커밋 → 서버 재기동 → 다른 새 Sonnet 세션의 승인 판정 | [1차 대응](migration-review-response.md), [2차 리뷰](migration-review-2.md), [근거 대조](migration-review-adjudication.md) |
| 외주 PR 검증 | 3개 patch의 적용 확인·브랜치 분리·실제 테스트·독립 리뷰, 3건 반려 | [판정표](vendor-pr-verdicts.md), [지적 대조](vendor-review-adjudication.md) |
| 승인 기준 v1 | 기존 CLAUDE.md에 근거한 실습용 초안, 3줄 변경 요약·예/아니오 항목, 3건 재판정 | `templates/approval-checklist.md`, `templates/verification-loop.md`, [재판정](criteria-v1-validation.md) |
| 최신 기준 호출 확인 | 새 `/verify` 세션이 v1 파일을 읽고 새 항목 인용, patch 미적용·추가 테스트 미실행을 명시 | [호출 결과](verify-v1-check.md) |

## 테스트

- Java API: **80개 통과**, 실패·오류·건너뜀 0건.
- 레거시 비교: **54개 통과**. 새 API 비교: **53개 통과·기존 레거시 전용 1개 건너뜀**. 검색 40개는 양쪽 모두 통과.
- 외주 검토 브랜치: PR1 **80개**, PR2 **81개**, PR3 **82개** 통과. 기존 테스트가 통과해도 새 동작 누락·인증정보 하드코딩·범위 이탈이 있으면 반려한다.

## 작성·검토 역할

작성·수정·리뷰 지적 대조는 Codex, 독립 리뷰는 매번 새 `claude-sonnet-5-5` 세션으로 했다. 실제 Opus 작성 세션이나 사람의 최종 승인을 수행했다고 기록하지 않는다.
페어가 없는 현재 상황에서는 3개 예제를 모두 각각 독립 리뷰했다. 실제 페어의 한 행을 가져오거나 팀원과 합의한 기록은 아니다. 기준 v1은 기존 팀 규칙 파일을 이용한 **실습 초안**이다.

## 보존과 Git

- 기존 PHP·DB·테스트·스냅샷·승인된 source-map-js 보안 수정은 시작 상태를 유지했다.
- 기존 기능 코드는 응답 계약의 설명 주석만 보강했다. 확정 기능 결함이 없어 실행 로직 변경은 없었다.
- 외주 patch는 검토 브랜치에만 적용했고 day1에 머지하지 않았다. 인증정보는 교육용 더미이며 보고서에 원문·인코딩값을 기록하지 않았다.
- 이 차시의 변경은 **로컬 커밋**으로 남긴다. 원격 푸시는 수행하지 않는다.
- PHP·MariaDB·Java API는 후속 차시에서 사용하므로 실행 상태로 유지한다.

## 업무 적용

AI가 작성한 문항 검색·배포 기능이나 외주 납품 PR의 검수에서 같은 순서로 요청 범위·테스트·보안·코딩 규칙을 대조한다. 코드 작성 세션과 리뷰 컨텍스트를 분리하고, AI의 지적도 근거를 확인하여 수용·반박·확인 필요를 남긴다.

팀 저장소 반영은 Day2-4에서 수행한다. 현재 초대가 없어도 이 차시의 로컬 결과물을 만드는 데는 영향이 없다. 실제 팀원 간 기준 합의와 사람의 머지 판단은 이후 별도로 수행할 단계다.

증빙 폴더: `/Users/jwhan/work/mirae-n-preparation/day2-1-evidence`. 요약 파일: `/Users/jwhan/work/mirae-n-preparation/day2-1-results.txt`.
