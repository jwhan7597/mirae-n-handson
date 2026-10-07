import { describe, expect, it } from 'vitest';
import { fetchNormalized } from '../lib/target.mjs';

// 기대값은 레거시의 실제 응답으로만 만든다. 입력과 순서를 이관 후에도 유지한다.
const cases = [
  ['정상 · 제목/지문 키워드', { q: '분수' }, 'BR-04'],
  ['정상 · 단원 코드', { unit: 'M5-1' }, 'BR-07'],
  ['정상 · 태그 이름', { tag: '계산' }, 'BR-12'],
  ['정상 · 조건 AND 결합', { unit: 'M5-1', level: '4', tag: '문장제' }, 'BR-02'],
  ['누락 · 모든 조건 기본값', {}, 'BR-09,BR-15'],
  ['빈 값 · 공백 조건', { q: ' ', unit: '', level: ' ', tag: '', sort: '', dir: '', page: '' }, 'BR-02,BR-09,BR-20'],
  ['경계 · 난이도 1', { level: '1' }, 'BR-10'],
  ['경계 · 난이도 4', { level: '4' }, 'BR-09,BR-10'],
  ['경계 · 난이도 5 명시', { level: '5' }, 'BR-09,BR-10'],
  ['이상 · 음수 난이도', { level: '-1' }, 'BR-11'],
  ['이상 · 난이도 05 정수 변환', { level: '05' }, 'BR-11'],
  ['이상 · 난이도 숫자 접두사', { level: '3abc' }, 'BR-11'],
  ['이상 · 난이도 소수 버림', { level: '3.7' }, 'BR-11'],
  ['이상 · 난이도 지수 표기', { level: '1e0' }, 'BR-11'],
  ['이상 · 존재하지 않는 단원', { unit: 'Z99-99' }, 'BR-08,BR-22'],
  ['경계 · 소문자 단원 DB 비교', { unit: 'm5-1' }, 'BR-07,BR-08'],
  ['경계 · 키워드 100자', { q: '가'.repeat(100) }, 'BR-05'],
  ['경계 · 키워드 101자', { q: '가'.repeat(101) }, 'BR-05'],
  ['경계 · 키워드 한 글자', { q: '분' }, 'BR-05'],
  ['이상 · 키워드 LIKE 와일드카드', { q: '%_' }, 'BR-06'],
  ['이상 · 태그 와일드카드는 정확 비교', { tag: '%' }, 'BR-13'],
  ['경계 · 태그 51자', { tag: '가'.repeat(51) }, 'BR-13'],
  ['경계 · 페이지 0', { page: '0' }, 'BR-20'],
  ['경계 · 총 페이지 밖 빈 표', { page: '2' }, 'BR-21'],
  ['경계 · 페이지 999', { page: '999' }, 'BR-20,BR-21'],
  ['경계 · 페이지 1000 상한', { page: '1000' }, 'BR-20'],
  ['이상 · 페이지 공백은 제거하지 않음', { page: ' 2 ' }, 'BR-20'],
  ['정렬 · id 내림차순', { sort: 'id', dir: 'desc' }, 'BR-17'],
  ['정렬 · 제목 오름차순', { sort: 'title', dir: 'asc' }, 'BR-17'],
  ['정렬 · 제목 내림차순', { sort: 'title', dir: 'desc' }, 'BR-17'],
  ['정렬 · 단원 오름차순 보조 키', { sort: 'unit', dir: 'asc' }, 'BR-17'],
  ['정렬 · 단원 내림차순 보조 키', { sort: 'unit', dir: 'desc' }, 'BR-17'],
  ['정렬 · 난이도 오름차순', { sort: 'level', dir: 'asc' }, 'BR-17'],
  ['정렬 · 등록 시각 오름차순', { sort: 'created', dir: 'asc' }, 'BR-17'],
  ['정렬 · 등록 시각 기본 내림차순', { sort: 'created' }, 'BR-17'],
  ['이상 · 미등록 정렬 기준', { sort: 'stem', dir: 'asc' }, 'BR-16'],
  ['이상 · 잘못된 정렬 방향', { sort: 'id', dir: 'sideways' }, 'BR-18'],
  ['정상 · 정렬 대소문자와 공백', { sort: ' UNIT ', dir: ' DESC ' }, 'BR-18'],
  ['배열 · PHP 대괄호 입력은 첫 값', { 'q[]': ['분수', '소수'] }, 'BR-03'],
  ['중복 · PHP 단일 키 입력은 마지막 값', { q: ['분수', '소수'] }, 'BR-03'],
];

describe('item-bank · 문항 검색(search.php)', () => {
  for (const [name, params, rules] of cases) {
    it(`${name} [${rules}]`, async () => {
      const result = await fetchNormalized('item-bank', '/search.php', params);
      expect(result).toMatchSnapshot();
    });
  }
});
