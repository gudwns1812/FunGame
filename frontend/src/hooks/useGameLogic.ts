import { useState, useCallback, useEffect, useRef } from 'react';
import axios from 'axios';
import type {
  Player,
  GameStatus,
  Room,
  GameStartInfo,
  RoundEndInfo,
  HangmanStatus,
  RankingEntry,
  RoomSettings,
  RoomState,
  CreateRoomInput,
} from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { PLAYER_COLOR_INDEX_KEY } from '../utils/playerColor';
import { roomChat, roomTopic } from '../utils/stompDestination';
import { playSound } from '../utils/sound';
import { useSse } from '../contexts/SseContext';
import { useStomp, type StompChannel } from '../contexts/StompContext';
import type { RoomInvite } from '../types/presence';

// Configure axios base URL
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL;
axios.defaults.withCredentials = true; // 세션 인증을 위해 추가

/** 행맨에서 단어를 완성했을 때의 HANGMAN_ACTION result */
const HANGMAN_SOLVED_RESULT = 'CORRECT';
/** 내 회원 번호를 담아두는 로컬스토리지 키. 방·게임의 모든 "나" 판정이 이 값으로 이뤄진다 */
const MY_MEMBER_ID_KEY = 'ums_member_id';
const KICKED_NOTICE = '방장이 회원님을 방에서 내보냈습니다.';

const toRooms = (rawRooms: any[]): Room[] =>
  rawRooms.map((room) => ({
    id: room.roomId,
    name: room.title,
    hostMemberId: room.hostMemberId,
    hostName: room.hostNickname,
    playerCount: room.currentPlayers,
    maxPlayers: room.maxPlayers,
    status: room.status || 'WAITING',
    gameType: room.gameType,
    csDifficulty: room.csDifficulty,
  }));

const pushedRoomsOf = (data: string): Room[] | null => {
  try {
    const parsed = JSON.parse(data);
    return Array.isArray(parsed) ? toRooms(parsed) : null;
  } catch {
    return null;
  }
};

