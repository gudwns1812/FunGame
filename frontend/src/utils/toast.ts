export type ToastTone = 'error' | 'success' | 'info';

export interface Toast {
  id: number;
  tone: ToastTone;
  message: string;
}

const AT_MOST_STACKED = 3;

let stacked: Toast[] = [];
let lastId = 0;
const listeners = new Set<() => void>();

const replaceStacked = (next: Toast[]) => {
  stacked = next;
  listeners.forEach((listener) => listener());
};

const stack = (tone: ToastTone, message: string) => {
  lastId += 1;
  replaceStacked([...stacked, { id: lastId, tone, message }].slice(-AT_MOST_STACKED));
};

export const notifyError = (message: string) => stack('error', message);

export const notifySuccess = (message: string) => stack('success', message);

export const notifyInfo = (message: string) => stack('info', message);

export const dismissToast = (id: number) => replaceStacked(stacked.filter((toast) => toast.id !== id));

export const clearToasts = () => replaceStacked([]);

export const stackedToasts = () => stacked;

export const subscribeToToasts = (listener: () => void) => {
  listeners.add(listener);

  return () => {
    listeners.delete(listener);
  };
};
