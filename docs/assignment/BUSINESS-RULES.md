# assignment 모듈 업무 규칙 — 새 과제 배포 화면 · 등록 처리

범위는 `GET /distributions/new`, `POST /distributions`와 직접 호출하는 코드뿐이다. 재배포와 배포 목록의 규칙은 다루지 않았고, 이 문서에 없다고 해서 그 기능에 규칙이 없다는 뜻이 아니다.
경로 약칭: `DC` = `legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java`, `DS` = `legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java`, `AD` = `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java`, `DD` = `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java`, `CD` = `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/ClassDao.java`, `AC` = `legacy/assignment-thymeleaf/src/main/java/com/example/assign/AppClock.java`, `FORM` = `legacy/assignment-thymeleaf/src/main/resources/templates/distribution_form.html`.

## 규칙

### BR-01
- 규칙: 새 배포 화면의 과제 선택 목록은 상태가 `O`, `X`, `C`인 과제만 보여 주고 마감 시각 빠른 순으로 정렬한다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:36-37`
- 코드:
  ```java
  + " WHERE a.status IN ('O', 'X', 'C') "
  + " ORDER BY a.due_at ASC, a.id ASC";
  ```
- 확신도: 확실
- 비고: 상태 `D`(삭제)와 `R`은 목록에 나오지 않는다. 스키마 주석은 상태를 `O=진행 C=마감`으로만 적고 있어 (`db/mariadb/init/01-schema.sql:87`) `X`는 코드에서만 확인된다. DB 비교는 테이블 collation을 따르므로 `x` 같은 소문자가 목록에 포함되는지는 확인하지 못했다.

### BR-02
- 규칙: 화면에서는 마감이 지난 과제의 선택 항목을 비활성화하되, 상태가 `X`이면 비활성화하지 않는다.
- 근거: `legacy/assignment-thymeleaf/src/main/resources/templates/distribution_form.html:16`
- 코드:
  ```html
  th:disabled="${a.dueAt != null and a.dueAt.isBefore(now) and a.status != 'X'}"
  ```
- 확신도: 확실
- 비고: 항목 문구에 `[연장]`(상태 `X`) 또는 `[마감]`을 붙인다 (`legacy/assignment-thymeleaf/src/main/resources/templates/distribution_form.html:18`). 화면 제어일 뿐이라 서버 검증은 BR-03이 맡는다. 템플릿의 `now`는 `form`이 담은 `AppClock.now()` 값이다 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:44`).

