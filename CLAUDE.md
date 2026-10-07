# CLAUDE.md — 에듀테크R&D팀 실습 규칙 v1

공개 실습 저장소 `mirae-n-handson`에서 사용하는 팀 규칙 초안이다. 팀 코드는 `rnd`이다.
실제 사내 표준을 확인한 문서가 아니며, 현재 `modern/` 코드와 교육용 템플릿을 기준으로 작성했다.
답변과 문서는 한국어로 작성한다. 도메인 용어는 문항 `item`, 단원 `unit`, 난이도 `level`, 태그 `tag`를 사용한다.

## 1. 빌드 · 테스트 명령

JDK 21, Node.js 22.5 이상을 사용한다. 아래 명령은 저장소 루트에서 실행한다.
괄호 안의 `cd`는 해당 명령에만 적용된다. npm 의존성은 잠금 파일을 따르는 `npm ci`로 설치한다.

```bash
# API: 테스트는 H2를 사용하므로 Docker DB 없이 실행한다.
(cd modern/api && ./gradlew test)
(cd modern/api && ./gradlew build)

# 화면: 처음 한 번 의존성을 설치하고, 변경 후 세 검사를 실행한다.
(cd modern/web && npm ci)
(cd modern/web && npm run lint && npm run typecheck && npm test)
(cd modern/web && npm run build)

# 로컬 실행: DB를 먼저 올리고 API와 화면은 별도 터미널에서 실행한다.
docker compose --profile modern up -d
(cd modern/api && ./gradlew bootRun)   # http://localhost:8080
(cd modern/web && npm run dev)         # http://localhost:5173

# 실습 환경 전체 점검
bash scripts/check-env.sh
```

- API 동작을 변경하면 API 테스트를, 화면 동작을 변경하면 lint · 타입 검사 · 화면 테스트를 실행한다.
- 완료 보고에는 변경 파일, 실행한 명령, 테스트 통과 · 실패 수, 남은 문제를 적는다.
- 실행하지 못한 검사는 이유와 함께 `실행하지 않음`으로 표시한다. 실패한 검사를 통과했다고 보고하지 않는다.
- 문서만 바꾼 경우 해당 문서의 경로 · 명령 · 규칙을 확인하고, 애플리케이션 테스트가 필요한지 판단한다.

## 2. 코딩 컨벤션

### API (`modern/api`)

- 도메인 패키지 안에서 Controller → Service → Repository 순으로 호출한다. 생성자 주입을 사용한다.
- 컨트롤러에서 Repository를 주입받거나 SQL을 직접 실행하지 않는다.
- 요청 · 응답에는 record DTO를 사용한다. JPA 엔티티를 HTTP 응답으로 직접 반환하지 않는다.
- 엔티티를 DTO로 변환하는 작업은 서비스의 트랜잭션 범위 안에서 수행한다.
- 빈 `catch` 또는 예외를 무시하고 성공처럼 처리하는 코드를 추가하지 않는다.
- 예외를 HTTP 응답으로 바꾸는 처리는 `common/GlobalExceptionHandler`에 둔다.
- 기존 오류 응답 규약을 유지한다: 없는 리소스 404, 잘못된 입력 400, 상태 충돌 409, 예상하지 못한 오류 500.
- 로그는 SLF4J를 사용한다. `System.out.println`, `System.err.println`, `printStackTrace()`를 추가하지 않는다.
- 테스트에서 시각을 고정할 수 있도록 기존 `Clock` 주입 방식을 따른다.
- 문항 상태는 `ItemStatus`를 사용한다. 공개 목록의 `ACTIVE` 필터와 상세 조회의 기존 동작을 임의로 바꾸지 않는다.
- 클래스 · 타입은 PascalCase, 메서드 · 변수는 camelCase, 상수는 UPPER_SNAKE_CASE로 작성한다.

### 화면 (`modern/web`)

- 함수 컴포넌트와 기존 `src/components/` 파일 배치를 따른다.
- 조회 흐름은 컴포넌트 → `src/hooks/` → `src/api/items.ts` → `src/api/client.ts`로 유지한다.
- HTTP 요청은 공통 `getJson` 함수를 거친다. 컴포넌트에서 `fetch`를 직접 호출하지 않는다.
- 응답 타입은 `src/api/types.ts`에 둔다. 서버의 JSON 필드 이름을 유지한다.
- TypeScript `any`, `@ts-ignore`, 사유 없는 린트 비활성화 주석을 추가하지 않는다.
- 타입 전용 import는 `import type`을 사용한다. 구체적인 형식은 현재 ESLint 설정을 따른다.
- 조회 화면에는 로딩 · 오류 · 빈 결과 상태를 표시하고, 기존 `QueryState` 구조를 사용한다.
- 디버깅용 `console.log`를 변경 결과에 남기지 않는다.

### 테스트 · R&D 작업 기준

