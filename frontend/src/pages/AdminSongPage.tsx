import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import axios from 'axios';

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

  const categories = [
    { value: 'KPOP', label: 'K-POP' },
    { value: 'POP', label: 'POP' },
    { value: 'BALLADE', label: '발라드' },
    { value: 'RAP', label: '랩/힙합' },
    { value: 'OST', label: 'OST' },
  ];

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
      }
    } catch (err) {
      if (axios.isAxiosError(err)) {
        const errorCode = err.response?.data?.error?.errorCode;

        if (errorCode === 'G010') {
          setMessage({ text: '이미 등록된 노래입니다.', type: 'error' });
        } else {
          setMessage({
            text: err.response?.data?.error?.message || '노래 등록 중 오류가 발생했습니다.',
            type: 'error',
          });
        }
      } else {
        setMessage({ text: '노래 등록 중 오류가 발생했습니다.', type: 'error' });
      }
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-background-dark text-white p-6 relative overflow-hidden">
      <div className="absolute inset-0 grid-overlay opacity-10 pointer-events-none"></div>

      <div className="max-w-3xl mx-auto relative z-10 animate-fade-in">
        <button
          onClick={() => navigate('/rooms')}
          className="flex items-center gap-2 text-primary hover:text-primary/80 transition-colors mb-8 font-bold uppercase tracking-widest text-xs">
          <span className="material-symbols-outlined text-sm">arrow_back</span>
          로비로 돌아가기
        </button>

        <div className="panel-border bg-slate-900/80 backdrop-blur-xl p-8 md:p-10 rounded-3xl shadow-2xl space-y-8">
          <div className="border-b border-white/10 pb-6">
            <h1 className="text-3xl font-black tracking-tighter uppercase mb-1">
              <span className="text-primary">노래 추가</span>
            </h1>
            <p className="text-slate-400 text-sm font-medium uppercase tracking-widest">새로운 노래 퀴즈 추가</p>
          </div>

          <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-6">
            {/* 제목 */}
            <div className="space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">
                노래 제목 (필수)
              </label>
              <input
                type="text"
                className="w-full bg-slate-950 border border-primary/20 rounded-xl py-3 px-4 text-white focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                placeholder="노래 제목 입력"
                value={formData.title}
                onChange={(e) => setFormData({ ...formData, title: e.target.value })}
              />
            </div>

            {/* 가수 */}
            <div className="space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">
                가수 명 (필수)
              </label>
              <input
                type="text"
                className="w-full bg-slate-950 border border-primary/20 rounded-xl py-3 px-4 text-white focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                placeholder="가수 이름 입력"
                value={formData.singer}
                onChange={(e) => setFormData({ ...formData, singer: e.target.value })}
              />
            </div>

            {/* 발매일 */}
            <div className="space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">
                발매일 (YYYY-MM-DD)
              </label>
              <input
                type="date"
                className="w-full bg-slate-950 border border-primary/20 rounded-xl py-3 px-4 text-white focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                value={formData.releaseDate}
                onChange={(e) => setFormData({ ...formData, releaseDate: e.target.value })}
              />
            </div>

            {/* 힌트 */}
            <div className="space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">
                초성 힌트 (또는 기타)
              </label>
              <input
                type="text"
                className="w-full bg-slate-950 border border-primary/20 rounded-xl py-3 px-4 text-white focus:border-primary focus:ring-1 focus:ring-primary outline-none transition-all"
                placeholder="예: ㄴㄹㅈㅅ"
                value={formData.hint}
                onChange={(e) => setFormData({ ...formData, hint: e.target.value })}
              />
            </div>

            {/* 카테고리 선택 */}
            <div className="md:col-span-2 space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">
                카테고리 (필수 선택)
              </label>
              <div className="flex flex-wrap gap-2">
                {categories.map((cat) => (
                  <button
                    key={cat.value}
                    type="button"
                    onClick={() => handleCategoryToggle(cat.value)}
                    className={`px-4 py-2 rounded-lg text-xs font-black transition-all border ${
                      formData.categories.includes(cat.value)
                        ? 'bg-primary text-background-dark border-primary'
                        : 'bg-slate-800 text-slate-400 border-white/5 hover:border-primary/30'
                    }`}>
                    {cat.label}
                  </button>
                ))}
              </div>
            </div>

            {/* 정답 추가 */}
            <div className="md:col-span-2 space-y-2">
              <label className="text-[10px] font-bold text-primary/70 uppercase tracking-widest pl-1">
                정답 리스트 (복수 입력 가능)
              </label>
              <div className="flex gap-2 mb-3">
                <input
                  type="text"
                  className="flex-1 bg-slate-950 border border-primary/20 rounded-xl py-3 px-4 text-white focus:border-primary outline-none transition-all"
                  placeholder="추가할 정답 입력"
                  value={currentAnswer}
                  onChange={(e) => setCurrentAnswer(e.target.value)}
                  onKeyDown={(e) => e.key === 'Enter' && (e.preventDefault(), handleAddAnswer())}
                />
                <button
                  type="button"
                  onClick={handleAddAnswer}
                  className="px-6 bg-slate-800 border border-white/10 rounded-xl font-bold hover:bg-slate-700 transition-colors">
                  추가
                </button>
              </div>
              <div className="flex flex-wrap gap-2 min-h-[40px] p-3 bg-slate-950/50 rounded-xl border border-white/5">
                {formData.answers.length === 0 && (
                  <span className="text-xs text-slate-600 italic">등록된 정답이 없습니다.</span>
                )}
                {formData.answers.map((ans, idx) => (
                  <span
                    key={idx}
                    className="inline-flex items-center gap-2 px-3 py-1 bg-primary/10 border border-primary/30 rounded-full text-xs font-bold text-primary">
                    {ans}
                    <button type="button" onClick={() => handleRemoveAnswer(idx)} className="hover:text-white">
                      <span className="material-symbols-outlined text-sm">close</span>
                    </button>
                  </span>
                ))}
              </div>
            </div>

            <div className="md:col-span-2 pt-4">
              {message.text && (
                <div
                  className={`p-4 rounded-xl text-xs font-bold flex items-center gap-2 mb-4 ${
                    message.type === 'success'
                      ? 'bg-green-500/10 text-green-400 border border-green-500/20'
                      : 'bg-red-500/10 text-red-400 border border-red-500/20'
                  }`}>
                  <span className="material-symbols-outlined text-sm">
                    {message.type === 'success' ? 'check_circle' : 'error'}
                  </span>
                  {message.text}
                </div>
              )}

              <button
                type="submit"
                disabled={isLoading}
                className="w-full bg-primary hover:bg-primary/90 text-background-dark font-black py-4 rounded-xl transition-all transform hover:scale-[1.01] shadow-[0_0_20px_rgba(37,192,244,0.3)] disabled:opacity-30 flex items-center justify-center gap-2 uppercase tracking-tighter">
                {isLoading ? '등록 중...' : '시스템에 노래 추가'}
              </button>
            </div>
          </form>
        </div>
      </div>
    </div>
  );
};

export default AdminSongPage;
