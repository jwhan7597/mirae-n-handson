# 문항 은행(레거시 PHP) 업무 규칙 후보

대상: `legacy/item-bank-php` · 분석일: 2026-10-07 · 근거 표기는 저장소 루트 기준 `파일:줄번호`이다.

- 읽은 파일: `docs/item-bank/ARCHITECTURE.md`, `docs/item-bank/ERD.md`, `legacy/item-bank-php`의 `index.php` · `search.php` · `register.php` · `units.php` · `inc/db.php` · `inc/layout.php` 전체, `db/mariadb/init/01-schema.sql` 전체.
- 읽지 않은 파일: `legacy/item-bank-php/vendor/simplelog/Log.php`(외부 라이브러리 본문), `db/mariadb/init`의 다른 파일, 그 밖의 모듈.
- 초안은 정적 분석으로 작성했다. 후속 검증에서 HTTP 요청 12개와 로컬 DB 컬럼·상태 분포를 확인했다. 성공 등록 요청과 원본 코드 변경은 하지 않았다.
- 범위: 검색 조건(단원 · 난이도 · 태그 · 키워드) 조합, 등록 검증과 저장, 목록 기본 정렬과 제외 조건.
- 확신도 기준: **확실** = 코드 · DDL 줄에서 조건 · 숫자 · 부등호를 직접 확인한 것. **추정** = DB 동작(collation, 잠금 등)이나 실행 결과에 달려 있어 코드만으로 단정할 수 없는 것.
- 규칙은 실제 코드 기준으로 썼다. 주석과 코드가 다른 곳은 비고에 차이를 적었다.
- 인용 코드는 실제 소스의 연속된 1~3줄이다. 별도 위치를 인용할 때는 코드 블록을 나누고 근거 경로를 각각 적었다.

## 1. 검색 — 대상과 조건 조합

### BR-01

- 규칙: 검색하면 상태가 `'A'`(공개)인 문항만 대상으로 한다.
- 근거: `legacy/item-bank-php/search.php:522`, `db/mariadb/init/01-schema.sql:70`

```php
    $from   = " FROM v_item_public";
```

```sql
WHERE i.status = 'A';
```

- 확신도: 확실
- 비고: 검색 코드에는 `status` 조건이 없고 뷰가 걸러 낸다. 뷰는 `status` 컬럼을 내보내지 않는다(`db/mariadb/init/01-schema.sql:53-67`). 스키마 주석은 "등록 화면도 이 뷰를 기준으로 삼는다"(`db/mariadb/init/01-schema.sql:51`)고 하지만 `register.php`의 SQL은 뷰를 쓰지 않는다(`legacy/item-bank-php/register.php:27`, `legacy/item-bank-php/register.php:34`, `legacy/item-bank-php/register.php:87`, `legacy/item-bank-php/register.php:93`, `legacy/item-bank-php/register.php:105`).

### BR-02

- 규칙: 검색 조건이 여러 개이면 모두 AND로 결합하고, 키워드 · 단원 · 태그는 값이 비어 있으면 조건에서 뺀다.
- 근거: `legacy/item-bank-php/search.php:44`, `legacy/item-bank-php/search.php:98`, `legacy/item-bank-php/search.php:118` · 인용 위치: `legacy/item-bank-php/search.php:97-99`

```php
        $like = '%' . $q . '%';
        $where .= " AND (title LIKE ? OR stem LIKE ?)";
        $types .= 'ss';
```

- 확신도: 확실
- 비고: OR는 키워드의 제목/지문 사이에만 있고 괄호로 묶여 있다. 난이도는 비어 있어도 조건이 붙는다(BR-09). 빈값 판정은 `trim` 뒤 `!== ''`이다(`legacy/item-bank-php/search.php:90`, `legacy/item-bank-php/search.php:110`, `legacy/item-bank-php/search.php:236`).

### BR-03

- 규칙: 검색 파라미터가 배열로 들어오면 첫 값만 사용한다.
- 근거: `legacy/item-bank-php/search.php:55`

```php
        $q = is_array($params['q']) ? (string)reset($params['q']) : (string)$params['q'];
```

- 확신도: 확실
- 비고: `unit` · `level` · `tag` · `sort` · `dir` · `page`도 같은 형태이다(`legacy/item-bank-php/search.php:59`, `legacy/item-bank-php/search.php:63`, `legacy/item-bank-php/search.php:67`, `legacy/item-bank-php/search.php:71`, `legacy/item-bank-php/search.php:75`, `legacy/item-bank-php/search.php:79`). 요청 메서드는 검사하지 않고 `$_GET`만 읽는다(`legacy/item-bank-php/search.php:722`).

## 2. 검색 — 키워드

### BR-04

- 규칙: 키워드가 앞뒤 공백을 뺀 뒤 비어 있지 않으면 제목 또는 지문에 키워드가 포함된 문항만 찾는다.
- 근거: `legacy/item-bank-php/search.php:85`, `legacy/item-bank-php/search.php:97-98` · 인용 위치: `legacy/item-bank-php/search.php:97-99`

```php
        $like = '%' . $q . '%';
        $where .= " AND (title LIKE ? OR stem LIKE ?)";
        $types .= 'ss';
```

