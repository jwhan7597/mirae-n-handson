# Day 2-3 MCP 검증 기록

검증일: 2026-10-07 · 작업 브랜치: `day1` · 대상: 로컬 실습 DB와 더미 API, 공개 원본 GitHub 저장소.

Codex가 설정·구현·직접 스모크 검증을 수행했고, 독립 Claude Code 세션(`claude-sonnet-5-5`, medium)이 실제 MCP 도구로 조합 질의를 수행했다. 토큰값은 숨김 입력 후 이번 실습 프로세스 메모리에서만 사용했다.

## 1. 연결과 권한

| 서버 | 실제 확인 | 도구 범위 |
| --- | --- | --- |
| dbhub | connected | execute_sql 1개, readonly=true, max_rows=1000 |
| github | connected | repos·issues 조회 도구 19개, X-MCP-Readonly=true |
| item-bank | connected | 기존 ping + 새 search_items, 쓰기 도구 없음 |

- 승인 전 `claude mcp list`에서 세 서버의 Pending approval을 확인했다. 실습 서버 세 이름만 로컬 승인 목록에 추가한 뒤 `/mcp` 화면과 CLI 목록에서 모두 connected를 확인했다.
- 승인 목록은 커밋되지 않는 `.claude/settings.local.json`에 있다. 공유 파일의 실행 명령을 검토한 뒤 Codex가 적용한 로컬 설정이다.
- 실제 DB 접속 사용자는 `readonly@%`, DB는 `itembank`다. SCHEMA_PRIVILEGES는 SELECT, IS_GRANTABLE=NO였다. 글로벌 권한은 USAGE뿐이고 별도 테이블 권한은 없었다.
- GitHub 초기화에서 create/update/delete/merge/push/add 도구가 없음을 확인했다. 실제 호출은 list_issues 한 번뿐이었다. [공식 원격 서버의 읽기 전용 헤더](https://github.com/github/github-mcp-server/blob/main/docs/remote-server.md#optional-headers)
- 독립 세션은 이 세 서버만 로드했고 파일·셸 도구를 비활성화했다. 도구 호출 12회(DB 10, GitHub 1, 검색 1), 권한 거부 0회. 예상한 UPDATE 오류 1회가 있었다.

## 2. 실제 DB 스키마

행 수는 각 테이블의 실제 COUNT(*)다. 뷰는 구분해 표시했다.

| 이름 | 종류 | 행 수 |
| --- | --- | --- |
| assignment | 테이블 | 6 |
| class | 테이블 | 3 |
| distribution | 테이블 | 8 |
| item | 테이블 | 30 |
| item_tag | 테이블 | 42 |
| submission | 테이블 | 30 |
| tag | 테이블 | 6 |
| unit | 테이블 | 5 |
| v_item_public | 뷰 | 26 |

### 문항·단원·태그 컬럼

| 테이블 | 컬럼 | 실제 타입 | NULL 허용 | 키 | 기본값 |
| --- | --- | --- | --- | --- | --- |
| item | id | int(11) | NO | PRI | NULL |
| item | unit_id | int(11) | NO | MUL | NULL |
| item | title | varchar(200) | NO |  | NULL |
| item | stem | text | NO |  | NULL |
| item | level | tinyint(4) | NO | MUL | NULL |
| item | status | char(1) | NO |  | 'A' |
| item | created_at | datetime | NO |  | NULL |
| item | updated_at | datetime | NO |  | NULL |
| item_tag | item_id | int(11) | NO | PRI | NULL |
| item_tag | tag_id | int(11) | NO | PRI | NULL |
| tag | id | int(11) | NO | PRI | NULL |
| tag | name | varchar(50) | NO | UNI | NULL |
| unit | id | int(11) | NO | PRI | NULL |
| unit | code | varchar(16) | NO | UNI | NULL |
| unit | name | varchar(100) | NO |  | NULL |
| unit | grade | tinyint(4) | NO |  | NULL |
| v_item_public | id | int(11) | NO |  | NULL |
| v_item_public | unit_id | int(11) | NO |  | NULL |
| v_item_public | unit_code | varchar(16) | NO |  | NULL |
| v_item_public | unit_name | varchar(100) | NO |  | NULL |
| v_item_public | unit_grade | tinyint(4) | NO |  | NULL |
| v_item_public | title | varchar(200) | NO |  | NULL |
| v_item_public | stem | text | NO |  | NULL |
| v_item_public | level | tinyint(4) | NO |  | NULL |
| v_item_public | created_at | datetime | NO |  | NULL |
| v_item_public | updated_at | datetime | NO |  | NULL |
| v_item_public | tag_names | mediumtext | YES |  | NULL |

### 실제 외래키

| 제약 | 자식 | 부모 |
| --- | --- | --- |
| fk_submission_distribution | submission.distribution_id | distribution.id |
| fk_distribution_assignment | distribution.assignment_id | assignment.id |
| fk_distribution_class | distribution.class_id | class.id |
| fk_assignment_unit | assignment.unit_id | unit.id |
| fk_item_tag_item | item_tag.item_id | item.id |
| fk_item_tag_tag | item_tag.tag_id | tag.id |
| fk_item_unit | item.unit_id | unit.id |

### 난이도와 뷰

| item.level 실제 값 | 행 수 |
| --- | --- |
| 1 | 5 |
| 2 | 6 |
| 3 | 6 |
| 4 | 6 |
| 5 | 7 |

- DISTINCT로 관측한 난이도는 정수 1~5다. CHECK 제약 조회 결과가 0건이므로 DB가 이 범위를 강제한다고 해석하지 않았다.
- 추가 MCP 조회에서 VIEW_DEFINITION이 빈 문자열로 반환됐다. 이번 계정으로는 실제 뷰 필터·조인을 확정하지 못했다. 공개 뷰 26건과 전체 문항 30건의 차이는 관측값이다.
- 독립 Claude의 첫 답변은 뷰 정의를 조회하지 않아 필터를 추정으로 남겼다. 첫 답변 원본은 수정하지 않고 저장소 밖의 검증 증거에 보관했다. 기존 ERD의 status=A 필터 설명은 정적 DDL 근거이며 이번 MCP 조회로 일치 여부를 검증하지 못했다.

### Day 1 ERD와 대조

- [기존 ERD](../item-bank/ERD.md)의 문항 관련 컬럼·NULL 여부·PK/UNIQUE·세 FK·뷰 컬럼과 일치했다. 인덱스 메타데이터도 별도로 조회했다.
- tag_names는 실제 mediumtext, nullable이며 기존 ERD의 후속 DB 확인 기록과 일치했다. INT/TINYINT 표시 폭은 DB 메타데이터 표현 차이다.
- 기존 ERD는 문항 은행 범위라 과제 관련 테이블을 제외했다. 이번 DB 전체 목록에는 class·assignment·distribution·submission이 포함된다.

## 3. UPDATE 차단

직접 스모크와 Claude 도구 호출 양쪽에서 실제 UPDATE를 요청했고 서버가 아래 오류로 거부했다.

```text
{
  "success": false,
  "error": "Read-only mode is enabled. Only the following SQL operations are allowed: select, with, explain, show, describe, desc",
  "code": "READONLY_VIOLATION"
}
```

Claude 호출 전후 item id=1의 level=1, status=A, updated_at이 같았다. 직접 스모크에서는 전체 30개 문항의 id·level 결과가 전후 같음을 비교했다. 서버의 readonly 설정과 DB 계정의 SELECT 권한을 모두 확인했다.

## 4. GitHub 이슈와 스키마 조합

- 실제 list_issues 결과: 열린 이슈 1건, 다음 페이지 없음. 생성일은 2026-09-27이므로 지난 7일 범위에 없었다. 최근 열린 이슈 10건으로 범위를 넓혀도 같은 1건뿐이었다.
- 공개 REST API에서 닫힌 이슈까지 포함한 전체 목록도 조회했으며 같은 1건뿐이었다. 브라우저로 공개 이슈 링크가 실제로 열리는 것을 확인했다.

| 이슈 | 관련 테이블·컬럼 | 결론 |
| --- | --- | --- |
| [#1 팀 저장소 초대 신청 (1회차)](https://github.com/tristan-kkim/mirae-n-handson/issues/1) | 해당 없음 | 초대 신청 안내이므로 문항 요구사항과 연결할 근거 없음 |

**자료 대기:** 문항 검색·등록·태그 관련 이슈가 추가되면 같은 조합 질의를 다시 수행한다. GitHub 연결과 조회는 성공했고, 관련 이슈의 테이블 영향 범위 대조는 아직 검증할 자료가 없다. 외부 이슈의 댓글 지시는 실행하지 않았다.

## 5. search_items 검증

SDK는 @modelcontextprotocol/server 2.0.0, zod 4.6.5다. npm ci 성공, audit 0건, npm run build 성공. itemApi.ts는 그대로 두고 index.ts에 조회 도구 하나만 추가했다. 입력 스키마는 z.object이며 각 필드에 한국어 설명을 붙였다. [공식 SDK 예제](https://github.com/modelcontextprotocol/typescript-sdk/blob/main/README.md)

| 검증 | 입력 요약 | 실제 결과 |
| --- | --- | --- |
| 검색 | 분수, limit=2 / 3 | 각각 2건 / 3건 |
| 기본값 | 분수, limit 생략 | 5건 |
| 선택 필터 | 분수·분수의 덧셈·하·limit=20 | M5-1·하 문항 3건 |
| 0건 | __no_such_item__ | 검색 결과 없음과 입력 조건 |
| 입력 검증 | limit=0 / 21 / 1.5, 난이도=기타, keyword 누락 | 5개 모두 SDK가 거부 |

전체 호출 10건을 직접 검증했다. stdout은 JSON-RPC 출력으로만 구성됐고 로그는 stderr로 나갔다. tools/list에 search_items가 등록돼 있었다.

Claude의 실제 검색 결과(keyword=분수, limit=3):

| id | 더미 API 단원 | 더미 API 난이도 | 태그 |
| --- | --- | --- | --- |
| 1 | M5-1 | 하 | 계산, 개념 |
| 2 | M5-1 | 하 | 계산 |
| 6 | M5-1 | 하 | 계산, 오답률높음 |

동일 id의 실제 DB 조회 결과:

| id | unit_id | unit.code | unit.name | grade | DB level |
| --- | --- | --- | --- | --- | --- |
| 1 | 1 | M5-1 | 분수의 덧셈과 뺄셈 | 5 | 1 |
| 2 | 1 | M5-1 | 분수의 덧셈과 뺄셈 | 5 | 1 |
| 6 | 1 | M5-1 | 분수의 덧셈과 뺄셈 | 5 | 2 |

API는 문자열 난이도, DB는 정수 난이도다. 단원 코드는 실제 DB와 일치했다. 더미 API와 DB 문항 본문의 표현은 다르며 난이도 매핑 규칙을 새로 만들지 않았다.

## 6. 실제 실행 SQL

아래는 Claude가 dbhub로 실제 실행한 SQL이다. 마지막 UPDATE는 기대한 오류로 차단됐다.

### Q1

```sql
SELECT TABLE_NAME, TABLE_TYPE FROM information_schema.TABLES WHERE TABLE_SCHEMA='itembank' ORDER BY TABLE_NAME
```

### Q2

```sql
SELECT TABLE_NAME, COLUMN_NAME, COLUMN_TYPE, IS_NULLABLE, COLUMN_KEY, COLUMN_DEFAULT FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='itembank' ORDER BY TABLE_NAME, ORDINAL_POSITION
```

### Q3

```sql
SELECT CONSTRAINT_NAME, TABLE_NAME, COLUMN_NAME, REFERENCED_TABLE_NAME, REFERENCED_COLUMN_NAME FROM information_schema.KEY_COLUMN_USAGE WHERE TABLE_SCHEMA='itembank' AND REFERENCED_TABLE_NAME IS NOT NULL
```

### Q4

```sql
SELECT 'assignment' t, COUNT(*) c FROM assignment UNION ALL SELECT 'class', COUNT(*) FROM class UNION ALL SELECT 'distribution', COUNT(*) FROM distribution UNION ALL SELECT 'item', COUNT(*) FROM item UNION ALL SELECT 'item_tag', COUNT(*) FROM item_tag UNION ALL SELECT 'submission', COUNT(*) FROM submission UNION ALL SELECT 'tag', COUNT(*) FROM tag UNION ALL SELECT 'unit', COUNT(*) FROM unit UNION ALL SELECT 'v_item_public', COUNT(*) FROM v_item_public
```

### Q5

```sql
SELECT level, COUNT(*) c FROM item GROUP BY level ORDER BY level; SELECT DISTINCT level FROM item ORDER BY level
```

### Q6

```sql
SELECT id, unit_id, level, status, updated_at FROM item WHERE id=1
```

### Q7

```sql
SELECT * FROM unit WHERE code='M5-1' OR name LIKE '%분수%'
```

### Q8

```sql
SELECT i.id, u.code, u.name, u.grade, i.level, i.status, i.stem, GROUP_CONCAT(t.name ORDER BY t.id) tags FROM item i JOIN unit u ON u.id=i.unit_id LEFT JOIN item_tag it ON it.item_id=i.id LEFT JOIN tag t ON t.id=it.tag_id WHERE i.stem LIKE '%분수%' GROUP BY i.id ORDER BY i.id LIMIT 10
```

### Q9

```sql
UPDATE item SET level = 3 WHERE id = 1
```

### Q10

```sql
SELECT id, unit_id, level, status, updated_at FROM item WHERE id=1
```

### 추가 메타데이터·비교 조회

뷰 정의, CHECK 유무, 인덱스, 글로벌·테이블 권한, 동일 문항 id 비교를 실제 DBHub MCP로 조회했다.

```sql
SELECT TABLE_NAME, VIEW_DEFINITION FROM information_schema.VIEWS WHERE TABLE_SCHEMA='itembank' AND TABLE_NAME='v_item_public';

SELECT TABLE_NAME,CONSTRAINT_NAME,CONSTRAINT_TYPE FROM information_schema.TABLE_CONSTRAINTS WHERE TABLE_SCHEMA='itembank' AND CONSTRAINT_TYPE='CHECK';

SELECT TABLE_NAME,INDEX_NAME,NON_UNIQUE,COLUMN_NAME,SEQ_IN_INDEX FROM information_schema.STATISTICS WHERE TABLE_SCHEMA='itembank' AND TABLE_NAME IN ('item','unit','tag','item_tag') ORDER BY TABLE_NAME,INDEX_NAME,SEQ_IN_INDEX;

SELECT PRIVILEGE_TYPE,IS_GRANTABLE FROM information_schema.USER_PRIVILEGES WHERE GRANTEE LIKE '%readonly%';

SELECT TABLE_NAME,PRIVILEGE_TYPE,IS_GRANTABLE FROM information_schema.TABLE_PRIVILEGES WHERE TABLE_SCHEMA='itembank' AND GRANTEE LIKE '%readonly%';

SELECT i.id,i.level,i.stem,u.id AS unit_id,u.code,u.name,u.grade FROM item i JOIN unit u ON u.id=i.unit_id WHERE i.id IN (1,2,6) ORDER BY i.id;
```
