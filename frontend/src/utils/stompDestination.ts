export const roomTopic = (roomId: string) => `/subscribe/room/${roomId}`;

export const roomChat = (roomId: string) => `/publish/room/${roomId}/chat`;
