import '@testing-library/jest-dom/vitest';

/**
 * Node 22+ 는 자체 Web Storage 전역(localStorage)을 노출하는데,
 * `--localstorage-file` 경로가 없으면 메서드가 없는 껍데기가 되고
 * 이게 jsdom의 Storage 까지 덮어쓴다. (getItem/clear is not a function)
 * 테스트에서는 실제로 동작하는 인메모리 구현으로 교체한다.
 */
class MemoryStorage {
  #store = new Map<string, string>();

  get length() {
    return this.#store.size;
  }

  clear() {
    this.#store.clear();
  }

  getItem(key: string) {
    return this.#store.has(String(key)) ? (this.#store.get(String(key)) as string) : null;
  }

  key(index: number) {
    return [...this.#store.keys()][index] ?? null;
  }

  removeItem(key: string) {
    this.#store.delete(String(key));
  }

  setItem(key: string, value: string) {
    this.#store.set(String(key), String(value));
  }
}

/**
 * jsdom 에는 EventSource 가 없다. 방 목록 SSE 구독 때문에 훅을 렌더링하는 것만으로
 * ReferenceError 가 나므로 아무 것도 하지 않는 스텁을 둔다.
 */
class NoopEventSource {
  static readonly CONNECTING = 0;
  static readonly OPEN = 1;
  static readonly CLOSED = 2;

  readonly readyState = NoopEventSource.CONNECTING;
  onopen: ((event: Event) => void) | null = null;
  onerror: ((event: Event) => void) | null = null;

  addEventListener() { }

  removeEventListener() { }

  close() { }
}

if (typeof globalThis.EventSource === 'undefined') {
  Object.defineProperty(globalThis, 'EventSource', {
    value: NoopEventSource,
    configurable: true,
    writable: true,
  });
}

const storage = new MemoryStorage() as unknown as Storage;

for (const target of [globalThis, typeof window === 'undefined' ? null : window]) {
  if (!target) continue;
  Object.defineProperty(target, 'localStorage', {
    value: storage,
    configurable: true,
    writable: true,
  });
}
