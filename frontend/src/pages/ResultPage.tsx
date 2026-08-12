import Result from '../components/Result';
import ResultBgm from '../components/ResultBgm';
import TopBar from '../components/layout/TopBar';
import type { Player } from '../types/game';

interface ResultPageProps {
  rankings: Player[];
  onBackToLobby: () => void;
  onBackToRoom: () => void;
}

const ResultPage: React.FC<ResultPageProps> = ({ rankings, onBackToLobby, onBackToRoom }) => {
  return (
    <div className="app-frame">
      <TopBar title="게임 결과" />
      <ResultBgm />

      <main className="flex-1 min-h-0 scroll-y custom-scrollbar flex items-center justify-center p-4">
        <Result rankings={rankings} onBackToLobby={onBackToLobby} onBackToRoom={onBackToRoom} />
      </main>
    </div>
  );
};

export default ResultPage;
