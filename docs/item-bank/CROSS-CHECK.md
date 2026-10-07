# 문항 은행(item-bank-php) 업무 규칙 교차검증 문서

- 분석 범위: `legacy/item-bank-php`(`search.php`, `register.php`, `units.php`, `index.php`, `inc/db.php`)와 `db/mariadb/init/01-schema.sql`.
- 읽지 않은 것: `docs/`, 타인이 만든 문서, 외부 라이브러리 폴더(`vendor/simplelog` 포함), `02-seed.sql`, `layout.php`. `layout.php`는 status·UPDATE·DELETE 키워드 검색에만 쓰였고 내용은 열람하지 않았습니다.
- 판단 기준: 주석과 코드가 다르면 코드를 따릅니다.
- 한계: DB를 실행해 확인하지 않았습니다. collation 관련 규칙은 스키마 선언과 MariaDB 일반 동작에서 추론한 것이라 확신도에 반영했습니다.
- 줄번호는 파일 원문 기준입니다.

## A. 노출 대상과 기본 제외 조건

### CX-01
- **규칙**: 검색 대상은 `status='A'`(공개) 문항뿐이며, 검수중(R)·삭제(D) 문항은 어떤 검색 조건에서도 나오지 않는다.
- **근거**: `legacy/item-bank-php/search.php:522`, `db/mariadb/init/01-schema.sql:52-70` · 인용 위치: `db/mariadb/init/01-schema.sql:68-70`
- **코드 인용**:
```sql
FROM item i
JOIN unit u ON u.id = i.unit_id
WHERE i.status = 'A';
```
- **확신도**: 높음
- **비고**: 목록 조회와 건수 조회(`$countSql`, 526행) 모두 이 뷰를 씁니다. 스키마 주석(50~51행)은 "삭제 플래그가 아니라 status 코드로 판정"이라고 설명합니다. 같은 주석은 "등록 화면도 이 뷰를 기준으로 삼는다"고 하지만 `register.php`는 뷰를 쓰지 않습니다. 이 부분은 주석이 틀렸고 코드가 기준입니다.

### CX-02
- **규칙**: 난이도를 비워 검색하면 난이도 5 문항은 제외된다(`level < 5`). 정상 입력 `level=5`로 조회하며, 비정상 표기 05도 정수 변환으로 5가 되어 조회될 수 있다(후속 검증으로 정정).
- **근거**: `legacy/item-bank-php/search.php:213-216` · 인용 위치: `legacy/item-bank-php/search.php:214-216`
- **코드 인용**:
```php
    if ($level == '') {
        $where .= " AND level < 5";
    }
```
- **확신도**: 높음
- **비고**: 주석은 "1~5 모두 포함"이라고 하지만 코드는 5를 제외합니다. 이 주석은 신뢰하지 않았고 코드를 기준으로 했습니다. 이 제외는 건수(`count`)에도 같이 적용됩니다. `units.php`의 "공개 문항 수"는 이 제외를 하지 않으므로(17행) 단원 목록의 수와 검색 결과 건수가 어긋날 수 있습니다. 의도인지 결함인지는 코드만으로 판단할 수 없습니다.

## B. 검색 조건 조합

### CX-03
- **규칙**: 키워드·단원·난이도·태그 조건은 모두 AND로 결합하고, 비어 있는 조건은 적용하지 않는다(단, 난이도는 CX-02의 예외가 있다).
- **근거**: `legacy/item-bank-php/search.php:44`, `legacy/item-bank-php/search.php:98`, `legacy/item-bank-php/search.php:118`, `legacy/item-bank-php/search.php:244-245` · 인용 위치: `legacy/item-bank-php/search.php:97-99`
- **코드 인용**:
```php
        $like = '%' . $q . '%';
        $where .= " AND (title LIKE ? OR stem LIKE ?)";
        $types .= 'ss';
```
- **확신도**: 높음
- **비고**: 각 파라미터가 배열로 들어오면 첫 값만 씁니다(53~80행). 태그·단원은 한 번에 하나만 지정할 수 있습니다.

