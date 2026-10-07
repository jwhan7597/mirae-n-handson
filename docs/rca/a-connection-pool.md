# RCA — a-connection-pool

2026-10-07 · 에듀테크R&D팀(rnd) 실습 · 분석: Codex · 교육용 더미 로그.

## 요약

2026-09-16 KST 14:40:52~14:51:47에 API 5xx 186건(500 49·502 82·504 55)이 기록됐다.
DB 연결 5개가 모두 점유되고 3초 대기가 만료되는 현상은 로그로 확인했다.
전체 요청량 급증보다 보고서 요청 집중과 보고서 경로의 긴 트랜잭션이 유력한 원인 조합이다.
느린 조회와 DB 이후 서명 계산을 구분해 개선해야 한다. 직접 증상 확신은 높음, 배경 원인 기여도는 중간이다.
별도 API-2 연결 거부의 원인은 로그가 부족해 판단하지 못했다. 모든 502를 이 가설 하나로 설명하지 않는다.

## 1. 수집 범위

| 파일 | 줄 수 | 기록 기간(KST) | 시각 형식·변환 |
|---|---:|---|---|
| incident-logs/a-connection-pool/app.log | 5,371 | 06:43:56.753~15:29:51.760 | ISO +09:00, 그대로 사용 |
| incident-logs/a-connection-pool/nginx-access.log | 21,086 | 06:43:56~15:29:59 | combined +0900, 그대로 사용 |
| incident-logs/a-connection-pool/nginx-error.log | 168 | 09:12:03~14:51:47 | 시간대 표기 없음, access의 같은 요청·시각과 대조해 KST로 해석 |
| incident-logs/a-connection-pool/mariadb-slow.log | 21 | 종료 시각 09:14:09~14:37:52 | # Time은 UTC로 해석해 +9시간. SET timestamp(epoch)는 조회 시작 시각 |

핵심 구간은 14:40:52~14:51:47이다. 원인 선행 사건과 평소 수치를 보기 위해 14:25:00~15:02:00을 읽기 범위로 잡았다.
전체 로그를 모델에 넣지 않고 스트리밍 집계·선택한 줄만 읽었다. slow log의 `:20` epoch는 KST 14:37:42, `:18`의 9.812417초 후 `:15`가 14:37:52이므로 UTC 해석이 맞는다.
여기서부터 짧은 `app.log:번호` 등은 모두 `incident-logs/a-connection-pool/` 기준이다. SQL 시작·종료와 타임존 가정의 검증 근거를 구분했다.

## 2. 타임라인

아래 발췌는 마스킹 후 80자 이내다. 다른 파일의 몇 초 차이는 시계 오차 정보가 없어 인과의 확정 순서로 보지 않는다.

| KST | 출처(파일:줄번호) | 사건 요약 | 원문 발췌(마스킹) |
|---|---|---|---|
| 14:25:02 | nginx-access.log:18200 | 평소 요청의 식별자는 별칭으로 유지 | `GET /api/distributions/6?student=학생A HTTP/1.1` |
| 14:37:42 | mariadb-slow.log:20 | 제출 조회 시작(epoch 변환) | `SET timestamp=1789537062;` |
| 14:37:52 | mariadb-slow.log:18 | 조회 9.81초, 잠금 대기는 거의 없음 | `# Query_time: 9.812417  Lock_time: 0.000071` |
| 14:37:52.199 | app.log:1243 | 첫 장시간 연결 점유 경고. SQL 종료와 선후 불확실 | `Connection leak detection triggered` |
| 14:37:52.199 | app.log:1253 | 위 경고의 연결 획득 스택은 보고서 서비스 | `ReportService$$SpringCGLIB$$0.buildClassReport(<generated>)` |
| 14:38:01.900 | app.log:1258 | 같은 스레드 보고서 완료 | `built class report for class 3: 3 distributions, 10 submissions` |
| 14:38:01.902 | app.log:1259 | 점유했던 연결 반환 확인 | `was returned to the pool (unleaked)` |
| 14:40:52.319 | app.log:1522 | 첫 풀 대기 만료, 5개 전부 사용 중 | `request timed out after 3000ms (total=5, active=5, idle=0, waiting=4)` |
| 14:40:52 | nginx-access.log:18918 | 첫 HTTP500. 앱 오류와 선후 불확실 | `"GET /api/units/M6-2/items HTTP/1.1" 500` |
| 14:41:06 | nginx-error.log:6 | API-1 응답 헤더 대기 만료 | `upstream: "http://API-1:8080/api/classes/3/report"` |
| 14:42:01 | nginx-error.log:11 | 다른 대상 API-2 연결 거부 | `upstream: "http://API-2:8080/api/classes/2/report"` |
| 14:51:47 | nginx-access.log:19381 | 관측 구간의 마지막 5xx | `"GET /api/classes/3/report HTTP/1.1" 504` |
| 14:52:00 | nginx-access.log:19393 | 이후 10분 5xx 0건, 회복 관측(조치 원인 불명) | `"GET /api/distributions/1?student=학생B HTTP/1.1" 200` |

