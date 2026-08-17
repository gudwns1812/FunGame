import { render, screen, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import ToastStack from './ToastStack';
import { clearToasts, notifyError, notifyInfo, notifySuccess } from '../utils/toast';

const VISIBLE_MS = 4000;

describe('ToastStack', () => {
  beforeEach(() => {
    clearToasts();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it('아무 알림도 없으면 화면에 아무것도 그리지 않는다', () => {
    const { container } = render(<ToastStack />);

    expect(container).toBeEmptyDOMElement();
  });

  it('실패는 오류로, 성공은 완료로, 그 밖은 알림으로 구분해 보여준다', () => {
    render(<ToastStack />);

    act(() => notifyError('방이 가득찼습니다.'));

    expect(screen.getByRole('alert')).toHaveTextContent('방이 가득찼습니다.');
    expect(screen.getByText('오류')).toBeInTheDocument();

    act(() => notifySuccess('승급이 완료되었습니다.'));

    expect(screen.getByText('완료').closest('[role="status"]')).toHaveTextContent('승급이 완료되었습니다.');

    act(() => notifyInfo('방장이 회원님을 방에서 내보냈습니다.'));

    expect(screen.getByText('알림').closest('[role="status"]')).toHaveTextContent(
      '방장이 회원님을 방에서 내보냈습니다.',
    );
  });

  it('알림은 잠시 뒤 저절로 사라진다', () => {
    vi.useFakeTimers();
    render(<ToastStack />);

    act(() => notifyError('방이 가득찼습니다.'));
    act(() => vi.advanceTimersByTime(VISIBLE_MS - 1));
    expect(screen.getByRole('alert')).toBeInTheDocument();

    act(() => vi.advanceTimersByTime(1));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('닫기를 누르면 그 알림만 사라진다', async () => {
    const user = userEvent.setup();
    render(<ToastStack />);

    act(() => notifyError('먼저 온 알림'));
    act(() => notifyError('나중에 온 알림'));

    await user.click(screen.getAllByRole('button', { name: '알림 닫기' })[0]);

    expect(screen.queryByText('먼저 온 알림')).not.toBeInTheDocument();
    expect(screen.getByText('나중에 온 알림')).toBeInTheDocument();
  });

  it('알림이 쌓여도 최근 세 개까지만 보여준다', () => {
    render(<ToastStack />);

    act(() => notifyError('첫번째'));
    act(() => notifyError('두번째'));
    act(() => notifyError('세번째'));
    act(() => notifyError('네번째'));

    expect(screen.queryByText('첫번째')).not.toBeInTheDocument();
    expect(screen.getAllByRole('alert')).toHaveLength(3);
    expect(screen.getByText('네번째')).toBeInTheDocument();
  });
});
