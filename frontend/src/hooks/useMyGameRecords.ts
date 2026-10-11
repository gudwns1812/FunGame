import { useEffect, useState } from 'react';
import axios from 'axios';
import type { GameRecord } from '../types/record';

const FALLBACK_FAILURE_MESSAGE = '전적을 불러오지 못했습니다.';

export const useMyGameRecords = () => {
  const [records, setRecords] = useState<GameRecord[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      try {
        const response = await axios.get('/api/records/mine');
        setRecords(response.data.data ?? []);
        setErrorMessage(null);
      } catch {
        setErrorMessage(FALLBACK_FAILURE_MESSAGE);
      } finally {
        setIsLoading(false);
      }
    };
    load();
  }, []);

  return { records, isLoading, errorMessage };
};
