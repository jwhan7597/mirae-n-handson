# Day 1-3 문항 검색 이관 계획

작성: 2026-10-07. 대상은 읽기 전용 `GET /search.php` 하나이다.
등록·단원 목록·화면 렌더링을 이관하지 않는다. 현재 응답을 기준으로 삼고 의심 동작도 보존한다.

## 1. 입력 케이스 설계

기대 결과는 아래에 적지 않는다. PHP의 실제 응답을 `npm run baseline -- item-bank`로 저장한다.
동일한 입력·정규화·스냅샷을 유지하고 `TARGET_BASE_URL`만 바꾸어 새 API를 검증한다.

| 케이스 | 요청 `/search.php`의 파라미터 | 규칙 | 선택 이유 |
|---|---|---|---|
| 정상 · 제목/지문 키워드 | `{ q: '분수' }` | BR-04 | 제목/지문 키워드의 경계·현재 동작 확인 |
| 정상 · 단원 코드 | `{ unit: 'M5-1' }` | BR-07 | 단원 코드의 경계·현재 동작 확인 |
| 정상 · 태그 이름 | `{ tag: '계산' }` | BR-12 | 태그 이름의 경계·현재 동작 확인 |
| 정상 · 조건 AND 결합 | `{ unit: 'M5-1', level: '4', tag: '문장제' }` | BR-02 | 조건 AND 결합의 경계·현재 동작 확인 |
| 누락 · 모든 조건 기본값 | `{}` | BR-09,BR-15 | 모든 조건 기본값의 경계·현재 동작 확인 |
| 빈 값 · 공백 조건 | `{ q: ' ', unit: '', level: ' ', tag: '', sort: '', dir: '', page: '' }` | BR-02,BR-09,BR-20 | 공백 조건의 경계·현재 동작 확인 |
| 경계 · 난이도 1 | `{ level: '1' }` | BR-10 | 난이도 1의 경계·현재 동작 확인 |
| 경계 · 난이도 4 | `{ level: '4' }` | BR-09,BR-10 | 난이도 4의 경계·현재 동작 확인 |
| 경계 · 난이도 5 명시 | `{ level: '5' }` | BR-09,BR-10 | 난이도 5 명시의 경계·현재 동작 확인 |
| 이상 · 음수 난이도 | `{ level: '-1' }` | BR-11 | 음수 난이도의 경계·현재 동작 확인 |
| 이상 · 난이도 05 정수 변환 | `{ level: '05' }` | BR-11 | 난이도 05 정수 변환의 경계·현재 동작 확인 |
| 이상 · 난이도 숫자 접두사 | `{ level: '3abc' }` | BR-11 | 난이도 숫자 접두사의 경계·현재 동작 확인 |
| 이상 · 난이도 소수 버림 | `{ level: '3.7' }` | BR-11 | 난이도 소수 버림의 경계·현재 동작 확인 |
| 이상 · 난이도 지수 표기 | `{ level: '1e0' }` | BR-11 | 난이도 지수 표기의 경계·현재 동작 확인 |
| 이상 · 존재하지 않는 단원 | `{ unit: 'Z99-99' }` | BR-08,BR-22 | 존재하지 않는 단원의 경계·현재 동작 확인 |
| 경계 · 소문자 단원 DB 비교 | `{ unit: 'm5-1' }` | BR-07,BR-08 | 소문자 단원 DB 비교의 경계·현재 동작 확인 |
| 경계 · 키워드 100자 | `{ q: '가'.repeat(100) }` | BR-05 | 키워드 100자의 경계·현재 동작 확인 |
| 경계 · 키워드 101자 | `{ q: '가'.repeat(101) }` | BR-05 | 키워드 101자의 경계·현재 동작 확인 |
| 경계 · 키워드 한 글자 | `{ q: '분' }` | BR-05 | 키워드 한 글자의 경계·현재 동작 확인 |
| 이상 · 키워드 LIKE 와일드카드 | `{ q: '%_' }` | BR-06 | 키워드 LIKE 와일드카드의 경계·현재 동작 확인 |
| 이상 · 태그 와일드카드는 정확 비교 | `{ tag: '%' }` | BR-13 | 태그 와일드카드는 정확 비교의 경계·현재 동작 확인 |
| 경계 · 태그 51자 | `{ tag: '가'.repeat(51) }` | BR-13 | 태그 51자의 경계·현재 동작 확인 |
| 경계 · 페이지 0 | `{ page: '0' }` | BR-20 | 페이지 0의 경계·현재 동작 확인 |
| 경계 · 총 페이지 밖 빈 표 | `{ page: '2' }` | BR-21 | 총 페이지 밖 빈 표의 경계·현재 동작 확인 |
| 경계 · 페이지 999 | `{ page: '999' }` | BR-20,BR-21 | 페이지 999의 경계·현재 동작 확인 |
| 경계 · 페이지 1000 상한 | `{ page: '1000' }` | BR-20 | 페이지 1000 상한의 경계·현재 동작 확인 |
| 이상 · 페이지 공백은 제거하지 않음 | `{ page: ' 2 ' }` | BR-20 | 페이지 공백은 제거하지 않음의 경계·현재 동작 확인 |
| 정렬 · id 내림차순 | `{ sort: 'id', dir: 'desc' }` | BR-17 | id 내림차순의 경계·현재 동작 확인 |
| 정렬 · 제목 오름차순 | `{ sort: 'title', dir: 'asc' }` | BR-17 | 제목 오름차순의 경계·현재 동작 확인 |
| 정렬 · 제목 내림차순 | `{ sort: 'title', dir: 'desc' }` | BR-17 | 제목 내림차순의 경계·현재 동작 확인 |
| 정렬 · 단원 오름차순 보조 키 | `{ sort: 'unit', dir: 'asc' }` | BR-17 | 단원 오름차순 보조 키의 경계·현재 동작 확인 |
| 정렬 · 단원 내림차순 보조 키 | `{ sort: 'unit', dir: 'desc' }` | BR-17 | 단원 내림차순 보조 키의 경계·현재 동작 확인 |
| 정렬 · 난이도 오름차순 | `{ sort: 'level', dir: 'asc' }` | BR-17 | 난이도 오름차순의 경계·현재 동작 확인 |
| 정렬 · 등록 시각 오름차순 | `{ sort: 'created', dir: 'asc' }` | BR-17 | 등록 시각 오름차순의 경계·현재 동작 확인 |
| 정렬 · 등록 시각 기본 내림차순 | `{ sort: 'created' }` | BR-17 | 등록 시각 기본 내림차순의 경계·현재 동작 확인 |
| 이상 · 미등록 정렬 기준 | `{ sort: 'stem', dir: 'asc' }` | BR-16 | 미등록 정렬 기준의 경계·현재 동작 확인 |
| 이상 · 잘못된 정렬 방향 | `{ sort: 'id', dir: 'sideways' }` | BR-18 | 잘못된 정렬 방향의 경계·현재 동작 확인 |
| 정상 · 정렬 대소문자와 공백 | `{ sort: ' UNIT ', dir: ' DESC ' }` | BR-18 | 정렬 대소문자와 공백의 경계·현재 동작 확인 |
| 배열 · PHP 대괄호 입력은 첫 값 | `{ 'q[]': ['분수', '소수'] }` | BR-03 | PHP 대괄호 입력은 첫 값의 경계·현재 동작 확인 |
| 중복 · PHP 단일 키 입력은 마지막 값 | `{ q: ['분수', '소수'] }` | BR-03 | PHP 단일 키 입력은 마지막 값의 경계·현재 동작 확인 |

