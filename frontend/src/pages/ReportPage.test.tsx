import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import axios from 'axios';
import ReportPage from './ReportPage';

vi.mock('axios');

const mockedAxios = axios as unknown as {
  post: ReturnType<typeof vi.fn>;
  defaults: Partial<typeof axios.defaults>;
};

const renderPage = () =>
  render(
    <MemoryRouter>
      <ReportPage />
    </MemoryRouter>,
  );

const writeDetail = (text: string) => fireEvent.change(screen.getByRole('textbox'), { target: { value: text } });

const submit = () => fireEvent.click(screen.getByRole('button', { name: '접수' }));

describe('ReportPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockedAxios.post = vi.fn().mockResolvedValue({ data: { result: 'SUCCESS', data: null } });
    mockedAxios.defaults = { baseURL: '', withCredentials: true };
  });

  it('콘텐츠 오류 제보를 고르면 게임 종류를 고를 수 있다', () => {
    renderPage();

    fireEvent.click(screen.getByRole('radio', { name: /콘텐츠 오류/ }));

    expect(screen.getByLabelText('게임 종류', { selector: 'select' })).toBeInTheDocument();
  });

  it('콘텐츠 오류 제보는 고른 게임 종류를 함께 보낸다', async () => {
    renderPage();

    fireEvent.click(screen.getByRole('radio', { name: /콘텐츠 오류/ }));
    fireEvent.change(screen.getByLabelText('게임 종류', { selector: 'select' }), { target: { value: 'HANGMAN' } });
    writeDetail('행맨 단어에 오타가 있어요');
    submit();

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
    expect(mockedAxios.post).toHaveBeenCalledWith('/api/reports', {
      source: 'LOBBY',
      roomId: null,
      reason: 'ETC',
      detail: '행맨 단어에 오타가 있어요',
      gameType: 'HANGMAN',
    });
  });

  it('버그·서비스 문의는 게임 종류 없이 보낸다', async () => {
    renderPage();

    fireEvent.click(screen.getByRole('radio', { name: /버그/ }));
    writeDetail('로그인하면 가끔 튕겨요');
    submit();

    await waitFor(() => expect(mockedAxios.post).toHaveBeenCalledTimes(1));
    expect(mockedAxios.post).toHaveBeenCalledWith('/api/reports', {
      source: 'LOBBY',
      roomId: null,
      reason: 'ETC',
      detail: '로그인하면 가끔 튕겨요',
      gameType: null,
    });
  });

  it('내용이 비어 있으면 접수하지 않는다', () => {
    renderPage();

    writeDetail('   ');
    submit();

    expect(mockedAxios.post).not.toHaveBeenCalled();
  });

  it('접수하면 접수됐다고 알려준다', async () => {
    renderPage();

    writeDetail('내용');
    submit();

    expect(await screen.findByText(/접수했습니다/)).toBeInTheDocument();
  });

  it('접수가 거절되면 서버가 준 이유를 보여준다', async () => {
    mockedAxios.post = vi.fn().mockRejectedValue({
      response: { data: { result: 'FAIL', error: { code: 'R001', message: '신고가 너무 잦습니다.' } } },
    });
    renderPage();

    writeDetail('내용');
    submit();

    expect(await screen.findByText('신고가 너무 잦습니다.')).toBeInTheDocument();
  });
});
