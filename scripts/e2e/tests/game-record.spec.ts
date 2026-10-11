import { test, expect, Browser, Page, TestInfo } from '@playwright/test';

const PASSWORD = 'password1';
const SHOT_DIR = 'screenshots/game-record';

const CS_ANSWERS: Record<string, string> = {
  'TCP 3-way handshake 의 두 번째 단계에서 서버가 보내는 플래그 조합은?': 'synack',
  '프로세스 간 통신에서 공유 자원 접근을 제어하는 정수 기반 동기화 도구는?': '세마포어',
  'FIFO 방식으로 동작하는 선형 자료구조는?': '큐',
  '트랜잭션의 ACID 중 A 가 뜻하는 것은?': '원자성',
  'HTTP 상태코드 404 의 의미는?': 'notfound',
};

const RECORD_CARDS = ['음악 퀴즈 전적', 'CS 퀴즈 전적', '행맨 전적'];
const NO_GAMES_YET = '아직 플레이한 판이 없습니다.';

const unique = () => Date.now().toString().slice(-6) + Math.floor(Math.random() * 100);

async function signUp(page: Page, loginId: string, nickname: string) {
  await page.goto('/signup');
  await page.getByPlaceholder('닉네임 입력 (최대 16자)').fill(nickname);
  await page.getByRole('button', { name: '중복 확인' }).first().click();
  await expect(page.getByText('사용 가능한 닉네임입니다.')).toBeVisible();
  await page.getByPlaceholder('아이디 입력').fill(loginId);
  await page.getByRole('button', { name: '중복 확인' }).last().click();
  await expect(page.getByText('사용 가능한 아이디입니다.')).toBeVisible();
  await page.getByPlaceholder('이메일 입력').fill(`${loginId}@fun-game.club`);
  await page.getByPlaceholder('비밀번호 입력').fill(PASSWORD);
  await page.getByPlaceholder('비밀번호 다시 입력').fill(PASSWORD);
  await page.getByRole('button', { name: '계정 생성하기' }).click();
  await page.waitForURL('**/login', { timeout: 20_000 });
}

async function logIn(page: Page, loginId: string) {
  await page.goto('/login');
  await page.getByPlaceholder('아이디 입력').fill(loginId);
  await page.getByPlaceholder('비밀번호 입력').fill(PASSWORD);
  await page.getByRole('button', { name: '로그인 ▶' }).click();
  await page.waitForURL('**/rooms', { timeout: 20_000 });
}

async function newUser(browser: Browser, loginId: string, nickname: string) {
  const context = await browser.newContext();
  const page = await context.newPage();
  await signUp(page, loginId, nickname);
  await logIn(page, loginId);
  return { context, page, nickname };
}

async function createCsRoom(page: Page, title: string) {
  await page.getByRole('button', { name: '방 만들기 +' }).click();
  await page.getByPlaceholder('방 제목을 입력하세요').fill(title);
  await page.locator('select').first().selectOption('CS');
  await page.locator('select').nth(1).selectOption('EASY');
  await page.locator('select').last().selectOption({ index: 0 });
  await page.getByRole('button', { name: '방 생성' }).click();
  await page.waitForURL('**/waiting', { timeout: 20_000 });
}

async function joinRoom(page: Page, title: string) {
  const card = page.locator('div').filter({ hasText: new RegExp('^' + title) }).last();
  await card.getByText('입장 ▶').click();
  await page.waitForURL('**/waiting', { timeout: 20_000 });
}

async function currentQuestionOn(page: Page) {
  const text = await page.locator('body').innerText();
  return Object.keys(CS_ANSWERS).find((question) => text.includes(question)) ?? null;
}

async function answerEveryQuestion(page: Page) {
  const answered: string[] = [];
  for (;;) {
    const next = await expect
      .poll(async () => {
        const question = await currentQuestionOn(page);
        return question && !answered.includes(question) ? question : null;
      }, { timeout: 20_000 })
      .not.toBeNull()
      .then(() => currentQuestionOn(page))
      .catch(() => null);
    if (!next) {
      return answered;
    }
    await page.getByPlaceholder('정답을 입력하세요').fill(CS_ANSWERS[next]);
    await page.getByPlaceholder('정답을 입력하세요').press('Enter');
    answered.push(next);
  }
}

async function openMyRecords(page: Page) {
  await page.goto('/mypage');
  await page.getByRole('link', { name: '내 전적' }).click();
  await page.waitForURL('**/mypage/records');
  await expect(page.getByRole('region', { name: RECORD_CARDS[0] })).toBeVisible();
  await page.locator('.animate-pop').evaluate((frame) => Promise.all(frame.getAnimations().map((a) => a.finished)));
}

function recordCard(page: Page, name: string) {
  return page.getByRole('region', { name });
}

