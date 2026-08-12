export type PresenceStatus = 'LOBBY' | 'WAITING' | 'PLAYING';

export interface OnlineMember {
  memberId: number;
  nickname: string;
  status: PresenceStatus;
  currentRoomId: number | null;
}

export interface RoomInvite {
  inviteId: string;
  roomId: number;
  roomTitle: string;
  gameType: string;
  inviterNickname: string;
  expiresInSeconds: number;
}