## 2. 파일과 호출 계층

- `ItemSearchController`: `/api/items/search`에서 문자열 파라미터를 받고 서비스만 호출한다.
- 요청 record DTO: PHP의 누락·배열·중복 키 입력을 구분한다.
- `ItemSearchService`: 문자열·난이도·페이지를 레거시처럼 변환하고 응답을 구성한다.
- `ItemSearchRepository`: 기존 `v_item_public`에서 바인딩한 조건으로 전체 건수와 현재 페이지를 읽는다.
- 응답 record DTO: `{status, rows, count, message}`; 행은 `id,title,unit,level,tags`만 가진다.
- 검색 단위 테스트: 변환·정렬·빈 페이지 등 기존 동작을 검증한다.

기존 상세 조회 및 단원 조회는 변경하지 않는다. JDBC는 이미 JPA 스타터에 포함되어 있어 새 의존성을 추가하지 않는다.
NamedParameterJdbcTemplate의 바인딩/RowMapper 사용은 Context7으로 Spring 공식 문서를 확인했다.

## 3. 규칙 대응표

| 규칙 | 레거시 위치 | 새 구현 위치(예정) | 보존 범위 |
|---|---|---|---|
| BR-01 | legacy/item-bank-php/search.php:522 · legacy/item-bank-php/register.php:27 · legacy/item-bank-php/register.php:34 | ItemSearchRepository | 현재 동작 유지 |
| BR-02 | legacy/item-bank-php/search.php:44 · legacy/item-bank-php/search.php:98 · legacy/item-bank-php/search.php:118 | ItemSearchRepository | 현재 동작 유지 |
| BR-03 | legacy/item-bank-php/search.php:55 · legacy/item-bank-php/search.php:59 · legacy/item-bank-php/search.php:63 | ItemSearchService / ItemSearchController | 현재 동작 유지 |
| BR-04 | legacy/item-bank-php/search.php:85 · legacy/item-bank-php/search.php:97-98 · legacy/item-bank-php/search.php:97-99 | ItemSearchRepository | 현재 동작 유지 |
| BR-05 | legacy/item-bank-php/search.php:86-88 · legacy/item-bank-php/search.php:494 | ItemSearchService / ItemSearchController | 현재 동작 유지 |
| BR-06 | legacy/item-bank-php/search.php:94-95 · legacy/item-bank-php/search.php:97 · legacy/item-bank-php/search.php:94-96 | ItemSearchRepository | 현재 동작 유지 |
| BR-07 | legacy/item-bank-php/search.php:109 · legacy/item-bank-php/search.php:118 · legacy/item-bank-php/search.php:120 | ItemSearchRepository | 현재 동작 유지 |
| BR-08 | legacy/item-bank-php/search.php:111 · legacy/item-bank-php/search.php:115 · legacy/item-bank-php/search.php:141 | ItemSearchService / ItemSearchController | 검색 결과 호환; 화면 경고·요약은 정규화 대상 제외 |
| BR-09 | legacy/item-bank-php/search.php:214-215 · legacy/item-bank-php/search.php:213 · legacy/item-bank-php/search.php:355-356 | ItemSearchRepository | 현재 동작 유지 |
| BR-10 | legacy/item-bank-php/search.php:217-218 · legacy/item-bank-php/search.php:220 · legacy/item-bank-php/search.php:217-219 | ItemSearchRepository | 현재 동작 유지 |
| BR-11 | legacy/item-bank-php/search.php:225-226 · legacy/item-bank-php/search.php:228 · legacy/item-bank-php/search.php:225-227 | ItemSearchRepository | 현재 동작 유지 |
| BR-12 | legacy/item-bank-php/search.php:244-245 | ItemSearchRepository | 현재 동작 유지 |
| BR-13 | legacy/item-bank-php/search.php:240-242 | ItemSearchService / ItemSearchController | 현재 동작 유지 |
| BR-14 | legacy/item-bank-php/search.php:244-245 · legacy/item-bank-php/search.php:258-260 · legacy/item-bank-php/search.php:237-238 | ItemSearchService / ItemSearchController | 검색 결과 호환; 화면 경고·요약은 정규화 대상 제외 |
| BR-15 | legacy/item-bank-php/search.php:317-319 · legacy/item-bank-php/search.php:266-267 | ItemSearchRepository | 현재 동작 유지 |
| BR-16 | legacy/item-bank-php/search.php:322-325 · legacy/item-bank-php/search.php:324 | ItemSearchRepository | 현재 동작 유지 |
| BR-17 | legacy/item-bank-php/search.php:278 · legacy/item-bank-php/search.php:297 · legacy/item-bank-php/search.php:302 | ItemSearchRepository | 현재 동작 유지 |
| BR-18 | legacy/item-bank-php/search.php:268-270 · legacy/item-bank-php/search.php:328-330 | ItemSearchService / ItemSearchController | 검색 결과 호환; 화면 경고·요약은 정규화 대상 제외 |
| BR-19 | legacy/item-bank-php/search.php:349 · legacy/item-bank-php/search.php:523 · legacy/item-bank-php/search.php:521-523 | ItemSearchRepository | 현재 동작 유지 |
| BR-20 | legacy/item-bank-php/search.php:337 · legacy/item-bank-php/search.php:342 · legacy/item-bank-php/search.php:345 | ItemSearchService / ItemSearchController | 현재 동작 유지 |
| BR-21 | legacy/item-bank-php/search.php:657 · legacy/item-bank-php/search.php:685-686 · legacy/item-bank-php/search.php:684-686 | ItemSearchRepository | 현재 동작 유지 |
| BR-22 | legacy/item-bank-php/search.php:657-659 · legacy/item-bank-php/search.php:652-655 | ItemSearchService / ItemSearchController | 현재 동작 유지 |
| BR-23 | legacy/item-bank-php/search.php:689-700 · legacy/item-bank-php/search.php:686 · legacy/item-bank-php/search.php:430 | ItemSearchService / ItemSearchController | 범위 제외: HTML 검색 폼·링크 |
| BR-24 | legacy/item-bank-php/search.php:678 · legacy/item-bank-php/search.php:674-678 | ItemSearchRepository | 현재 동작 유지 |
| BR-25 | legacy/item-bank-php/search.php:726 · legacy/item-bank-php/search.php:728 · legacy/item-bank-php/search.php:726-728 | ItemSearchService / ItemSearchController | 검색 결과 호환; 화면 경고·요약은 정규화 대상 제외 |

