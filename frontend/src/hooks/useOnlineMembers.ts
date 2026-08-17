import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import { useStomp } from '../contexts/StompContext';
import { PRESENCE_QUEUE } from '../utils/stompDestination';
import type { OnlineMember } from '../types/presence';

export const useOnlineMembers = (enabled: boolean) => {
  const { onConnection } = useStomp();
  const [members, setMembers] = useState<OnlineMember[]>([]);

  const fetchMembers = useCallback(async () => {
    try {
      const response = await axios.get('/api/members/online');
      if (response.data.result === 'SUCCESS') {
        setMembers(response.data.data ?? []);
      }
    } catch (error) {
      console.error('접속 중인 유저를 불러오지 못했습니다.', error);
    }
  }, []);

  useEffect(() => {
    if (!enabled) {
      setMembers([]);
      return;
    }

    // 구독을 먼저 걸고 목록을 읽는다. 그 사이에 온 알림은 읽어 온 목록이 메운다.
    return onConnection((channel) => {
      channel.subscribe(PRESENCE_QUEUE, (pushedMembers) => {
        if (!Array.isArray(pushedMembers)) {
          void fetchMembers();
          return;
        }
        setMembers(pushedMembers as OnlineMember[]);
      });

      void fetchMembers();
    });
  }, [enabled, fetchMembers, onConnection]);

  return members;
};