### CX-04
- **규칙**: 키워드는 앞뒤 공백을 제거하고 100자(문자 수 기준)로 잘라, 제목 또는 지문에 부분 일치(`LIKE %q%`)하는 문항을 찾는다.
- **근거**: `legacy/item-bank-php/search.php:85-98` · 인용 위치: `legacy/item-bank-php/search.php:85-87`
- **코드 인용**:
```php
    $q = trim($q);
    if (mb_strlen($q, 'UTF-8') > 100) {
        $q = mb_substr($q, 0, 100, 'UTF-8');
```
- **확신도**: 높음
- **비고**: 100자 초과 시 경고가 나옵니다. 한 글자 키워드도 경고만 나오고 검색은 수행합니다(91~93행). 결과 표에는 제목만 보이지만 지문에만 있는 단어도 매칭됩니다. 빈 키워드는 조건을 추가하지 않습니다.

### CX-05
- **규칙**: 키워드의 `%`와 `_`는 이스케이프하지 않아 LIKE 와일드카드로 동작한다. 경고만 표시한다.
- **근거**: `legacy/item-bank-php/search.php:94-97` · 인용 위치: `legacy/item-bank-php/search.php:94-96`
- **코드 인용**:
```php
        if (strpos($q, '%') !== false || strpos($q, '_') !== false) {
            $warnings[] = '키워드의 % 와 _ 는 와일드카드로 처리됩니다.';
        }
```
- **확신도**: 높음
- **비고**: 입력의 `%`·`_`를 별도로 이스케이프하지 않습니다. 역슬래시를 포함한 LIKE 패턴의 세부 처리는 별도 미확인으로 둡니다. 예를 들어 `_` 하나만 입력하면 사실상 모든 문항이 맞습니다.

### CX-06
- **규칙**: 키워드의 LIKE 비교는 title·stem에 선언된 utf8mb4_unicode_ci collation을 따르며, 대소문자·악센트의 세부 실행 결과는 별도 미확인이다.
- **근거**: `db/mariadb/init/01-schema.sql:5`, `db/mariadb/init/01-schema.sql:20-33`, `legacy/item-bank-php/inc/db.php:27` · 인용 위치: `db/mariadb/init/01-schema.sql:5`
- **코드 인용**:
```sql
CREATE DATABASE IF NOT EXISTS itembank CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```
- **확신도**: 중간
- **비고**: `item.title`과 `item.stem`에 컬럼 단위 collation 지정은 없고 테이블 기본값 `utf8mb4_unicode_ci`를 상속합니다. 이 collation은 `_ci`(대소문자 무시)이며 악센트 처리는 이 분석에서 직접 실행해 확인하지 않았습니다. 접속 charset은 `utf8mb4`만 지정합니다(`set_charset`). 접속 collation은 지정하지 않으며, 바인딩 값은 컬럼 collation을 따른다고 해석했습니다. 실제 DB로 `LIKE 'abc'`와 `'ABC'`를 비교해 확인하지는 않았습니다.

### CX-07
- **규칙**: 단원 조건은 단원 코드(`unit_code`) 정확 일치(`=`)이며, 형식이 틀리거나 소문자여도 차단하지 않고 경고만 낸 채 그대로 조회한다.
- **근거**: `legacy/item-bank-php/search.php:109-120` · 인용 위치: `legacy/item-bank-php/search.php:115-117`
- **코드 인용**:
```php
        if ($unit !== strtoupper($unit)) {
            $warnings[] = '단원 코드는 대문자로 입력하세요. (입력값 그대로 조회합니다)';
        }
```
- **확신도**: 높음
- **비고**: 값은 `trim`만 합니다. 코드가 소문자를 변환하지 않으므로 DB 비교는 CX-08을 따릅니다.

