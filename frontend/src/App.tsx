import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import SignupPage from './pages/SignupPage';
import LoginPage from './pages/LoginPage';
import RoomListPage from './pages/RoomListPage';
import MyPage from './pages/MyPage';
import AdminSongPage from './pages/AdminSongPage';
import UserManagementPage from './pages/UserManagementPage';
import WaitingRoomPage from './pages/WaitingRoomPage';
import GamePage from './pages/GamePage';
import HaliGaliPage from './pages/HaliGaliPage';
import HangmanPage from './pages/HangmanPage';
import HangmanResultPage from './pages/HangmanResultPage';
import ResultPage from './pages/ResultPage';
import { useGameLogic } from './hooks/useGameLogic';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { useEffect } from 'react';

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
    haliGaliStatus,
    sendHaliGaliAction,
    isBootstrapping,
    isCreatingRoom,
    enterLobby,
    joinRoom,
    createRoom,
    leaveRoom,
    returnToLobby,
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
  } = useGameLogic();

  const { isAuthenticated, isInitialLoading, user } = useAuth();

  // 로그인한 사용자의 닉네임을 기존 게임 로직에 연동
  useEffect(() => {
    if (isAuthenticated && user && nickname !== user.nickname) {
      enterLobby(user.nickname);
    }
  }, [isAuthenticated, user, nickname, enterLobby]);

  const statusToPath = (s: typeof status) => {
    switch (s) {
      case 'LOBBY':
        return '/rooms';
      case 'ROOM_LIST':
        return '/rooms';
      case 'WAITING':
        return '/waiting';
      case 'PLAYING':
        if (gameType === 'HALLIGALLI') return '/haligali';
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
    return (
      <div className="w-full min-h-screen flex flex-col items-center justify-center gap-4 bg-black">
        <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
        <p className="text-primary text-xs font-mono uppercase tracking-widest animate-pulse">
          시스템 초기화 중...
        </p>
      </div>
    );
  }

  const isAdmin = user?.role === 'ADMIN' || user?.role === 'MASTER';
  const isMaster = user?.role === 'MASTER';

  return (
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

      {/* 보호된 경로 */}
      <Route
        path="/rooms"
        element={
          isAuthenticated ? (
            isCreatingRoom ? (
              <div className="w-full min-h-screen flex flex-col items-center justify-center gap-4 bg-black">
                <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin" />
                <p className="text-primary text-xs font-mono uppercase tracking-widest animate-pulse">
                  방 생성 중...
                </p>
              </div>
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
              onSendMessage={sendMessage}
              maxPlayers={roomMaxPlayers}
            />
          ) : (
            <Navigate to={isAuthenticated ? currentPath : "/login"} replace />
          )
        }
      />
      <Route
        path="/game"
        element={
          isAuthenticated && status === 'PLAYING' && gameType !== 'HALLIGALLI' ? (
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
        path="/haligali"
        element={
          isAuthenticated && status === 'PLAYING' && gameType === 'HALLIGALLI' ? (
            <HaliGaliPage
              haliGaliStatus={haliGaliStatus}
              onHaliGaliAction={sendHaliGaliAction}
              myNickname={nickname}
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
              myNickname={nickname}
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
            <HangmanResultPage rankings={players} onBackToLobby={returnToLobby} />
          ) : (
            <Navigate to={isAuthenticated ? currentPath : "/login"} replace />
          )
        }
      />
      <Route
        path="/result"
        element={
          isAuthenticated && status === 'RESULT' && gameType !== 'HANGMAN' ? (
            <ResultPage rankings={players} onBackToLobby={returnToLobby} />
          ) : (
            <Navigate to={isAuthenticated ? currentPath : "/login"} replace />
          )
        }
      />

      {/* 기본 경로 */}
      <Route path="/" element={<Navigate to={currentPath} replace />} />
      <Route path="*" element={<Navigate to={currentPath} replace />} />
    </Routes>
  );
}

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <AppContent />
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;
