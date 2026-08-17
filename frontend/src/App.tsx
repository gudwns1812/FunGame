import { BrowserRouter, Routes, Route, Navigate, useLocation } from 'react-router-dom';
import LandingPage from './pages/LandingPage';
import SignupPage from './pages/SignupPage';
import LoginPage from './pages/LoginPage';
import ForgotPasswordPage from './pages/ForgotPasswordPage';
import ResetPasswordPage from './pages/ResetPasswordPage';
import RoomListPage from './pages/RoomListPage';
import MyPage from './pages/MyPage';
import AdminSongPage from './pages/AdminSongPage';
import UserManagementPage from './pages/UserManagementPage';
import WaitingRoomPage from './pages/WaitingRoomPage';
import GamePage from './pages/GamePage';
import HangmanPage from './pages/HangmanPage';
import HangmanResultPage from './pages/HangmanResultPage';
import ResultPage from './pages/ResultPage';
import { useGameLogic } from './hooks/useGameLogic';
import { useButtonClickSound } from './hooks/useButtonClickSound';
import type { GameStatus } from './types/game';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { StompProvider } from './contexts/StompContext';
import { useRoomInvites } from './hooks/useRoomInvites';
import InviteToast from './components/InviteToast';
import KickedNotice from './components/KickedNotice';
import MaintenanceRoutes from './components/MaintenanceRoutes';
import { PUBLIC_PAGES } from './publicPages';
import { isMaintenanceMode } from './utils/maintenance';
import { useEffect } from 'react';

/** 방에 들어가 있는 동안은 클릭음이 게임을 방해해서 끈다 */
const IN_ROOM_STATUSES: GameStatus[] = ['WAITING', 'PLAYING', 'RESULT'];

/** 부트스트랩 · 방 생성 대기 화면 */
function LoadingScreen({ label }: { label: string }) {
  return (
    <div className="min-h-[100dvh] relative z-[1] flex flex-col items-center justify-center gap-4">
      <div className="flex items-end gap-1.5 h-10">
        {[0, 1, 2, 3].map((i) => (
          <span
            key={i}
            className="px-eq-bar w-3 h-full border-2 border-ink bg-cherry"
            style={{ animationDelay: `${i * 0.12}s` }}
          />
        ))}
      </div>
      <p className="px-title text-sm">{label}</p>
    </div>
  );
}

