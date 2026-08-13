import { useEffect } from 'react';

const SITE_NAME = 'FUNGAME';
const SITE_ORIGIN = 'https://www.fun-game.club';

const upsertHeadTag = <T extends HTMLElement>(selector: string, create: () => T): T => {
  const existing = document.head.querySelector<T>(selector);
  if (existing) {
    return existing;
  }

  const created = create();
  document.head.appendChild(created);
  return created;
};

export const useDocumentMeta = (title: string, description: string, path: string) => {
  useEffect(() => {
    document.title = `${title} · ${SITE_NAME}`;

    const descriptionTag = upsertHeadTag<HTMLMetaElement>('meta[name="description"]', () => {
      const tag = document.createElement('meta');
      tag.name = 'description';
      return tag;
    });
    descriptionTag.content = description;

    const canonicalTag = upsertHeadTag<HTMLLinkElement>('link[rel="canonical"]', () => {
      const tag = document.createElement('link');
      tag.rel = 'canonical';
      return tag;
    });
    canonicalTag.href = `${SITE_ORIGIN}${path}`;
  }, [title, description, path]);
};