### CX-08
- **규칙**: 소문자로 입력한 단원 코드(예: `m5-1`)는 경고가 뜨지만, DB collation 때문에 실제로는 대문자 코드(`M5-1`) 문항이 조회될 가능성이 높다.
- **근거**: `db/mariadb/init/01-schema.sql:11-18`, `legacy/item-bank-php/search.php:115-118` · 인용 위치: `db/mariadb/init/01-schema.sql:16-18`
- **코드 인용**:
```sql
  PRIMARY KEY (id),
  UNIQUE KEY uk_unit_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```
- **확신도**: 중간
- **비고**: `unit.code`는 `utf8mb4_unicode_ci`이고 뷰의 `unit_code`도 같은 컬럼입니다. `unit_code = 'm5-1'`은 `'M5-1'`과 같다고 판정될 것입니다. 단, 폼 select의 선택 표시는 PHP `===`를 쓰므로(162행) 소문자 입력일 때는 option이 선택 상태로 표시되지 않습니다. 이 때문에 경고 문구("대문자로 입력하세요")와 실제 결과가 일치하지 않을 수 있습니다. 실행해서 확인하지는 않았습니다.

### CX-09
- **규칙**: 존재하지 않는 단원 코드로 검색해도 오류 없이 조회하며, "등록되지 않은 단원 코드" 경고와 함께 보통 0건이 나온다.
- **근거**: `legacy/item-bank-php/search.php:123-146` · 인용 위치: `legacy/item-bank-php/search.php:141-143`
- **코드 인용**:
```php
        if ($unitName === '') {
            $warnings[] = '등록되지 않은 단원 코드입니다: ' . $unit;
            $summary[] = '단원 ' . $unit;
```
- **확신도**: 높음
- **비고**: 이 조회는 요약 문구에 단원명을 붙이기 위한 것이며 검색 SQL에는 영향을 주지 않습니다.

### CX-10
- **규칙**: 난이도 1~5 정수 문자열이면 `level = ?`로 정확 일치 검색한다.
- **근거**: `legacy/item-bank-php/search.php:217-221` · 인용 위치: `legacy/item-bank-php/search.php:217-219`
- **코드 인용**:
```php
    else if (preg_match('/^[1-5]$/', $level)) {
        $where .= " AND level = ?";
        $types .= 'i';
```
- **확신도**: 높음
- **비고**: `level`은 `trim`을 거치고(211행) 폼 선택지 값도 1~5입니다(193~199행).

### CX-11
- **규칙**: 난이도가 1~5 형식이 아니면(예: `0`, `6`, `abc`) 오류로 막지 않고 경고와 함께 `(int)` 변환값으로 `level = ?` 조회한다. `abc`는 0으로 변환되지만 `05` 같은 숫자 표기는 5로 변환되므로 문자열 전체를 0으로 일반화할 수 없다(후속 검증으로 정정).
- **근거**: `legacy/item-bank-php/search.php:223-229` · 인용 위치: `legacy/item-bank-php/search.php:225-227`
- **코드 인용**:
```php
        $warnings[] = '난이도는 1~5 사이여야 합니다.';
        $where .= " AND level = ?";
        $types .= 'i';
```
- **확신도**: 높음
- **비고**: 이 경로에서는 `level < 5` 조건이 붙지 않습니다.

### CX-12
- **규칙**: 태그 조건은 태그 이름과 정확히 일치하는 태그가 하나라도 붙은 문항만 대상으로 한다. 부분 일치는 지원하지 않는다.
- **근거**: `legacy/item-bank-php/search.php:236-247` · 인용 위치: `legacy/item-bank-php/search.php:244-246`
- **코드 인용**:
```php
        $where .= " AND EXISTS (SELECT 1 FROM item_tag it JOIN tag t ON t.id = it.tag_id"
                . " WHERE it.item_id = v_item_public.id AND t.name = ?)";
        $types .= 's';
```
- **확신도**: 높음
- **비고**: `%`나 `_`가 들어 있으면 경고만 나옵니다. `=` 비교이므로 와일드카드로 해석되지 않습니다. 태그는 `trim` 후 50자로 잘리며 잘리면 경고가 나옵니다(240~243행). 결과 행의 `tag_names`는 문항의 전체 태그를 `id` 순으로 쉼표로 이어 붙입니다(스키마 64~67행). 검색한 태그만 보이는 것이 아닙니다.

