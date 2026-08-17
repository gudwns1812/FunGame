import React, { createContext, useCallback, useContext, useEffect, useRef } from 'react';
import { Client, TickerStrategy, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from './AuthContext';

export type StompHandler = (payload: any) => void;

export interface StompChannel {
  /** 구독한다. 돌려주는 함수로 이 연결이 끊기기 전에 먼저 구독을 끊을 수 있다. */
  subscribe: (destination: string, handler: StompHandler) => () => void;
}

/**
 * 연결 하나 동안 무엇을 받을지 기술한다. 구독 전에 준비할 일(방 소속 확정 같은)이 있으면
 * 여기서 먼저 하고 구독하면 된다. 재연결되면 같은 setUp 이 처음부터 다시 돈다.
 */
export type StompSetUp = (channel: StompChannel) => void | Promise<void>;

export interface StompContextType {
  onConnection: (setUp: StompSetUp) => () => void;
  publish: (destination: string, body: unknown) => void;
}

export const StompContext = createContext<StompContextType | undefined>(undefined);

const RECONNECT_DELAY_MS = 5000;
const HEARTBEAT_MS = 10000;

/** 서버가 ApiResponse 로 감싸 보내므로 성공 payload 만 꺼내 넘긴다 */
const payloadOf = (body: string): unknown => {
  try {
    const response = JSON.parse(body);
    if (response?.result !== 'SUCCESS' || response.data === undefined || response.data === null) {
      return undefined;
    }
    return response.data;
  } catch {
    console.warn('해석할 수 없는 STOMP 메시지를 버립니다.');
    return undefined;
  }
};

const closeWithoutWaitingForServer = async (client: Client) => {
  try {
    await client.deactivate({ force: true });
  } catch (error) {
    console.warn('WebSocket 연결을 닫지 못했습니다.', error);
  }
};

/**
 * 연결은 서비스 단위로 하나다. 로그인하면 열고 로그아웃하면 닫는다.
 * 방 이동은 구독을 바꾸는 일일 뿐이라 핸드셰이크를 다시 하지 않는다.
 */
export const StompProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const clientRef = useRef<Client | null>(null);
  const setUps = useRef<Set<StompSetUp>>(new Set());
  const subscriptionsBySetUp = useRef<Map<StompSetUp, StompSubscription[]>>(new Map());

  const dropSubscription = useCallback((setUp: StompSetUp) => {
    subscriptionsBySetUp.current.get(setUp)?.forEach((subscription) => {
      try {
        subscription.unsubscribe();
      } catch {
        // 연결이 이미 끊긴 뒤라면 구독도 함께 사라졌으므로 무시한다
      }
    });
    subscriptionsBySetUp.current.delete(setUp);
  }, []);

  const dropEverySubscription = useCallback(() => {
    subscriptionsBySetUp.current.clear();
  }, []);

  const runSetUp = useCallback((setUp: StompSetUp) => {
    const client = clientRef.current;
    if (!client?.connected) return;

    const opened: StompSubscription[] = [];
    subscriptionsBySetUp.current.set(setUp, opened);

    const stillTheSameConnection = () => clientRef.current === client && client.connected;

    const channel: StompChannel = {
      subscribe: (destination, handler) => {
        if (!stillTheSameConnection()) return () => {};

        const subscription = client.subscribe(destination, (message) => {
          const payload = payloadOf(message.body);
          if (payload !== undefined) {
            handler(payload);
          }
        });
        opened.push(subscription);

        return () => {
          if (!stillTheSameConnection()) return;
          subscription.unsubscribe();
        };
      },
    };

    Promise.resolve(setUp(channel)).catch((error) => {
      console.error('구독 준비 중 문제가 발생했습니다.', error);
    });
  }, []);

  const onConnection = useCallback(
    (setUp: StompSetUp) => {
      setUps.current.add(setUp);
      runSetUp(setUp);

      return () => {
        setUps.current.delete(setUp);
        dropSubscription(setUp);
      };
    },
    [runSetUp, dropSubscription],
  );

  const publish = useCallback((destination: string, body: unknown) => {
    const client = clientRef.current;
    if (!client?.connected) return;

    client.publish({ destination, body: JSON.stringify(body) });
  }, []);

  useEffect(() => {
    if (!isAuthenticated) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL),
      reconnectDelay: RECONNECT_DELAY_MS,
      heartbeatIncoming: HEARTBEAT_MS,
      heartbeatOutgoing: HEARTBEAT_MS,
      heartbeatStrategy: TickerStrategy.Worker,
      onConnect: () => {
        setUps.current.forEach(runSetUp);
      },
      onWebSocketClose: () => {
        dropEverySubscription();
      },
      onStompError: (frame) => {
        console.error('STOMP Error:', frame);
      },
    });

    clientRef.current = client;
    client.activate();

    const reconnectIfAbandoned = () => {
      if (document.visibilityState !== 'visible') return;
      if (client.active) return;

      console.warn('탭 복귀 시점에 연결이 버려져 있어 즉시 재연결합니다.');
      client.activate();
    };

    document.addEventListener('visibilitychange', reconnectIfAbandoned);

    return () => {
      document.removeEventListener('visibilitychange', reconnectIfAbandoned);
      clientRef.current = null;
      dropEverySubscription();
      closeWithoutWaitingForServer(client);
    };
  }, [isAuthenticated, runSetUp, dropEverySubscription]);

  return <StompContext.Provider value={{ onConnection, publish }}>{children}</StompContext.Provider>;
};

export const useStomp = () => {
  const context = useContext(StompContext);
  if (!context) {
    throw new Error('useStomp 는 StompProvider 안에서만 쓸 수 있습니다.');
  }
  return context;
};