function AppContent() {
  const {
    status,
    nickname,
    roomId,
    players,
    rooms,
    timeLeft,
    totalTime,
    logs,
    currentVideoId,
    isHost,
    gameStartInfo,
    gameType,
    roundEndInfo,
    currentRound,
    totalRound,
    hint,
    isBootstrapping,
    isCreatingRoom,
    identify,
    enterLobby,
    joinRoom,
    acceptInvite,
    createRoom,
    leaveRoom,
    kickPlayer,
    kickedNotice,
    dismissKickedNotice,
    returnToLobby,
    returnToWaitingRoom,
    roomSettings,
    changeRoomSettings,
    startGame,
    toggleReady,
    skipRound,
    sendMessage,
    fetchRooms,
    fetchRank,
    changeNickname,
    hangmanStatus,
    sendHangmanAction,
    roomMaxPlayers,
    roomName,
  } = useGameLogic();

  const { isAuthenticated, isInitialLoading, user } = useAuth();
  const { currentInvite, dropInvite, declineInvite } = useRoomInvites();
  const location = useLocation();

  useButtonClickSound({ enabled: !IN_ROOM_STATUSES.includes(status) });

  // 로그인한 사용자가 누구인지 게임 로직에 알린다.
  // 방 안에서 새로고침한 경우에도 회원 번호를 잃지 않도록 화면 상태와 분리한다.
  useEffect(() => {
    if (isAuthenticated && user) {
      identify(user.id, user.nickname);
    }
  }, [isAuthenticated, user, identify]);

  // 닉네임이 아직 연동되지 않았다면 로비에서 시작한다.
  useEffect(() => {
    if (isAuthenticated && user && nickname !== user.nickname) {
      enterLobby(user.id, user.nickname);
    }
  }, [isAuthenticated, user, nickname, enterLobby]);

  const publicPage = PUBLIC_PAGES.find((page) => page.path === location.pathname);
  if (publicPage) {
    return publicPage.element;
  }

  const statusToPath = (s: typeof status) => {
    switch (s) {
      case 'LOBBY':
        return '/rooms';
      case 'ROOM_LIST':
        return '/rooms';
      case 'WAITING':
        return '/waiting';
      case 'PLAYING':
        if (gameType === 'HANGMAN') return '/hangman';
        return '/game';
      case 'RESULT':
        if (gameType === 'HANGMAN') return '/hangman-result';
        return '/result';
      default:
        return '/rooms';
    }
  };

  const currentPath = statusToPath(status);

  if (isInitialLoading || isBootstrapping) {
    return <LoadingScreen label="불러오는 중..." />;
  }

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'MASTER';
  const isMaster = user?.role === 'MASTER';

  return (
    <>
      {kickedNotice && <KickedNotice message={kickedNotice} onClose={dismissKickedNotice} />}

      {currentInvite && (
        <InviteToast
          key={currentInvite.inviteId}
          invite={currentInvite}
          onAccept={(invite) => {
            dropInvite(invite.inviteId);
            acceptInvite(invite);
          }}
          onDecline={declineInvite}
          onExpire={dropInvite}
        />
      )}

      <Routes>
      {/* 인증 관련 페이지 */}
      <Route
        path="/login"
        element={!isAuthenticated ? <LoginPage /> : <Navigate to={currentPath} replace />}
      />
      <Route
        path="/signup"
        element={!isAuthenticated ? <SignupPage /> : <Navigate to={currentPath} replace />}
      />
      <Route
        path="/forgot-password"
        element={!isAuthenticated ? <ForgotPasswordPage /> : <Navigate to={currentPath} replace />}
      />
      <Route
        path="/reset-password"
        element={!isAuthenticated ? <ResetPasswordPage /> : <Navigate to={currentPath} replace />}
      />

      {/* 보호된 경로 */}
      <Route
        path="/rooms"
        element={
          isAuthenticated ? (
            isCreatingRoom ? (
              <LoadingScreen label="방 생성 중..." />
            ) : status === 'ROOM_LIST' || status === 'LOBBY' ? (
              <RoomListPage
                rooms={rooms}
                nickname={nickname}
                onJoinRoom={joinRoom}
                onCreateRoom={createRoom}
                onRefreshRooms={fetchRooms}
                onChangeNickname={changeNickname}
              />
            ) : (
              <Navigate to={currentPath} replace />
            )
          ) : (
            <Navigate to="/login" replace />
          )
        }
      />
      <Route
        path="/mypage"
        element={
          isAuthenticated ? <MyPage /> : <Navigate to="/login" replace />
        }
      />
      <Route
        path="/admin/songs"
        element={
          isAuthenticated && isAdmin ? <AdminSongPage /> : <Navigate to="/rooms" replace />
        }
      />
      <Route
        path="/master/users"
        element={
          isAuthenticated && isMaster ? <UserManagementPage /> : <Navigate to="/rooms" replace />
        }
      />
      <Route
        path="/waiting"
        element={
          isAuthenticated && status === 'WAITING' ? (
            <WaitingRoomPage
              players={players}
              logs={logs}
              isHost={isHost}
              onStart={startGame}
              onLeave={leaveRoom}
              onToggleReady={toggleReady}
              onKickPlayer={kickPlayer}
              onSendMessage={sendMessage}
              maxPlayers={roomMaxPlayers}
              myMemberId={user?.id ?? null}
              roomSettings={roomSettings}
              onChangeSettings={changeRoomSettings}
              roomName={roomName}
              roomId={roomId}
            />
          ) : (
            <Navigate to={isAuthenticated ? currentPath : "/login"} replace />
          )
        }
      />
      <Route
        path="/game"
        element={
          isAuthenticated && status === 'PLAYING' && gameType !== 'HANGMAN' ? (
            <GamePage
              players={players}
              roomId={roomId || ''}
              timeLeft={timeLeft}
              totalTime={totalTime}
              currentVideoId={currentVideoId}
              logs={logs}
              onAnswerSubmit={sendMessage}
              onSkipRound={skipRound}
              onFetchRank={fetchRank}
              gameStartInfo={gameStartInfo}
              gameType={gameType}
              roundEndInfo={roundEndInfo}
              currentRound={currentRound}
              totalRound={totalRound}
              hint={hint}
            />
          ) : (
            <Navigate to={isAuthenticated ? currentPath : "/login"} replace />
          )
        }
      />
      <Route
        path="/hangman"
        element={
          isAuthenticated && status === 'PLAYING' && gameType === 'HANGMAN' ? (
            <HangmanPage
              status={hangmanStatus}
              onGuess={sendHangmanAction}
              myMemberId={user?.id ?? null}
              logs={logs}
              players={players}
              onSendMessage={sendMessage}
            />
          ) : (
            <Navigate to={isAuthenticated ? currentPath : "/login"} replace />
          )
        }
      />
      <Route
        path="/hangman-result"
        element={
          isAuthenticated && status === 'RESULT' && gameType === 'HANGMAN' ? (
            <HangmanResultPage rankings={players} onBackToLobby={returnToLobby} onBackToRoom={returnToWaitingRoom} />
          ) : (
            <Navigate to={isAuthenticated ? currentPath : "/login"} replace />
          )
        }
      />
      <Route
        path="/result"
        element={
          isAuthenticated && status === 'RESULT' && gameType !== 'HANGMAN' ? (
            <ResultPage rankings={players} onBackToLobby={returnToLobby} onBackToRoom={returnToWaitingRoom} />
          ) : (
            <Navigate to={isAuthenticated ? currentPath : "/login"} replace />
          )
        }
      />

      {/* 기본 경로 */}
      <Route path="/" element={isAuthenticated ? <Navigate to={currentPath} replace /> : <LandingPage />} />
      <Route path="*" element={<Navigate to={currentPath} replace />} />
      </Routes>
    </>
  );
}

function App() {
  if (isMaintenanceMode()) {
    return (
      <BrowserRouter>
        <MaintenanceRoutes />
      </BrowserRouter>
    );
  }

  return (
    <BrowserRouter>
      <AuthProvider>
        <StompProvider>
          <AppContent />
        </StompProvider>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