### CX-13
- **규칙**: 태그 이름 비교는 대소문자를 구분하지 않을 가능성이 높고, 등록 여부 경고는 PHP의 대소문자 구분 비교로 따로 판정한다.
- **근거**: `db/mariadb/init/01-schema.sql:35-40`, `legacy/item-bank-php/search.php:252-260` · 인용 위치: `legacy/item-bank-php/search.php:253-255`
- **코드 인용**:
```php
            if ($opt['value'] !== '' && $opt['value'] === $tag) {
                $tagKnown = true;
                break;
```
- **확신도**: 중간
- **비고**: `tag.name`은 `utf8mb4_unicode_ci`이므로 SQL `t.name = ?`는 대소문자를 무시할 것입니다. 반면 "등록되지 않은 태그" 경고는 PHP `===`로 판정합니다. 대소문자만 다른 입력은 결과가 나와도 경고가 뜰 수 있습니다. 같은 collation 때문에 `uk_tag_name` 유니크 키도 대소문자만 다른 중복 태그명을 막을 것입니다. 이 내용도 실행해서 확인하지는 않았습니다.

### CX-14
- **규칙**: 존재하지 않는 태그로 검색해도 오류 없이 조회하며 경고를 표시한 뒤에도 DB의 `t.name = ?` 비교 결과를 사용한다. 경고만으로 0건이라고 단정하지 않는다(후속 검증으로 정정).
- **근거**: `legacy/item-bank-php/search.php:244-245`, `legacy/item-bank-php/search.php:250-260` · 인용 위치: `legacy/item-bank-php/search.php:258-260`
- **코드 인용**:
```php
        if (!$tagKnown) {
            $warnings[] = '등록되지 않은 태그입니다: ' . $tag;
        }
```
- **확신도**: 높음
- **비고**: 없음.

## C. 목록 정렬과 페이지

### CX-15
- **규칙**: 정렬을 지정하지 않으면 난이도 내림차순, 같은 난이도면 `id` 오름차순이다.
- **근거**: `legacy/item-bank-php/search.php:317-320` · 인용 위치: `legacy/item-bank-php/search.php:317-319`
- **코드 인용**:
```php
        case '':
            // 기본 정렬: 어려운 문항부터, 같은 난이도면 번호 순
            $orderBy = " ORDER BY level DESC, id ASC";
```
- **확신도**: 높음
- **비고**: 이 정렬은 CX-02로 난이도 5가 빠진 뒤의 결과에 적용되므로, 기본 화면에서는 난이도 4가 맨 위에 옵니다. `dir`만 주고 `sort`를 비우면 `dir`은 정렬에 영향이 없습니다.

### CX-16
- **규칙**: 알 수 없는 `sort` 값은 경고 후 기본 정렬(난이도 내림차순, `id` 오름차순)로 처리한다.
- **근거**: `legacy/item-bank-php/search.php:322-326` · 인용 위치: `legacy/item-bank-php/search.php:323-325`
- **코드 인용**:
```php
            $warnings[] = '알 수 없는 정렬 기준입니다. 기본 정렬을 사용합니다.';
            $sort = '';
            $orderBy = " ORDER BY level DESC, id ASC";
```
- **확신도**: 높음
- **비고**: `sort`는 소문자로 변환하고 `trim`합니다(266행). 허용값은 `id`, `title`, `unit`, `level`, `created`입니다.

