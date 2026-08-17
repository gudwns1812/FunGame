export const roomTopic = (roomId: string) => `/topic/room/${roomId}`;

export const LOBBY_TOPIC = '/topic/lobby';

export const PRESENCE_QUEUE = '/user/queue/presence';

export const INVITE_QUEUE = '/user/queue/invite';

export const roomChat = (roomId: string) => `/app/room/${roomId}/chat`;
