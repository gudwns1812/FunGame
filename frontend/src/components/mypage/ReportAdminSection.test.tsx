import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import ReportAdminSection from './ReportAdminSection';
import type { AdminReport } from '../../types/report';

vi.mock('axios');

const mockedAxios = axios as unknown as {
  get: ReturnType<typeof vi.fn>;
  post: ReturnType<typeof vi.fn>;
  patch: ReturnType<typeof vi.fn>;
};

const REPORT: AdminReport = {
  id: 7,
  memberId: 1,
  reporterNickname: '신고한사람',
  source: 'IN_GAME',
  reason: 'HINT_WRONG',
  detail: null,
  gameType: 'SONG',
  quizCategory: 'KPOP',
  contentId: 777,
  roomId: 42,
  currentRound: 2,
  totalRound: 5,
  quizContent: 'https://youtu.be/BzYnNdJhZQw',
  quizAnswer: '아이유 - 밤편지',
  quizHint: '아이유 - ㅂㅍㅈ',
  status: 'OPEN',
  createdAt: '2026-08-18T00:00:00',
  comments: [],
};

const givenReports = (reports: AdminReport[]) => {
  mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: reports } });
};

const writeAnswer = (text: string) => fireEvent.change(screen.getByRole('textbox'), { target: { value: text } });

describe('ReportAdminSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: null } });
    mockedAxios.patch = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: null } });
  });

  it('처음에는 접수된 문의만 불러온다', async () => {
    givenReports([REPORT]);

    render(<ReportAdminSection />);

    await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(1));
    expect(mockedAxios.get).toHaveBeenCalledWith('/api/admin/reports', { params: { status: 'OPEN' } });
  });

  it('전체를 고르면 상태 조건 없이 불러온다', async () => {
    givenReports([REPORT]);

    render(<ReportAdminSection />);
    await screen.findByText('신고한사람');
    fireEvent.change(screen.getByLabelText('처리 상태', { selector: 'select' }), { target: { value: 'ALL' } });

    await waitFor(() => expect(mockedAxios.get).toHaveBeenLastCalledWith('/api/admin/reports', { params: {} }));
  });

  it('신고자와 정답, 힌트를 함께 보여준다', async () => {
    givenReports([REPORT]);

    render(<ReportAdminSection />);

    expect(await screen.findByText('신고한사람')).toBeInTheDocument();
    expect(screen.getByText('아이유 - 밤편지')).toBeInTheDocument();
    expect(screen.getByText('아이유 - ㅂㅍㅈ')).toBeInTheDocument();
  });

  it('답변이 신고자에게 보인다는 것을 알려준다', async () => {
    givenReports([REPORT]);

    render(<ReportAdminSection />);
    await screen.findByText('신고한사람');

    expect(screen.getByText(/신고자에게 그대로 보입니다/)).toBeInTheDocument();
  });

  it('답변을 남기면 목록을 다시 읽는다', async () => {
    givenReports([REPORT]);

    render(<ReportAdminSection />);
    await screen.findByText('신고한사람');
    writeAnswer('힌트를 고쳤습니다.');
    fireEvent.click(screen.getByRole('button', { name: '답변 남기기' }));

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
    expect(mockedAxios.post).toHaveBeenCalledWith('/api/admin/reports/7/comments', {
      content: '힌트를 고쳤습니다.',
    });
    await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));
  });

  it('빈 답변은 보내지 않는다', async () => {
    givenReports([REPORT]);

    render(<ReportAdminSection />);
    await screen.findByText('신고한사람');
    writeAnswer('   ');
    fireEvent.click(screen.getByRole('button', { name: '답변 남기기' }));

    expect(mockedAxios.post).not.toHaveBeenCalled();
  });

  it('처리 완료로 바꾸면 목록을 다시 읽는다', async () => {
    givenReports([REPORT]);

    render(<ReportAdminSection />);
    await screen.findByText('신고한사람');
    fireEvent.click(screen.getByRole('button', { name: '처리 완료로' }));

    await waitFor(() => expect(mockedAxios.patch).toHaveBeenCalledTimes(1));
    expect(mockedAxios.patch).toHaveBeenCalledWith('/api/admin/reports/7/status', { status: 'RESOLVED' });
    await waitFor(() => expect(mockedAxios.get).toHaveBeenCalledTimes(2));
  });

  it('이미 처리한 문의는 다시 접수 상태로 되돌릴 수 있다', async () => {
    givenReports([{ ...REPORT, status: 'RESOLVED' }]);

    render(<ReportAdminSection />);
    await screen.findByText('신고한사람');
    fireEvent.click(screen.getByRole('button', { name: '접수 상태로' }));

    await waitFor(() => expect(mockedAxios.patch).toHaveBeenCalledTimes(1));
    expect(mockedAxios.patch).toHaveBeenCalledWith('/api/admin/reports/7/status', { status: 'OPEN' });
  });

  it('처리할 문의가 없으면 빈 안내를 보여준다', async () => {
    givenReports([]);

    render(<ReportAdminSection />);

    expect(await screen.findByText(/처리할 문의가 없습니다/)).toBeInTheDocument();
  });
});