- 확신도: 확실
- 비고: 키워드가 정확히 1자이면 경고 문구만 추가하고 그대로 검색한다(`legacy/item-bank-php/search.php:91-92`). 키워드 안쪽 공백으로 단어를 나누지 않는다. `LIKE`의 대소문자 구분 여부는 컬럼 collation(`utf8mb4_unicode_ci`, `db/mariadb/init/01-schema.sql:33`)에 달려 있으며 실행해 확인하지 않았다(미확인 #2).

### BR-05

- 규칙: 키워드가 100자를 넘으면 앞 100자만 사용하고 경고를 표시한다.
- 근거: `legacy/item-bank-php/search.php:86-88`

```php
    if (mb_strlen($q, 'UTF-8') > 100) {
        $q = mb_substr($q, 0, 100, 'UTF-8');
        $warnings[] = '키워드가 너무 길어 100자까지만 사용했습니다.';
```

- 확신도: 확실
- 비고: 부등호는 `> 100`이라 정확히 100자는 자르지 않는다. 바이트가 아니라 글자 수 기준이다. 입력 칸에도 `maxlength="100"`이 있다(`legacy/item-bank-php/search.php:494`).

### BR-06

- 규칙: 키워드에 `%` 또는 `_`가 있으면 이스케이프하지 않고 경고만 표시한 채 LIKE 패턴에 그대로 넣는다.
- 근거: `legacy/item-bank-php/search.php:94-95`, `legacy/item-bank-php/search.php:97` · 인용 위치: `legacy/item-bank-php/search.php:94-96`

```php
        if (strpos($q, '%') !== false || strpos($q, '_') !== false) {
            $warnings[] = '키워드의 % 와 _ 는 와일드카드로 처리됩니다.';
        }
```

- 확신도: 확실
- 비고: 값은 바인딩으로 전달되므로(`legacy/item-bank-php/search.php:99-101`) SQL 구문에는 영향이 없고 LIKE 와일드카드로만 동작한다. `\` 문자의 처리는 확인하지 않았다(미확인 #2).

## 3. 검색 — 단원

### BR-07

- 규칙: 단원 값이 앞뒤 공백을 뺀 뒤 비어 있지 않으면 단원 코드가 그 값과 일치하는 문항만 찾는다.
- 근거: `legacy/item-bank-php/search.php:109`, `legacy/item-bank-php/search.php:118`, `legacy/item-bank-php/search.php:120` · 인용 위치: `legacy/item-bank-php/search.php:118-120`

```php
        $where .= " AND unit_code = ?";
        $types .= 's';
        $values[] = $unit;
```

- 확신도: 확실
- 비고: 부분 일치가 아니라 `=` 비교이다. `unit_code`는 뷰가 `unit.code`에 붙인 별칭이다(`db/mariadb/init/01-schema.sql:56`). 입력값을 대문자로 바꾸거나 16자(`db/mariadb/init/01-schema.sql:13`)로 자르는 코드는 없다.

### BR-08

- 규칙: 단원 값이 형식(`영문 1자 + 숫자 1~2자 + - + 숫자 1~2자`)에 맞지 않거나, 소문자가 섞였거나, `unit` 테이블에서 찾지 못하면 경고만 표시하고 입력값 그대로 조회한다.
- 근거: `legacy/item-bank-php/search.php:111`, `legacy/item-bank-php/search.php:115`, `legacy/item-bank-php/search.php:141` · 인용 위치: `legacy/item-bank-php/search.php:111-113`

```php
        if (!preg_match('/^[A-Za-z][0-9]{1,2}-[0-9]{1,2}$/', $unit)) {
            // 형식이 달라도 그대로 조회한다 (결과는 대개 0건)
            $warnings[] = '단원 코드 형식이 올바르지 않습니다. (예: M5-1)';
```

- 확신도: 확실 (경고만 내고 조회를 막지 않는다는 점) / 추정 (아래 비고의 DB 비교 결과)
- 비고:
  - 형식 정규식은 소문자도 허용한다(`[A-Za-z]`). 소문자 경고는 별도 PHP 문자열 비교(`strtoupper`)이다.
  - PHP 경고는 대소문자를 구분하지만 DB 쪽 `unit_code = ?`와 `code = ?`(`legacy/item-bank-php/search.php:125`)는 컬럼 collation을 따른다. `unit.code`는 `utf8mb4_unicode_ci`로 선언되어 있어(`db/mariadb/init/01-schema.sql:18`) 소문자로 입력해도 일치하는 행이 나올 수 있다고 **추정**한다. 즉 "대문자로 입력하세요" 경고가 떠도 결과가 0건이라고 단정할 수 없다. 후속 로컬 HTTP 검증에서 `M5-1`과 `m5-1` 모두 같은 5건을 반환함을 확인했다. 나머지 문자열 비교 동작은 미확인 #2로 남긴다.
  - 주석 "결과는 대개 0건"(`legacy/item-bank-php/search.php:112`)은 주석일 뿐이라 규칙으로 삼지 않았다.
  - 단원 선택 목록의 선택 표시는 PHP `===` 비교라 대소문자가 다르면 선택되지 않는다(`legacy/item-bank-php/search.php:162`).

## 4. 검색 — 난이도

### BR-09

- 규칙: 난이도 값이 앞뒤 공백을 뺀 뒤 비어 있으면 난이도가 5 미만인 문항만 찾는다(난이도 5 문항은 빠진다).
- 근거: `legacy/item-bank-php/search.php:214-215`

```php
    if ($level == '') {
        $where .= " AND level < 5";
```

- 확신도: 확실 (코드 동작) — 의도인지는 미확인 #1
- 비고:
  - **주석과 코드가 다르다.** 바로 위 주석은 "전체 난이도 검색 (1~5 모두 포함)"(`legacy/item-bank-php/search.php:213`)인데 실제 조건은 `< 5`이다. 코드를 기준으로 썼다.
  - 하한이 없으므로 `level`이 0 이하인 행이 있으면 포함된다. DDL에 범위 제약이 없다(`db/mariadb/init/01-schema.sql:25`).
  - 이 조건은 화면의 조건 요약에 나타나지 않는다. 다른 조건이 없으면 "조건 없이 검색했습니다."가 출력된다(`legacy/item-bank-php/search.php:355-356`).
  - 정상 입력은 `level=5`로 난이도 5를 조회한다(BR-10). 비정상 표기 `level=05`도 정수 변환 결과가 5이므로 경고와 함께 조회될 수 있다(BR-11); 로컬 HTTP 검증에서는 6건이 나왔다.

### BR-10

- 규칙: 난이도 값이 `1`~`5` 한 글자이면 난이도가 그 값과 같은 문항만 찾는다.
- 근거: `legacy/item-bank-php/search.php:217-218`, `legacy/item-bank-php/search.php:220` · 인용 위치: `legacy/item-bank-php/search.php:217-219`

```php
    else if (preg_match('/^[1-5]$/', $level)) {
        $where .= " AND level = ?";
        $types .= 'i';
```

- 확신도: 확실
- 비고: 범위(이상 · 이하) 검색은 없고 `=` 비교뿐이다.

### BR-11

- 규칙: 난이도 값이 비어 있지도 않고 `1`~`5` 한 글자도 아니면 경고를 표시하고 값을 정수로 변환해 난이도가 그 정수와 같은 문항을 찾는다.
- 근거: `legacy/item-bank-php/search.php:225-226`, `legacy/item-bank-php/search.php:228` · 인용 위치: `legacy/item-bank-php/search.php:225-227`

```php
        $warnings[] = '난이도는 1~5 사이여야 합니다.';
        $where .= " AND level = ?";
        $types .= 'i';
```

- 확신도: 확실 (분기와 변환 코드) / 추정 (아래 예시의 실제 결과)
- 비고: 검색을 거부하지 않는다. PHP `(int)` 변환 규칙으로 보면 `abc` → 0, `0` → 0, `05` → 5, `3.7` → 3이 되어, `05`는 후속 로컬 HTTP 검증에서 경고와 함께 난이도 5 문항 6건을 반환했다. 나머지 예시의 응답은 직접 요청하지 않았다. 주석 "대개 0건"(`legacy/item-bank-php/search.php:224`)은 규칙으로 삼지 않았다.

## 5. 검색 — 태그

### BR-12

- 규칙: 태그 값이 앞뒤 공백을 뺀 뒤 비어 있지 않으면 이름이 그 값과 일치하는 태그가 붙은 문항만 찾는다.
- 근거: `legacy/item-bank-php/search.php:244-245`

```php
        $where .= " AND EXISTS (SELECT 1 FROM item_tag it JOIN tag t ON t.id = it.tag_id"
                . " WHERE it.item_id = v_item_public.id AND t.name = ?)";
```

- 확신도: 확실
- 비고: 태그는 한 번에 하나만 지정할 수 있다(배열이면 첫 값, BR-03). 검색은 태그 이름으로, 등록은 태그 번호로 다룬다(BR-33). 태그가 하나도 없는 문항은 태그 조건이 있으면 제외된다.

### BR-13

- 규칙: 태그 값이 50자를 넘으면 앞 50자만 사용하고 경고를 표시한다.
- 근거: `legacy/item-bank-php/search.php:240-242`

```php
        if (mb_strlen($tag, 'UTF-8') > 50) {
            $tag = mb_substr($tag, 0, 50, 'UTF-8');
            $warnings[] = '태그 이름이 너무 길어 50자까지만 사용했습니다.';
```

- 확신도: 확실
- 비고: `> 50`이라 정확히 50자는 자르지 않는다. `tag.name VARCHAR(50)`(`db/mariadb/init/01-schema.sql:37`)과 숫자가 같다.

### BR-14

- 규칙: 태그 값에 `%` · `_`가 있거나 태그 목록에 없는 이름이면 경고만 표시하고 그대로 `=` 비교로 조회한다.
- 근거: `legacy/item-bank-php/search.php:244-245`, `legacy/item-bank-php/search.php:258-260`, `legacy/item-bank-php/search.php:237-238`, `legacy/item-bank-php/search.php:253` · 인용 위치: `legacy/item-bank-php/search.php:237-239`

```php
        if (strpos($tag, '%') !== false || strpos($tag, '_') !== false) {
            $warnings[] = '태그는 부분 일치를 지원하지 않습니다.';
        }
```

- 확신도: 확실 (경고만 내고 조회한다는 점) / 추정 (DB 비교 결과)
- 비고:
  - `%` · `_` 검사는 50자 자르기보다 먼저 실행된다(`legacy/item-bank-php/search.php:237`, `legacy/item-bank-php/search.php:240`).
  - "등록되지 않은 태그" 판정은 PHP `===`(대소문자 · 공백까지 구분)이고, 실제 조회는 DB의 `t.name = ?`이다. `tag.name`은 `utf8mb4_unicode_ci`(`db/mariadb/init/01-schema.sql:40`)라 대소문자만 다른 값은 경고가 뜨면서도 결과가 나올 수 있다고 **추정**한다. 실행해 확인하지 않았다(미확인 #2).
  - 주석 "없어도 조회는 그대로 한다 → 0건"(`legacy/item-bank-php/search.php:250`)의 "0건"은 규칙으로 삼지 않았다.

## 6. 검색 — 정렬

### BR-15

- 규칙: 정렬 기준(`sort`)이 비어 있으면 난이도 내림차순, 같은 난이도 안에서는 문항 번호 오름차순으로 정렬한다.
- 근거: `legacy/item-bank-php/search.php:317-319`

```php
        case '':
            // 기본 정렬: 어려운 문항부터, 같은 난이도면 번호 순
            $orderBy = " ORDER BY level DESC, id ASC";
```

- 확신도: 확실
- 비고: 이때 `dir` 값은 정렬에 쓰이지 않는다. `sort`와 `dir`은 `trim` 후 소문자로 바꿔 비교한다(`legacy/item-bank-php/search.php:266-267`).

### BR-16

- 규칙: 정렬 기준이 `id` · `title` · `unit` · `level` · `created` 중 하나가 아니면 경고를 표시하고 기본 정렬(BR-15)을 사용한다.
- 근거: `legacy/item-bank-php/search.php:322-325`

```php
            $warnings[] = '알 수 없는 정렬 기준입니다. 기본 정렬을 사용합니다.';
            $sort = '';
            $orderBy = " ORDER BY level DESC, id ASC";
```

- 확신도: 확실
- 비고: `sort`도 빈값으로 되돌린다(`legacy/item-bank-php/search.php:324`).

### BR-17

- 규칙: 정렬 기준이 `id` · `title` · `unit`이면 `dir`이 `desc`일 때만 내림차순으로, `level` · `created`이면 `dir`이 `asc`일 때만 오름차순으로 정렬한다; `id` 기준은 문항 번호 자체를 지정 방향으로 정렬하며, 다른 기준의 마지막 정렬 키는 문항 번호 오름차순이다.
- 근거: `legacy/item-bank-php/search.php:278`, `legacy/item-bank-php/search.php:297`, `legacy/item-bank-php/search.php:302` · 인용 위치: `legacy/item-bank-php/search.php:294-296`

```php
            if ($dir === 'desc') {
                $orderBy = " ORDER BY unit_code DESC, level DESC, id ASC";
            } else {
```

- 확신도: 확실
- 비고: 실제 ORDER BY는 아래와 같다.

  | `sort` | `dir` 미지정 · 기본 | 반대 방향 | 근거 |
  |---|---|---|---|
  | `id` | `id ASC` | `id DESC` | `legacy/item-bank-php/search.php:278-282` |
  | `title` | `title ASC, id ASC` | `title DESC, id ASC` | `legacy/item-bank-php/search.php:286-290` |
  | `unit` | `unit_code ASC, level DESC, id ASC` | `unit_code DESC, level DESC, id ASC` | `legacy/item-bank-php/search.php:294-298` |
  | `level` | `level DESC, id ASC` | `level ASC, id ASC` | `legacy/item-bank-php/search.php:302-306` |
  | `created` | `created_at DESC, id ASC` | `created_at ASC, id ASC` | `legacy/item-bank-php/search.php:310-314` |

  - `unit` 정렬의 둘째 키 `level DESC`는 방향과 무관하게 고정이다.
  - `created`는 열 머리글 링크가 없어(`legacy/item-bank-php/search.php:392-397`) URL로만 지정할 수 있다.
  - 문자열 정렬 순서(`title`, `unit_code`)는 collation을 따른다(미확인 #2).

### BR-18

- 규칙: 정렬 방향(`dir`)이 `asc` · `desc`가 아니면 방향이 지정되지 않은 것으로 처리하고, 값이 비어 있지 않았을 때만 경고를 표시한다.
- 근거: `legacy/item-bank-php/search.php:268-270`

```php
    if ($dir !== 'asc' && $dir !== 'desc') {
        if ($dir !== '') {
            $warnings[] = '정렬 방향은 asc 또는 desc 만 가능합니다.';
```

- 확신도: 확실
- 비고: 방향 미지정이면 BR-17 표의 기본 방향이 적용된다. 화면 표시용 방향은 `level` · `created`가 `desc`, 나머지가 `asc`이다(`legacy/item-bank-php/search.php:328-330`).

## 7. 검색 — 페이지 · 결과 표시

### BR-19

- 규칙: 검색 결과는 한 페이지에 20건씩, `(페이지 − 1) × 20`건을 건너뛰어 표시한다.
- 근거: `legacy/item-bank-php/search.php:349`, `legacy/item-bank-php/search.php:523` · 인용 위치: `legacy/item-bank-php/search.php:521-523`

```php
    $select = "SELECT id, title, unit_code, level, tag_names, created_at";
    $from   = " FROM v_item_public";
    $limit  = " LIMIT 20 OFFSET " . (int)$offset;
```

- 확신도: 확실
- 비고: 20이 세 곳에 따로 적혀 있다(`legacy/item-bank-php/search.php:349`, `legacy/item-bank-php/search.php:523`, `legacy/item-bank-php/search.php:553`). 페이지 크기를 바꾸는 파라미터는 없다. 총 건수는 같은 조건의 `COUNT(*)`로 따로 구한다(`legacy/item-bank-php/search.php:526`).

### BR-20

- 규칙: 페이지 값이 숫자만으로 이루어지지 않았으면 1페이지로, 1 미만이면 1페이지로, 999를 넘으면 999페이지로 보정한다.
- 근거: `legacy/item-bank-php/search.php:337`, `legacy/item-bank-php/search.php:342`, `legacy/item-bank-php/search.php:345` · 인용 위치: `legacy/item-bank-php/search.php:336-338`

```php
    $page = 1;
    if (preg_match('/^[0-9]+$/', $pageRaw)) {
        $page = (int)$pageRaw;
```

- 확신도: 확실
- 비고:
  - 경고는 숫자가 아닌 값(빈 문자열 제외)과 999 초과일 때만 나온다(`legacy/item-bank-php/search.php:339-340`, `legacy/item-bank-php/search.php:347`). `page=0`은 경고 없이 1페이지가 된다.
  - `page` 파라미터가 없으면 1이다(`legacy/item-bank-php/search.php:77`).
  - 다른 파라미터와 달리 `page`는 `trim`하지 않는다. 앞뒤에 공백이 있으면 숫자가 아닌 값으로 처리된다. `-1`도 숫자가 아닌 값으로 처리된다.

### BR-21

- 규칙: 페이지 값이 실제 총 페이지 수보다 크면 보정하지 않고 그대로 조회한다.
- 근거: `legacy/item-bank-php/search.php:657`, `legacy/item-bank-php/search.php:685-686` · 인용 위치: `legacy/item-bank-php/search.php:684-686`

```php
    $pageSize = $built['page_size'];
    $pages = (int)ceil($total / $pageSize);
    if ($pages > 1) {
```

- 확신도: 확실 (코드 대조 및 로컬 HTTP 확인)
- 비고: 페이지 상한 검사는 999 고정값뿐이고(BR-20) 총 건수와 비교하는 코드가 없다. "결과 없음" 문구는 총 건수가 0일 때만 나오므로, 총 건수가 1 이상인데 페이지가 범위를 넘으면 건수 문구와 행이 없는 표가 출력한다. 후속 로컬 HTTP 검증에서 기본 검색의 `page=2`는 총 20건을 표시하되 행이 없고, 결과 없음 문구도 출력하지 않는 것을 확인했다.

### BR-22

- 규칙: 총 건수가 0이면 "검색 결과가 없습니다" 문구만 표시하고 결과 표와 페이지 링크는 출력하지 않는다.
- 근거: `legacy/item-bank-php/search.php:657-659`

```php
    if ($total === 0) {
        echo '<p id="message">검색 결과가 없습니다</p>' . "\n";
        return;
```

- 확신도: 확실
- 비고: 건수 문구 `검색 결과 0건`, 경고, 조건 요약은 그 앞에서 이미 출력된다(`legacy/item-bank-php/search.php:652-655`).

### BR-23

- 규칙: 총 페이지 수가 2 이상이면 페이지 번호 링크를 표시하고, 앞·뒤 페이지가 있을 때 이전·다음 링크를 추가하며 검색 조건과 정렬을 유지한다.
- 근거: `legacy/item-bank-php/search.php:689-700`, `legacy/item-bank-php/search.php:686`, `legacy/item-bank-php/search.php:430` · 인용 위치: `legacy/item-bank-php/search.php:685-687`

```php
    $pages = (int)ceil($total / $pageSize);
    if ($pages > 1) {
        $cur = $built['page'];
```

- 확신도: 확실
- 비고: 총 페이지 수는 `ceil(총 건수 / 20)`이다(`legacy/item-bank-php/search.php:685`). 번호 링크는 1부터 총 페이지까지 전부 출력하며 묶음(예: 10개 단위) 처리는 없다(`legacy/item-bank-php/search.php:692`). 열 머리글 정렬 링크에는 `page`가 없어 정렬을 바꾸면 1페이지로 돌아간다(`legacy/item-bank-php/search.php:414-415`).

### BR-24

- 규칙: 결과 표의 태그 칸에는 문항에 붙은 태그 이름을 태그 번호 순으로 쉼표로 이어 표시하고, 태그가 없으면 빈 칸으로 표시한다.
- 근거: `db/mariadb/init/01-schema.sql:64`, `legacy/item-bank-php/search.php:678`

```sql
  (SELECT GROUP_CONCAT(t.name ORDER BY t.id SEPARATOR ',')
```

```php
        echo '<td>' . h($r['tag_names'] === null ? '' : $r['tag_names']) . '</td>';
```

- 확신도: 확실 (정의와 출력 코드) / 추정 (태그 없는 문항에서 `tag_names`가 실제로 `NULL`인지)
- 비고: 표의 열은 번호 · 제목 · 단원 코드 · 난이도 · 태그 순이다(`legacy/item-bank-php/search.php:674-678`). 지문과 등록일은 표에 나오지 않는다. 문항 상세 · 수정으로 가는 링크도 없다.

### BR-25

- 규칙: 검색 조건 조립이나 조회 중 `RuntimeException`이 발생하면 결과 대신 "검색 중 오류가 발생했습니다." 문구를 표시한다.
- 근거: `legacy/item-bank-php/search.php:726`, `legacy/item-bank-php/search.php:728` · 인용 위치: `legacy/item-bank-php/search.php:726-728`

```php
} catch (RuntimeException $e) {
    Log::error('search failed', array('msg' => $e->getMessage()));
    echo '<p class="error">검색 중 오류가 발생했습니다.</p>' . "\n";
```

- 확신도: 확실
- 비고: 상세 사유는 화면에 내지 않고 로그 호출로만 넘긴다(`legacy/item-bank-php/search.php:727`). DB 연결 실패는 별도 분기에서 연결 오류 문구를 그대로 화면에 낸다(`legacy/item-bank-php/search.php:714-716`).

## 8. 등록 — 검증

### BR-26

- 규칙: 요청 메서드가 POST이면 입력을 검증해 저장하고, 그 밖의 메서드이면 빈 등록 폼만 표시한다.
- 근거: `legacy/item-bank-php/register.php:40`

```php
if ($_SERVER['REQUEST_METHOD'] === 'POST') {
```

- 확신도: 확실
- 비고: 로그인 · 권한 · CSRF 토큰 검사는 이 파일에 없다(`legacy/item-bank-php/register.php:1-177`).

### BR-27

- 규칙: 제목 · 지문 · 단원 · 난이도 입력값은 앞뒤 공백을 제거한 뒤 검증하고 저장한다.
- 근거: `legacy/item-bank-php/register.php:41-42`, `legacy/item-bank-php/register.php:44` · 인용 위치: `legacy/item-bank-php/register.php:41-43`

```php
    $title  = isset($_POST['title'])  ? trim((string)$_POST['title'])  : '';
    $stem   = isset($_POST['stem'])   ? trim((string)$_POST['stem'])   : '';
    $unitId = isset($_POST['unit_id']) ? trim((string)$_POST['unit_id']) : '';
```

- 확신도: 확실
- 비고: 단원도 같은 형태이다(`legacy/item-bank-php/register.php:43`). 값이 없으면 빈 문자열로 본다. 값이 배열로 들어온 경우의 동작은 미확인 #8.

### BR-28

- 규칙: 제목이 5자 미만이면 등록을 거부한다.
- 근거: `legacy/item-bank-php/register.php:49-50`

```php
    if (mb_strlen($title, 'UTF-8') < 5) {
        $errors[] = '제목은 5자 이상 입력해야 합니다.';
```

- 확신도: 확실
- 비고: `< 5`이므로 정확히 5자는 통과한다. 글자 수 기준이며 제목 안쪽 공백도 글자로 센다(앞뒤 공백만 제거, BR-27). 빈 제목도 이 조건에 걸린다.

### BR-29

- 규칙: 제목이 200자를 넘으면 등록을 거부한다.
- 근거: `legacy/item-bank-php/register.php:52-53`

```php
    if (mb_strlen($title, 'UTF-8') > 200) {
        $errors[] = '제목은 200자를 넘을 수 없습니다.';
```

- 확신도: 확실
- 비고: `> 200`이므로 정확히 200자는 통과한다. `title VARCHAR(200)`(`db/mariadb/init/01-schema.sql:23`)과 숫자가 같다. 검색 키워드와 달리 자르지 않고 거부한다.

### BR-30

- 규칙: 지문이 앞뒤 공백을 뺀 뒤 비어 있으면 등록을 거부한다.
- 근거: `legacy/item-bank-php/register.php:56-57`

```php
    if ($stem === '') {
        $errors[] = '지문을 입력해야 합니다.';
```

- 확신도: 확실
- 비고: 지문의 최소 · 최대 길이 검사는 없다. `stem`은 `TEXT`(`db/mariadb/init/01-schema.sql:24`)이며 한도를 넘는 값의 처리는 미확인 #7.

### BR-31

- 규칙: 단원 값이 `unit` 테이블에 있는 단원 번호와 일치하지 않으면 등록을 거부한다.
- 근거: `legacy/item-bank-php/register.php:62`, `legacy/item-bank-php/register.php:66-67` · 인용 위치: `legacy/item-bank-php/register.php:66-68`

```php
    if (!$unitOk) {
        $errors[] = '단원을 선택해야 합니다.';
    }
```

- 확신도: 확실
- 비고: 등록은 단원 번호(`unit.id`)로, 검색은 단원 코드(`unit.code`)로 받는다. 문자열 `===` 비교라 `01`처럼 표기가 다른 값은 일치하지 않는다. 미선택(빈값)과 없는 번호가 같은 문구로 처리된다.

### BR-32

- 규칙: 난이도 값이 `1`~`5` 한 글자가 아니면 등록을 거부한다.
- 근거: `legacy/item-bank-php/register.php:70-71`

```php
    if (!preg_match('/^[1-5]$/', $level)) {
        $errors[] = '난이도는 1~5 사이여야 합니다.';
```

- 확신도: 확실
- 비고: 검색은 같은 문구의 경고만 내고 조회하지만(BR-11) 등록은 거부한다. DB에는 범위 제약이 없어(`db/mariadb/init/01-schema.sql:25`) 이 검사가 유일한 차단 지점이다. 난이도 5로 등록한 문항은 공개된 뒤에도 난이도 미지정 검색에는 나오지 않는다(BR-09).

### BR-33

- 규칙: 선택한 태그 중 `tag` 테이블에 없는 번호는 오류 없이 버리고, 태그를 하나도 선택하지 않아도 등록을 허용한다.
- 근거: `legacy/item-bank-php/register.php:77-78`, `legacy/item-bank-php/register.php:104` · 인용 위치: `legacy/item-bank-php/register.php:77-79`

```php
            if ((string)$t['id'] === (string)$tid) {
                $validTagIds[] = (int)$tid;
            }
```

- 확신도: 확실
- 비고: `tags`가 배열이 아니면 태그 없음으로 본다(`legacy/item-bank-php/register.php:45`). 태그 개수 상한이 없다. 같은 번호가 중복으로 와도 걸러 내지 않는다(미확인 #5). 새 태그를 만드는 기능은 없다(`tag` INSERT 없음).

### BR-34

- 규칙: 검증 오류가 하나라도 있으면 저장하지 않고, 발견된 오류를 모두 표시하며 입력값을 폼에 그대로 남긴다.
- 근거: `legacy/item-bank-php/register.php:83`, `legacy/item-bank-php/register.php:133`, `legacy/item-bank-php/register.php:144` · 인용 위치: `legacy/item-bank-php/register.php:83-85`

```php
    if (empty($errors)) {
        // 등록 직후에는 검수중(R) 상태로 들어간다 → 검수 완료 전까지 검색 화면에 나오지 않는다.
        $conn->begin_transaction();
```

- 확신도: 확실
- 비고: 검증은 첫 오류에서 멈추지 않고 제목 · 지문 · 단원 · 난이도를 모두 검사한다(`legacy/item-bank-php/register.php:49-72`). 제목이 같은 문항이 이미 있는지 확인하는 중복 검사는 없다.

## 9. 등록 — 저장

### BR-35

- 규칙: 새 문항 번호는 `item` 테이블의 가장 큰 번호에 1을 더한 값으로 하고, 문항이 하나도 없으면 1로 한다.
- 근거: `legacy/item-bank-php/register.php:87`

```php
            $res = $conn->query("SELECT COALESCE(MAX(id), 0) + 1 AS next_id FROM item FOR UPDATE");
```

- 확신도: 확실 (계산식) / 추정 (동시 등록 시 번호 중복 방지)
- 비고: `item.id`에 `AUTO_INCREMENT`가 없다(`db/mariadb/init/01-schema.sql:21`). 최댓값은 뷰가 아닌 `item` 전체(모든 상태)에서 구한다. `FOR UPDATE`가 동시 등록을 실제로 직렬화하는지는 확인하지 않았다(미확인 #4).

### BR-36

- 규칙: 등록한 문항은 상태 `'R'`(검수중)으로 저장하고 등록 시각과 수정 시각에 같은 현재 시각을 넣는다.
- 근거: `legacy/item-bank-php/register.php:93-94`

```php
                "INSERT INTO item (id, unit_id, title, stem, level, status, created_at, updated_at)
                 VALUES (?, ?, ?, ?, ?, 'R', NOW(), NOW())"
```

- 확신도: 확실
- 비고: 스키마의 `status` 기본값은 `'A'`이지만(`db/mariadb/init/01-schema.sql:26`) INSERT가 `'R'`을 명시하므로 기본값은 적용되지 않는다. 시각은 PHP가 아니라 DB의 `NOW()`이다. `'R'`의 뜻 "검수중"은 스키마 주석(`db/mariadb/init/01-schema.sql:26`)과 코드 주석(`legacy/item-bank-php/register.php:84`)에서 온 것이다.

### BR-37

- 규칙: 문항과 문항-태그 연결은 한 트랜잭션으로 저장하고, 저장 중 `Exception`을 잡으면 전부 되돌린 뒤 "저장 중 오류가 발생했습니다."를 표시한다.
- 근거: `legacy/item-bank-php/register.php:85`, `legacy/item-bank-php/register.php:114`, `legacy/item-bank-php/register.php:120` · 인용 위치: `legacy/item-bank-php/register.php:119-121`

```php
        } catch (Exception $e) {
            $conn->rollback();
            Log::error('item register failed', array('msg' => $e->getMessage()));
```

- 확신도: 확실 (트랜잭션 범위) / 추정 (모든 실패 경로에서 rollback이 실행되는지)
- 비고:
  - 순서는 번호 계산 → `item` INSERT → 태그마다 `item_tag` INSERT → commit이다(`legacy/item-bank-php/register.php:87-114`).
  - `execute()`가 실패하면 `RuntimeException`을 던져 rollback한다(`legacy/item-bank-php/register.php:99-100`, `legacy/item-bank-php/register.php:108-109`).
  - `catch`는 `Exception`만 받는다(`legacy/item-bank-php/register.php:119`). `query()` · `prepare()`가 `false`를 돌려준 뒤 메서드를 호출해 생기는 오류는 이 `catch`에 잡히지 않을 수 있다고 **추정**한다(미확인 #6).
  - 실패 시 입력값은 폼에 남는다(값 초기화는 성공 경로에만 있다, `legacy/item-bank-php/register.php:117-118`).

### BR-38

- 규칙: 저장에 성공하면 문항 번호와 "검수 완료 후 검색에 노출됩니다" 안내를 표시하고 폼 입력값을 비운다.
- 근거: `legacy/item-bank-php/register.php:116-117`

```php
            $notice = '문항 #' . $newId . ' 을(를) 등록했습니다. 검수 완료 후 검색에 노출됩니다.';
            $title = $stem = $unitId = $level = '';
```

- 확신도: 확실
- 비고: 저장 후 다른 화면으로 이동(redirect)하지 않고 같은 응답에서 폼을 다시 그린다(`legacy/item-bank-php/register.php:127-177`). 새로 고침 시 재전송되는지는 실행해 확인하지 않았다.

### BR-39

- 규칙: 방금 등록한 문항은 상태가 `'R'`이므로 검색 결과와 단원별 공개 문항 수에 포함되지 않는다.
- 근거: `legacy/item-bank-php/units.php:17`, `legacy/item-bank-php/register.php:94`, `db/mariadb/init/01-schema.sql:70`

```php
                 VALUES (?, ?, ?, ?, ?, 'R', NOW(), NOW())"
```

```sql
WHERE i.status = 'A';
```

- 확신도: 확실
- 비고: `'R'`을 `'A'`로 바꾸는 SQL(검수 완료 처리)은 이 모듈에 없다(10장, 미확인 #3). 안내 문구의 "검수 완료"를 누가 어떻게 하는지는 코드에서 확인되지 않는다.

## 10. 목록 — 기본 정렬과 제외 조건

검색 목록의 기본 정렬은 BR-15, 제외 조건은 BR-01 · BR-09에 적었다. 아래는 단원 목록과 선택 목록이다.

### BR-40

- 규칙: 단원 목록은 모든 단원을 학년 오름차순, 같은 학년 안에서는 단원 코드 오름차순으로 표시하며 공개 문항이 0건인 단원도 포함한다.
- 근거: `legacy/item-bank-php/units.php:18-19`

```php
          FROM unit u
         ORDER BY u.grade ASC, u.code ASC";
```

- 확신도: 확실
- 비고: 문항 수는 JOIN이 아니라 SELECT 절 서브쿼리라 단원 행이 빠지지 않는다(`legacy/item-bank-php/units.php:17`). 페이지 나눔 · 검색 조건 · 정렬 변경 기능은 없다. 단원 코드의 정렬 순서는 collation을 따른다(미확인 #2).

### BR-41

- 규칙: 단원별 문항 수는 상태가 `'A'`인 문항만 센다.
- 근거: `legacy/item-bank-php/units.php:17`

```php
               (SELECT COUNT(*) FROM item i WHERE i.unit_id = u.id AND i.status = 'A') AS item_count
```

- 확신도: 확실
- 비고: 뷰를 쓰지 않고 `item`에 조건을 직접 건다. 난이도 조건이 없으므로 난이도 5 문항도 센다. 따라서 단원 목록의 문항 수와, 같은 단원을 난이도 미지정으로 검색한 건수(BR-09)가 다를 수 있다.

### BR-42

- 규칙: 검색 · 등록 화면의 단원 선택 목록은 학년 오름차순 · 단원 코드 오름차순으로, 태그 선택 목록은 태그 번호 오름차순으로 표시한다.
- 근거: `legacy/item-bank-php/search.php:156`, `legacy/item-bank-php/search.php:178` · 인용 위치: `legacy/item-bank-php/search.php:155-157`

```php
    if ($conn !== null) {
        $res = $conn->query("SELECT code, name, grade FROM unit ORDER BY grade ASC, code ASC");
        if ($res !== false) {
```

- 확신도: 확실
- 비고: 등록 화면도 같은 정렬이다(`legacy/item-bank-php/register.php:27`, `legacy/item-bank-php/register.php:34`). 선택 목록에는 문항이 없는 단원 · 쓰이지 않는 태그도 모두 나온다(조건 없음). 검색 화면은 단원을 학년별로 묶어 보여 준다(`legacy/item-bank-php/search.php:454-455`).

## 11. 수정 화면

**문항 수정 화면은 없다.** 삭제 · 상세 조회 · 상태 변경(검수 완료) 화면도 없다.

- 진입점은 `index.php` · `search.php` · `register.php` · `units.php` 네 개뿐이다(`docs/item-bank/ARCHITECTURE.md` 3장, 파일 목록 확인).
- 메뉴에는 검색 · 등록 · 단원 목록만 있다(`legacy/item-bank-php/inc/layout.php:10-12`).

```php
        'search'   => array('search.php',   '문항 검색'),
        'register' => array('register.php', '문항 등록'),
        'units'    => array('units.php',    '단원 목록'),
```

- 읽은 PHP 파일 6개에 `UPDATE` · `DELETE` 문이 없다. 쓰기 SQL은 `item` INSERT(`legacy/item-bank-php/register.php:93-94`)와 `item_tag` INSERT(`legacy/item-bank-php/register.php:105`)뿐이다.
- 검색 결과 표의 행에는 링크가 없다(`legacy/item-bank-php/search.php:674-678`).
- 따라서 `updated_at`은 등록 시각으로 들어간 뒤 이 모듈에서 바뀌지 않는다(BR-36). 수정 관련 업무 규칙은 이 코드에서 추출할 수 없다.

## 12. 매직 넘버 표

| 값 | 위치 | 추정 의미 |
|---|---|---|
| `100` | `legacy/item-bank-php/search.php:86-87`, `legacy/item-bank-php/search.php:494` | 검색 키워드 최대 글자 수(초과분 잘라 냄), 입력 칸 `maxlength` |
| `1` | `legacy/item-bank-php/search.php:91` | 키워드가 한 글자일 때 경고를 내는 기준 |
| `{1,2}` | `legacy/item-bank-php/search.php:111` | 단원 코드 형식의 숫자 자릿수(학년 · 단원 번호 각 1~2자리로 추정) |
| `5` | `legacy/item-bank-php/search.php:215` | 난이도 미지정 검색의 상한(`level < 5`). 주석은 "1~5 모두 포함"이라 의도 미확인 |
| `1`~`5` | `legacy/item-bank-php/search.php:217`, `legacy/item-bank-php/search.php:194-198`, `legacy/item-bank-php/register.php:70`, `legacy/item-bank-php/register.php:163` | 유효한 난이도 범위. 스키마 주석(`db/mariadb/init/01-schema.sql:25`)에도 있으나 DB 제약은 없음 |
| `50` | `legacy/item-bank-php/search.php:240-241` | 태그 이름 최대 글자 수. `tag.name VARCHAR(50)`(`db/mariadb/init/01-schema.sql:37`)과 같음 |
| `20` | `legacy/item-bank-php/search.php:349`, `legacy/item-bank-php/search.php:523`, `legacy/item-bank-php/search.php:553` | 한 페이지 건수. 세 곳에 따로 적혀 있음 |
| `1` | `legacy/item-bank-php/search.php:336`, `legacy/item-bank-php/search.php:342-343` | 페이지 번호 하한 · 기본값 |
| `999` | `legacy/item-bank-php/search.php:345-346` | 페이지 번호 상한. 페이지당 20건 기준 마지막 페이지의 끝 위치는 19,980번째다. 정한 근거는 미확인 |
| `5` | `legacy/item-bank-php/register.php:49` | 제목 최소 글자 수 |
| `200` | `legacy/item-bank-php/register.php:52` | 제목 최대 글자 수. `title VARCHAR(200)`(`db/mariadb/init/01-schema.sql:23`)과 같음 |
| `0`, `+ 1` | `legacy/item-bank-php/register.php:87` | 문항이 없을 때의 기준 번호와 증가 폭(첫 문항 번호 1) |
| `'R'` | `legacy/item-bank-php/register.php:94` | 등록 직후 상태(검수중) |
| `'A'` | `db/mariadb/init/01-schema.sql:70`, `legacy/item-bank-php/units.php:17`, 기본값 `db/mariadb/init/01-schema.sql:26` | 공개 상태. 검색 · 단원별 문항 수의 포함 기준 |
| `'D'` | `db/mariadb/init/01-schema.sql:26` (주석만) | 삭제 상태로 추정. PHP 코드의 SQL에는 나오지 않음 |
| `16` | `db/mariadb/init/01-schema.sql:13` | 단원 코드 최대 길이. 검색 입력에는 대응하는 길이 검사가 없음 |

## 13. 미확인 목록

| # | 항목 | 확인한 범위 | 확인하지 못한 이유 |
|---|---|---|---|
| 1 | 난이도 미지정 검색의 `level < 5`가 의도인지 | 주석과 코드의 불일치(`legacy/item-bank-php/search.php:213-215`) | 요구사항 문서 · 담당자 확인이 없다 |
| 2 | DB 문자열 비교의 실제 동작: 단원 코드 · 태그 이름 `=` 비교와 키워드 `LIKE`의 대소문자 구분, 후행 공백 처리, `\` 처리, 문자열 정렬 순서 | 테이블 collation 선언 `utf8mb4_unicode_ci`(`db/mariadb/init/01-schema.sql:18`, `db/mariadb/init/01-schema.sql:33`, `db/mariadb/init/01-schema.sql:40`), 연결 문자셋 `utf8mb4`(`legacy/item-bank-php/inc/db.php:27`) | DB를 실행하지 않았고 연결 collation · 서버 설정을 확인하지 않았다. PHP 경고(`strtoupper`, `===`)와 DB 결과가 같다고 단정할 수 없다 |
| 3 | `status`를 `'R'` → `'A'`(검수 완료) 또는 `'D'`(삭제)로 바꾸는 주체와 규칙 | 이 모듈에 `UPDATE` · `DELETE`가 없음(11장) | 다른 모듈 · 수작업 SQL · 초기화 파일은 읽지 않았다 |
| 4 | 동시에 등록할 때 `MAX(id) + 1 … FOR UPDATE`가 번호 중복을 막는지 | 트랜잭션 안에서 실행된다는 것(`legacy/item-bank-php/register.php:85-87`) | 격리 수준 · 잠금 동작을 실행해 확인하지 않았다 |
| 5 | 같은 태그 번호가 중복으로 전송될 때의 결과 | 중복 제거 코드 없음(`legacy/item-bank-php/register.php:75-81`), 복합 PK(`db/mariadb/init/01-schema.sql:45`) | 두 번째 INSERT 실패로 전체 rollback될 것으로 추정하나 실행하지 않았다 |
| 6 | `query()` · `prepare()` 실패 시 등록 트랜잭션의 처리 | 반환값 `false` 검사 없이 메서드를 호출하는 줄(`legacy/item-bank-php/register.php:87-88`, `legacy/item-bank-php/register.php:92`, `legacy/item-bank-php/register.php:98`, `legacy/item-bank-php/register.php:105`, `legacy/item-bank-php/register.php:107`), `catch (Exception …)`(`legacy/item-bank-php/register.php:119`), `mysqli_report(MYSQLI_REPORT_OFF)`(`legacy/item-bank-php/inc/db.php:20`) | PHP 7.4에서 어떤 종류의 오류가 발생하고 rollback이 실행되는지 실행해 확인하지 않았다 |
| 7 | `TEXT` 한도를 넘는 지문, DB가 거부하는 값의 처리 | 지문 길이 검사 없음(`legacy/item-bank-php/register.php:56`) | DB의 `sql_mode`를 확인하지 않았다 |
| 8 | 등록 폼의 제목 · 지문 · 단원 · 난이도가 배열로 전송될 때의 동작 | `(string)` 변환 줄(`legacy/item-bank-php/register.php:41-44`) | PHP의 배열 → 문자열 변환 결과와 경고 출력을 실행해 확인하지 않았다 |
| 9 | 총 페이지 수를 넘는 요청 | `page=2` HTTP 요청으로 총 20건·빈 표·결과 없음 문구 미출력을 확인함(BR-21) | 현재 로컬 기본 검색은 확인 완료; 다른 데이터 분포는 미실행 |
| 10 | 시드 데이터에 난이도 5 · 범위 밖 난이도 · `'A'`/`'R'`/`'D'` 외 상태가 있는지 | DDL에 제약 없음(`db/mariadb/init/01-schema.sql:25-26`) | `db/mariadb/init`의 다른 파일을 읽지 않았고 DB를 조회하지 않았다 |
| 11 | 태그가 없는 문항의 `tag_names` 실제 값(`NULL` 여부)과 최대 길이 | 뷰 정의(`db/mariadb/init/01-schema.sql:64-67`), `null` 처리 코드(`legacy/item-bank-php/search.php:678`) | DB를 실행하지 않았다 |
| 12 | 제목 5자 · 키워드 100자 · 페이지 999 등 기준값을 정한 근거 | 코드의 숫자만 확인(12장) | 요구사항 문서를 확인하지 않았다 |
| 13 | 등록 권한 · 로그인 규칙 | `register.php`에 인증 코드 없음(`legacy/item-bank-php/register.php:1-177`) | 웹 서버 · 상위 계층 설정은 저장소 범위 밖이다 |
| 14 | 로그 호출(`Log::debug` · `info` · `error`)이 실제로 무엇을 기록하는지 | 호출 줄만 확인(`legacy/item-bank-php/search.php:103`, `legacy/item-bank-php/search.php:528`, `legacy/item-bank-php/search.php:537`, `legacy/item-bank-php/search.php:727`, `legacy/item-bank-php/register.php:115`, `legacy/item-bank-php/register.php:121`) | 외부 라이브러리 본문은 읽지 않았다. 업무 규칙으로 나누지 않았다 |

## 14. 검증 이력

| 일자 | 단계 | 내용 | 결과 |
|---|---|---|---|
| 2026-10-07 | 생성 | 소스 · 스키마 정적 분석으로 BR-01~BR-42 초안 작성. 조건 · 숫자 · 부등호는 작성 시 해당 줄과 대조함 | **검증 미실시** — 별도 검증 단계(독립 재대조, 컨테이너 실행, `characterization` 테스트 대조)는 아직 수행하지 않았다 |

### 표본 점검 결과

| 표본 | 선정 이유 | 직접 연 근거 | 판정 |
|---|---|---|---|
| BR-36 | 가장 자연스러운 등록 상태·시각 규칙 | `legacy/item-bank-php/register.php:92-116` | 통과: INSERT가 R과 NOW()를 명시한다 |
| BR-09 | 주석과 실행 조건이 다름 | `legacy/item-bank-php/search.php:209-230` | 통과: 주석은 전체지만 실제 SQL은 level < 5다 |
| BR-42 | 마지막 규칙 | `legacy/item-bank-php/search.php:155-178`, `legacy/item-bank-php/register.php:27-34` | 통과: 단원은 학년·코드 순, 태그는 id 순이다 |

### 독립 분석과의 비교·판정

첫 분석은 Opus 5.5, 두 번째는 Sonnet 5.5 새 세션이다. 두 번째 작업 폴더에는 첫 문서가 없었고 docs/를 읽지 않았다. 아래는 수정 전 두 결과를 비교한 것이다.

| 항목 | BUSINESS-RULES 초안 | CROSS-CHECK 초안 | 확인한 코드·결과와 판정 |
|---|---|---|---|
| 잘못된 난이도 표기 | BR-11은 05가 정수 5로 조회될 수 있다고 설명 | CX-11은 문자열이 0이 된다고 일반화 | `legacy/item-bank-php/search.php:223-229`; level=05 요청이 경고와 함께 6건을 반환함. BR-11 해석을 유지하고 양쪽의 “5를 명시해야만 조회” 표현도 정정 |
| 정렬의 보조 키 | BR-17 본문은 모든 경우 마지막 키가 id ASC라고 설명 | CX-17 비고는 unit desc에서 id 방향도 바뀐다고 설명 | `legacy/item-bank-php/search.php:277-298`; 둘 다 일부 부정확. id 기준은 id DESC도 가능하며 unit 기준의 보조 키 level DESC·id ASC는 고정. 두 문서를 수정 |
| 미등록 태그 경고 | BR-14는 PHP 경고와 DB 비교가 달라 결과를 단정하지 않음 | CX-14는 미등록 경고이면 0건이라고 설명 | `legacy/item-bank-php/search.php:244-260`; 경고가 조회를 중단하지 않으므로 DB 비교 결과를 사용한다. 0건 단정은 제거 |
| 태그 연결 저장의 실패 | BR-37은 catch 범위의 한계를 비고로 구분 | CX-28은 중간 실패가 모두 rollback된다고 설명 | `legacy/item-bank-php/register.php:119-123`; Exception을 잡는 경로만 보장하며 Error·false 반환 후 호출은 별도 미확인. 양쪽 문구를 좁힘 |
| 첫 분석에만 명시된 규칙 | BR-23·24·25·26·34·38·40·42: 링크 유지, 태그 표시, 검색 예외, POST 처리, 오류·성공 폼 상태, 단원·선택 목록 정렬 | 별도 규칙으로 없음 | `legacy/item-bank-php/search.php:430`, `legacy/item-bank-php/search.php:674-703`, `legacy/item-bank-php/search.php:726-728`, `legacy/item-bank-php/register.php:40`, `legacy/item-bank-php/register.php:83-85`, `legacy/item-bank-php/register.php:116-144`, `legacy/item-bank-php/units.php:16-19`; 코드로 확인해 유지 |

공통으로 확인한 핵심은 공개 상태 필터, 난이도 미지정 시 5 제외, AND 조건, 키워드·태그 처리, 제목·지문·단원·난이도 검증, R 상태 저장, 채번과 트랜잭션이다. CX 번호와 BR 번호는 규칙을 나눈 단위가 달라 1:1로 대응하지 않는다.

| 일자 | 단계 | 내용 | 결과 |
|---|---|---|---|
| 2026-10-07 | 표본 점검 | BR-36·09·42의 실제 소스·조건·숫자를 직접 열어 확인 | 3개 통과 |
| 2026-10-07 | 교차 검증 | 난이도·정렬·태그·트랜잭션의 불일치와 첫 분석에만 있는 규칙을 코드로 판정 | 과도한 단정을 수정하고 확인된 규칙 유지 |
| 2026-10-07 | 문서 근거 검사 | 약칭 경로를 저장소 기준 경로로 바꾸고, 떨어진 코드·재작성된 코드를 연속된 원문 인용으로 교체 | 근거와 인용 형식 정정 |
| 2026-10-07 | 구조·ERD 재검증 | id 컬럼이 있는 테이블은 unit·item·tag 3개로 정정; item_tag는 복합 키. tag_names는 DB 메타데이터에서 mediumtext임을 확인 | 관련 ERD 문구·그림 수정 |
| 2026-10-07 | 로컬 응답 검증 | 화면·검색 HTTP 12건 및 DB 컬럼·문항 상태 분포 조회 | 전부 통과; 데이터 저장 요청 없음 |

표본 확인은 Codex가 소스를 직접 대조한 기록이며, 수강자가 직접 확인했다는 기록은 아니다. 다음 차시에는 확실로 표시된 규칙과 실제 분기를 테스트 케이스로 사용한다.


## 이관 회고

| 바뀐 지점 | 레거시 동작 | AI가 만든 동작 | 테스트가 잡았나 | 다음에 막을 방법 |
|---|---|---|---|---|
| BR-03 색인 배열·중복·스칼라 혼용 | 색인 배열의 삽입 순서에서 첫 값, 같은 색인은 마지막 값, 스칼라/배열 혼용은 원문 출현 순서대로 덮어쓴다 | 최초 ItemSearchRequest는 q[]만 알아보고 q[a]/q[7]을 무시했다. q=분수&q[]=소수&q=분수도 잘못 소수를 선택했다 | 기존 검색40개는 못 잡음. 독립 대조 후 추가 HTTP16개 중5개에서 차이를 재현함. 수정 후16개 모두 동일 | PHP 실행 결과를 기준으로 ItemSearchRequestTest 9개 배열 회귀 입력과 Controller의 실제 혼용 쿼리 테스트를 추가한다. CLAUDE.md에 파라미터 Map의 순서 보존을 가정하지 말라는 규칙을 넣는다. |

수정은 modern/api에만 적용했다. 기존 테스트·스냅샷·레거시는 변경하지 않았다.
최종 규칙 대조와 검증 범위·한계는 `docs/item-bank/MIGRATION-REVIEW.md`에 기록했다.
