# assignment 모듈 데이터 흐름 — 새 과제 배포 화면 · 등록 처리

범위는 `GET /distributions/new`와 `POST /distributions`가 직접 읽고 쓰는 테이블이다. 스키마 출처는 `db/mariadb/init/01-schema.sql`이고, 이 모듈의 접속 대상이 MariaDB `itembank`임은 `legacy/assignment-thymeleaf/src/main/resources/application.properties:2`에서 확인했다. 범위 안에서 뷰는 쓰지 않는다.

## 테이블과 주요 컬럼
| 테이블 | 컬럼 (타입, 제약) | 근거 |
|---|---|---|
| `unit` | `id INT NOT NULL` (PK), `code VARCHAR(16) NOT NULL` (UNIQUE), `name VARCHAR(100) NOT NULL`, `grade TINYINT NOT NULL` | `db/mariadb/init/01-schema.sql:11-18` |
| `assignment` | `id INT NOT NULL` (PK), `title VARCHAR(200) NOT NULL`, `unit_id INT NOT NULL`, `due_at DATETIME NOT NULL`, `status CHAR(1) NOT NULL DEFAULT 'O'` | `db/mariadb/init/01-schema.sql:82-90` |
| `class` | `id INT NOT NULL` (PK), `name VARCHAR(50) NOT NULL`, `teacher_id VARCHAR(20) NOT NULL` | `db/mariadb/init/01-schema.sql:75-80` |
| `distribution` | `id INT NOT NULL` (PK), `assignment_id INT NOT NULL`, `class_id INT NOT NULL`, `distributed_at DATETIME NOT NULL`, `redistributed TINYINT NOT NULL DEFAULT 0` | `db/mariadb/init/01-schema.sql:92-103` |

- 범위 안 코드가 읽는 `unit` 컬럼은 `id`, `code`, `name`뿐이다 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:22`). `grade`는 쓰지 않는다.
- `distribution.id`에는 AUTO_INCREMENT가 없다. 코드가 `MAX(id)+1`로 번호를 만든다 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:55-56`).
- `assignment.status`의 스키마 주석은 `O=진행 C=마감`이다 (`db/mariadb/init/01-schema.sql:87`). 코드는 `O`, `X`, `C`, `D`를 쓴다 (BR-01, BR-03 참고).
- `submission`은 이 두 진입점이 호출하는 SQL에 나오지 않아 제외했다.

## ER 다이어그램
```mermaid
erDiagram
    unit ||--o{ assignment : "unit_id (선언)"
    assignment ||--o{ distribution : "assignment_id (선언)"
    class ||--o{ distribution : "class_id (선언)"

    unit {
        INT id PK
        VARCHAR code
        VARCHAR name
    }
    assignment {
        INT id PK
        VARCHAR title
        INT unit_id FK
        DATETIME due_at
        CHAR status
    }
    class {
        INT id PK
        VARCHAR name
        VARCHAR teacher_id
    }
    distribution {
        INT id PK
        INT assignment_id FK
        INT class_id FK
        DATETIME distributed_at
        TINYINT redistributed
    }
```

## 관계 표
| 관계 | 구분 | 근거 |
|---|---|---|
| `assignment.unit_id` → `unit.id` | 선언 | `db/mariadb/init/01-schema.sql:89` |
| `distribution.assignment_id` → `assignment.id` | 선언 | `db/mariadb/init/01-schema.sql:101` |
| `distribution.class_id` → `class.id` | 선언 | `db/mariadb/init/01-schema.sql:102` |

범위 안 SQL의 JOIN(`LEFT JOIN unit u ON u.id = a.unit_id`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:24`)과 서브쿼리(`d.assignment_id = a.id`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:23`)는 모두 위 선언 관계와 같다. JOIN에서만 추정한 관계는 없다.

`(assignment_id, class_id)`에는 UNIQUE 제약이 없고 일반 인덱스만 있다 (`db/mariadb/init/01-schema.sql:99-100`). 코드 주석도 같은 내용이다 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:46`).

## 테이블별 읽기 · 쓰기 위치
| 테이블 | 읽기 (SELECT) | 쓰기 (INSERT · UPDATE · DELETE) |
|---|---|---|
| `unit` | `AssignmentDao.findAllForSelect` (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:34-39`), `AssignmentDao.findById` (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:41-48`). 둘 다 상수 `BASE_SELECT`(`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:21-24`)의 LEFT JOIN으로 읽는다. | 범위 안에 없음 |
| `assignment` | 위와 같음. `findAllForSelect`는 `WHERE a.status IN ('O', 'X', 'C')` (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:36`), `findById`는 `WHERE a.id = ?` (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:42`). | 범위 안에 없음 |
| `class` | `ClassDao.findAll` (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/ClassDao.java:20-22`), `ClassDao.findById` (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/ClassDao.java:24-30`) | 범위 안에 없음 |
| `distribution` | `BASE_SELECT`의 `class_cnt` 서브쿼리 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:23`, `findAllForSelect` · `findById` 경유), `DistributionDao.countByAssignmentAndClass` (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:47-52`), `DistributionDao.nextId` (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:54-57`) | INSERT: `DistributionDao.insert` (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:59-63`) |

- 모든 쓰기는 `DistributionService.distribute`의 `@Transactional` 안에서 일어난다 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/service/DistributionService.java:46-47`).
- 범위 안에서 UPDATE · DELETE를 실행하는 코드는 확인하지 못했다. (재배포의 `UPDATE`는 범위 밖이다.)

## 미확인
- 시드 데이터에 `assignment.status`로 `X`, `R`, `D`가 실제 들어 있는지 읽지 않았다. 스키마에는 값 제약(CHECK 등)이 없다.
- `assignment.due_at`은 `NOT NULL`이지만 코드는 null을 검사한다 (BR-03 비고). 실제 데이터에 null이 올 수 있는 경로는 확인하지 못했다.
- 동시에 두 요청이 들어올 때 `nextId`의 `MAX(id)+1`이 충돌하는지는 실행하지 않았다. PK 중복 시 `DataAccessException`이 될 것으로 추정한다.
- `CHAR(1)` 컬럼의 비교는 테이블 collation `utf8mb4_unicode_ci`(`db/mariadb/init/01-schema.sql:90`)를 따른다. 대소문자 구분 여부는 collation 문서 기준 추정이며, Java의 `"X".equals(...)` 비교와 같다고 가정하지 않는다. 서버 · 연결 collation은 확인하지 못했다.
- `AssignmentRow`가 `unit_id`를 `rs.getInt`로 읽고 `distribution.id`는 Java `long`으로 다룬다 (`legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/AssignmentDao.java:56`, `legacy/assignment-thymeleaf/src/main/java/com/example/assign/dao/DistributionDao.java:54`). DB 타입과 다르지만 이 범위에서 오류 경로는 확인하지 못했다.