export const useGameLogic = () => {
  const { onEvent: onSseEvent } = useSse();
  const { onConnection, publish } = useStomp();
  const [myMemberId, setMyMemberId] = useState<number | null>(() => {
    const saved = localStorage.getItem(MY_MEMBER_ID_KEY);
    return saved ? Number(saved) : null;
  });
  const [nickname, setNickname] = useState(() => localStorage.getItem('ums_nickname') || '');
  const [roomId, setRoomId] = useState<string | null>(() => localStorage.getItem('ums_roomId'));
  const [status, setStatus] = useState<GameStatus>(() => {
    const savedStatus = localStorage.getItem('ums_status');
    const savedNickname = localStorage.getItem('ums_nickname');
    return (savedStatus as GameStatus) || (savedNickname ? 'ROOM_LIST' : 'LOBBY');
  });
  const [players, setPlayers] = useState<Player[]>([]);
  const [rooms, setRooms] = useState<Room[]>([]);
  const [timeLeft, setTimeLeft] = useState(30);
  const [totalTime, setTotalTime] = useState(30);
  const [logs, setLogs] = useState<string[]>(() => {
    const savedLogs = localStorage.getItem('ums_logs');
    return savedLogs ? JSON.parse(savedLogs) : [];
  });
  const [currentVideoId, setCurrentVideoId] = useState(() => localStorage.getItem('ums_currentVideoId') || '');
  const [playerIndex, setPlayerIndex] = useState<number | null>(null);
  const [gameStartInfo, setGameStartInfo] = useState<GameStartInfo | null>(null);
  const [roomMaxPlayers, setRoomMaxPlayers] = useState<number>(12);
  const [roomName, setRoomName] = useState<string>(() => localStorage.getItem('ums_roomName') || '');
  const [gameType, setGameType] = useState<string | null>(() => localStorage.getItem('ums_gameType'));
  const gameTypeRef = useRef<string | null>(gameType);
  const [roundEndInfo, setRoundEndInfo] = useState<RoundEndInfo | null>(null);
  const [roundIndex, setRoundIndex] = useState<number>(0);
  const [isMusicStart, setIsMusicStart] = useState(false);
  const [currentRound, setCurrentRound] = useState<number>(0);
  const [totalRound, setTotalRound] = useState<number>(0);
  const [hint, setHint] = useState<string>('');
  const [hangmanStatus, setHangmanStatus] = useState<HangmanStatus | null>(null);
  const [roomSettings, setRoomSettings] = useState<RoomSettings | null>(null);
  const [kickedNotice, setKickedNotice] = useState<string | null>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [isCreatingRoom, setIsCreatingRoom] = useState(false);
  const [myColorIndex, setMyColorIndex] = useState<number | null>(() => {
    const saved = localStorage.getItem(PLAYER_COLOR_INDEX_KEY);
    return saved !== null ? Number(saved) : null;
  });

  const hostMemberId = players.find((player) => player.isHost)?.memberId ?? roomSettings?.hostMemberId ?? null;
  const isHost = myMemberId !== null && hostMemberId === myMemberId;

  // 제목없는 음원으로 미디어 플레이어 제목 가리기

  const fetchRankRef = useRef<() => Promise<void>>(async () => { });
  const hasBootstrapped = useRef(false);
  const enterRoomChannelRef = useRef<(channel: StompChannel, targetRoomId: string, rejoinFirst: boolean) => Promise<void>>(
    async () => { },
  );
  /** 방 토픽 구독을 끊는 함수. 퇴장은 구독을 먼저 끊고 leave API 를 보낸다 */
  const unsubscribeRoom = useRef<(() => void) | null>(null);
  /** 사용자가 방금 join 한 방. 이 방의 첫 구독에서는 join 을 다시 하지 않는다 */
  const justJoinedRoom = useRef<string | null>(null);
  const returnToLobbyRef = useRef<() => Promise<void>>(async () => { });
  const statusRef = useRef<GameStatus>(status);
  /** 지금 그리고 있는 방 상태의 버전. 방을 떠나거나 방 아닌 것을 그리면 -1 로 되돌린다 */
  const roomVersion = useRef(-1);

  /** 상태를 바꾸면서 ref 도 같이 맞춘다. onConnect 가 어떤 스냅샷을 읽을지 이 값으로 정한다 */
  const enterStatus = useCallback((next: GameStatus) => {
    statusRef.current = next;
    setStatus(next);
  }, []);

  const addLog = useCallback((msg: string) => {
    setLogs((prev) => [...prev.slice(-49), msg]);
  }, []);

  const clearLogs = useCallback(() => {
    setLogs([]);
  }, []);

  /** 연결은 서비스 단위로 유지하고 방 토픽 구독만 끊는다 */
  const disconnectRoom = useCallback(() => {
    unsubscribeRoom.current?.();
    unsubscribeRoom.current = null;
  }, []);

  const clearRoomState = useCallback(() => {
    roomVersion.current = -1;
    setRoomId(null);
    setRoomName('');
    setStatus('ROOM_LIST');
    setPlayers([]);
    setRoomSettings(null);
    setPlayerIndex(null);
    setGameStartInfo(null);
    setRoundEndInfo(null);
    setGameType(null);
    gameTypeRef.current = null;
    setHint('');
    setCurrentVideoId('');
    localStorage.removeItem('ums_currentVideoId');
    localStorage.removeItem(PLAYER_COLOR_INDEX_KEY);
    setMyColorIndex(null);
  }, []);

  const forgetRoom = useCallback(() => {
    disconnectRoom();
    clearRoomState();
  }, [disconnectRoom, clearRoomState]);

  /** 이벤트로 실려 온 방 상태든 스냅샷으로 읽은 방 상태든 같은 자리에서 적용한다 */
  const applyRoomState = useCallback((room: RoomState | null | undefined) => {
    if (!room || room.version <= roomVersion.current) return;
    roomVersion.current = room.version;

    setPlayers((prev) => {
      const scoreOf = new Map(prev.map((player) => [player.memberId, player.score]));
      return room.players.map((player, slotIndex) => ({
        memberId: player.memberId,
        name: player.nickname,
        isHost: player.memberId === room.hostMemberId,
        isReady: player.isReady,
        score: scoreOf.get(player.memberId) ?? 0,
        colorIndex: slotIndex,
      }));
    });
  }, []);

  const fetchRoomState = useCallback(
    async (targetRoomId: string) => {
      try {
        const response = await axios.get(`/game/rooms/${targetRoomId}/users`);
        if (response.data?.result === 'SUCCESS') {
          applyRoomState(response.data.data);
        }
      } catch (error: any) {
        console.error('Failed to fetch room state:', error);
        const errorCode = error?.response?.data?.error?.code;
        if (errorCode === 'G002' || errorCode === 'G008') {
          window.alert('방이 종료되었거나 더 이상 존재하지 않습니다.');
          returnToLobbyRef.current();
        }
      }
    },
    [applyRoomState],
  );

  const applyRoomSettings = useCallback((settings: RoomSettings) => {
    setRoomSettings(settings);
    setRoomMaxPlayers(settings.maxPlayers);
    setRoomName(settings.title);
  }, []);

  const fetchRoomSettings = useCallback(
    async (targetRoomId: string) => {
      try {
        const response = await axios.get(`/game/rooms/${targetRoomId}/settings`);
        if (response.data?.result === 'SUCCESS') {
          applyRoomSettings(response.data.data);
        }
      } catch (error) {
        console.error('Failed to fetch room settings:', error);
      }
    },
    [applyRoomSettings],
  );

  const changeRoomSettings = useCallback(
    async (changes: Omit<RoomSettings, 'title' | 'hostMemberId' | 'hostNickname'>) => {
      if (!roomId) return;
      try {
        const response = await axios.patch(`/game/rooms/${roomId}/settings`, changes);
        if (response.data?.result === 'SUCCESS') {
          applyRoomSettings(response.data.data);
        }
      } catch (error: any) {
        window.alert(error?.response?.data?.error?.message ?? '방 설정을 바꾸지 못했습니다.');
      }
    },
    [roomId, applyRoomSettings],
  );

  const handleEvent = useCallback(
    (event: any) => {
      switch (event.type) {
        case 'ROOM_SETTINGS_CHANGED':
          applyRoomSettings(event.settings);
          applyRoomState(event.room);
          addLog('[시스템] 방장이 방 설정을 변경했습니다. 준비 상태가 초기화되었습니다.');
          break;

        case 'PLAYER_JOIN':
        case 'PLAYER_LEAVE': {
          applyRoomState(event.room);
          if (event.memberId === myMemberId && event.type === 'PLAYER_JOIN') break;
          const action = event.type === 'PLAYER_JOIN' ? '입장' : '퇴장';
          addLog(`[시스템] ${stripTag(event.nickname)}님이 ${action}하셨습니다.`);
          break;
        }

        case 'PLAYER_KICKED':
          if (event.memberId === myMemberId) {
            setKickedNotice(KICKED_NOTICE);
            forgetRoom();
            break;
          }
          applyRoomState(event.room);
          addLog(`[시스템] ${stripTag(event.nickname)}님이 방장에게 내보내졌습니다.`);
          break;

        case 'PLAYER_READY':
          applyRoomState(event.room);
          break;

        case 'CHAT': {
          const sender = event.nickname || '알 수 없음';
          const msg = event.message || '';
          addLog(`${stripTag(sender)}: ${msg}`);
          break;
        }

        case 'GAME_START': {
          setStatus('PLAYING');
          setHint('');
          setRoundIndex(0);
          setCurrentRound(0);
          setPlayers((prev) => prev.map((player) => ({ ...player, score: 0 })));
          const normalizedGameType =
            event.gameType === 'CS' ? 'CS' : event.gameType === 'HANGMAN' ? 'HANGMAN' : 'SONG';
          setGameType(normalizedGameType);
          gameTypeRef.current = normalizedGameType;
          setGameStartInfo({
            gameType: normalizedGameType,
            category: event.category,
            songCount: event.songCount,
            message: event.message,
          });

          // 행맨인 경우 즉시 초기 상태 설정 (로딩창 방지)
          if (normalizedGameType === 'HANGMAN') {
            setHangmanStatus({
              currentDisplay: '',
              wrongLetters: [],
              remainingTries: 6,
              currentTurnPlayer: '대기 중...',
              currentTurnMemberId: null,
              isGameOver: false,
              isWin: false,
            });
          }

          setLogs([]);
          break;
        }

        case 'ROUND_START':
          setStatus('PLAYING');
          setHint('');

          if (gameTypeRef.current === 'HANGMAN') {
            setHangmanStatus((prev) => {
              if (prev && prev.currentTurnPlayer !== '대기 중...' && prev.currentTurnPlayer !== '불러오는 중...') {
                return prev;
              }
              return {
                currentDisplay: event.content || '',
                wrongLetters: [],
                remainingTries: 6,
                currentTurnPlayer: '불러오는 중...',
                currentTurnMemberId: null,
                isGameOver: false,
                isWin: false,
              };
            });
          } else {
            setCurrentVideoId(event.content);
          }

          setRoundEndInfo(null);
          setGameStartInfo(null);
          setRoundIndex(event.round);
          setCurrentRound(event.round);
          setTotalRound(event.totalRound);
          addLog(`================================================================================`);
          break;

        case 'TIMER_TICK':
          setTimeLeft(event.remainingSeconds);
          setTotalTime(30);
          break;

        case 'ROUND_HINT':
          if (event.hint && event.hint.trim() !== '') {
            setHint(event.hint);
          }
          break;

        case 'CORRECT_ANSWER':
          setPlayers((prev) => prev.map((p) => (p.memberId === event.memberId ? { ...p, score: event.score } : p)));
          break;

        case 'ROUND_SKIP':
          break;

        case 'ROUND_END': {
          setHint('');
          const isCsRound = gameTypeRef.current === 'CS';
          if (event.winnerMemberId !== null) {
            playSound('correctAnswer');
          }
          setRoundEndInfo({
            answer: event.answer,
            explanation: isCsRound && event.explanation?.trim() ? event.explanation : null,
            winner: event.winnerNickname,
          });

          fetchRankRef.current();
          break;
        }

        case 'HANGMAN_ACTION': {
          const s = event.status;
          if (event.result === HANGMAN_SOLVED_RESULT) {
            playSound('correctAnswer');
          }
          setHangmanStatus({
            currentDisplay: s[0],
            wrongLetters: s[1] ? s[1].split(',') : [],
            remainingTries: parseInt(s[2], 10),
            currentTurnPlayer: s[3],
            currentTurnMemberId: s[6] ? Number(s[6]) : null,
            isGameOver: s[4] === 'true',
            isWin: s[5] === 'true',
          });
          break;
        }

        case 'GAME_RESULT': {
          setStatus('RESULT');
          roomVersion.current = -1;
          setIsMusicStart(false);
          setHint('');
          setPlayerIndex(null);
          setGameStartInfo(null);
          setRoundEndInfo(null);

          if (event.answer && event.score !== undefined) {
            // 행맨 결과 처리
            addLog(`[게임 종료] 정답: ${event.answer}`);
            addLog(`[게임 종료] 최종 점수(남은 기회): ${event.score}`);
            setPlayers([
              { memberId: myMemberId ?? 0, name: nickname, score: event.score, isHost: false, isReady: false },
            ]);
          } else if (event.rankings?.length) {
            // 기존 퀴즈 결과 처리
            const finalRankings: Player[] = event.rankings.map((entry: RankingEntry) => ({
              memberId: entry.memberId ?? 0,
              name: entry.nickname,
              score: entry.score,
              isHost: false,
              isReady: false,
            }));
            setPlayers(finalRankings);
          }
          break;
        }
      }
    },
    [addLog, nickname, myMemberId, applyRoomState, forgetRoom, applyRoomSettings],
  );

  const handleEventRef = useRef(handleEvent);

  useEffect(() => {
    handleEventRef.current = handleEvent;
  }, [handleEvent]);

  useEffect(() => {
    localStorage.setItem('ums_logs', JSON.stringify(logs));
  }, [logs]);


  /** 입장이 join API → subscribe 였으니 퇴장은 역순으로 unsubscribe → leave API 다 */
  const leaveRoom = useCallback(async () => {
    disconnectRoom();

    if (roomId) {
      try {
        await axios.post(`/game/rooms/${roomId}/leave`);
      } catch (error) {
        console.error('Leave room failed:', error);
      }
    }

    clearRoomState();
  }, [roomId, disconnectRoom, clearRoomState]);

  const kickPlayer = useCallback(
    async (targetMemberId: number) => {
      if (!roomId) return;

      try {
        await axios.post(`/game/rooms/${roomId}/kick`, { targetMemberId });
      } catch (error: any) {
        window.alert(error?.response?.data?.error?.message ?? '플레이어를 내보내지 못했습니다.');
      }
    },
    [roomId],
  );

  const dismissKickedNotice = useCallback(() => {
    setKickedNotice(null);
  }, []);

  const returnToLobby = leaveRoom;

  useEffect(() => {
    returnToLobbyRef.current = returnToLobby;
  }, [returnToLobby]);

  const returnToWaitingRoom = useCallback(async () => {
    setGameStartInfo(null);
    setRoundEndInfo(null);
    setHint('');
    setCurrentVideoId('');
    localStorage.removeItem('ums_currentVideoId');
    clearLogs();
    localStorage.removeItem('ums_logs');
    setStatus('WAITING');

    if (roomId) {
      // 결과 화면은 방 참가자가 아니라 순위를 그렸으므로 방 상태를 처음부터 다시 읽는다
      roomVersion.current = -1;
      await fetchRoomState(roomId);
      await fetchRoomSettings(roomId);
    }
  }, [roomId, fetchRoomState, fetchRoomSettings, clearLogs]);

  /**
   * 진행 중인 게임에 재입장했을 때, 놓친 라운드 상태를 서버 스냅샷으로 복원한다.
   * 웹소켓만 다시 붙으면 다음 라운드까지 화면이 비어 있게 되므로 반드시 선행돼야 한다.
   */
  const restorePlayState = useCallback(async (targetRoomId: string) => {
    const response = await axios.get(`/game/rooms/${targetRoomId}/play/state`);
    if (response.data?.result !== 'SUCCESS' || !response.data.data) return;

    const state = response.data.data;
    const restoredGameType =
      state.gameType === 'CS' ? 'CS' : state.gameType === 'HANGMAN' ? 'HANGMAN' : 'SONG';

    setGameType(restoredGameType);
    gameTypeRef.current = restoredGameType;
    setGameStartInfo(null);
    setRoundEndInfo(null);
    setHint('');
    setCurrentRound(state.currentRound);
    setRoundIndex(state.currentRound);
    setTotalRound(state.totalRound);

    if (restoredGameType === 'HANGMAN') {
      const data: string[] = state.statusData ?? [];
      setHangmanStatus({
        currentDisplay: data[0] ?? '',
        wrongLetters: data[1] ? data[1].split(',') : [],
        remainingTries: parseInt(data[2] ?? '6', 10),
        currentTurnPlayer: data[3] ?? '대기 중...',
        currentTurnMemberId: data[6] ? Number(data[6]) : null,
        isGameOver: data[4] === 'true',
        isWin: data[5] === 'true',
      });
    } else if (state.content) {
      setCurrentVideoId(state.content);
    }

    // 재입장자는 이전 점수를 그대로 이어받으므로 현재 순위도 함께 복원한다.
    const rankResponse = await axios.get(`/game/rooms/${targetRoomId}/play/rank`);
    const rankData: RankingEntry[] = rankResponse.data?.data ?? [];
    if (rankData.length > 0) {
      setPlayers(
        rankData.map(({ memberId, nickname: name, score }) => ({
          memberId: memberId ?? 0,
          name,
          isHost: false,
          isReady: false,
          score,
        })),
      );
    }

    setStatus('PLAYING');
    addLog('[알림] 진행 중인 게임에 다시 입장했습니다.');
  }, [addLog]);

  /**
   * 방 소속은 API 가 정하고, 구독은 수신 채널일 뿐이다. 그래서 순서가 하나로 정해진다.
   * join API 로 소속을 확정하고 → 그 다음 구독하고 → 마지막에 스냅샷으로 빈 구간을 메운다.
   * join 과 구독 사이에 발행된 이벤트는 스트림에서 유실되지만 뒤이어 읽는 스냅샷이 메운다.
   */
  const enterRoomChannel = useCallback(
    async (channel: StompChannel, targetRoomId: string, rejoinFirst: boolean) => {
      try {
        if (rejoinFirst) {
          await axios.post(`/game/rooms/${targetRoomId}/join`);
        }

        unsubscribeRoom.current = channel.subscribe(roomTopic(targetRoomId), (payload) =>
          handleEventRef.current(payload),
        );

        if (statusRef.current === 'PLAYING') {
          await restorePlayState(targetRoomId);
          return;
        }

        await fetchRoomState(targetRoomId);
        await fetchRoomSettings(targetRoomId);
      } catch (error: any) {
        console.error('Failed to enter room channel:', error);
        const message =
          error?.response?.data?.error?.message || '연결이 끊긴 사이 방에서 나가게 되었습니다.';
        window.alert(message);
        returnToLobby();
      }
    },
    [fetchRoomState, fetchRoomSettings, restorePlayState, returnToLobby],
  );

  /**
   * 방에 들어가 있는 동안은 연결이 맺어질 때마다 방 채널을 처음부터 다시 세운다.
   * 방금 join 한 방이 아니라면(새로고침·재연결) 구독 전에 소속부터 다시 확정한다.
   */
  useEffect(() => {
    enterRoomChannelRef.current = enterRoomChannel;
  }, [enterRoomChannel]);

  useEffect(() => {
    if (!roomId) return;

    return onConnection((channel) => {
      const alreadyJoined = justJoinedRoom.current === roomId;
      justJoinedRoom.current = null;

      return enterRoomChannelRef.current(channel, roomId, !alreadyJoined);
    });
  }, [roomId, onConnection]);

  useEffect(() => {
    statusRef.current = status;
  }, [status]);

  useEffect(() => {
    localStorage.setItem('ums_status', status);
    if (roomId) {
      localStorage.setItem('ums_roomId', roomId);
    } else {
      localStorage.removeItem('ums_roomId');
    }
    if (roomName) {
      localStorage.setItem('ums_roomName', roomName);
    } else {
      localStorage.removeItem('ums_roomName');
    }
  }, [status, roomId, roomName]);

  // 방 안에서 새로고침했다면 join → subscribe → 스냅샷은 방 채널 이펙트가 전부 맡는다.
  // 여기서는 방 없이 방 화면으로 돌아온 경우만 바로잡는다.
  useEffect(() => {
    // StrictMode 는 개발 모드에서 마운트 이펙트를 두 번 실행한다.
    if (hasBootstrapped.current) return;
    hasBootstrapped.current = true;

    if ((status === 'WAITING' || status === 'PLAYING') && roomId === null) {
      enterStatus('ROOM_LIST');
    }
    setIsBootstrapping(false);
  }, []); // Run once on mount

  useEffect(() => {
    const handlePopState = () => {
      if (status === 'WAITING' || status === 'RESULT') {
        leaveRoom();
      }
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, [status, leaveRoom]);

  useEffect(() => {
    if (gameType) localStorage.setItem('ums_gameType', gameType);
    else localStorage.removeItem('ums_gameType');
  }, [gameType]);

  useEffect(() => {
    if (currentVideoId) localStorage.setItem('ums_currentVideoId', currentVideoId);
    else localStorage.removeItem('ums_currentVideoId');
  }, [currentVideoId]);

  const fetchRooms = useCallback(async () => {
    try {
      const response = await axios.get('/game/rooms');
      if (response.data && response.data.result === 'SUCCESS') {
        setRooms(toRooms(response.data.data));
      }
    } catch (error) {
      console.error('Failed to fetch rooms:', error);
    }
  }, []);

  useEffect(() => {
    if (status !== 'ROOM_LIST') return;

    let debounceTimer: ReturnType<typeof setTimeout>;
    const debouncedFetchRooms = () => {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        fetchRooms();
      }, 300);
    };

    const applyPushedRooms = (event: MessageEvent) => {
      const pushedRooms = pushedRoomsOf(event.data);
      if (!pushedRooms) {
        debouncedFetchRooms();
        return;
      }

      setRooms(pushedRooms);
    };

    const resyncOnTabReturn = () => {
      if (document.visibilityState !== 'visible') return;
      debouncedFetchRooms();
    };

    fetchRooms();
    const stopListeningRoomUpdate = onSseEvent('room-update', applyPushedRooms);
    const stopListeningConnected = onSseEvent('connected', debouncedFetchRooms);
    document.addEventListener('visibilitychange', resyncOnTabReturn);

    return () => {
      document.removeEventListener('visibilitychange', resyncOnTabReturn);
      stopListeningRoomUpdate();
      stopListeningConnected();
      clearTimeout(debounceTimer);
    };
  }, [status, fetchRooms, onSseEvent]);

  /**
   * 로그인한 사람이 누구인지 게임 로직에 알린다.
   * 방 안에서 새로고침한 경우에도 불리므로 화면 상태는 건드리지 않는다.
   */
  const identify = useCallback((memberId: number, name: string) => {
    localStorage.setItem(MY_MEMBER_ID_KEY, String(memberId));
    localStorage.setItem('ums_nickname', name);
    setMyMemberId(memberId);
    setNickname(name);
  }, []);

  const enterLobby = useCallback(
    (memberId: number, name: string) => {
      identify(memberId, name);
      setStatus('ROOM_LIST');
    },
    [identify],
  );

  const enterRoom = useCallback(
    (room: Room, slotIndex: number | null) => {
      setRoomMaxPlayers(room.maxPlayers);
      setRoomName(room.name);
      if (slotIndex !== null) {
        localStorage.setItem(PLAYER_COLOR_INDEX_KEY, String(slotIndex));
        setMyColorIndex(slotIndex);
      }
      clearLogs();
      localStorage.removeItem('ums_logs');
      roomVersion.current = -1;
      justJoinedRoom.current = room.id;
      setRoomId(room.id);

      if (room.status === 'PLAYING') {
        // 서버가 재입장을 허용한 경우에만 여기까지 온다. 라운드 상태는 구독 뒤 스냅샷이 복원한다.
        enterStatus('PLAYING');
      } else {
        enterStatus('WAITING');
        setCurrentVideoId(''); // 이전 비디오 아이디 초기화
        setHint('');
        localStorage.removeItem('ums_currentVideoId');
        setPlayers([
          {
            memberId: myMemberId ?? 0,
            name: nickname,
            isHost: room.hostMemberId === myMemberId,
            isReady: room.hostMemberId === myMemberId,
            score: 0,
            colorIndex: slotIndex ?? undefined,
          },
        ]);
        addLog(`[시스템] ${room.name} 방에 입장했습니다.`);
      }

      window.history.pushState({ room: room.id }, '');
    },
    [myMemberId, nickname, clearLogs, addLog, enterStatus],
  );

  const acceptInvite = useCallback(
    async (invite: RoomInvite) => {
      try {
        const response = await axios.post(`/api/invites/${invite.inviteId}/accept`);
        if (response.data.result === 'SUCCESS') {
          const { room, playerSequence } = response.data.data;
          enterRoom(
            {
              id: String(room.roomId),
              name: room.title,
              hostMemberId: room.hostMemberId,
              hostName: room.hostNickname,
              playerCount: room.currentPlayers,
              maxPlayers: room.maxPlayers,
              status: room.status,
              gameType: room.gameType,
              csDifficulty: room.csDifficulty,
            },
            typeof playerSequence === 'number' ? playerSequence : null,
          );
        }
      } catch (error: any) {
        console.error('Accept invite failed:', error);
        window.alert(error?.response?.data?.error?.message || '방에 입장할 수 없습니다.');
      }
    },
    [enterRoom],
  );

  const joinRoom = useCallback(
    async (room: Room) => {
      try {
        const response = await axios.post(`/game/rooms/${room.id}/join`);
        if (response.data.result === 'SUCCESS') {
          const slotIndex = typeof response.data.data === 'number' ? response.data.data : null;
          enterRoom(room, slotIndex);
        }
      } catch (error: any) {
        console.error('Join room failed:', error);
        const httpStatus = error?.response?.status;
        const redirectRoomId = error?.response?.data?.data?.redirectRoomId ?? error?.response?.data?.redirectRoomId;
        if (httpStatus === 409 && redirectRoomId) {
          roomVersion.current = -1;
          setRoomId(redirectRoomId);
          setRoomName('');
          enterStatus('PLAYING');
          setPlayers([{ memberId: myMemberId ?? 0, name: nickname, isHost: false, isReady: false, score: 0 }]);
          return;
        }
        const message = error?.response?.data?.error?.message || '방에 입장할 수 없습니다.';
        window.alert(message);
      }
    },
    [myMemberId, nickname, enterRoom, enterStatus],
  );

  const createRoom = useCallback(
    async ({ title, maxPlayers, category, totalRound, gameType, difficulty, csDifficulty }: CreateRoomInput) => {
      setIsCreatingRoom(true);
      try {
        const response = await axios.post('/game/rooms', {
          title,
          maxPlayers,
          category,
          totalRound,
          gameType,
          difficulty,
          csDifficulty,
        });
        if (response.data.result === 'SUCCESS') {
          const newRoomId = response.data.data;
          setRoomMaxPlayers(maxPlayers);
          setRoomName(title);
          localStorage.setItem(PLAYER_COLOR_INDEX_KEY, '0');
          setMyColorIndex(0);
          clearLogs();
          localStorage.removeItem('ums_logs');
          roomVersion.current = -1;
          justJoinedRoom.current = newRoomId;
          setRoomId(newRoomId);
          enterStatus('WAITING');
          setCurrentVideoId(''); // 이전 비디오 아이디 초기화
          setHint('');
          localStorage.removeItem('ums_currentVideoId');
          setPlayers([
            { memberId: myMemberId ?? 0, name: nickname, isHost: true, isReady: true, score: 0, colorIndex: 0 },
          ]);
          window.history.pushState({ room: newRoomId }, '');
        }
      } catch (error) {
        console.error('Create room failed:', error);
        addLog('[오류] 방 생성에 실패했습니다.');
      } finally {
        setIsCreatingRoom(false);
      }
    },
    [myMemberId, nickname, addLog, clearLogs, enterStatus],
  );

  const toggleReady = useCallback(async () => {
    if (!roomId) return;
    try {
      const response = await axios.post(`/game/rooms/${roomId}/ready`);
      if (response.data.result === 'SUCCESS') {
        const { memberId, ready } = response.data.data;
        setPlayers((prev) => prev.map((p) => (p.memberId === memberId ? { ...p, isReady: ready } : p)));
      }
    } catch (error: any) {
      console.error('Toggle ready failed:', error);
      const message = error?.response?.data?.error?.message || '준비 상태 변경에 실패했습니다.';
      window.alert(message);
    }
  }, [roomId]);

  const startGame = useCallback(async () => {
    if (!roomId || !isHost) return;
    try {
      const response = await axios.post(`/game/rooms/${roomId}/start`);
      if (response.data.result === 'FAIL') {
        window.alert(response.data.error.message);
      }
    } catch (error: any) {
      console.error('Start game failed:', error);
      const message = error?.response?.data?.error?.message || '게임 시작에 실패했습니다.';
      window.alert(message);
    }
  }, [roomId, isHost, nickname]);

  const skipRound = useCallback(async () => {
    if (!roomId) return;
    try {
      await axios.post(`/game/rooms/${roomId}/skip`);
    } catch (error) {
      console.error('Skip vote failed:', error);
    }
  }, [roomId, nickname]);

  const fetchRank = useCallback(async () => {
    if (!roomId) return;
    try {
      const response = await axios.get(`/game/rooms/${roomId}/play/rank`);
      if (response.data?.result === 'SUCCESS' && Array.isArray(response.data.data)) {
        const rankData: RankingEntry[] = response.data.data;
        setPlayers((prev) => {
          const prevMap = new Map(prev.map((p) => [p.memberId, p]));
          return rankData.map(({ memberId, nickname: name, score }) => ({
            memberId: memberId ?? 0,
            name,
            isHost: prevMap.get(memberId ?? 0)?.isHost ?? false,
            isReady: prevMap.get(memberId ?? 0)?.isReady ?? false,
            colorIndex: prevMap.get(memberId ?? 0)?.colorIndex,
            score,
          }));
        });
      }
    } catch (error: any) {
      console.error('Failed to fetch rank:', error);
      const errorCode = error?.response?.data?.error?.code;
      if (errorCode === 'G002' || errorCode === 'G008') {
        window.alert('방이 종료되었거나 더 이상 존재하지 않습니다.');
        returnToLobby();
      }
    }
  }, [roomId, nickname, returnToLobby]);

  useEffect(() => {
    fetchRankRef.current = fetchRank;
  }, [fetchRank]);

  const changeNickname = useCallback((newName: string) => {
    localStorage.setItem('ums_nickname', newName);
    setNickname(newName);
  }, []);

  const sendMessage = useCallback(
    (message: string) => {
      if (!roomId) return;
      publish(roomChat(roomId), { message });
    },
    [roomId, publish],
  );

  const sendHangmanAction = useCallback(
    async (letter: string) => {
      if (!roomId) return;
      try {
        await axios.post(`/game/rooms/${roomId}/action`, {
          type: 'SUBMIT_ANSWER',
          value: letter,
        });
      } catch (error) {
        console.error('Hangman action failed:', error);
      }
    },
    [roomId, nickname],
  );

  return {
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
    playerIndex,
    gameStartInfo,
    gameType,
    roundEndInfo,
    roundIndex,
    currentRound,
    totalRound,
    hint,
    isBootstrapping,
    isCreatingRoom,
    myColorIndex,
    isMusicStart,
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
    setStatus,
    addLog,
    clearLogs,
    changeNickname,
    fetchRooms,
    fetchRank,
    hangmanStatus,
    sendHangmanAction,
    roomMaxPlayers,
    roomName,
  };
};
