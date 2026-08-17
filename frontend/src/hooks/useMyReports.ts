import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import type { MyReport } from '../types/report';

const FALLBACK_FAILURE_MESSAGE = '문의 내역을 불러오지 못했습니다.';

export const useMyReports = () => {
  const [reports, setReports] = useState<MyReport[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    try {
      const response = await axios.get('/api/reports/mine');
      setReports(response.data.data ?? []);
      setErrorMessage(null);
    } catch {
      setErrorMessage(FALLBACK_FAILURE_MESSAGE);
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    refresh();
  }, [refresh]);

  return { reports, isLoading, errorMessage, refresh };
};
