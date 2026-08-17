import { useCallback, useEffect, useState } from 'react';
import axios from 'axios';
import type { AdminReport, ReportStatus } from '../types/report';

export const ALL_STATUSES = 'ALL';

export type StatusFilter = ReportStatus | typeof ALL_STATUSES;

const FALLBACK_FAILURE_MESSAGE = '문의를 처리하지 못했습니다. 잠시 후 다시 시도해주세요.';

const failureMessageOf = (error: unknown): string => {
  const message = (error as { response?: { data?: { error?: { message?: string } } } })?.response?.data?.error?.message;

  return message ?? FALLBACK_FAILURE_MESSAGE;
};

export const useReportAdmin = () => {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('OPEN');
  const [reports, setReports] = useState<AdminReport[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const refresh = useCallback(async () => {
    setIsLoading(true);
    try {
      const params = statusFilter === ALL_STATUSES ? {} : { status: statusFilter };
      const response = await axios.get('/api/admin/reports', { params });
      setReports(response.data.data ?? []);
      setErrorMessage(null);
    } catch (error) {
      setErrorMessage(failureMessageOf(error));
    } finally {
      setIsLoading(false);
    }
  }, [statusFilter]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  const comment = useCallback(
    async (reportId: number, content: string) => {
      try {
        await axios.post(`/api/admin/reports/${reportId}/comments`, { content });
        await refresh();
      } catch (error) {
        setErrorMessage(failureMessageOf(error));
      }
    },
    [refresh],
  );

  const changeStatus = useCallback(
    async (reportId: number, status: ReportStatus) => {
      try {
        await axios.patch(`/api/admin/reports/${reportId}/status`, { status });
        await refresh();
      } catch (error) {
        setErrorMessage(failureMessageOf(error));
      }
    },
    [refresh],
  );

  return { reports, isLoading, errorMessage, statusFilter, setStatusFilter, comment, changeStatus };
};
