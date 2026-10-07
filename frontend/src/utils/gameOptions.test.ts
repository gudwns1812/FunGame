import { describe, it, expect } from 'vitest';
import { CATEGORIES, SONG_CATEGORIES } from './gameOptions';

const valuesOf = (options: { value: string }[]) => options.map((option) => option.value);

describe('곡 카테고리', () => {
  it('지역(KPOP · POP · JPOP)과 장르(발라드 · 댄스 · 랩 · R&B · 록 · OST)로 고른다', () => {
    expect(valuesOf(SONG_CATEGORIES)).toEqual([
      'KPOP', 'POP', 'JPOP',
      'BALLAD', 'DANCE', 'RAP', 'RNB', 'ROCK', 'OST',
    ]);
  });

  it('세대와 소속사로는 더 이상 고르지 않는다', () => {
    const retired = ['GEN1', 'GEN2', 'GEN3', 'GEN4', 'SM', 'YG', 'JYP', 'HYBE', 'STARSHIP'];

    expect(valuesOf(CATEGORIES).filter((value) => retired.includes(value))).toEqual([]);
  });

  it('방을 만들 때는 맨 앞에 전체를 두고 곡 카테고리를 모두 고를 수 있다', () => {
    expect(valuesOf(CATEGORIES)).toEqual(['TOTAL', ...valuesOf(SONG_CATEGORIES)]);
  });
});
