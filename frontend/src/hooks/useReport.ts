import { useCallback, useState } from 'react';
import axios from 'axios';
import type { ReportPayload, ReportResult } from '../types/report';

const FALLBACK_FAILURE_MESSAGE = '신고를 접수하지 못했습니다. 잠시 후 다시 시도해주세요.';

const failureMessageOf = (error: unknown): string => {
  const message = (error as { response?: { data?: { error?: { message?: string } } } })?.response?.data?.error?.message;

  return message ?? FALLBACK_FAILURE_MESSAGE;
};

export const useReport = () => {
  const [isSubmitting, setIsSubmitting] = useState(false);

  const submitReport = useCallback(async (payload: ReportPayload): Promise<ReportResult> => {
    setIsSubmitting(true);
    try {
      await axios.post('/api/reports', payload);
      return { ok: true, message: null };
    } catch (error) {
      return { ok: false, message: failureMessageOf(error) };
    } finally {
      setIsSubmitting(false);
    }
  }, []);

  return { submitReport, isSubmitting };
};
