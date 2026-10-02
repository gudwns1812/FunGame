import { test, expect, Browser, Page } from '@playwright/test';

/**
 * 이 시나리오의 핵심은 WebSocket 을 끊지 않고 닉네임을 바꾸는 것이다.
 * STOMP 프린시펄은 핸드셰이크 때 박히므로, page.goto 로 전체 페이지를 다시 띄우면
 * 소켓이 새로 붙어 버그가 가려진다. 로그인 뒤에는 화면 안의 버튼만 눌러 이동한다.
 */

const PASSWORD = 'password1';

const SHOT_DIR = 'screenshots';

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
  return { context, page };
}

async function createRoom(page: Page, title: string) {
  await page.getByRole('button', { name: '방 만들기 +' }).click();
  await page.getByPlaceholder('방 제목을 입력하세요').fill(title);
  await page.getByRole('button', { name: '방 생성' }).click();
  await page.waitForURL('**/waiting', { timeout: 20_000 });
}

async function joinRoom(page: Page, title: string) {
  const card = page.locator('div').filter({ hasText: new RegExp('^' + title) }).last();
  await card.getByText('입장 ▶').click();
  await page.waitForURL('**/waiting', { timeout: 20_000 });
}

/** 로비에서 화면 안의 버튼만 눌러 마이페이지를 다녀온다. 소켓은 살아 있다. */
async function changeNicknameWithoutReload(page: Page, currentNickname: string, newNickname: string) {
  await page.getByRole('button', { name: new RegExp(currentNickname) }).click();
  await page.getByRole('button', { name: '마이페이지' }).click();
  await page.waitForURL('**/mypage', { timeout: 20_000 });

  await page.getByPlaceholder('변경할 닉네임을 입력하세요').fill(newNickname);
  await page.getByRole('button', { name: '닉네임 변경' }).click();
  await expect(page.getByText('닉네임이 성공적으로 변경되었습니다.')).toBeVisible();

  await page.getByRole('button', { name: '◀ 로비' }).click();
  await page.waitForURL('**/rooms', { timeout: 20_000 });
}


async function leaveRoom(page: Page) {
  await page.getByRole('button', { name: '나가기' }).click();
  await page.waitForURL('**/rooms', { timeout: 20_000 });
}

async function shot(page: Page, name: string, testInfo: import('@playwright/test').TestInfo) {
  const file = SHOT_DIR + '/' + name + '.png';
  await page.screenshot({ path: file, fullPage: true });
  await testInfo.attach(name, { path: file, contentType: 'image/png' });
}

function chatLine(page: Page, nickname: string, message: string) {
  return page.getByText(new RegExp(nickname + '.{0,3}' + message));
}

function playerInRoom(page: Page, nickname: string) {
  return page.getByRole('button', { name: nickname + ' 내보내기' });
}

