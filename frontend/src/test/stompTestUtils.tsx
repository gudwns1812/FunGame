import React from 'react';
import { StompContext, type StompChannel, type StompHandler, type StompSetUp } from '../contexts/StompContext';

export const createStompStub = (options?: { onSubscribe?: (destination: string) => void }) => {
  const setUps = new Set<StompSetUp>();
  const handlersByDestination = new Map<string, Set<StompHandler>>();
  const published: { destination: string; body: unknown }[] = [];
  let connected = false;

  const channel: StompChannel = {
    subscribe: (destination, handler) => {
      options?.onSubscribe?.(destination);
      const handlers = handlersByDestination.get(destination) ?? new Set<StompHandler>();
      handlers.add(handler);
      handlersByDestination.set(destination, handlers);

      return () => {
        handlers.delete(handler);
      };
    },
  };

  const onConnection = (setUp: StompSetUp) => {
    setUps.add(setUp);
    if (connected) {
      void setUp(channel);
    }

    return () => {
      setUps.delete(setUp);
    };
  };

  const publish = (destination: string, body: unknown) => {
    published.push({ destination, body });
  };

  /** 연결이 (다시) 맺어진 상황을 만든다. 등록된 setUp 이 모두 처음부터 다시 돈다. */
  const connect = async () => {
    connected = true;
    handlersByDestination.clear();
    for (const setUp of setUps) {
      await setUp(channel);
    }
  };

  const emit = (destination: string, payload: unknown) => {
    handlersByDestination.get(destination)?.forEach((handler) => handler(payload));
  };

  const subscriberCountOf = (destination: string) => handlersByDestination.get(destination)?.size ?? 0;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <StompContext.Provider value={{ onConnection, publish }}>{children}</StompContext.Provider>
  );

  return { wrapper, connect, emit, subscriberCountOf, published };
};

export const nestWrappers =
  (...wrappers: React.FC<{ children: React.ReactNode }>[]): React.FC<{ children: React.ReactNode }> =>
  ({ children }) =>
    wrappers.reduceRight((tree, Wrapper) => <Wrapper>{tree}</Wrapper>, children as React.ReactElement);