### BR-03
- 규칙: 마감 시각이 현재보다 앞선 과제는 상태가 `X`가 아니면 배포 등록을 거부하고 `/distributions/new`로 되돌린다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:60-61`
- 코드:
  ```java
  if (a.getDueAt() != null && a.getDueAt().isBefore(now)) {
      if (!"X".equals(a.getStatus())) {
  ```
- 확신도: 확실
- 비고: `isBefore`는 엄격 비교라 마감 시각이 정확히 `now`와 같으면 거부하지 않는다. `dueAt`이 null이면 이 검사를 건너뛴다 (스키마는 `due_at NOT NULL`, `db/mariadb/init/01-schema.sql:86`). 이 검사는 컨트롤러 `create`에만 있고 `DistributionService.distribute`에는 없다. `distribute`를 부르는 다른 경로가 있는지는 범위 밖이라 확인하지 않았으므로, 모든 호출 경로의 규칙이라고 단정하지 않는다. 주석(`legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:59`)의 "연장(X) 상태만 예외"는 코드와 일치한다. `"X".equals`는 대소문자를 구분한다.

### BR-04
- 규칙: 요청한 과제 ID가 없으면 "존재하지 않는 과제입니다."를 flash로 남기고 `/distributions/new`로 되돌린다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:53-55`
- 코드:
  ```java
  AssignmentRow a = assignmentDao.findById(assignmentId);
  if (a == null) {
      ra.addFlashAttribute("error", "존재하지 않는 과제입니다.");
  ```
- 확신도: 확실
- 비고: 서비스도 같은 조건에서 `IllegalArgumentException`을 던진다 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java:48-51`). 컨트롤러 검사가 먼저 실행되므로 정상 경로에서 서비스의 이 분기는 도달하기 어렵다고 추정한다.

### BR-05
- 규칙: 요청한 학급 ID가 없으면 배포를 만들지 않고 `IllegalArgumentException`을 던지며, 컨트롤러가 그 메시지를 flash 오류로 보여 준다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java:52-54`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:69-71`
- 코드:
  ```java
  ClassRow c = classDao.findById(classId);
  if (c == null) {
      throw new IllegalArgumentException("존재하지 않는 학급입니다. (id=" + classId + ")");
  ```
- 확신도: 확실
- 비고: 컨트롤러 `create`는 학급 존재를 직접 확인하지 않는다. 학급 검사는 서비스에서만 이루어진다.

### BR-06
- 규칙: 상태가 `D`(삭제)인 과제는 배포할 수 없다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java:56-57`
- 코드:
  ```java
  if ("D".equals(a.getStatus())) {
      throw new IllegalStateException("삭제된 과제는 배포할 수 없습니다.");
  ```
- 확신도: 확실
- 비고: 마감이 지난 `D` 과제는 서비스까지 가기 전에 BR-03이 마감 메시지로 먼저 거부한다 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:60-63`). 상태 `R`을 막는 코드는 이 범위 안에서 확인하지 못했다. 막지 않는다고 단정하지는 않는다.

### BR-07
- 규칙: 같은 과제를 같은 학급에 이미 배포했으면 새로 배포할 수 없다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java:59-60`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:49`
- 코드:
  ```java
  if (distributionDao.countByAssignmentAndClass(assignmentId, classId) > 0) {
      throw new IllegalStateException("이미 이 학급에 배포된 과제입니다. 다시 내려면 배포 목록에서 재배포를 사용하세요.");
  ```
- 확신도: 확실
- 비고: DB에는 UNIQUE 제약이 없고 코드로만 막는다 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:46` 주석, `db/mariadb/init/01-schema.sql:99-100`). 조회와 INSERT 사이에 잠금이 없어 동시 요청 시 중복이 생길 수 있는지는 실행해 확인하지 못했다.

### BR-08
- 규칙: 배포 시각과 마감 판정에 쓰는 "현재 시각"은 시스템 속성 `app.clock=system`일 때만 실제 시각이고, 아니면 고정값 `2026-09-15 00:00:00`이다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/AppClock.java:9`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/AppClock.java:15-16`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/AppClock.java:19`
- 코드:
  ```java
  private static final String FIXED = "2026-09-15 00:00:00";
  ```
  ```java
  String sys = System.getProperty("app.clock");
  if (sys != null && sys.equals("system")) {
  ```
  ```java
  return LocalDateTime.parse(FIXED, FMT);
  ```
- 확신도: 확실
- 비고: 주석(`legacy/assignment-thymeleaf/src/main/java/com/example/assign/AppClock.java:8`)은 "2024-03 QA 기간 중 임시로 고정, 운영 반영 전 되돌릴 것 (아직 안 되돌림)"이라고 적고 있지만, 코드의 고정값은 2026-09-15라서 주석의 연도와 맞지 않는다. 코드 기준으로 적었다. 이 분석 범위의 `AppClock.now`는 시스템 속성 값에 따라 분기한다. 저장소·실행 환경 전체에서 속성이 설정되지 않는다고 단정하지 않는다. 속성 미설정 시 BR-02, BR-03의 마감 판정과 `distributed_at`은 기본적으로 2026-09-15 00:00:00 기준이다. 배포 환경에서 JVM 옵션으로 따로 켜는지는 확인하지 못했다.

### BR-09
- 규칙: 새 배포의 번호는 기존 최대 `id`에 1을 더해 만들고, `redistributed`는 0으로 저장한다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:55-56`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:61`
- 코드:
  ```java
  Long max = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM distribution", Long.class);
  ```
  ```java
  "INSERT INTO distribution (id, assignment_id, class_id, distributed_at, redistributed) VALUES (?, ?, ?, ?, 0)",
  ```
- 확신도: 확실
- 비고: `distribution.id`에 AUTO_INCREMENT가 없다 (`db/mariadb/init/01-schema.sql:93`). `redistributed = 0`이 "재배포되지 않음"을 뜻하는지는 컬럼 이름에서 추정한 것이다.

### BR-10
- 규칙: INSERT가 정확히 1건을 저장하지 않으면 "배포 저장에 실패했습니다."로 처리한다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java:63-65`
- 코드:
  ```java
  int n = distributionDao.insert(id, assignmentId, classId, AppClock.now());
  if (n != 1) {
      throw new IllegalStateException("배포 저장에 실패했습니다.");
  ```
- 확신도: 확실
- 비고: `@Transactional` 메서드 안에서 던지므로 롤백되는지는 Spring 기본 동작(런타임 예외 롤백)에 따른 추정이다 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java:46`).

### BR-11
- 규칙: 등록이 성공하면 `/distributions`로, 서비스 호출 중 잡은 입력 · 상태 오류와 DB 오류는 `/distributions/new`로 이동하며, 상태 오류는 예외 메시지를 그대로 보여 주고 DB 오류는 고정 문구를 보여 준다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:69-70`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:72-73`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:76`
- 코드:
  ```java
  } catch (IllegalArgumentException | IllegalStateException e) {
      ra.addFlashAttribute("error", e.getMessage());
  ```
  ```java
  } catch (DataAccessException e) {
      ra.addFlashAttribute("error", "DB 오류로 배포에 실패했습니다.");
  ```
- 확신도: 확실
- 비고: 처음의 `assignmentDao.findById`는 try 밖(`legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:53`)이므로 그 조회의 DB 오류까지 이 catch가 처리한다고 단정하지 않는다. 그 경로 및 위 세 종류 외의 예외가 어떻게 처리되는지는 `DbErrorAdvice`를 읽지 않아 미확인이다.

### BR-12
- 규칙: 과제와 학급은 둘 다 필수 선택이며, 서버도 두 값을 필수 요청 파라미터로 받는다.
- 근거: `legacy/assignment-thymeleaf/src/main/resources/templates/distribution_form.html:11`, `legacy/assignment-thymeleaf/src/main/resources/templates/distribution_form.html:23`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:50-51`
- 코드:
  ```html
  <select id="assignmentId" name="assignmentId" required>
  ```
  ```java
  public String create(@RequestParam("assignmentId") long assignmentId,
                       @RequestParam("classId") long classId,
  ```
- 확신도: 확실
- 비고: 값이 비었을 때 서버 응답은 코드에 처리가 없어 Spring 기본 동작(추정)이며 실행으로 확인하지 못했다. 브라우저 `required`는 화면 검증일 뿐이다.

### BR-13
- 규칙: 학급 선택 목록은 조건 없이 모든 학급을 `id` 오름차순으로 보여 준다.
- 근거: `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/ClassDao.java:21`
- 코드:
  ```java
  return jdbc.query("SELECT id, name, teacher_id FROM `class` ORDER BY id ASC", MAPPER);
  ```
- 확신도: 확실
- 비고: 담당 교사나 작업자에 따라 학급을 제한하는 코드는 이 범위 안에서 확인하지 못했다. 항목에는 `이름 (teacher_id)`가 표시된다 (`legacy/assignment-thymeleaf/src/main/resources/templates/distribution_form.html:25`).

## 매직 넘버 · 코드값
| 값 | 위치 | 추정 의미 |
|---|---|---|
| `'O'`, `'C'` | `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:36` | 진행, 마감 (스키마 주석 `db/mariadb/init/01-schema.sql:87` 근거) |
| `'X'` | `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:36`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:61`, `legacy/assignment-thymeleaf/src/main/resources/templates/distribution_form.html:16`, `legacy/assignment-thymeleaf/src/main/resources/templates/distribution_form.html:18` | 연장 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/web/DistributionController.java:59` 주석과 `legacy/assignment-thymeleaf/src/main/resources/templates/distribution_form.html:18`의 `[연장]` 표기 근거) |
| `'D'` | `legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java:56` | 삭제 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java:57` 메시지 근거) |
| `0` | `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:61` | `redistributed` 초기값, 재배포 안 됨 (컬럼명에서 추정) |
| `1L` | `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:56` | `MAX(id)`에 더하는 다음 번호 증분 |
| `1` (`n != 1`) | `legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java:64` | INSERT 정상 건수 |
| `"2026-09-15 00:00:00"` | `legacy/assignment-thymeleaf/src/main/java/com/example/assign/AppClock.java:9` | QA 기간용 고정 "현재" 시각 (BR-08 비고 참고) |
| `"yyyy-MM-dd HH:mm"` | `legacy/assignment-thymeleaf/src/main/java/com/example/assign/AppClock.java:12`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/AppClock.java:26` | 오류 메시지의 마감 시각 표시 형식 |
| `'MM-dd HH:mm'` | `legacy/assignment-thymeleaf/src/main/resources/templates/distribution_form.html:17` | 선택 항목의 마감 시각 표시 형식 |

## 미확인 사항
- 상태 `R`(검수중) 과제를 새로 배포할 수 있는지: 막는 코드를 이 범위에서 찾지 못했다.
- `distribute`를 부르는 다른 호출자가 있는지, 있다면 BR-03이 적용되는지.
- `DbErrorAdvice` 및 위 세 예외 이외의 예외 처리.
- 시드 데이터에서 `assignment.status`의 실제 값 분포와 `due_at`의 null 가능성.
- 상태 비교에서 DB collation 비교와 Java `equals`의 대소문자 차이가 실제 데이터에서 문제가 되는지.
- 동시 요청에서 `MAX(id)+1` 채번과 중복 검사의 안전성.
- 템플릿 렌더링 결과와 서버 실행 결과는 직접 확인하지 않았다 (코드 읽기만 수행).

## 검증 이력
- 2026-10-07: 세 문서의 `파일:줄번호`와 BR-01~BR-13 인용 코드를 소스에서 다시 검색해 같은 줄 번호에 있는지 확인했다. 줄 번호 오류는 없었다.
- 2026-10-07: BR-09 인용문 `jdbc.queryForObject` 중간에 보이지 않는 문자(U+200B)가 들어가 있어 제거했다. 소스 원문(`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:55`)과 일치한다.
- 2026-10-07: 앱을 실행하거나 템플릿을 렌더링해 확인하지는 않았다. 동작 서술은 모두 코드 읽기 기준이다.

- 2026-10-07: 재사용 결과의 경로 약칭을 저장소 기준 전체 경로로 바꾸고 인용과 대조했다.
- 2026-10-07: BR-11의 DB 예외 처리를 서비스 호출 try 범위로 좁혔다. 첫 assignmentDao 조회(컨트롤러 53줄)는 try 밖이며, catch(72줄)와 같은 범위로 일반화하지 않는다.
