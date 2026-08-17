import { describe, it, expect } from 'vitest';

const NATIVE_DIALOG = /\b(?:window\.)?(?:alert|confirm|prompt)\s*\(/;

const sources = import.meta.glob<string>('./**/*.{ts,tsx}', {
  query: '?raw',
  import: 'default',
  eager: true,
});

const isTestFile = (file: string) => /\.test\.tsx?$/.test(file);

const nativeDialogCallsIn = (file: string, source: string) =>
  source
    .split('\n')
    .map((line, index) => ({ text: line.trim(), lineNumber: index + 1 }))
    .filter(({ text }) => NATIVE_DIALOG.test(text))
    .map(({ text, lineNumber }) => `${file}:${lineNumber} ${text}`);

describe('브라우저 기본 대화상자', () => {
  it('어디에서도 alert · confirm · prompt 를 쓰지 않는다', () => {
    const found = Object.entries(sources)
      .filter(([file]) => !isTestFile(file))
      .flatMap(([file, source]) => nativeDialogCallsIn(file, source));

    expect(found).toEqual([]);
  });
});
