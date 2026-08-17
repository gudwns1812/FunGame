/** 전체가 함께 받는 방 이벤트 */
export const roomTopic = (roomId: string) => `/topic/room/${roomId}`;

/** 방 목록 갱신 */
export const LOBBY_TOPIC = '/topic/lobby';

/** 받는 사람마다 내용이 다른 destination 은 /user 를 앞에 붙여 구독한다 */
export const PRESENCE_QUEUE = '/user/queue/presence';

export const INVITE_QUEUE = '/user/queue/invite';

export const roomChat = (roomId: string) => `/app/room/${roomId}/chat`;
