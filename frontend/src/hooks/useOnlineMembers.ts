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
