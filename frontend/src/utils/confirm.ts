export interface ConfirmRequest {
  id: number;
  message: string;
  settle: (accepted: boolean) => void;
}

let waiting: ConfirmRequest[] = [];
let lastId = 0;
const listeners = new Set<() => void>();

const replaceWaiting = (next: ConfirmRequest[]) => {
  waiting = next;
  listeners.forEach((listener) => listener());
};

export const askConfirm = (message: string) =>
  new Promise<boolean>((resolve) => {
    lastId += 1;
    replaceWaiting([...waiting, { id: lastId, message, settle: resolve }]);
  });

export const answerConfirm = (id: number, accepted: boolean) => {
  const answered = waiting.find((request) => request.id === id);
  if (!answered) return;

  replaceWaiting(waiting.filter((request) => request !== answered));
  answered.settle(accepted);
};

export const declineWaitingConfirms = () => {
  const declined = waiting;
  replaceWaiting([]);
  declined.forEach((request) => request.settle(false));
};

export const confirmBeingAsked = () => waiting[0] ?? null;

export const subscribeToConfirms = (listener: () => void) => {
  listeners.add(listener);

  return () => {
    listeners.delete(listener);
  };
};
