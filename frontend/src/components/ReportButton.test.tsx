import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import ReportButton from './ReportButton';

vi.mock('axios');

const mockedAxios = axios as unknown as {
  post: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

const ROOM_ID = 42;

const openReportModal = () => {
  render(<ReportButton roomId={ROOM_ID} gameType="SONG" />);
  fireEvent.click(screen.getByRole('button', { name: '신고' }));
};

describe('ReportButton', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: null } });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
  });

  it('신고 버튼을 누르면 사유를 고르는 모달이 열린다', () => {
    openReportModal();

    expect(screen.getByRole('button', { name: '답이 이상해요' })).toBeInTheDocument();
  });

  it('사유만 담아 접수하고 게임 정보는 보내지 않는다', async () => {
    openReportModal();

    fireEvent.click(screen.getByRole('button', { name: '초성 힌트가 이상해요' }));

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
    expect(mockedAxios.post).toHaveBeenCalledWith('/api/reports', {
      source: 'IN_GAME',
      roomId: ROOM_ID,
      reason: 'HINT_WRONG',
      detail: null,
      gameType: null,
    });
  });

  it('접수하면 접수됐다고 알려준다', async () => {
    openReportModal();

    fireEvent.click(screen.getByRole('button', { name: '답이 이상해요' }));

    expect(await screen.findByText(/접수/)).toBeInTheDocument();
  });

  it('접수가 거절되면 서버가 준 이유를 보여준다', async () => {
    mockedAxios.post = vi.fn().mockRejectedValue({
      response: { data: { result: 'FAIL', error: { code: 'R001', message: '신고가 너무 잦습니다.' } } },
    });
    openReportModal();

    fireEvent.click(screen.getByRole('button', { name: '답이 이상해요' }));

    expect(await screen.findByText('신고가 너무 잦습니다.')).toBeInTheDocument();
  });

  it('방 번호를 모르면 신고 버튼을 내보내지 않는다', () => {
    render(<ReportButton roomId={null} gameType="SONG" />);

    expect(screen.queryByRole('button', { name: '신고' })).not.toBeInTheDocument();
  });
});
