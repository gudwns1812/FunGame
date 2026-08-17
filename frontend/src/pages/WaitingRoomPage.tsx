import WaitingRoom from '../components/WaitingRoom';
import OnlineUserList from '../components/OnlineUserList';
import TopBar from '../components/layout/TopBar';
import AdSlot from '../components/AdSlot';
import { AD_SLOTS } from '../utils/adsense';
import type { Player, RoomSettings } from '../types/game';

interface WaitingRoomPageProps {
  players: Player[];
  logs: string[];
  isHost: boolean;
  onStart: () => void;
  onLeave: () => void;
  onToggleReady: () => void;
  onKickPlayer: (targetMemberId: number) => void;
  onSendMessage: (message: string) => void;
  maxPlayers: number;
  myMemberId: number | null;
  roomSettings: RoomSettings | null;
  onChangeSettings: (changes: Omit<RoomSettings, 'title' | 'hostMemberId' | 'hostNickname'>) => void;
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
  onKickPlayer,
  onSendMessage,
  maxPlayers,
  myMemberId,
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
            onKickPlayer={onKickPlayer}
            onSendMessage={onSendMessage}
            maxPlayers={maxPlayers}
            roomSettings={roomSettings}
            onChangeSettings={onChangeSettings}
            myMemberId={myMemberId}
          />
        </div>

        <OnlineUserList invitingRoomId={roomId} />
      </main>

      <AdSlot
        slot={AD_SLOTS.waitingRoomBottom}
        format="horizontal"
        labeled
        className="shrink-0 px-3 sm:px-4 pb-3 sm:pb-4"
      />
    </div>
  );
};

export default WaitingRoomPage;
