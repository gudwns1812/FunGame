import React from 'react';
import { SseContext, type SseHandler } from '../contexts/SseContext';

export const createSseStub = () => {
  const handlersByEvent = new Map<string, Set<SseHandler>>();

  const onEvent = (eventName: string, handler: SseHandler) => {
    const handlers = handlersByEvent.get(eventName) ?? new Set<SseHandler>();
    handlers.add(handler);
    handlersByEvent.set(eventName, handlers);
    return () => {
      handlers.delete(handler);
    };
  };

  const emit = (eventName: string, data: string) => {
    handlersByEvent.get(eventName)?.forEach((handler) => handler({ data } as MessageEvent));
  };

  const listenerCountOf = (eventName: string) => handlersByEvent.get(eventName)?.size ?? 0;

  const wrapper = ({ children }: { children: React.ReactNode }) => (
    <SseContext.Provider value={{ onEvent }}>{children}</SseContext.Provider>
  );

  return { wrapper, emit, listenerCountOf };
};
