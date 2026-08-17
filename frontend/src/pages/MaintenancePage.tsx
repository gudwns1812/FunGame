import React, { useEffect } from 'react';
import { Link } from 'react-router-dom';

const PAGE_TITLE = '서버 점검 중';
const SITE_NAME = 'FUNGAME';
const CONTACT_EMAIL = 'gudwns1812@naver.com';

const NOTICE_LINKS = [
  { to: '/how-to-play', label: '게임 방법' },
  { to: '/privacy', label: '개인정보처리방침' },
  { to: '/terms', label: '이용약관' },
];

const EQUALIZER_BARS = [0, 1, 2, 3];

const useNoIndexWhileDown = () => {
  useEffect(() => {
    document.title = `${PAGE_TITLE} · ${SITE_NAME}`;

    const robots = document.createElement('meta');
    robots.name = 'robots';
    robots.content = 'noindex';
    document.head.appendChild(robots);

    return () => robots.remove();
  }, []);
};

const MaintenancePage: React.FC = () => {
  useNoIndexWhileDown();

  return (
    <div className="min-h-[100dvh] relative z-[1] flex items-center justify-center px-5 py-10">
      <div className="w-full max-w-md">
        <h1 className="px-title text-3xl text-center mb-6">{SITE_NAME}</h1>

        <div className="px-card">
          <div className="px-head flex items-center justify-between">
            <span className="px-label">SYSTEM</span>
            <span className="w-2 h-2 bg-cherry animate-blink" />
          </div>

          <div className="p-6 flex flex-col items-center gap-5 text-center">
            <div className="flex items-end gap-1.5 h-10">
              {EQUALIZER_BARS.map((bar) => (
                <span
                  key={bar}
                  className="px-eq-bar w-3 h-full border-2 border-ink bg-cherry"
                  style={{ animationDelay: `${bar * 0.12}s` }}
                />
              ))}
            </div>

            <div>
              <h2 className="px-title text-2xl">{PAGE_TITLE}</h2>
              <p className="text-sm leading-relaxed mt-3">
                서비스를 더 좋게 만들고 있습니다.
                <br />
                잠시 후 다시 찾아와 주세요.
              </p>
            </div>

            <div className="px-inset w-full p-3">
              <p className="px-label mb-1.5">문의</p>
              <a href={`mailto:${CONTACT_EMAIL}`} className="text-cherry underline text-xs break-all">
                {CONTACT_EMAIL}
              </a>
            </div>
          </div>
        </div>

        <nav className="flex flex-wrap items-center justify-center gap-x-4 gap-y-2 mt-6">
          {NOTICE_LINKS.map((link) => (
            <Link key={link.to} to={link.to} className="px-label underline hover:text-cherry">
              {link.label}
            </Link>
          ))}
        </nav>

        <p className="px-label text-center mt-4">
          © {new Date().getFullYear()} {SITE_NAME} · 노래 퀴즈
        </p>
      </div>
    </div>
  );
};

export default MaintenancePage;