연결 점유 경고 134건, 반환(unleaked) 134건. 풀 ERROR 49건, 문자열 출현은 스택 반복 때문에 147건이다. 이를 147개 요청으로 세지 않는다.
nginx 응답 대기 만료 55건, 연결 거부 15건. 같은 종류는 위 표에 첫 발생과 아래 집계로 요약했다.

## 3. 가설과 검증

| 가설(계층) | 지지 근거 | 반증 조건 | 실제 확인 방법·결과 | 판정 |
|---|---|---|---|---|
| H1 전체 트래픽 급증이 풀 고갈을 일으켰다(트래픽) | 오류 시각 요청 존재, nginx-access.log:18918 | 장애 구간 분당 요청이 직전 수준 이하라면 전체 급증 가설은 틀림 | 시간·HTTP 상태 집계: 직전 447/10분=44.7, 장애 509/12분=42.42. 약 0.95배 | 기각. 단, 요청 종류 변화는 아래 별도 확인 |
| H2 보고서 경로의 긴 DB 점유가 풀을 채웠다(앱·트랜잭션) | app.log:1243·1253·1522 | 보고서가 DB 이후 작업 전에 연결을 반환하거나 점유 경고 스택이 다른 경로라면 약화 | ReportService.java:41·71·88: 조회와 200,000회 서명 계산이 같은 트랜잭션. 보고서 요청 6/10분→165/12분, 약 22.92배/분 | 유력 원인 조합으로 채택. CPU 기여도는 미측정 |
| H3 제출 조회 지연 또는 DB 잠금이 점유를 늘렸다(DB) | mariadb-slow.log:18·21 | 사고 직전 조회가 빠르거나 상당한 잠금 대기가 없으면 각각의 주장 약화 | 9.812417초·1,284,310행 검사·4행 반환, Lock_time 0.000071초. SubmissionRepository.java:8과 SQL 조건 일치 | 느린 조회 기여 유지, 이 쿼리의 잠금 병목은 약화. 반복 여부는 불명 |
| H4 연결을 영구적으로 반환하지 않는 누수다(자원) | app.log:1243의 leak 문구 | 같은 연결·스레드가 반환되고 사용량이 회복되면 영구 누수 주장은 약화 | 경고 134건·unleaked 134건, 첫 반환 app.log:1259. 보고서 완료 직후 반환 | 약화. 경고를 영구 누수의 증거로 단정하지 않음 |
| H5 다른 API 인스턴스 중단이 모든 오류를 만들었다(인프라) | nginx-error.log:11 | 연결 가능한 API-1에서도 풀 대기 실패·504가 있으면 단독 원인은 아님 | nginx-error.log:6은 API-1, :11은 API-2. app.log:1522에는 실제 풀 고갈 | 단독 원인으로 기각. API-2 중단 자체는 판단 불가 |

