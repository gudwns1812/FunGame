import { render, screen } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import MyReportsSection from './MyReportsSection';
import type { MyReport } from '../../types/report';

vi.mock('axios');

const mockedAxios = axios as unknown as { get: ReturnType<typeof vi.fn> };

const OPEN_REPORT: MyReport = {
  id: 1,
  source: 'IN_GAME',
  reason: 'HINT_WRONG',
  detail: null,
  gameType: 'SONG',
  status: 'OPEN',
  createdAt: '2026-08-18T00:00:00',
  comments: [],
};

const RESOLVED_REPORT: MyReport = {
  id: 2,
  source: 'LOBBY',
  reason: 'ETC',
  detail: '로그인하면 가끔 튕겨요',
  gameType: null,
  status: 'RESOLVED',
  createdAt: '2026-08-17T00:00:00',
  comments: [
    { id: 10, authorNickname: '관리자', content: '고쳤습니다.', createdAt: '2026-08-17T01:00:00' },
  ],
};

const givenReports = (reports: MyReport[]) => {
  mockedAxios.get = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: reports } });
};

describe('MyReportsSection', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('접수한 신고의 사유와 상태를 한글로 보여준다', async () => {
    givenReports([OPEN_REPORT]);

    render(<MyReportsSection />);

    expect(await screen.findByText('힌트가 이상함')).toBeInTheDocument();
    expect(screen.getByText('접수됨')).toBeInTheDocument();
  });

  it('답변이 없으면 아직 확인 중이라고 알려준다', async () => {
    givenReports([OPEN_REPORT]);

    render(<MyReportsSection />);

    expect(await screen.findByText(/확인 중/)).toBeInTheDocument();
  });

  it('관리자 답변을 작성자와 함께 보여준다', async () => {
    givenReports([RESOLVED_REPORT]);

    render(<MyReportsSection />);

    expect(await screen.findByText('고쳤습니다.')).toBeInTheDocument();
    expect(screen.getByText(/관리자/)).toBeInTheDocument();
    expect(screen.getByText('처리 완료')).toBeInTheDocument();
  });

  it('내가 쓴 내용을 그대로 보여준다', async () => {
    givenReports([RESOLVED_REPORT]);

    render(<MyReportsSection />);

    expect(await screen.findByText('로그인하면 가끔 튕겨요')).toBeInTheDocument();
  });

  it('접수한 신고가 없으면 빈 안내를 보여준다', async () => {
    givenReports([]);

    render(<MyReportsSection />);

    expect(await screen.findByText(/접수한 문의가 없습니다/)).toBeInTheDocument();
  });

  it('내가 접수한 신고만 묻는다', async () => {
    givenReports([]);

    render(<MyReportsSection />);

    expect(await screen.findByText(/접수한 문의가 없습니다/)).toBeInTheDocument();
    expect(mockedAxios.get).toHaveBeenCalledWith('/api/reports/mine');
  });
});
