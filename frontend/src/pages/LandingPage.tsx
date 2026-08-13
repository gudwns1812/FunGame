import React from 'react';
import { Link } from 'react-router-dom';
import SiteFooter from '../components/SiteFooter';
import AdSlot from '../components/AdSlot';
import { AD_SLOTS } from '../utils/adsense';
import { useDocumentMeta } from '../hooks/useDocumentMeta';

const PAGE_TITLE = '노래 맞추기 게임';
const PAGE_DESCRIPTION =
  'FUNGAME은 친구들과 함께 실시간으로 즐기는 무료 노래 맞추기 게임입니다. K-POP·발라드·랩·OST 등 14개 카테고리의 음악 퀴즈와 CS 퀴즈, 행맨 모드를 방을 만들어 최대 12명까지 함께 플레이할 수 있습니다.';

const GAME_MODES = [
  {
    name: '음악 퀴즈',
    summary: '유튜브로 재생되는 노래를 듣고 채팅으로 정답을 맞히는 기본 모드입니다.',
    detail: '라운드당 30초, 10~100라운드, 최대 12명',
  },
  {
    name: 'CS 퀴즈',
    summary: '컴퓨터 과학 상식 문제를 푸는 모드입니다. 스터디용으로도 쓸 수 있습니다.',
    detail: '라운드당 30초, 10~50문제, 최대 12명',
  },
  {
    name: '행맨',
    summary: '순서대로 알파벳을 하나씩 골라 숨겨진 영어 단어를 완성하는 턴제 모드입니다.',
    detail: '기회 6번, 난이도 1~4단계, 2~6명',
  },
];

const HIGHLIGHTS = [
  { title: '설치 없이 바로', body: '브라우저만 있으면 됩니다. 앱을 따로 받을 필요가 없습니다.' },
  { title: '실시간 대전', body: '같은 방에 있는 사람들이 동시에 문제를 듣고 채팅으로 겨룹니다.' },
  { title: '친구 초대', body: '접속 중인 유저 목록에서 원하는 사람을 방으로 바로 초대할 수 있습니다.' },
  { title: '완전 무료', body: '모든 게임 모드를 요금 없이 이용할 수 있습니다.' },
];

const LandingPage: React.FC = () => {
  useDocumentMeta(PAGE_TITLE, PAGE_DESCRIPTION, '/');

  return (
    <div className="min-h-[100dvh] relative z-[1] px-5 py-10">
      <div className="w-full max-w-2xl mx-auto">
        <header className="text-center mb-8">
          <h1 className="px-title text-3xl">FUNGAME</h1>
          <p className="px-title text-base mt-2">친구들과 함께하는 실시간 노래 맞추기</p>
          <p className="text-xs leading-relaxed text-ink-soft mt-4">{PAGE_DESCRIPTION}</p>
        </header>

        <div className="flex flex-col sm:flex-row gap-3 mb-8">
          <Link to="/login" className="px-btn px-btn-primary flex-1 py-3">
            게임 시작하기
          </Link>
          <Link to="/how-to-play" className="px-btn px-btn-paper flex-1 py-3">
            게임 방법 보기
          </Link>
        </div>

        <AdSlot slot={AD_SLOTS.landingHero} className="mb-8" />

        <section className="mb-4">
          <h2 className="px-title text-lg mb-3">게임 모드</h2>
          <div className="space-y-3">
            {GAME_MODES.map((mode) => (
              <article key={mode.name} className="px-card p-4">
                <h3 className="px-title text-sm mb-2">{mode.name}</h3>
                <p className="text-xs leading-relaxed">{mode.summary}</p>
                <p className="px-label mt-2">{mode.detail}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="mb-4">
          <h2 className="px-title text-lg mb-3">이런 점이 좋습니다</h2>
          <div className="grid sm:grid-cols-2 gap-3">
            {HIGHLIGHTS.map((highlight) => (
              <article key={highlight.title} className="px-card p-4">
                <h3 className="px-title text-sm mb-2">{highlight.title}</h3>
                <p className="text-xs leading-relaxed">{highlight.body}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="px-card p-5">
          <h2 className="px-title text-base mb-3">처음이신가요?</h2>
          <p className="text-xs leading-relaxed mb-4">
            회원가입 후 방 목록에서 원하는 방에 들어가거나 직접 방을 만들면 바로 시작할 수 있습니다. 규칙과 점수 계산
            방식은 게임 방법 페이지에 자세히 정리해 두었습니다.
          </p>
          <div className="flex flex-col sm:flex-row gap-3">
            <Link to="/signup" className="px-btn px-btn-sea flex-1 py-3">
              회원가입
            </Link>
            <Link to="/login" className="px-btn px-btn-paper flex-1 py-3">
              로그인
            </Link>
          </div>
        </section>

        <AdSlot slot={AD_SLOTS.landingBottom} className="mt-8" />

        <SiteFooter />
      </div>
    </div>
  );
};

export default LandingPage;
