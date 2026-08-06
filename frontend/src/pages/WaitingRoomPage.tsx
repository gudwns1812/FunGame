import WaitingRoom from '../components/WaitingRoom';
import TopBar from '../components/layout/TopBar';
import type { Player } from '../types/game';

interface WaitingRoomPageProps {
  players: Player[];
  logs: string[];
  isHost: boolean;
  onStart: () => void;
  onLeave: () => void;
  onToggleReady: () => void;
  onSendMessage: (message: string) => void;
  maxPlayers: number;
  /** 헤더에 표시할 방 이름 */
  roomName?: string;
}

const WaitingRoomPage: React.FC<WaitingRoomPageProps> = ({
  players,
  logs,
  isHost,
  onStart,
  onLeave,
  onToggleReady,
  onSendMessage,
  maxPlayers,
  roomName,
}) => {
  return (
    <div className="app-frame">
      <TopBar
        title="대기실"
        subtitle={roomName}
        right={<span className="px-chip">{isHost ? '방장' : '참가자'}</span>}
      />

      <main className="flex-1 min-h-0 p-3 sm:p-4 flex">
        <WaitingRoom
          players={players}
          logs={logs}
          isHost={isHost}
          onStart={onStart}
          onLeave={onLeave}
          onToggleReady={onToggleReady}
          onSendMessage={onSendMessage}
          maxPlayers={maxPlayers}
        />
      </main>
    </div>
  );
};

export default WaitingRoomPage;
