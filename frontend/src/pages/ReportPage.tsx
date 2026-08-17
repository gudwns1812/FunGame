import React, { useState } from 'react';
import StaticPageLayout, { StaticSection } from '../components/StaticPageLayout';
import { useReport } from '../hooks/useReport';
import { REPORTABLE_GAME_TYPES } from '../utils/reportReasons';

const PAGE_TITLE = '문의·신고';
const PAGE_DESCRIPTION = '노래가 재생되지 않거나 정답·힌트가 이상한 콘텐츠 오류, 서비스 버그를 접수합니다.';

const CONTENT_ERROR = 'CONTENT_ERROR';
const SERVICE_ISSUE = 'SERVICE_ISSUE';

const KINDS = [
  { kind: CONTENT_ERROR, label: '콘텐츠 오류 제보', hint: '노래가 안 나오거나 정답·힌트가 틀린 경우' },
  { kind: SERVICE_ISSUE, label: '버그 · 서비스 문의', hint: '화면이 멈추거나 로그인이 안 되는 경우' },
];

const ReportPage: React.FC = () => {
  const [kind, setKind] = useState(SERVICE_ISSUE);
  const [gameType, setGameType] = useState(REPORTABLE_GAME_TYPES[0].gameType);
  const [detail, setDetail] = useState('');
  const [isSubmitted, setIsSubmitted] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const { submitReport, isSubmitting } = useReport();

  const submit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!detail.trim()) {
      return;
    }

    const result = await submitReport({
      source: 'LOBBY',
      roomId: null,
      reason: 'ETC',
      detail: detail.trim(),
      gameType: kind === CONTENT_ERROR ? gameType : null,
    });

    setIsSubmitted(result.ok);
    setErrorMessage(result.message);
    if (result.ok) {
      setDetail('');
    }
  };

  return (
    <StaticPageLayout title={PAGE_TITLE} description={PAGE_DESCRIPTION} path="/report">
      <StaticSection heading="무엇을 알려주시겠어요?">
        <form className="space-y-4" onSubmit={submit}>
          <div className="space-y-2">
            {KINDS.map((choice) => (
              <label key={choice.kind} className="flex items-start gap-2">
                <input
                  type="radio"
                  name="report-kind"
                  value={choice.kind}
                  checked={kind === choice.kind}
                  onChange={() => setKind(choice.kind)}
                />
                <span>
                  <span className="px-title text-xs">{choice.label}</span>
                  <span className="block px-label text-[10px]">{choice.hint}</span>
                </span>
              </label>
            ))}
          </div>

          {kind === CONTENT_ERROR && (
            <div>
              <label className="px-label block mb-1.5" htmlFor="report-game-type">
                게임 종류
              </label>
              <select
                id="report-game-type"
                className="px-input"
                value={gameType}
                onChange={(event) => setGameType(event.target.value)}>
                {REPORTABLE_GAME_TYPES.map((option) => (
                  <option key={option.gameType} value={option.gameType}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          )}

          <div>
            <label className="px-label block mb-1.5" htmlFor="report-detail">
              어떤 문제였나요?
            </label>
            <textarea
              id="report-detail"
              className="px-input w-full h-28"
              value={detail}
              onChange={(event) => setDetail(event.target.value)}
            />
          </div>

          <button type="submit" className="px-btn px-btn-primary w-full py-2.5" disabled={isSubmitting}>
            접수
          </button>

          {isSubmitted && <p className="text-sm text-grass">접수했습니다. 확인 뒤 고치겠습니다.</p>}
          {errorMessage && <p className="text-sm text-cherry">{errorMessage}</p>}
        </form>
      </StaticSection>
    </StaticPageLayout>
  );
};

export default ReportPage;