async function expectOnlyWhatTheUserNeeds(page: Page, otherNickname: string) {
  await expect(page.getByRole('region')).toHaveCount(RECORD_CARDS.length);
  expect(await page.getByRole('region').evaluateAll((cards) => cards.map((card) => card.getAttribute('aria-label'))))
    .toEqual(RECORD_CARDS);

  const section = page.locator('main');
  await expect(section).not.toContainText(/SONG|HANGMAN|NONE/);
  await expect(section).not.toContainText(/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}/);
  await expect(section).not.toContainText(/합계|전체|총 /);
  await expect(section).not.toContainText(/\d+위|\d+점/);
  await expect(section).not.toContainText(otherNickname);
}

async function shot(page: Page, name: string, testInfo: TestInfo) {
  const file = SHOT_DIR + '/' + name + '.png';
  await page.screenshot({ path: file, fullPage: true });
  await testInfo.attach(name, { path: file, contentType: 'image/png' });
}

test.describe('판을 끝내면 마이페이지에서 내 전적을 게임별로 본다', () => {
  test.setTimeout(180_000);

  test('CS 퀴즈 한 판을 이긴 사람과 진 사람이 각자 자기 전적만 정확히 본다', async ({ browser }, testInfo) => {
    const stamp = unique();
    const roomTitle = '전적' + stamp;
    const winner = await newUser(browser, 'recwin' + stamp, '이긴' + stamp);
    const loser = await newUser(browser, 'reclose' + stamp, '진' + stamp);

    await test.step('판을 하기 전에는 세 게임 모두 아직 판이 없다고 보인다', async () => {
      await openMyRecords(winner.page);

      for (const name of RECORD_CARDS) {
        await expect(recordCard(winner.page, name).getByText('0판')).toBeVisible();
        await expect(recordCard(winner.page, name).getByText(NO_GAMES_YET)).toBeVisible();
        await expect(recordCard(winner.page, name)).not.toContainText('%');
      }
      await expectOnlyWhatTheUserNeeds(winner.page, loser.nickname);
      await shot(winner.page, '1-판-전-빈-전적', testInfo);
    });

    await test.step('둘이 CS 퀴즈를 끝까지 하고, 한 사람이 모든 문제를 맞힌다', async () => {
      await winner.page.goto('/rooms');
      await createCsRoom(winner.page, roomTitle);
      await joinRoom(loser.page, roomTitle);
      await loser.page.getByRole('button', { name: '준비 완료' }).click();
      await winner.page.getByRole('button', { name: '게임 시작 ▶' }).click();

      const answered = await answerEveryQuestion(winner.page);
      expect(answered.length).toBeGreaterThan(0);
      await expect(winner.page.getByRole('button', { name: '게임방으로 돌아가기' })).toBeVisible({ timeout: 20_000 });
      await expect(loser.page.getByRole('button', { name: '게임방으로 돌아가기' })).toBeVisible({ timeout: 20_000 });
      await shot(winner.page, '2-결과-화면', testInfo);
    });

    await test.step('이긴 사람은 CS 퀴즈에만 1판 1승과 승률 100% 가 보인다', async () => {
      await openMyRecords(winner.page);

      const cs = recordCard(winner.page, 'CS 퀴즈 전적');
      await expect(cs.getByText('1판')).toBeVisible();
      await expect(cs.getByText('1승 0패')).toBeVisible();
      await expect(cs.getByText('100%')).toBeVisible();
      await expect(recordCard(winner.page, '음악 퀴즈 전적').getByText(NO_GAMES_YET)).toBeVisible();
      await expect(recordCard(winner.page, '행맨 전적').getByText(NO_GAMES_YET)).toBeVisible();
      await expectOnlyWhatTheUserNeeds(winner.page, loser.nickname);
      await shot(winner.page, '3-이긴-사람의-전적', testInfo);
    });

    await test.step('진 사람은 CS 퀴즈에 1판 1패와 승률 0% 가 보인다', async () => {
      await openMyRecords(loser.page);

      const cs = recordCard(loser.page, 'CS 퀴즈 전적');
      await expect(cs.getByText('1판')).toBeVisible();
      await expect(cs.getByText('0승 1패')).toBeVisible();
      await expect(cs.getByText('0%')).toBeVisible();
      await expect(recordCard(loser.page, '음악 퀴즈 전적').getByText(NO_GAMES_YET)).toBeVisible();
      await expect(recordCard(loser.page, '행맨 전적').getByText(NO_GAMES_YET)).toBeVisible();
      await expectOnlyWhatTheUserNeeds(loser.page, winner.nickname);
      await shot(loser.page, '4-진-사람의-전적', testInfo);
    });

    await winner.context.close();
    await loser.context.close();
  });
});
