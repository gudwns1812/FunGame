import WaitingRoom from '../components/WaitingRoom';
import OnlineUserList from '../components/OnlineUserList';
import TopBar from '../components/layout/TopBar';
import type { Player, RoomSettings } from '../types/game';

interface WaitingRoomPageProps {
  players: Player[];
  logs: string[];
  isHost: boolean;
  onStart: () => void;
  onLeave: () => void;
  onToggleReady: () => void;
  onSendMessage: (message: string) => void;
  maxPlayers: number;
  roomSettings: RoomSettings | null;
  onChangeSettings: (changes: Omit<RoomSettings, 'title' | 'host'>) => void;
  /** 헤더에 표시할 방 이름 */
  roomName?: string;
  /** 이 방으로 초대를 보낼 때 쓰는 방 번호 */
  roomId: string | null;
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
  roomSettings,
  onChangeSettings,
  roomName,
  roomId,
}) => {
  return (
    <div className="app-frame">
      <TopBar
        title="대기실"
        subtitle={roomName}
        right={<span className="px-chip">{isHost ? '방장' : '참가자'}</span>}
      />

      <main className="flex-1 min-h-0 p-3 sm:p-4 flex flex-col md:flex-row gap-3 sm:gap-4">
        <div className="flex-1 min-h-0 flex">
          <WaitingRoom
            players={players}
            logs={logs}
            isHost={isHost}
            onStart={onStart}
            onLeave={onLeave}
            onToggleReady={onToggleReady}
            onSendMessage={onSendMessage}
            maxPlayers={maxPlayers}
            roomSettings={roomSettings}
            onChangeSettings={onChangeSettings}
          />
        </div>

        <OnlineUserList invitingRoomId={roomId} />
      </main>
    </div>
  );
};

export default WaitingRoomPage;
