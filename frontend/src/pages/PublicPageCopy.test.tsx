import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LandingPage from './LandingPage';
import HowToPlayPage from './HowToPlayPage';
import { SONG_CATEGORIES } from '../utils/gameOptions';

describe('공개 페이지 문구', () => {
  it('첫 화면 소개는 사람들이 검색하는 말로 시작하고, 지금 고를 수 있는 곡 카테고리 수를 말한다', () => {
    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    );

    const introduction = screen.getByText(new RegExp(`${SONG_CATEGORIES.length}개 카테고리`));
    expect(introduction.textContent).toMatch(/^친구들과 함께하는 온라인 노래 맞추기 멀티 게임 사이트 FUNGAME\(펀게임\)/);
    expect(introduction).toHaveTextContent('맞힙니다');
  });

  it('첫 화면 부제와 장점 카드에 멀티와 멀티플레이를 모두 쓴다', () => {
    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    );

    expect(screen.getByText('친구들과 함께하는 노래 맞추기 멀티 게임')).toBeInTheDocument();
    expect(screen.getByRole('heading', { name: '실시간 멀티플레이' })).toBeInTheDocument();
  });

  it('게임 방법은 지금 고를 수 있는 곡 카테고리를 모두 적고, 없어진 세대·소속사는 적지 않는다', () => {
    render(
      <MemoryRouter>
        <HowToPlayPage />
      </MemoryRouter>,
    );

    const categoryGuide = screen.getByText('카테고리').closest('li');
    for (const category of SONG_CATEGORIES) {
      expect(categoryGuide).toHaveTextContent(category.label);
    }
    expect(categoryGuide).not.toHaveTextContent(/세대|소속사/);
  });
});
