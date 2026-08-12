import { renderHook, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { SseProvider, useSse } from './SseContext';

const authState = { isAuthenticated: true };

vi.mock('./AuthContext', () => ({
  useAuth: () => authState,
}));

class MockEventSource {
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSED = 2;
  static instances: MockEventSource[] = [];

  readyState: number = MockEventSource.OPEN;
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

  emit(type: string, data: string) {
    this.listeners.get(type)?.forEach((listener) => listener({ data } as MessageEvent));
  }

  listenerCountOf(type: string) {
    return this.listeners.get(type)?.size ?? 0;
  }

  dropConnectionPermanently() {
    this.readyState = MockEventSource.CLOSED;
  }
}

const setTabVisibility = (state: DocumentVisibilityState) => {
  Object.defineProperty(document, 'visibilityState', {
    configurable: true,
    get: () => state,
  });
  document.dispatchEvent(new Event('visibilitychange'));
};

const renderSse = () => renderHook(() => useSse(), { wrapper: SseProvider });

describe('SseContext', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    MockEventSource.instances = [];
    authState.isAuthenticated = true;
    vi.stubGlobal('EventSource', MockEventSource);
    setTabVisibility('visible');
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('로그인 상태면 회원 전용 구독을 연다', async () => {
    renderSse();

    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1));
    expect(MockEventSource.instances[0].url).toContain('/api/sse/subscribe');
  });

  it('로그인하지 않았으면 연결하지 않는다', async () => {
    authState.isAuthenticated = false;

    renderSse();

    expect(MockEventSource.instances).toHaveLength(0);
  });

  it('화면을 떠나면 연결을 닫는다', async () => {
    const { unmount } = renderSse();
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1));

    unmount();

    expect(MockEventSource.instances[0].close).toHaveBeenCalled();
  });

  it('구독한 이벤트를 핸들러에 전달한다', async () => {
    const { result } = renderSse();
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1));
    const received: string[] = [];

    act(() => {
      result.current.onEvent('room-invite', (event) => received.push(event.data));
    });
    act(() => MockEventSource.instances[0].emit('room-invite', '초대'));

    expect(received).toEqual(['초대']);
  });

  it('구독을 해제하면 더 이상 전달하지 않는다', async () => {
    const { result } = renderSse();
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1));
    const received: string[] = [];
    let stopListening = () => {};

    act(() => {
      stopListening = result.current.onEvent('room-invite', (event) => received.push(event.data));
    });
    act(() => stopListening());
    act(() => MockEventSource.instances[0].emit('room-invite', '초대'));

    expect(received).toEqual([]);
  });

  it('같은 이벤트를 여러 곳에서 구독해도 리스너는 하나만 붙인다', async () => {
    const { result } = renderSse();
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1));

    act(() => {
      result.current.onEvent('presence-update', () => {});
      result.current.onEvent('presence-update', () => {});
    });

    expect(MockEventSource.instances[0].listenerCountOf('presence-update')).toBe(1);
  });

  it('연결이 열리기 전에 구독해도 연결된 뒤 이벤트를 받는다', async () => {
    authState.isAuthenticated = false;
    const { result, rerender } = renderSse();
    const received: string[] = [];

    act(() => {
      result.current.onEvent('room-invite', (event) => received.push(event.data));
    });

    authState.isAuthenticated = true;
    rerender();
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1));
    act(() => MockEventSource.instances[0].emit('room-invite', '연결 후 초대'));

    expect(received).toEqual(['연결 후 초대']);
  });

  it('브라우저가 재연결을 포기한 뒤 탭으로 돌아오면 직접 다시 연결한다', async () => {
    renderSse();
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1));

    act(() => MockEventSource.instances[0].dropConnectionPermanently());
    act(() => setTabVisibility('visible'));

    await waitFor(() => expect(MockEventSource.instances).toHaveLength(2));
  });

  it('연결이 살아있으면 탭으로 돌아와도 중복 연결하지 않는다', async () => {
    renderSse();
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1));

    act(() => setTabVisibility('visible'));

    expect(MockEventSource.instances).toHaveLength(1);
  });

  it('다시 연결하면 기존 구독을 새 연결에 다시 붙인다', async () => {
    const { result } = renderSse();
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(1));
    const received: string[] = [];

    act(() => {
      result.current.onEvent('room-invite', (event) => received.push(event.data));
    });
    act(() => MockEventSource.instances[0].dropConnectionPermanently());
    act(() => setTabVisibility('visible'));
    await waitFor(() => expect(MockEventSource.instances).toHaveLength(2));

    act(() => MockEventSource.instances[1].emit('room-invite', '재연결 후 초대'));

    expect(received).toEqual(['재연결 후 초대']);
  });
});
