import React from 'react';
import { Link } from 'react-router-dom';

const FOOTER_LINKS = [
  { to: '/', label: '서비스 소개' },
  { to: '/how-to-play', label: '게임 방법' },
  { to: '/privacy', label: '개인정보처리방침' },
  { to: '/terms', label: '이용약관' },
];

const SiteFooter: React.FC = () => (
  <footer className="mt-10 border-t-[3px] border-ink pt-5 pb-10">
    <nav className="flex flex-wrap items-center justify-center gap-x-4 gap-y-2">
      {FOOTER_LINKS.map((link) => (
        <Link key={link.to} to={link.to} className="px-label underline hover:text-cherry">
          {link.label}
        </Link>
      ))}
    </nav>
    <p className="px-label text-center mt-4">© {new Date().getFullYear()} FUNGAME · 노래 퀴즈</p>
  </footer>
);

export default SiteFooter;
