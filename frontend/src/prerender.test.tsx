import { describe, it, expect } from 'vitest';
import template from '../index.html?raw';
import sitemap from '../public/sitemap.xml?raw';
import { PRERENDERED_PAGES, prerenderPage } from './prerender';

const SITE_ORIGIN = 'https://www.fun-game.club';

const pageAt = (path: string) => {
  const page = PRERENDERED_PAGES.find((candidate) => candidate.path === path);
  if (!page) {
    throw new Error(`${path} 는 미리 그리는 페이지가 아니다`);
  }
  return prerenderPage(template, page);
};

const countOf = (html: string, pattern: RegExp) => html.match(pattern)?.length ?? 0;

describe('공개 페이지 미리 그리기', () => {
  it('첫 화면과 공개 페이지를 모두 미리 그린다', () => {
    expect(PRERENDERED_PAGES.map((page) => page.path)).toEqual(['/', '/how-to-play', '/privacy', '/terms']);
  });

  it.each(['/', '/how-to-play', '/privacy', '/terms'])('%s 의 canonical 과 og:url 은 자기 주소를 가리킨다', (path) => {
    const html = pageAt(path);

    expect(html).toContain(`<link rel="canonical" href="${SITE_ORIGIN}${path}" />`);
    expect(html).toContain(`<meta property="og:url" content="${SITE_ORIGIN}${path}" />`);
    expect(countOf(html, /rel="canonical"/g)).toBe(1);
  });

  it('페이지마다 제목과 설명이 다르다', () => {
    const heads = PRERENDERED_PAGES.map((page) => {
      const html = prerenderPage(template, page);
      return {
        title: html.match(/<title>(.*?)<\/title>/)?.[1],
        description: html.match(/<meta name="description" content="(.*?)" \/>/)?.[1],
      };
    });

    expect(new Set(heads.map((head) => head.title)).size).toBe(PRERENDERED_PAGES.length);
    expect(new Set(heads.map((head) => head.description)).size).toBe(PRERENDERED_PAGES.length);
  });

  it('첫 화면 제목은 온라인 노래 맞추기 멀티 게임이다', () => {
    const html = pageAt('/');

    expect(html).toContain('<title>온라인 노래 맞추기 멀티 게임 · FUNGAME</title>');
    expect(html).toContain('<meta property="og:title" content="온라인 노래 맞추기 멀티 게임 · FUNGAME" />');
  });

  it('게임 방법 페이지는 제목, 설명, og 태그를 그 페이지 것으로 채운다', () => {
    const html = pageAt('/how-to-play');

    expect(html).toContain('<title>게임 방법 · FUNGAME</title>');
    expect(html).toContain('<meta property="og:title" content="게임 방법 · FUNGAME" />');
    expect(html).toMatch(/<meta name="description" content="FUNGAME 노래 맞추기의 시작 방법[^"]*" \/>/);
    expect(html).toMatch(/<meta property="og:description" content="FUNGAME 노래 맞추기의 시작 방법[^"]*" \/>/);
    expect(countOf(html, /<title>/g)).toBe(1);
  });

  it('본문을 자바스크립트 없이도 읽을 수 있게 root 안에 그려 둔다', () => {
    const landing = pageAt('/');
    const howToPlay = pageAt('/how-to-play');

    expect(landing).toMatch(/<div id="root"><div[^>]*>[\s\S]*<h1[^>]*>FUNGAME<\/h1>/);
    expect(landing).toContain('게임 모드');
    expect(howToPlay).toMatch(/<div id="root"><div[^>]*>[\s\S]*<h1[^>]*>게임 방법<\/h1>/);
    expect(howToPlay).toContain('음악 퀴즈 규칙');
  });

  it('사이트맵은 미리 그리는 페이지를 빠짐없이, 그것만 싣는다', () => {
    const locations = [...sitemap.matchAll(/<loc>(.*?)<\/loc>/g)].map((match) => match[1]);

    expect(locations).toEqual(PRERENDERED_PAGES.map((page) => `${SITE_ORIGIN}${page.path}`));
  });
});
