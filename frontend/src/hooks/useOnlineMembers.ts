import { useCallback, useEffect, useRef, useState } from 'react';
import axios from 'axios';
import { useSse } from '../contexts/SseContext';
import type { OnlineMember } from '../types/presence';

const REFRESH_DEBOUNCE_MS = 300;

export const useOnlineMembers = (enabled: boolean) => {
  const { onEvent } = useSse();
  const [members, setMembers] = useState<OnlineMember[]>([]);
  const debounceTimer = useRef<ReturnType<typeof setTimeout> | undefined>(undefined);

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

    const refreshSoon = () => {
      clearTimeout(debounceTimer.current);
      debounceTimer.current = setTimeout(fetchMembers, REFRESH_DEBOUNCE_MS);
    };

    fetchMembers();
    const stopListeningPresence = onEvent('presence-update', refreshSoon);
    const stopListeningConnected = onEvent('connected', refreshSoon);

    return () => {
      clearTimeout(debounceTimer.current);
      stopListeningPresence();
      stopListeningConnected();
    };
  }, [enabled, fetchMembers, onEvent]);

  return members;
};
