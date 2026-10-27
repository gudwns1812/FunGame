import { createContext, useContext, useEffect } from 'react';

const SITE_NAME = 'FUNGAME';
const SITE_ORIGIN = 'https://www.fun-game.club';

export interface DocumentHead {
  title: string;
  description: string;
  canonical: string;
}

export const DocumentHeadCollector = createContext<((head: DocumentHead) => void) | null>(null);

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
  const head: DocumentHead = {
    title: `${title} · ${SITE_NAME}`,
    description,
    canonical: `${SITE_ORIGIN}${path}`,
  };

  useContext(DocumentHeadCollector)?.(head);

  useEffect(() => {
    document.title = head.title;

    const descriptionTag = upsertHeadTag<HTMLMetaElement>('meta[name="description"]', () => {
      const tag = document.createElement('meta');
      tag.name = 'description';
      return tag;
    });
    descriptionTag.content = head.description;

    const canonicalTag = upsertHeadTag<HTMLLinkElement>('link[rel="canonical"]', () => {
      const tag = document.createElement('link');
      tag.rel = 'canonical';
      return tag;
    });
    canonicalTag.href = head.canonical;
  }, [head.title, head.description, head.canonical]);
};
