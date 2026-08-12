import React, { createContext, useCallback, useContext, useEffect, useRef } from 'react';
import { useAuth } from './AuthContext';

export type SseHandler = (event: MessageEvent) => void;

export interface SseContextType {
  onEvent: (eventName: string, handler: SseHandler) => () => void;
}

export const SseContext = createContext<SseContextType | undefined>(undefined);

const SUBSCRIBE_URL = `${import.meta.env.VITE_API_BASE_URL}/api/sse/subscribe`;

export const SseProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const { isAuthenticated } = useAuth();
  const handlersByEvent = useRef<Map<string, Set<SseHandler>>>(new Map());
  const dispatchersByEvent = useRef<Map<string, SseHandler>>(new Map());
  const sourceRef = useRef<EventSource | null>(null);

  const dispatcherFor = useCallback((eventName: string) => {
    const existing = dispatchersByEvent.current.get(eventName);
    if (existing) return existing;

    const dispatcher: SseHandler = (event) => {
      handlersByEvent.current.get(eventName)?.forEach((handler) => handler(event));
    };
    dispatchersByEvent.current.set(eventName, dispatcher);
    return dispatcher;
  }, []);

  const onEvent = useCallback(
    (eventName: string, handler: SseHandler) => {
      const handlers = handlersByEvent.current.get(eventName) ?? new Set<SseHandler>();
      handlers.add(handler);
      handlersByEvent.current.set(eventName, handlers);

      const dispatcher = dispatcherFor(eventName);
      if (sourceRef.current) {
        sourceRef.current.addEventListener(eventName, dispatcher);
      }

      return () => {
        handlers.delete(handler);
      };
    },
    [dispatcherFor],
  );

  useEffect(() => {
    if (!isAuthenticated) {
      sourceRef.current?.close();
      sourceRef.current = null;
      return;
    }

    const openConnection = () => {
      const source = new EventSource(SUBSCRIBE_URL, { withCredentials: true });
      dispatchersByEvent.current.forEach((dispatcher, eventName) => {
        source.addEventListener(eventName, dispatcher);
      });
      sourceRef.current = source;
    };

    const browserGaveUpReconnecting = () => sourceRef.current?.readyState === EventSource.CLOSED;

    const reconnectOnTabReturn = () => {
      if (document.visibilityState !== 'visible') return;
      if (!browserGaveUpReconnecting()) return;

      sourceRef.current?.close();
      openConnection();
    };

    openConnection();
    document.addEventListener('visibilitychange', reconnectOnTabReturn);

    return () => {
      document.removeEventListener('visibilitychange', reconnectOnTabReturn);
      sourceRef.current?.close();
      sourceRef.current = null;
    };
  }, [isAuthenticated]);

  return <SseContext.Provider value={{ onEvent }}>{children}</SseContext.Provider>;
};

export const useSse = () => {
  const context = useContext(SseContext);
  if (!context) {
    throw new Error('useSse 는 SseProvider 안에서만 쓸 수 있습니다.');
  }
  return context;
};
