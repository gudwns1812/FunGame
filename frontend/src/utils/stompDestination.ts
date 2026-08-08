/**
 * STOMP 목적지 규약. 백엔드의 StompDestination 과 짝을 이룬다.
 * 한쪽만 바꾸면 서버가 발행한 메시지를 아무도 받지 못하므로 함께 수정해야 한다.
 */
export const roomTopic = (roomId: string) => `/subscribe/room/${roomId}`;

export const roomChat = (roomId: string) => `/publish/room/${roomId}/chat`;
