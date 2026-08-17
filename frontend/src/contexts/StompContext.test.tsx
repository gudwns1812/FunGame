import React from 'react';
import { render, act, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import type { StompConfig } from '@stomp/stompjs';
import { TickerStrategy } from '@stomp/stompjs';
import { StompProvider, useStomp, type StompChannel } from './StompContext';

vi.mock('sockjs-client');

type CapturedConfig = Required<Pick<StompConfig, 'onConnect' | 'onWebSocketClose'>> &
  Pick<StompConfig, 'heartbeatStrategy' | 'reconnectDelay'>;

type FakeSubscription = { destination: string; handler: (message: { body: string }) => void; unsubscribe: () => void };

const configs: CapturedConfig[] = [];
const clients: FakeClient[] = [];

class FakeClient {
  connected = false;
  active = false;
  subscriptions: FakeSubscription[] = [];
  published: { destination: string; body: string }[] = [];
  deactivate = vi.fn().mockResolvedValue(undefined);

  constructor(public readonly config: CapturedConfig) {
    configs.push(config);
    clients.push(this);
  }

  activate() {
    this.active = true;
    this.connected = true;
    this.config.onConnect(undefined as never);
  }

  subscribe(destination: string, handler: (message: { body: string }) => void) {
    const subscription: FakeSubscription = {
      destination,
      handler,
      unsubscribe: () => {
        this.subscriptions = this.subscriptions.filter((open) => open !== subscription);
      },
    };
    this.subscriptions.push(subscription);
    return subscription;
  }

  publish(frame: { destination: string; body: string }) {
    this.published.push(frame);
  }

  dropConnection() {
    this.connected = false;
    this.config.onWebSocketClose(undefined as never);
  }
}

vi.mock('@stomp/stompjs', () => ({
  TickerStrategy: { Interval: 'interval', Worker: 'worker' },
  Client: class {
    constructor(config: CapturedConfig) {
      return new FakeClient(config) as never;
    }
  },
}));

let isAuthenticated = true;
vi.mock('./AuthContext', () => ({
  useAuth: () => ({ isAuthenticated }),
}));

const client = () => clients[clients.length - 1];

const deliver = (destination: string, payload: unknown) => {
  client()
    .subscriptions.filter((subscription) => subscription.destination === destination)
    .forEach((subscription) =>
      subscription.handler({ body: JSON.stringify({ result: 'SUCCESS', data: payload }) }),
    );
};

const DESTINATION = '/topic/lobby';

const Subscriber: React.FC<{ received: unknown[]; prepare?: () => Promise<void> }> = ({ received, prepare }) => {
  const { onConnection } = useStomp();

  React.useEffect(
    () =>
      onConnection(async (channel: StompChannel) => {
        await prepare?.();
        channel.subscribe(DESTINATION, (payload) => received.push(payload));
      }),
    [onConnection, prepare, received],
  );

  return null;
};

describe('StompProvider', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    configs.length = 0;
    clients.length = 0;
    isAuthenticated = true;
    Object.defineProperty(document, 'visibilityState', { configurable: true, get: () => 'visible' });
  });

  const renderWithSubscriber = (received: unknown[], prepare?: () => Promise<void>) =>
    render(
      <StompProvider>
        <Subscriber received={received} prepare={prepare} />
      </StompProvider>,
    );

  it('로그인하면 연결을 하나만 만든다', () => {
    renderWithSubscriber([]);

    expect(clients).toHaveLength(1);
    expect(client().active).toBe(true);
  });

  it('로그인하지 않았으면 연결하지 않는다', () => {
    isAuthenticated = false;

    renderWithSubscriber([]);

    expect(clients).toHaveLength(0);
  });

  it('백그라운드 탭에서도 하트비트가 밀리지 않도록 워커 티커를 쓴다', () => {
    renderWithSubscriber([]);

    expect(configs[0].heartbeatStrategy).toBe(TickerStrategy.Worker);
  });

  it('서버의 이탈 유예(15초)보다 재연결 대기가 짧다', () => {
    renderWithSubscriber([]);

    expect(configs[0].reconnectDelay).toBeLessThan(15_000);
  });

  it('연결이 맺어지면 등록된 구독을 세운다', async () => {
    const received: unknown[] = [];
    renderWithSubscriber(received);

    await waitFor(() => expect(client().subscriptions).toHaveLength(1));

    act(() => deliver(DESTINATION, ['방 하나']));

    expect(received).toEqual([['방 하나']]);
  });

  it('재연결되면 같은 구독을 처음부터 다시 세운다', async () => {
    const received: unknown[] = [];
    renderWithSubscriber(received);
    await waitFor(() => expect(client().subscriptions).toHaveLength(1));

    act(() => client().dropConnection());
    await act(async () => {
      client().activate();
    });

    await waitFor(() => expect(client().subscriptions).toHaveLength(2));

    act(() => deliver(DESTINATION, ['다시 받은 방']));

    expect(received).toContainEqual(['다시 받은 방']);
  });

  it('구독 전에 준비할 일이 끝나기 전에 연결이 끊기면 구독하지 않는다', async () => {
    let letPrepareFinish = () => {};
    const prepare = () => new Promise<void>((resolve) => (letPrepareFinish = resolve));

    renderWithSubscriber([], prepare);
    act(() => client().dropConnection());

    await act(async () => {
      letPrepareFinish();
    });

    expect(client().subscriptions).toHaveLength(0);
  });

  it('구독자가 사라지면 연결은 두고 구독만 끊는다', async () => {
    const received: unknown[] = [];
    const { rerender } = render(
      <StompProvider>
        <Subscriber received={received} />
      </StompProvider>,
    );
    await waitFor(() => expect(client().subscriptions).toHaveLength(1));

    rerender(<StompProvider>{null}</StompProvider>);

    expect(client().subscriptions).toHaveLength(0);
    expect(client().active).toBe(true);
  });

  it('로그아웃하면 연결을 서버 응답을 기다리지 않고 닫는다', async () => {
    const { unmount } = renderWithSubscriber([]);
    const connection = client();

    unmount();

    expect(connection.deactivate).toHaveBeenCalledWith({ force: true });
  });

  it('탭에 돌아왔을 때 연결이 버려져 있으면 다시 살린다', async () => {
    renderWithSubscriber([]);
    const connection = client();
    connection.active = false;
    connection.connected = false;

    act(() => {
      document.dispatchEvent(new Event('visibilitychange'));
    });

    expect(connection.active).toBe(true);
  });

  it('탭에 돌아왔을 때 스스로 재연결 중이면 건드리지 않는다', async () => {
    const received: unknown[] = [];
    renderWithSubscriber(received);
    await waitFor(() => expect(client().subscriptions).toHaveLength(1));
    const connection = client();
    connection.connected = false;

    act(() => {
      document.dispatchEvent(new Event('visibilitychange'));
    });

    expect(connection.subscriptions).toHaveLength(1);
  });

  it('발행은 JSON 본문으로 나간다', async () => {
    let publishChat = (_message: string) => {};
    const Publisher: React.FC = () => {
      const { publish } = useStomp();
      publishChat = (message) => publish('/app/room/7/chat', { message });
      return null;
    };

    render(
      <StompProvider>
        <Publisher />
      </StompProvider>,
    );

    act(() => publishChat('안녕'));

    expect(client().published).toEqual([
      { destination: '/app/room/7/chat', body: JSON.stringify({ message: '안녕' }) },
    ]);
  });

  it('연결이 끊긴 사이에는 발행하지 않는다', async () => {
    let publishChat = (_message: string) => {};
    const Publisher: React.FC = () => {
      const { publish } = useStomp();
      publishChat = (message) => publish('/app/room/7/chat', { message });
      return null;
    };

    render(
      <StompProvider>
        <Publisher />
      </StompProvider>,
    );
    act(() => client().dropConnection());

    act(() => publishChat('안녕'));

    expect(client().published).toEqual([]);
  });
});