BR-26~39는 등록 기능, BR-40~41은 단원 목록, BR-42는 HTML 선택 목록이므로 이번 이관 대상에서 제외한다.
검색 폼 경고·조건 요약·페이지/정렬 링크는 공용 정규화 함수가 수집하지 않는다. 결과 데이터와 결과 없음 메시지를 보존한다.
DB 장애의 HTML 안내 화면은 이번 정상 로컬 DB의 검색 호환 범위에 포함하지 않는다.

## 4. 의심 동작도 보존

- 난이도 미지정은 `level < 5`, 난이도 5 명시는 조회된다.
- 잘못된 난이도도 PHP 숫자 변환 뒤 검색한다(`05`, `3abc`, `3.7`, `1e0` 포함).
- 키워드 `%`, `_`는 LIKE 와일드카드지만 태그는 정확 비교한다.
- 소문자/없는 단원은 입력을 바꾸거나 404로 거부하지 않고 DB에 그대로 바인딩한다.
- 페이지 원문에는 trim을 하지 않으며 1~999만 적용한다. 실제 총 페이지 수로 보정하지 않는다.
- 총 건수가 양수이고 현재 페이지 행만 없으면 `message: null`이다.
- 정렬 기준에 따라 기본 방향이 다르고, 단원 정렬의 보조 키는 항상 level DESC, id ASC다.
- PHP 배열 키(`q[]`)는 첫 값, 같은 단일 키 반복(`q=x&q=y`)은 마지막 값이다.