집계 구간(nginx-access.log): 직전 `:18437`~`:18883`, 장애 `:18884`~`:19392`, 이후 `:19393`~`:19805`.
재현 명령: `python3 /tmp/mirae-n-day22-logtool.py stats a-connection-pool` 및 시간 구간을 지정한 스트리밍 카운트. 명령·숫자 증빙은 `day2-2-evidence/a-window-counts.json`, `a-event-counts.json`, `a-minute-counts.jsonl`에 있다.
집계는 H1을 실제 숫자로 기각했다. 실행 계획 없이 “인덱스가 없다”고 하지 않는다. 현재 DDL `db/mariadb/init/01-schema.sql:112`에는 distribution_id 인덱스가 있다.

**판정:** 직접 원인은 사용 가능한 연결의 고갈이다(app.log:1522). 배경으로 보고서 경로의 장시간 트랜잭션·느린 조회와 보고서 요청 비중 증가를 채택한다(app.log:1253, mariadb-slow.log:18, ReportService.java:41·71).
확신: 풀 고갈 높음, 배경 원인 중간. 로그에 CPU 프로파일·실행 계획·요청 추적 ID가 없어 두 작업의 기여율과 모든 502의 원인은 확정하지 못한다.
코드의 `ReportService.java`·`SubmissionRepository.java`는 `modern/api/src/main/java/com/example/assignment/` 기준이고, `application.yml`은 `modern/api/src/main/resources/application.yml`이다.

## 4. 원인

보고서 요청 비중이 늘어난 상태에서 느린 조회와 DB 작업 뒤 연산까지 같은 트랜잭션에 포함되는 구조가 연결 점유를 길게 만든 것으로 해석한다(nginx-access.log:18884, mariadb-slow.log:18, ReportService.java:41·71·88).
그 결과 연결 5개가 모두 사용 중이고 신규 요청은 3초 대기 후 실패했다(app.log:1522, modern/api/src/main/resources/application.yml:14·15). API 응답 실패와 프록시 대기 만료가 이어졌다(nginx-access.log:18918, nginx-error.log:6).
첫 느린 쿼리 이후 영구 누수 경고가 아니라 반환 가능한 장시간 점유가 관측됐다(app.log:1259). API-2 연결 거부는 별도 원인 확인이 필요하다(nginx-error.log:11).

## 5. 근거 로그 줄

| 원인 서술 | 근거(파일:줄번호) | 발췌(마스킹) |
|---|---|---|
| 보고서 서비스가 점유한 연결의 경고 | app.log:1253 | `ReportService$$SpringCGLIB$$0.buildClassReport(<generated>)` |
| 제출 조회 지연 | mariadb-slow.log:18 | `# Query_time: 9.812417  Lock_time: 0.000071` |
| 연결 전부 점유·대기 실패 | app.log:1522 | `request timed out after 3000ms (total=5, active=5, idle=0, waiting=4)` |
| HTTP500 및 프록시 시간 초과 | nginx-access.log:18918, nginx-error.log:6 | `"GET /api/units/M6-2/items HTTP/1.1" 500` |
| 경고 이후 반환 확인 | app.log:1259 | `was returned to the pool (unleaked)` |
| 설명하지 못한 다른 서버 연결 거부 | nginx-error.log:11 | `upstream: "http://API-2:8080/api/classes/2/report"` |

원인·수정안의 코드 근거는 ReportService.java:41·54·71·88, SubmissionRepository.java:8, application.yml:14·15·18이다. 파일 경로는 위 절의 기준을 따른다.

## 6. 수정안(제안)

