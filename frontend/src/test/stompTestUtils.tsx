import React from 'react';
import { StompContext, type StompChannel, type StompHandler, type StompSetUp } from '../contexts/StompContext';

type OpenSubscription = { destination: string; handler: StompHandler };

export const createStompStub = (options?: {
  onSubscribe?: (destination: string) => void;
  startConnected?: boolean;
}) => {
  const setUps = new Set<StompSetUp>();
  const openedBySetUp = new Map<StompSetUp, OpenSubscription[]>();
  const published: { destination: string; body: unknown }[] = [];
  let connected = options?.startConnected ?? false;

  const open = (setUp: StompSetUp) => {
    const opened: OpenSubscription[] = [];
    openedBySetUp.set(setUp, opened);

    const channel: StompChannel = {
      subscribe: (destination, handler) => {
        options?.onSubscribe?.(destination);
        const subscription = { destination, handler };
        opened.push(subscription);

        return () => {
          openedBySetUp.set(setUp, opened.filter((open) => open !== subscription));
        };
      },
    };

    return setUp(channel);
  };

  const onConnection = (setUp: StompSetUp) => {
    setUps.add(setUp);
    if (connected) {
      void open(setUp);
    }

    return () => {
      setUps.delete(setUp);
      openedBySetUp.delete(setUp);
    };
  };

  const publish = (destination: string, body: unknown) => {
    published.push({ destination, body });
  };

  const connect = async () => {
    connected = true;
    openedBySetUp.clear();
    for (const setUp of setUps) {
      await open(setUp);
    }
  };

  const subscribersOf = (destination: string) =>
    [...openedBySetUp.values()].flat().filter((open) => open.destination === destination);

  const emit = (destination: string, payload: unknown) => {
    subscribersOf(destination).forEach((open) => open.handler(payload));
  };

  const subscriberCountOf = (destination: string) => subscribersOf(destination).length;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <StompContext.Provider value={{ onConnection, publish }}>{children}</StompContext.Provider>
  );

  return { wrapper, connect, emit, subscriberCountOf, published };
};
