import React, { createContext, useCallback, useContext, useEffect, useRef } from 'react';
import { Client, TickerStrategy, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { useAuth } from './AuthContext';

export type StompHandler = (payload: any) => void;

export interface StompChannel {
  subscribe: (destination: string, handler: StompHandler) => () => void;
}

export type StompSetUp = (channel: StompChannel) => void | Promise<void>;

export interface StompContextType {
  onConnection: (setUp: StompSetUp) => () => void;
  publish: (destination: string, body: unknown) => void;
}

export const StompContext = createContext<StompContextType | undefined>(undefined);

const RECONNECT_DELAY_MS = 5000;
const HEARTBEAT_MS = 10000;

const successPayloadOf = (body: string): unknown => {
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

const unsubscribeQuietly = (subscription: StompSubscription) => {
  try {
    subscription.unsubscribe();
  } catch {
    return;
  }
};

const closeWithoutWaitingForServer = async (client: Client) => {
  try {
    await client.deactivate({ force: true });
  } catch (error) {
    console.warn('WebSocket 연결을 닫지 못했습니다.', error);
  }
};

export const StompProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const clientRef = useRef<Client | null>(null);
  const setUps = useRef<Set<StompSetUp>>(new Set());
  const subscriptionsBySetUp = useRef<Map<StompSetUp, StompSubscription[]>>(new Map());

  const dropSubscriptionsOf = useCallback((setUp: StompSetUp) => {
    subscriptionsBySetUp.current.get(setUp)?.forEach(unsubscribeQuietly);
    subscriptionsBySetUp.current.delete(setUp);
  }, []);

  const forgetSubscriptionsOfDeadConnection = useCallback(() => {
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
          const payload = successPayloadOf(message.body);
          if (payload !== undefined) {
            handler(payload);
          }
        });
        opened.push(subscription);

        return () => {
          if (!stillTheSameConnection()) return;
          unsubscribeQuietly(subscription);
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
        dropSubscriptionsOf(setUp);
      };
    },
    [runSetUp, dropSubscriptionsOf],
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
        forgetSubscriptionsOfDeadConnection();
      },
      onStompError: (frame) => {
        console.error('STOMP Error:', frame);
      },
    });

    clientRef.current = client;
    client.activate();

    const reviveAbandonedConnectionOnTabReturn = () => {
      if (document.visibilityState !== 'visible') return;
      if (client.active) return;

      console.warn('탭 복귀 시점에 연결이 버려져 있어 즉시 재연결합니다.');
      client.activate();
    };

    document.addEventListener('visibilitychange', reviveAbandonedConnectionOnTabReturn);

    return () => {
      document.removeEventListener('visibilitychange', reviveAbandonedConnectionOnTabReturn);
      clientRef.current = null;
      forgetSubscriptionsOfDeadConnection();
      closeWithoutWaitingForServer(client);
    };
  }, [isAuthenticated, runSetUp, forgetSubscriptionsOfDeadConnection]);

  return <StompContext.Provider value={{ onConnection, publish }}>{children}</StompContext.Provider>;
};

export const useStomp = () => {
  const context = useContext(StompContext);
  if (!context) {
    throw new Error('useStomp 는 StompProvider 안에서만 쓸 수 있습니다.');
  }
  return context;
};