### CX-17
- **규칙**: 정렬 기준별로 방향 기본값과 2차 정렬 키가 다르다. `id`·`title`·`unit`은 `asc`, `level`·`created`는 `desc`가 기본이다.
- **근거**: `legacy/item-bank-php/search.php:277-315` · 인용 위치: `legacy/item-bank-php/search.php:294-296`
- **코드 인용**:
```php
            if ($dir === 'desc') {
                $orderBy = " ORDER BY unit_code DESC, level DESC, id ASC";
            } else {
```
- **확신도**: 높음
- **비고**: `unit`은 `desc`여도 2차 키 `level DESC`가 그대로이고, `unit_code`만 방향이 달라지고 `id ASC`는 고정입니다(후속 검증으로 정정). `id` 정렬은 2차 키가 없고, `title`·`level`·`created`는 `id ASC`가 2차 키입니다. `level`·`created`만 `dir=asc`일 때 오름차순이고 그 외 값은 내림차순입니다. `title` 정렬은 `utf8mb4_unicode_ci`(대소문자 무시) 순서입니다. `created`는 열 머리글 링크에는 없고(392~397행) URL 파라미터로만 쓸 수 있습니다.

### CX-18
- **규칙**: `dir`은 `asc`·`desc`만 허용하며, 그 밖의 비어 있지 않은 값은 경고 후 빈 값(= 기준별 기본 방향)으로 처리한다.
- **근거**: `legacy/item-bank-php/search.php:266-273` · 인용 위치: `legacy/item-bank-php/search.php:268-270`
- **코드 인용**:
```php
    if ($dir !== 'asc' && $dir !== 'desc') {
        if ($dir !== '') {
            $warnings[] = '정렬 방향은 asc 또는 desc 만 가능합니다.';
```
- **확신도**: 높음
- **비고**: 화면 표시용 방향은 328~331행에서 `level`·`created`만 `desc`, 나머지는 `asc`로 채웁니다.

### CX-19
- **규칙**: 결과는 한 페이지 20건이며 페이지 번호는 1~999로 보정한다. 숫자가 아니면 1페이지, 0 이하이면 1, 999 초과이면 999로 처리한다.
- **근거**: `legacy/item-bank-php/search.php:336-349`, `legacy/item-bank-php/search.php:523` · 인용 위치: `legacy/item-bank-php/search.php:345-347`
- **코드 인용**:
```php
    if ($page > 999) {
        $page = 999;
        $warnings[] = '페이지 번호는 999 를 넘을 수 없습니다.';
```
- **확신도**: 높음
- **비고**: 숫자가 아닌 입력(`''`와 `'1'` 제외)과 999 초과는 경고가 나옵니다. `0`은 숫자 정규식을 통과해 1로 보정되며 경고가 없습니다. 총 건수가 있는데 범위를 넘는 페이지를 요청하면 표 머리글만 있고 행이 없는 화면이 나옵니다(`total === 0`일 때만 "결과 없음" 메시지, 657행).

### CX-20
- **규칙**: 검색 결과가 0건이면 표 대신 "검색 결과가 없습니다" 메시지를 보여 준다.
- **근거**: `legacy/item-bank-php/search.php:655-660` · 인용 위치: `legacy/item-bank-php/search.php:657-659`
- **코드 인용**:
```php
    if ($total === 0) {
        echo '<p id="message">검색 결과가 없습니다</p>' . "\n";
        return;
```
- **확신도**: 높음
- **비고**: 없음.

## D. 등록 검증과 저장

### CX-21
- **규칙**: 제목은 앞뒤 공백을 제거한 뒤 문자 수 기준 5자 이상 200자 이하여야 한다.
- **근거**: `legacy/item-bank-php/register.php:41`, `legacy/item-bank-php/register.php:48-54` · 인용 위치: `legacy/item-bank-php/register.php:49-51`
- **코드 인용**:
```php
    if (mb_strlen($title, 'UTF-8') < 5) {
        $errors[] = '제목은 5자 이상 입력해야 합니다.';
    }
```
- **확신도**: 높음
- **비고**: 주석에 "공백 제외 아님, 앞뒤 공백만 제거"라고 적혀 있고 코드와 일치합니다. 제목 내부 공백은 글자 수에 포함됩니다. DB 컬럼은 `title VARCHAR(200)`이라 상한이 일치합니다. 5자 하한은 DB에 없고 애플리케이션 규칙입니다. 제목 중복 검사는 없습니다.

