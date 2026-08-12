import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';
import TopBar from '../components/layout/TopBar';
import { SONG_CATEGORIES } from '../utils/gameOptions';

const AdminSongPage: React.FC = () => {
  const [formData, setFormData] = useState({
    title: '',
    singer: '',
    categories: [] as string[],
    releaseDate: '',
    answers: [] as string[],
    hint: '',
  });
  const [currentAnswer, setCurrentAnswer] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [message, setMessage] = useState({ text: '', type: '' });
  const navigate = useNavigate();

  // Duplicate Check States
  const [isCheckingDuplicate, setIsCheckingDuplicate] = useState(false);
  const [duplicateStatus, setDuplicateStatus] = useState<'idle' | 'available' | 'duplicate'>('idle');

  const handleCategoryToggle = (value: string) => {
    setFormData((prev) => ({
      ...prev,
      categories: prev.categories.includes(value)
        ? prev.categories.filter((c) => c !== value)
        : [...prev.categories, value],
    }));
  };

  const handleAddAnswer = () => {
    if (currentAnswer.trim() && !formData.answers.includes(currentAnswer.trim())) {
      setFormData((prev) => ({
        ...prev,
        answers: [...prev.answers, currentAnswer.trim()],
      }));
      setCurrentAnswer('');
    }
  };

  const handleRemoveAnswer = (index: number) => {
    setFormData((prev) => ({
      ...prev,
      answers: prev.answers.filter((_, i) => i !== index),
    }));
  };

  const handleCheckDuplicate = async () => {
    if (!formData.title.trim() || !formData.releaseDate) return;
    setIsCheckingDuplicate(true);
    try {
      const response = await axios.get('/api/admin/songs', {
        params: {
          title: formData.title,
          releaseDate: formData.releaseDate,
        },
      });
      // response.data.data가 실제 중복 여부를 나타내는 boolean 값입니다.
      if (response.data.data === true) {
        setDuplicateStatus('duplicate');
      } else {
        setDuplicateStatus('available');
      }
    } catch (err) {
      console.error('Duplicate check failed', err);
      setDuplicateStatus('idle');
    } finally {
      setIsCheckingDuplicate(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.title || !formData.singer || formData.categories.length === 0 || formData.answers.length === 0) {
      setMessage({ text: '필수 정보를 모두 입력해주세요 (제목, 가수, 카테고리, 정답 최소 1개).', type: 'error' });
      return;
    }

    setIsLoading(true);
    setMessage({ text: '', type: '' });

    try {
      const response = await axios.post('/api/admin/songs', formData);
      if (response.data.result === 'SUCCESS') {
        setMessage({ text: '노래가 성공적으로 등록되었습니다!', type: 'success' });
        setFormData({
          title: '',
          singer: '',
          categories: [],
          releaseDate: '',
          answers: [],
          hint: '',
        });
        setDuplicateStatus('idle'); // Reset duplicate status after successful submission
      }
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const errorCode = err.response?.data?.error?.errorCode;

        if (errorCode === 'G010') {
          setMessage({ text: '이미 등록된 노래입니다.', type: 'error' });
        } else {
          setMessage({
            text: '이미 등록된 노래입니다.',
            type: 'error',
          });
        }
      } else {
        setMessage({ text: '이미 등록된 노래입니다.', type: 'error' });
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="app-frame">
      <TopBar
        title="노래 추가"
        onLogoClick={() => navigate('/rooms')}
        right={
          <button onClick={() => navigate('/rooms')} className="px-btn px-btn-sm px-btn-paper">
            ◀ 로비
          </button>
        }
      />

      <main className="flex-1 min-h-0 scroll-y custom-scrollbar p-4 sm:p-6">
        <form onSubmit={handleSubmit} className="w-full max-w-3xl mx-auto flex flex-col gap-4 animate-pop">
          {/* 기본 정보 */}
          <div className="px-card">
            <div className="px-head">
              <span>노래 정보</span>
            </div>

            <div className="p-4 grid grid-cols-1 md:grid-cols-2 gap-3">
              <div className="md:col-span-2">
                <label className="px-label block mb-1.5">노래 제목 (필수)</label>
                <input
                  type="text"
                  className="px-input"
                  placeholder="노래 제목 입력"
                  value={formData.title}
                  onChange={(e) => {
                    setFormData({ ...formData, title: e.target.value });
                    setDuplicateStatus('idle');
                  }}
                />
              </div>

              <div>
                <label className="px-label block mb-1.5">가수 명 (필수)</label>
                <input
                  type="text"
                  className="px-input"
                  placeholder="가수 이름 입력"
                  value={formData.singer}
                  onChange={(e) => setFormData({ ...formData, singer: e.target.value })}
                />
              </div>

              <div>
                <label className="px-label block mb-1.5">발매일 (YYYY-MM-DD)</label>
                <div className="flex gap-2">
                  <input
                    type="date"
                    max="9999-12-31"
                    className="px-input flex-1"
                    value={formData.releaseDate}
                    onChange={(e) => {
                      if (e.target.value.length > 10) return;
                      setFormData({ ...formData, releaseDate: e.target.value });
                      setDuplicateStatus('idle');
                    }}
                    onPaste={(e) => {
                      const pastedData = e.clipboardData.getData('text');
                      const match = pastedData.match(/(\d{4})[./-](\d{1,2})[./-](\d{1,2})/);
                      if (match) {
                        e.preventDefault();
                        const year = match[1];
                        const month = match[2].padStart(2, '0');
                        const day = match[3].padStart(2, '0');
                        setFormData({ ...formData, releaseDate: `${year}-${month}-${day}` });
                        setDuplicateStatus('idle');
                      }
                    }}
                  />
                  <button
                    type="button"
                    onClick={handleCheckDuplicate}
                    disabled={!formData.title.trim() || !formData.releaseDate || isCheckingDuplicate}
                    className="px-btn px-btn-sm px-btn-paper shrink-0">
                    {isCheckingDuplicate ? '확인 중' : '중복 확인'}
                  </button>
                </div>

                {duplicateStatus === 'duplicate' && (
                  <p className="px-label text-cherry mt-1.5">이미 등록된 노래입니다.</p>
                )}
                {duplicateStatus === 'available' && (
                  <p className="px-label text-grass mt-1.5">등록 가능한 노래입니다.</p>
                )}
              </div>

              <div className="md:col-span-2">
                <label className="px-label block mb-1.5">초성 힌트 (또는 기타)</label>
                <input
                  type="text"
                  className="px-input"
                  placeholder="예: ㄴㄹㅈㅅ"
                  value={formData.hint}
                  onChange={(e) => setFormData({ ...formData, hint: e.target.value })}
                />
              </div>

              <div className="md:col-span-2">
                <label className="px-label block mb-1.5">카테고리 (필수 선택)</label>
                <div className="flex flex-wrap gap-2">
                  {SONG_CATEGORIES.map((cat) => (
                    <button
                      key={cat.value}
                      type="button"
                      onClick={() => handleCategoryToggle(cat.value)}
                      className={`px-btn px-btn-sm ${
                        formData.categories.includes(cat.value) ? 'px-btn-sea' : 'px-btn-paper'
                      }`}>
                      {cat.label}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          {/* 정답 리스트 */}
          <div className="px-card">
            <div className="px-head">
              <span>정답 리스트</span>
              <span className="px-label text-[10px]">복수 입력 가능</span>
            </div>

            <div className="p-4 space-y-3">
              <p className="px-label leading-5">영어는 반드시 소문자로 입력할 것. 모든 정답은 띄어쓰기 금지.</p>

              <div className="flex gap-2">
                <input
                  type="text"
                  className="px-input flex-1"
                  placeholder="추가할 정답 입력"
                  value={currentAnswer}
                  onChange={(e) => setCurrentAnswer(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddAnswer())}
                />
                <button type="button" onClick={handleAddAnswer} className="px-btn px-btn-sm px-btn-sea shrink-0">
                  추가
                </button>
              </div>

              <div className="px-inset flex flex-wrap gap-2 min-h-[48px] p-2.5">
                {formData.answers.length === 0 && <span className="px-label">등록된 정답이 없습니다.</span>}
                {formData.answers.map((ans, idx) => (
                  <span key={idx} className="px-chip gap-2">
                    {ans}
                    <button
                      type="button"
                      onClick={() => handleRemoveAnswer(idx)}
                      className="text-cherry leading-none"
                      aria-label="정답 삭제">
                      ✕
                    </button>
                  </span>
                ))}
              </div>
            </div>
          </div>

          {message.text && (
            <div
              className={`border-[3px] border-ink px-3 py-2.5 text-xs font-display ${
                message.type === 'success' ? 'bg-grass text-white' : 'bg-cherry text-white'
              }`}>
              {message.text}
            </div>
          )}

          <button type="submit" disabled={isLoading} className="px-btn px-btn-primary w-full py-3">
            {isLoading ? '등록 중...' : '노래 등록'}
          </button>
        </form>
      </main>
    </div>
  );
};

export default AdminSongPage;