- 새 엔드포인트와 서비스의 새 동작에는 정상 또는 경계 조건을 검증하는 테스트를 추가한다.
- API 테스트는 기존 `@WebMvcTest`, Mockito, `@DataJpaTest` 형식을 따르고 실제 운영 DB를 호출하지 않는다.
- 화면 테스트는 Vitest · Testing Library와 `src/test/mockFetch.ts`를 사용한다. 실제 API를 호출하지 않는다.
- 변경한 동작과 테스트의 관계를 보고한다. 검증 대상을 바꿔 실패한 테스트를 억지로 통과시키지 않는다.
- 분석 결과는 확인된 사실과 추정을 구분한다. 코드 · 로그에 관한 판단에는 `파일:줄번호` 근거를 붙인다.
- 프롬프트나 모델 동작을 변경하는 과제에서는 평가 데이터 · 조건과 변경 전후 결과를 함께 기록한다.
- 실제 LLM 호출이 필요한 기능도 단위 테스트에서는 가짜 객체로 대체한다.
- 학습자 식별자, 이메일, 토큰 등 민감정보를 로그 · 프롬프트 · 평가 데이터에 원문으로 남기지 않는다.

## 3. 금지 사항

- `legacy/`는 분석 · 이관 대상이며 명시적으로 요청받은 범위 외에는 수정하지 않는다.
- 새 의존성, DB 스키마 · 시드, 접속 · 커넥션 풀 설정 변경은 이유와 영향을 설명하고 승인된 범위에서 진행한다.
- 이미 승인된 작업은 같은 승인을 반복해서 요청하지 않는다. 요청에 포함되지 않은 변경은 먼저 확인한다.
- 회사 코드, 운영 데이터, 실제 시스템 이름 · 내부 주소 · 자격증명 · 사내 규정 원문을 공개 저장소에 넣지 않는다.
- `.env*`와 실제 비밀값을 읽거나 만들지 않는다. 실습에서 명시적으로 요구한 더미 파일은 그 범위에서만 다룬다.
- 운영 DB · 버킷 등 실제 서비스에 접속하는 명령을 만들거나 실행하지 않는다.
- 시작 전에 `git status`로 기존 변경을 확인한다. 기존 사용자 변경과 보안 수정은 임의로 되돌리지 않는다.
- 보안 수정이 반영된 잠금 파일을 단순 정리 목적으로 원본 버전으로 되돌리지 않는다.
- 실습용 위반 코드를 정리할 때는 이번 실습에서 만든 파일만 확인해 삭제한다.
- 기존 스냅샷이나 기대값을 변경해 동작 차이를 숨기지 않는다. 기준 변경이 필요하면 이유를 설명한다.
- 요청 범위 밖의 파일을 포맷팅 · 리팩토링 · 정리하지 않는다.

## 4. 아키텍처 안내

| 위치 | 역할 |
|---|---|
| `modern/api` | Spring Boot 3.3 · Java 21 API. `com.example.item`은 문항, `assignment`는 과제, `common`은 공통 응답 · 예외 |
| `modern/api/src/main/resources/application.yml` | 로컬 MariaDB 설정 |
| `modern/api/src/test/resources/application-test.yml` | H2 기반 테스트 설정 |
| `modern/web/src/components` | 화면 컴포넌트와 컴포넌트 테스트 |
| `modern/web/src/hooks` | 조회 상태와 요청 취소를 관리하는 훅 |
| `modern/web/src/api` | 공통 HTTP 클라이언트 · 요청 함수 · 응답 타입 |
| `modern/web/src/test` | 가짜 HTTP 응답과 테스트 데이터 |
| `legacy` | 분석할 PHP · Thymeleaf · MS-SQL 예제 |
| `characterization` | 레거시 응답을 정규화하고 비교하는 동작 보존 테스트 |
| `templates` | 프로젝트 유형별 규칙 · 검증 · 보안 템플릿 |
| `pipeline-samples`, `specs/starters/python` | 후속 차시의 배치 자료와 Python 시작 골격 |

- `php`, `thymeleaf`, `modern` Compose 프로필은 로컬 MariaDB를 공유한다.
- DB는 tmpfs를 사용하므로 컨테이너를 새로 만들면 더미 시드 상태로 돌아간다.
- 이 Mac은 Apple Silicon이므로 기본 실습은 `php` 또는 `thymeleaf` 프로필로 진행한다.
- 현재 없는 템플릿 경로나 라이브러리를 실제 프로젝트에 있다고 가정하지 않는다.
- 긴 반복 점검 절차는 별도 Skill로 작성하고, 이 파일에는 반복해서 필요한 짧은 규칙을 남긴다.

## 5. 완료 기준

- 이관 · 리팩토링은 `characterization/`의 `npm test`가 전부 통과하기 전에는 완료라고 보고하지 않는다.
- 테스트 실패 시 실패한 케이스와 실제 차이를 그대로 보고한다. `거의 됐다`고 요약하지 않는다.
- 통과시키기 위해 `characterization/`의 테스트 코드나 스냅샷을 고치지 않는다. 기준 변경이 필요하면 작업을 멈추고 사용자에게 묻는다.
- 레거시 동작이 버그처럼 보여도 이관 중에는 보존하고, `의심 동작` 목록으로 별도 보고한다.
