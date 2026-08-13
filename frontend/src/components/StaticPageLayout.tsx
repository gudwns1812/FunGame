import React from 'react';
import { Link } from 'react-router-dom';
import SiteFooter from './SiteFooter';
import { useDocumentMeta } from '../hooks/useDocumentMeta';

interface StaticPageLayoutProps {
  title: string;
  description: string;
  path: string;
  children: React.ReactNode;
}

const StaticPageLayout: React.FC<StaticPageLayoutProps> = ({ title, description, path, children }) => {
  useDocumentMeta(title, description, path);

  return (
    <div className="min-h-[100dvh] relative z-[1] px-5 py-8">
      <div className="w-full max-w-2xl mx-auto">
        <header className="mb-6">
          <Link to="/" className="px-label underline hover:text-cherry">
            ← FUNGAME 홈으로
          </Link>
          <h1 className="px-title text-2xl mt-3">{title}</h1>
          <p className="text-xs text-ink-soft leading-relaxed mt-2">{description}</p>
        </header>

        {children}

        <SiteFooter />
      </div>
    </div>
  );
};

export const StaticSection: React.FC<{ heading: string; children: React.ReactNode }> = ({ heading, children }) => (
  <section className="px-card p-5 mb-4">
    <h2 className="px-title text-base mb-3">{heading}</h2>
    <div className="text-xs leading-relaxed space-y-2">{children}</div>
  </section>
);

export default StaticPageLayout;