### CX-22
- **규칙**: 지문은 앞뒤 공백 제거 후 빈 문자열이면 등록할 수 없다.
- **근거**: `legacy/item-bank-php/register.php:42`, `legacy/item-bank-php/register.php:55-58` · 인용 위치: `legacy/item-bank-php/register.php:56-58`
- **코드 인용**:
```php
    if ($stem === '') {
        $errors[] = '지문을 입력해야 합니다.';
    }
```
- **확신도**: 높음
- **비고**: 지문 길이 상한 검사는 코드에 없습니다. 스키마는 `stem TEXT NOT NULL`입니다.

### CX-23
- **규칙**: 단원은 필수이며, 현재 `unit` 테이블에 존재하는 `id`와 일치해야 한다.
- **근거**: `legacy/item-bank-php/register.php:59-68` · 인용 위치: `legacy/item-bank-php/register.php:61-63`
- **코드 인용**:
```php
    foreach ($units as $u) {
        if ((string)$u['id'] === $unitId) {
            $unitOk = true;
```
- **확신도**: 높음
- **비고**: 검색은 단원 코드를 쓰지만 등록은 단원 `id`를 씁니다. 스키마에도 `fk_item_unit` 외래키가 있습니다.

### CX-24
- **규칙**: 난이도는 필수이며 `1`~`5` 한 자리 정수 문자열이어야 한다.
- **근거**: `legacy/item-bank-php/register.php:44`, `legacy/item-bank-php/register.php:69-72` · 인용 위치: `legacy/item-bank-php/register.php:70-72`
- **코드 인용**:
```php
    if (!preg_match('/^[1-5]$/', $level)) {
        $errors[] = '난이도는 1~5 사이여야 합니다.';
    }
```
- **확신도**: 높음
- **비고**: `level TINYINT NOT NULL`(스키마 25행)에는 CHECK 제약이 없고 "1~5"는 주석뿐입니다. 범위 보장은 PHP 검증에만 있습니다.

### CX-25
- **규칙**: 태그는 선택 사항이며, 태그 목록에 있는 `id`만 저장 대상으로 삼고 목록에 없는 값은 오류 없이 조용히 버린다.
- **근거**: `legacy/item-bank-php/register.php:73-81` · 인용 위치: `legacy/item-bank-php/register.php:77-79`
- **코드 인용**:
```php
            if ((string)$t['id'] === (string)$tid) {
                $validTagIds[] = (int)$tid;
            }
```
- **확신도**: 높음
- **비고**: 잘못된 태그 ID는 `errors`에 들어가지 않습니다. 같은 태그 ID가 중복으로 넘어오면 `$validTagIds`에도 중복되어 들어갑니다. 이 경우 `item_tag`의 기본키 `(item_id, tag_id)` 중복으로 두 번째 INSERT가 실패할 것입니다. 그러면 트랜잭션 전체가 롤백되고 "저장 중 오류가 발생했습니다"가 표시될 것입니다(CX-28). 이 중복 경로는 코드와 스키마에서 추론했으며 실행하지 않았습니다.

### CX-26
- **규칙**: 신규 문항은 항상 `status='R'`(검수중)로 저장되며, 이 때문에 등록 직후에는 검색 화면에 나오지 않는다.
- **근거**: `legacy/item-bank-php/register.php:84`, `legacy/item-bank-php/register.php:92-95` · 인용 위치: `legacy/item-bank-php/register.php:93-95`
- **코드 인용**:
```php
                "INSERT INTO item (id, unit_id, title, stem, level, status, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, 'R', NOW(), NOW())"
            );
```
- **확신도**: 높음
- **비고**: 성공 메시지도 "검수 완료 후 검색에 노출됩니다"입니다(116행). 이 디렉터리의 PHP 코드에는 R을 A로 바꾸는 UPDATE가 없습니다. 검색에 `status`/`UPDATE`/`DELETE`로 확인했으며 `layout.php`는 직접 읽지 않았습니다. 검수 승인은 다른 모듈이나 수작업으로 이뤄질 것으로 보이지만 코드로는 확인되지 않습니다.

