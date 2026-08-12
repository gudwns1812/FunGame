import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { useSse } from '../contexts/SseContext';
import type { RoomInvite } from '../types/presence';

export const useRoomInvites = () => {
  const { onEvent } = useSse();
  const [pendingInvites, setPendingInvites] = useState<RoomInvite[]>([]);

  useEffect(() => {
    const queueInvite = (event: MessageEvent) => {
      const invite: RoomInvite = JSON.parse(event.data);
      setPendingInvites((invites) =>
        invites.some((queued) => queued.inviteId === invite.inviteId) ? invites : [...invites, invite],
      );
    };

    return onEvent('room-invite', queueInvite);
  }, [onEvent]);

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
