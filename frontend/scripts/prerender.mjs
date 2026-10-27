import { mkdir, readFile, writeFile } from 'node:fs/promises';
import { dirname } from 'node:path';
import { PRERENDERED_PAGES, prerenderPage } from '../dist-ssr/prerender.js';

const template = await readFile('dist/index.html', 'utf8');

await writeFile('dist/app.html', template);

for (const page of PRERENDERED_PAGES) {
  const file = page.path === '/' ? 'dist/index.html' : `dist${page.path}/index.html`;
  await mkdir(dirname(file), { recursive: true });
  await writeFile(file, prerenderPage(template, page));
  console.log(`prerendered ${page.path} -> ${file}`);
}
