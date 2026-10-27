import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import LandingPage from './LandingPage';
import HowToPlayPage from './HowToPlayPage';
import { SONG_CATEGORIES } from '../utils/gameOptions';

describe('공개 페이지 문구', () => {
  it('첫 화면 소개는 지금 고를 수 있는 곡 카테고리 수를 말하고, 노래 퀴즈·맞히기라는 말로도 게임을 설명한다', () => {
    render(
      <MemoryRouter>
        <LandingPage />
      </MemoryRouter>,
    );

    const introduction = screen.getByText(new RegExp(`${SONG_CATEGORIES.length}개 카테고리`));
    expect(introduction).toHaveTextContent('노래 맞추기');
    expect(introduction).toHaveTextContent('노래 퀴즈');
    expect(introduction).toHaveTextContent('맞히');
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
