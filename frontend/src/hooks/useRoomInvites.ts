import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { useStomp } from '../contexts/StompContext';
import { INVITE_QUEUE } from '../utils/stompDestination';
import type { RoomInvite } from '../types/presence';

export const useRoomInvites = () => {
  const { onConnection } = useStomp();
  const [pendingInvites, setPendingInvites] = useState<RoomInvite[]>([]);

  useEffect(
    () =>
      onConnection((channel) => {
        channel.subscribe(INVITE_QUEUE, (invite: RoomInvite) => {
          setPendingInvites((invites) =>
            invites.some((queued) => queued.inviteId === invite.inviteId) ? invites : [...invites, invite],
          );
        });
      }),
    [onConnection],
  );

  const dropInvite = useCallback((inviteId: string) => {
    setPendingInvites((invites) => invites.filter((invite) => invite.inviteId !== inviteId));
  }, []);

  const declineInvite = useCallback(
    async (inviteId: string) => {
      dropInvite(inviteId);
      try {
        await axios.post(`/api/invites/${inviteId}/decline`);
      } catch (error) {
        console.error('초대 거절을 전달하지 못했습니다.', error);
      }
    },
    [dropInvite],
  );

  return {
    currentInvite: pendingInvites[0] ?? null,
    dropInvite,
    declineInvite,
  };
};