### CX-27
- **규칙**: 문항 `id`는 자동 증가가 아니라 `MAX(id)+1`(비어 있으면 1)을 `FOR UPDATE`로 읽어 직접 부여한다.
- **근거**: `legacy/item-bank-php/register.php:85-90`, `db/mariadb/init/01-schema.sql:21` · 인용 위치: `legacy/item-bank-php/register.php:87-89`
- **코드 인용**:
```php
            $res = $conn->query("SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM item FOR UPDATE");
            $row = $res->fetch_assoc();
            $res->free();
```
- **확신도**: 높음
- **비고**: `item.id INT NOT NULL`에는 `AUTO_INCREMENT`가 없습니다. 이 채번은 삭제(D)·검수중(R) 문항도 포함한 전체 `item`의 최댓값 기준입니다. 동시 등록 시 `FOR UPDATE` 동작은 트랜잭션 격리에 의존하며 실제 경합 상황은 확인하지 않았습니다.

### CX-28
- **규칙**: 문항 INSERT와 태그 INSERT는 한 트랜잭션이며, 저장 중 `Exception`을 잡는 경로에서는 전부 롤백하고 일반 오류 메시지만 보여 준다.
- **근거**: `legacy/item-bank-php/register.php:85`, `legacy/item-bank-php/register.php:99-101`, `legacy/item-bank-php/register.php:114`, `legacy/item-bank-php/register.php:119-123` · 인용 위치: `legacy/item-bank-php/register.php:119-121`
- **코드 인용**:
```php
        } catch (Exception $e) {
            $conn->rollback();
            Log::error('item register failed', array('msg' => $e->getMessage()));
```
- **확신도**: 높음
- **비고**: 오류가 나면 입력값은 폼에 유지됩니다. 성공하면 입력값을 비웁니다(117~118행). `created_at`·`updated_at`은 DB의 `NOW()`로 채웁니다.

## E. 교차검증 시 우선 대조할 항목

- **CX-02**: 기본 검색에서 난이도 5가 빠지는 문제입니다. 주석과 코드가 반대입니다.
- **CX-08, CX-13**: collation 때문에 대소문자 입력이 결과와 경고에서 다르게 처리될 수 있습니다.
- **CX-25**: 중복 태그 입력 시 전체 롤백으로 이어지는지 확인이 필요합니다.
- **CX-01, CX-26**: 등록된 문항은 검수 승인(R→A) 전까지 검색에 보이지 않습니다. 승인 경로는 코드에서 확인하지 못했습니다.

## 후속 검증 기록

- 이 문서의 초안은 첫 문서가 없는 별도 작업 폴더에서 Sonnet 5.5가 코드만 읽어 생성했다. 초기 출력은 실습 결과 기록에 그대로 보관했다.
- 2026-10-07: 코드 인용에서 줄 결합·축약·설명용 주석을 제거하고 실제 소스의 연속된 1~3줄로 교체했다. 근거 경로도 저장소 루트 기준으로 풀었다.
- 2026-10-07: CX-02·11의 난이도 표기, CX-14의 태그 0건 단정, CX-17의 보조 정렬 키, CX-28의 예외 범위를 코드로 판정해 정정했다. 상세 비교와 판정은 BUSINESS-RULES.md의 검증 이력에 있다.
- 최초 분석의 DB 비교·실행 결과에 대한 추정은 별도로 유지한다. 런타임 결과는 첫 문서의 후속 검증에서 확인한 범위만 적용한다.