test.describe('닉네임을 바꾸면 로그아웃 없이 바로 반영된다', () => {
  test('소켓을 끊지 않고 바꿔도 상대 화면의 채팅과 참가자 목록이 새 닉네임이다', async ({ browser }) => {
    const stamp = unique();
    const oldNickname = '반달' + stamp;
    const newNickname = '보름달' + stamp;
    const roomTitle = '닉네임확인' + stamp;

    const host = await newUser(browser, 'host' + stamp, '방장' + stamp);
    const guest = await newUser(browser, 'guest' + stamp, oldNickname);

    await test.step('손님이 로그인한 채로(소켓 유지) 닉네임을 바꾼다', async () => {
      await changeNicknameWithoutReload(guest.page, oldNickname, newNickname);
    });

    await test.step('방에 들어가 채팅을 보낸다', async () => {
      await createRoom(host.page, roomTitle);
      await joinRoom(guest.page, roomTitle);

      await guest.page.getByPlaceholder('메시지 입력...').fill('바꾼 뒤');
      await guest.page.getByPlaceholder('메시지 입력...').press('Enter');
    });

    await test.step('채팅에 새 닉네임이 찍힌다 — 프린시펄을 쓰면 옛 이름이 나간다', async () => {
      await expect(chatLine(host.page, newNickname, '바꾼 뒤')).toBeVisible();
      await expect(chatLine(host.page, oldNickname, '바꾼 뒤')).toHaveCount(0);
    });

    await test.step('대기실 참가자 목록도 새 닉네임이다', async () => {
      await expect(playerInRoom(host.page, newNickname)).toBeVisible();
      await expect(playerInRoom(host.page, oldNickname)).toHaveCount(0);
    });

    await host.context.close();
    await guest.context.close();
  });

  test('방에 있는 동안 닉네임을 바꿔도 로비로 튕기지 않는다', async ({ browser }) => {
    const stamp = unique();
    const roomTitle = '튕김확인' + stamp;

    const host = await newUser(browser, 'stay' + stamp, '머무는이' + stamp);
    await createRoom(host.page, roomTitle);

    await host.page.goto('/mypage');
    await host.page.getByPlaceholder('변경할 닉네임을 입력하세요').fill('새이름' + stamp);
    await host.page.getByRole('button', { name: '닉네임 변경' }).click();
    await expect(host.page.getByText('닉네임이 성공적으로 변경되었습니다.')).toBeVisible();

    await host.page.goto('/waiting');

    await expect(host.page).toHaveURL(/\/waiting/);
    await expect(host.page.getByPlaceholder('메시지 입력...')).toBeVisible();

    await host.context.close();
  });


  test('방에서 나가 이름을 바꾸고 다시 들어오면 상대 화면에 새 이름이 보인다', async ({ browser }, testInfo) => {
    const stamp = unique();
    const oldNickname = '옛이름' + stamp;
    const newNickname = '새이름' + stamp;
    const roomTitle = '재입장확인' + stamp;

    const host = await newUser(browser, 'owner' + stamp, '방장' + stamp);
    const guest = await newUser(browser, 'rejoin' + stamp, oldNickname);

    await test.step('손님이 옛 이름으로 방에 들어가 있다', async () => {
      await createRoom(host.page, roomTitle);
      await joinRoom(guest.page, roomTitle);

      await expect(playerInRoom(host.page, oldNickname)).toBeVisible();
      await shot(host.page, '1-상대화면-옛이름으로-참가중', testInfo);
    });

    await test.step('손님이 방에서 나간다', async () => {
      await leaveRoom(guest.page);
      await expect(playerInRoom(host.page, oldNickname)).toHaveCount(0);
    });

    await test.step('로그아웃하지 않고 닉네임을 바꾼다', async () => {
      await changeNicknameWithoutReload(guest.page, oldNickname, newNickname);
      await shot(guest.page, '2-본인화면-닉네임-변경함', testInfo);
    });

    await test.step('다시 들어오면 상대 화면에 새 이름이 보인다', async () => {
      await joinRoom(guest.page, roomTitle);

      await expect(playerInRoom(host.page, newNickname)).toBeVisible();
      await expect(playerInRoom(host.page, oldNickname)).toHaveCount(0);
      await shot(host.page, '3-상대화면-새이름으로-재입장', testInfo);
    });

    await test.step('채팅도 새 이름으로 나간다', async () => {
      await guest.page.getByPlaceholder('메시지 입력...').fill('다시 왔어요');
      await guest.page.getByPlaceholder('메시지 입력...').press('Enter');

      await expect(chatLine(host.page, newNickname, '다시 왔어요')).toBeVisible();
      await shot(host.page, '4-상대화면-채팅도-새이름', testInfo);
    });

    await host.context.close();
    await guest.context.close();
  });

  test('로비 접속자 목록에도 새 닉네임이 바로 보인다', async ({ browser }) => {
    const stamp = unique();
    const oldNickname = '옛이름' + stamp;
    const newNickname = '새이름' + stamp;

    const watcher = await newUser(browser, 'watch' + stamp, '구경꾼' + stamp);
    const mover = await newUser(browser, 'move' + stamp, oldNickname);

    await expect(watcher.page.getByText(oldNickname).first()).toBeVisible();

    await changeNicknameWithoutReload(mover.page, oldNickname, newNickname);

    await expect(watcher.page.getByText(newNickname).first()).toBeVisible();

    await watcher.context.close();
    await mover.context.close();
  });
});
