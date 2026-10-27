import type { ReactElement } from 'react';
import { renderToString } from 'react-dom/server';
import { StaticRouter } from 'react-router-dom';
import LandingPage from './pages/LandingPage';
import { PUBLIC_PAGES } from './publicPages';
import { DocumentHeadCollector, type DocumentHead } from './hooks/useDocumentMeta';

interface PrerenderedPage {
  path: string;
  element: ReactElement;
}

export const PRERENDERED_PAGES: PrerenderedPage[] = [{ path: '/', element: <LandingPage /> }, ...PUBLIC_PAGES];

const escapeHtml = (value: string) =>
  value.replace(/&/g, '&amp;').replace(/"/g, '&quot;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

const replaceOnce = (html: string, pattern: RegExp, replacement: string) => {
  if (!pattern.test(html)) {
    throw new Error(`템플릿에서 ${pattern} 를 찾지 못했다`);
  }
  return html.replace(pattern, () => replacement);
};

const metaContent = (attribute: 'name' | 'property', key: string) =>
  new RegExp(`<meta\\s+${attribute}="${key}"\\s+content="[^"]*"\\s*/>`);

const applyHead = (template: string, head: DocumentHead) => {
  const title = escapeHtml(head.title);
  const description = escapeHtml(head.description);
  const canonical = escapeHtml(head.canonical);

  const replacements: [RegExp, string][] = [
    [/<title>[^<]*<\/title>/, `<title>${title}</title>`],
    [metaContent('name', 'description'), `<meta name="description" content="${description}" />`],
    [/<link\s+rel="canonical"\s+href="[^"]*"\s*\/>/, `<link rel="canonical" href="${canonical}" />`],
    [metaContent('property', 'og:title'), `<meta property="og:title" content="${title}" />`],
    [metaContent('property', 'og:description'), `<meta property="og:description" content="${description}" />`],
    [metaContent('property', 'og:url'), `<meta property="og:url" content="${canonical}" />`],
  ];

  return replacements.reduce((html, [pattern, replacement]) => replaceOnce(html, pattern, replacement), template);
};

export const prerenderPage = (template: string, page: PrerenderedPage) => {
  const collected: DocumentHead[] = [];
  const body = renderToString(
    <DocumentHeadCollector.Provider value={(head) => collected.push(head)}>
      <StaticRouter location={page.path}>{page.element}</StaticRouter>
    </DocumentHeadCollector.Provider>,
  );

  const head = collected.at(-1);
  if (!head) {
    throw new Error(`${page.path} 는 useDocumentMeta 로 head 를 정하지 않는다`);
  }

  return replaceOnce(applyHead(template, head), /<div id="root"><\/div>/, `<div id="root">${body}</div>`);
};