1. ReportService의 DB 조회·집계를 별도 주입 서비스의 public `@Transactional(readOnly=true)` 메서드로 옮겨 필요한 값을 DTO로 반환하고, 트랜잭션이 끝난 뒤 기존 `sign`을 수행한다. 같은 클래스 내부 호출에 애노테이션만 추가하면 프록시를 우회할 수 있으므로 별도 빈 경계를 사용한다. 서명 payload·순서·반올림·없음/오류 응답은 보존한다. 근거: ReportService.java:41·52·71·88와 [Spring 공식 트랜잭션 문서](https://docs.spring.io/spring-framework/reference/data-access/transaction/declarative/annotations.html).
2. SubmissionRepository.java:8의 실제 배포 실행 계획·데이터량·인덱스 사용을 수집한 뒤 조회 비용을 줄인다. 현재 DDL에 단일 인덱스가 있으므로 복합 인덱스·조회 집계 변경은 측정과 변경 승인 후 결정한다(db/mariadb/init/01-schema.sql:112).
3. 보고서 API의 동시 요청 제한·큐잉/중복 요청 병합을 검토한다. 풀 크기·3초 대기 값을 단순 증가시켜 원인이 해결됐다고 하지 않는다(application.yml:14·15). API-2는 프로세스/컨테이너 로그를 확보해 재시작·네트워크 문제를 구분한다(nginx-error.log:11).

이 차시에서는 제안만 작성했다. 코드·설정 수정, 부하 테스트·벤치마크, API 재배포는 실행하지 않았다.

## 7. 재발 방지

- DB 구간과 서명 구간의 시간·트랜잭션 경계를 리뷰 기준에 넣고, 서명·응답 계약 보존과 종료 후 연결 반환을 검증하는 테스트를 후속 수정에 추가한다.
- 보고서의 입력 크기·조회량·동시 요청을 제한할 정책을 정하고 쿼리 실행 계획 변화와 느린 조회를 점검한다.
- 실제 운영에서 요청 추적 ID·인스턴스 역할·작업 단계별 시간을 남기되 학생 식별자·인증값은 수집 시점부터 마스킹한다.

## 8. 모니터링 항목

| 지표·제안 임계값 | 이번 로그에서 조건 확인 시각 | 해석 |
|---|---|---|
| 연결 점유 경고 1건 이상/분 | 14:37:52.199, app.log:1243 | 로그 즉시 수집 가정이면 첫 풀 실패보다 약 3분 빠름 |
| 제출 조회 3초 초과 | 종료 14:37:52, mariadb-slow.log:15·18 | 수집 지연 제외 시 약 3분 빠름. 시작 시점 경보라고 주장하지 않음 |
| API 5xx 비율 5% 초과/1분 | 14:41에 9/42=21.43%; 집계 종료 14:42:00 | 14:40은 1/42=2.38%. 이 지표는 선행 감지가 아닌 장애 발생 후 감지 |
| active=5, idle=0, waiting>0가 15초 유지 | 최초 표본 14:40:52.319, app.log:1522 | 연속 메트릭이 없어 15초 유지 경보 시각은 확인 불가 |
| API별 연결 거부 1건 이상/분 | 14:42:01, nginx-error.log:11 | API-2의 가용성 별도 알림; 풀 고갈 경보와 구분 |

임계값은 실습 제안이며 현재 서버의 경보 설정을 바꾸지 않았다. 요청 완료 로그 시각과 유입 시각의 차이는 확인하지 못했다.

## 9. 확인하지 못한 것

- DB 실행 계획·통계·실제 CPU/GC 프로파일·부하 재현이 없어 느린 쿼리와 서명 계산의 기여율, 필요한 풀 용량은 판단 불가.
- slow log의 한 쿼리를 특정 클래스/요청에 연결하는 추적 ID가 없고 보고서별 분포를 모른다.
- API-2 연결 거부의 원인과 502 82건의 일대일 연결, 14:52 회복을 만든 조치는 기록이 없다.
- 시간대 미표기 파일은 epoch·요청 시각으로 교차 검증했으나 서버별 시계 오차는 알 수 없다.

업무 적용: 문항·학급 보고서 API 장애 회고에서 요청 종류, DB 점유, 트랜잭션 범위를 함께 대조하고 근거 없는 원인 단정을 줄인다.

마스킹 적용(선택 발췌 변환 수, 재인용 중복은 제외): IP 2건 · 이메일 0건 · 학생 식별자 2건 · 토큰/세션/API 키 0건 · 비밀번호/접속 문자열 0건