## 5. 데이터와 응답

PHP와 modern 프로필은 동일한 MariaDB 컨테이너의 `itembank` DB·뷰·시드를 읽는다. 스키마와 시드를 변경하지 않는다.

```json
{"status":200,"rows":[],"count":0,"message":"검색 결과가 없습니다"}
```

결과가 있으면 rows에 기존 표의 데이터를 순서대로 넣고 message는 null이다.
태그는 뷰의 tag.id 순서 CSV를 기존 정규화처럼 나누고 공백을 제거한다.

## 6. 진행·검증 순서

1. PHP에서 40개 입력의 베이스라인을 찍고 전체 테스트를 두 번 연속 통과시킨다.
2. 테스트·스냅샷을 커밋한다. 완료 기준과 Day 1-1/2 문서·Skill을 별도 커밋한다.
3. 계획의 범위에서 구현하고 Java 테스트를 실행한다.
4. 새 API를 띄워 대상 주소만 바꿔 같은 스냅샷을 비교한다. 차이는 규칙/형식/환경으로 분류한다.
5. 새 코드만 수정한다. 원본·공용 정규화·스냅샷 SHA-256이 유지되는지 확인한다.
6. 규칙별 독립 대조와 실제 발견한 차이의 회고를 남긴다.
7. 개인 포크의 day1 및 초대를 받은 팀 저장소의 개인 브랜치에 반영하고 서비스를 종료한다.

사용자의 “day 1-3 진행” 요청으로 이 계획 범위의 구현·검증·실습 브랜치 반영이 승인되었다.
팀 저장소 초대가 없으면 개인 결과물을 준비하고 접근 불가 상태를 명시한다.
