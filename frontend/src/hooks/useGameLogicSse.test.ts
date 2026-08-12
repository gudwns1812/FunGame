import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import axios from 'axios';
import { useGameLogic } from './useGameLogic';

vi.mock('axios');
vi.mock('sockjs-client');

class MockEventSource {
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSED = 2;
  static instances: MockEventSource[] = [];

  readyState: number = MockEventSource.OPEN;
  onopen: ((event: Event) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;
  close = vi.fn(() => {
    this.readyState = MockEventSource.CLOSED;
  });

  readonly url: string;

  private readonly listeners = new Map<string, Set<(event: MessageEvent) => void>>();

  constructor(url: string) {
    this.url = url;
    MockEventSource.instances.push(this);
  }

  addEventListener(type: string, listener: (event: MessageEvent) => void) {
    if (!this.listeners.has(type)) this.listeners.set(type, new Set());
    this.listeners.get(type)!.add(listener);
  }

  removeEventListener(type: string, listener: (event: MessageEvent) => void) {
    this.listeners.get(type)?.delete(listener);
  }

  open() {
    this.readyState = MockEventSource.OPEN;
    this.onopen?.(new Event('open'));
  }

  dropConnectionPermanently() {
    this.readyState = MockEventSource.CLOSED;
    this.onerror?.(new Event('error'));
  }
}

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

const roomListFetchCount = () =>
  mockedAxios.get.mock.calls.filter((call) => call[0] === '/game/rooms').length;

const setTabVisibility = (state: DocumentVisibilityState) => {
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: () => state,
  });
  document.dispatchEvent(new Event('visibilitychange'));
};

const DEBOUNCE_SETTLE_MS = 400;

const letDebounceSettle = () =>
  act(async () => {
    await new Promise((resolve) => setTimeout(resolve, DEBOUNCE_SETTLE_MS));
  });

const renderInLobby = async () => {
  const rendered = renderHook(() => useGameLogic());
  await waitFor(() => expect(MockEventSource.instances).toHaveLength(1));
  return { ...rendered, sse: MockEventSource.instances[0] };
};

describe('useGameLogic 로비 SSE 연결', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    MockEventSource.instances = [];
    localStorage.clear();
    localStorage.setItem('ums_nickname', '나');

    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: 0 } });
    mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: [] } });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };

    vi.stubGlobal('EventSource', MockEventSource);
    setTabVisibility('visible');
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('로비에 진입하면 방 목록 구독을 연다', async () => {
    const { sse } = await renderInLobby();

    expect(sse.url).toContain('/api/sse/rooms/subscribe');
  });

  it('로비에 진입하면 방 목록을 한 번만 가져온다', async () => {
    const { sse } = await renderInLobby();

    act(() => sse.open());
    await letDebounceSettle();

    expect(roomListFetchCount()).toBe(1);
  });

  it('끊겼다 다시 연결되면 놓친 방 변경을 보정한다', async () => {
    const { sse } = await renderInLobby();
    act(() => sse.open());
    await letDebounceSettle();
    const fetchesBeforeReconnect = roomListFetchCount();

    act(() => sse.open());

    await waitFor(() => expect(roomListFetchCount()).toBe(fetchesBeforeReconnect + 1));
  });

  it('탭으로 돌아오면 놓친 방 변경을 보정하려고 목록을 다시 가져온다', async () => {
    await renderInLobby();
    await letDebounceSettle();
    const fetchesBeforeReturn = roomListFetchCount();

    act(() => setTabVisibility('visible'));

    await waitFor(() => expect(roomListFetchCount()).toBeGreaterThan(fetchesBeforeReturn));
  });

  it('브라우저가 재연결을 포기한 뒤 탭으로 돌아오면 직접 다시 연결한다', async () => {
    const { sse } = await renderInLobby();

    act(() => sse.dropConnectionPermanently());
    act(() => setTabVisibility('visible'));

    await waitFor(() => expect(MockEventSource.instances).toHaveLength(2));
  });

  it('연결이 살아있으면 탭으로 돌아와도 중복 연결하지 않는다', async () => {
    await renderInLobby();

    act(() => setTabVisibility('visible'));
    await letDebounceSettle();

    expect(MockEventSource.instances).toHaveLength(1);
  });

  it('탭이 백그라운드로 가는 시점에는 아무것도 하지 않는다', async () => {
    await renderInLobby();
    await letDebounceSettle();
    const fetchesBeforeHiding = roomListFetchCount();

    act(() => setTabVisibility('hidden'));
    await letDebounceSettle();

    expect(roomListFetchCount()).toBe(fetchesBeforeHiding);
    expect(MockEventSource.instances).toHaveLength(1);
  });

  it('로비를 벗어나면 구독을 닫고 탭 복귀에도 다시 열지 않는다', async () => {
    const { sse, unmount } = await renderInLobby();

    unmount();
    act(() => setTabVisibility('visible'));
    await letDebounceSettle();

    expect(sse.close).toHaveBeenCalled();
    expect(MockEventSource.instances).toHaveLength(1);
  });
});
