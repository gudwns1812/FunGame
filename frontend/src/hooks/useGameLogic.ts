import { useState, useCallback, useEffect, useRef } from 'react';
import axios from 'axios';
import { Client, TickerStrategy } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type {
  Player,
  GameStatus,
  Room,
  GameStartInfo,
  RoundEndInfo,
  HangmanStatus,
  RankingEntry,
  RoomSettings,
} from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { PLAYER_COLOR_INDEX_KEY } from '../utils/playerColor';
import { roomChat, roomTopic } from '../utils/stompDestination';
import { playSound } from '../utils/sound';
import { useSse } from '../contexts/SseContext';
import type { RoomInvite } from '../types/presence';

// Configure axios base URL
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL;
axios.defaults.withCredentials = true; // 세션 인증을 위해 추가

/** 행맨에서 단어를 완성했을 때의 HANGMAN_ACTION result */
const HANGMAN_SOLVED_RESULT = 'CORRECT';
/** 내 회원 번호를 담아두는 로컬스토리지 키. 방·게임의 모든 "나" 판정이 이 값으로 이뤄진다 */
const MY_MEMBER_ID_KEY = 'ums_member_id';

export const useGameLogic = () => {
  const { onEvent: onSseEvent } = useSse();
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
  const [isHost, setIsHost] = useState(false);
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
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [isCreatingRoom, setIsCreatingRoom] = useState(false);
  const [myColorIndex, setMyColorIndex] = useState<number | null>(() => {
    const saved = localStorage.getItem(PLAYER_COLOR_INDEX_KEY);
    return saved !== null ? Number(saved) : null;
  });

  // 제목없는 음원으로 미디어 플레이어 제목 가리기

  const stompClient = useRef<Client | null>(null);
  const fetchRankRef = useRef<() => Promise<void>>(async () => { });
  const hasBootstrapped = useRef(false);
  const resyncMembershipRef = useRef<(targetRoomId: string) => Promise<void>>(async () => { });
  const statusRef = useRef<GameStatus>(status);

  const addLog = useCallback((msg: string) => {
    setLogs((prev) => [...prev.slice(-49), msg]);
  }, []);

  const clearLogs = useCallback(() => {
    setLogs([]);
  }, []);

  const fetchRoomUsers = useCallback(
    async (targetRoomId: string) => {
      try {
        const response = await axios.get(`/game/rooms/${targetRoomId}/users`);

        if (response.data?.result === 'SUCCESS' && response.data.data) {
          const playersData: any[] = response.data.data.players ?? [];
          const hostMemberId: number | null = response.data.data.hostMemberId ?? null;
          setPlayers((prev) => {
            const prevMap = new Map(prev.map((p) => [p.memberId, p]));
            return playersData.map((pData, idx) => {
              const prevPlayer = prevMap.get(pData.memberId);
              return {
                memberId: pData.memberId,
                name: pData.nickname,
                isHost: pData.memberId === hostMemberId,
                isReady: pData.isReady,
                score: prevPlayer?.score ?? 0,
                colorIndex: idx,
              };
            });
          });
          setIsHost(hostMemberId === myMemberId);
        }
      } catch (error: any) {
        console.error('Failed to fetch room users:', error);
        const errorCode = error?.response?.data?.error?.code;
        if (errorCode === 'G002' || errorCode === 'G008') {
          window.alert('방이 종료되었거나 더 이상 존재하지 않습니다.');
          returnToLobby();
        }
      }
    },
    [myMemberId],
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
          addLog('[시스템] 방장이 방 설정을 변경했습니다.');
          break;

        case 'PLAYER_JOIN':
        case 'PLAYER_LEAVE':
          if (roomId) {
            if (event.memberId === myMemberId && event.type === 'PLAYER_JOIN') break;
            const action = event.type === 'PLAYER_JOIN' ? '입장' : '퇴장';
            addLog(`[시스템] ${stripTag(event.nickname)}님이 ${action}하셨습니다.`);
            fetchRoomUsers(roomId);
          }
          break;

        case 'PLAYER_READY':
          if (roomId) {
            fetchRoomUsers(roomId);
          }
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
    [addLog, nickname, roomId, fetchRoomUsers, gameType, status, applyRoomSettings],
  );

  const handleEventRef = useRef(handleEvent);

  useEffect(() => {
    handleEventRef.current = handleEvent;
  }, [handleEvent]);

  useEffect(() => {
    localStorage.setItem('ums_logs', JSON.stringify(logs));
  }, [logs]);

  const connectWebSocket = useCallback(
    (targetRoomId: string, options?: { resyncOnConnect?: boolean }) => {
      if (stompClient.current) {
        stompClient.current.deactivate();
      }

      let shouldResync = options?.resyncOnConnect ?? false;

      const client = new Client({
        webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL),
        reconnectDelay: 5000,
        heartbeatIncoming: 10000,
        heartbeatOutgoing: 10000,
        heartbeatStrategy: TickerStrategy.Worker,
        onConnect: () => {
          client.subscribe(roomTopic(targetRoomId), (message) => {
            const response = JSON.parse(message.body);
            if (response.result === 'SUCCESS' && response.data) {
              handleEventRef.current(response.data);
            }
          });

          if (shouldResync) {
            resyncMembershipRef.current(targetRoomId);
          }
          shouldResync = true;
        },
        onWebSocketClose: () => {
          console.warn('WebSocket 연결이 끊겼습니다. 재연결을 시도합니다.');
        },
        onStompError: (frame) => {
          console.error('STOMP Error:', frame);
          addLog('[오류] 서버 통신 중 문제가 발생했습니다.');
        },
      });

      client.activate();
      stompClient.current = client;
    },
    [addLog],
  );

  const leaveRoom = useCallback(async () => {
    if (roomId) {
      try {
        await axios.post(`/game/rooms/${roomId}/leave`);
      } catch (error) {
        console.error('Leave room failed:', error);
      }
    }
    if (stompClient.current) {
      stompClient.current.deactivate();
    }
    setRoomId(null);
    setRoomName('');
    setStatus('ROOM_LIST');
    setPlayers([]);
    setIsHost(false);
    setPlayerIndex(null);
    setGameStartInfo(null);
    setRoundEndInfo(null);
    setGameType(null);
    gameTypeRef.current = null;
    setHint('');
    setCurrentVideoId(''); // 비디오 아이디 초기화
    localStorage.removeItem('ums_currentVideoId'); // 로컬 스토리지도 함께 삭제
    localStorage.removeItem(PLAYER_COLOR_INDEX_KEY);
    setMyColorIndex(null);
  }, [roomId, nickname]);

  const returnToLobby = leaveRoom;

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
      await fetchRoomUsers(roomId);
    }
  }, [roomId, fetchRoomUsers, clearLogs]);

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

  const resyncMembership = useCallback(
    async (targetRoomId: string) => {
      try {
        await axios.post(`/game/rooms/${targetRoomId}/join`);

        if (statusRef.current === 'PLAYING') {
          await restorePlayState(targetRoomId);
        } else {
          await fetchRoomUsers(targetRoomId);
        }
      } catch (error: any) {
        console.error('Resync after reconnect failed:', error);
        const message =
          error?.response?.data?.error?.message || '연결이 끊긴 사이 방에서 나가게 되었습니다.';
        window.alert(message);
        returnToLobby();
      }
    },
    [fetchRoomUsers, restorePlayState, returnToLobby],
  );

  useEffect(() => {
    resyncMembershipRef.current = resyncMembership;
  }, [resyncMembership]);

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

  useEffect(() => {
    const bootstrap = async () => {
      if (status === 'WAITING' || status === 'PLAYING') {
        if (roomId) {
          try {
            // 헬스 체크 수행
            const healthRes = await axios.get(`/game/rooms/${roomId}/health`);
            if (healthRes.data?.result === 'SUCCESS' && healthRes.data.data === 'ok') {
              // 새로고침이나 탭 종료로 웹소켓이 끊기면 서버가 방에서 내보내므로 항상 다시 참가한다.
              // 진행 중인 방이면 이 게임의 참가자였던 경우에만 서버가 재입장을 허용한다.
              await axios.post(`/game/rooms/${roomId}/join`);

              if (status === 'PLAYING') {
                await restorePlayState(roomId);
              }

              connectWebSocket(roomId);
              await fetchRoomUsers(roomId);
              if (nickname) {
                setPlayers((prev) => {
                  if (prev.length === 0) {
                    return [{ memberId: myMemberId ?? 0, name: nickname, isHost, isReady: isHost, score: 0 }];
                  }
                  return prev;
                });
              }
            } else {
              throw new Error('Room health check failed');
            }
          } catch (error) {
            console.warn('Rejoin failed, returning to lobby:', error);
            returnToLobby();
          }
        } else {
          setStatus('ROOM_LIST');
        }
      }
      setIsBootstrapping(false);
    };

    // StrictMode 는 개발 모드에서 마운트 이펙트를 두 번 실행한다.
    // 재참가와 상태 복원이 중복으로 돌지 않도록 한 번만 수행한다.
    if (hasBootstrapped.current) return;
    hasBootstrapped.current = true;

    bootstrap();
  }, []); // Run once on mount

  useEffect(() => {
    if (status === 'WAITING' && roomId) {
      fetchRoomUsers(roomId);
      fetchRoomSettings(roomId);
    }
  }, [status, roomId, fetchRoomUsers, fetchRoomSettings]);

  useEffect(() => {
    if (!roomId || (status !== 'WAITING' && status !== 'PLAYING')) return;

    const reconnectImmediatelyIfDropped = () => {
      if (document.visibilityState !== 'visible') return;
      if (stompClient.current?.connected) return;

      console.warn('탭 복귀 시점에 연결이 끊겨 있어 즉시 재연결합니다.');
      connectWebSocket(roomId, { resyncOnConnect: true });
    };

    document.addEventListener('visibilitychange', reconnectImmediatelyIfDropped);
    return () => document.removeEventListener('visibilitychange', reconnectImmediatelyIfDropped);
  }, [roomId, status, connectWebSocket]);

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
        const mappedRooms: Room[] = response.data.data.map((r: any) => ({
          id: r.roomId,
          name: r.title,
          hostMemberId: r.hostMemberId,
          hostName: r.hostNickname,
          playerCount: r.currentPlayers,
          maxPlayers: r.maxPlayers,
          status: r.status || 'WAITING',
        }));
        setRooms(mappedRooms);
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

    const refreshOnRoomUpdate = (event: MessageEvent) => {
      if (event.data === 'REFRESH') {
        debouncedFetchRooms();
      }
    };

    const resyncOnTabReturn = () => {
      if (document.visibilityState !== 'visible') return;
      debouncedFetchRooms();
    };

    fetchRooms();
    const stopListeningRoomUpdate = onSseEvent('room-update', refreshOnRoomUpdate);
    const stopListeningConnected = onSseEvent('connected', debouncedFetchRooms);
    document.addEventListener('visibilitychange', resyncOnTabReturn);

    return () => {
      document.removeEventListener('visibilitychange', resyncOnTabReturn);
      stopListeningRoomUpdate();
      stopListeningConnected();
      clearTimeout(debounceTimer);
    };
  }, [status, fetchRooms, onSseEvent]);

  const enterLobby = useCallback((memberId: number, name: string) => {
    localStorage.setItem(MY_MEMBER_ID_KEY, String(memberId));
    localStorage.setItem('ums_nickname', name);
    setMyMemberId(memberId);
    setNickname(name);
    setStatus('ROOM_LIST');
  }, []);

  const enterRoom = useCallback(
    async (room: Room, slotIndex: number | null) => {
      setRoomMaxPlayers(room.maxPlayers);
      setRoomName(room.name);
      if (slotIndex !== null) {
        localStorage.setItem(PLAYER_COLOR_INDEX_KEY, String(slotIndex));
        setMyColorIndex(slotIndex);
      }
      clearLogs();
      localStorage.removeItem('ums_logs');
      setRoomId(room.id);
      setIsHost(room.hostMemberId === myMemberId);

      if (room.status === 'PLAYING') {
        // 서버가 재입장을 허용한 경우에만 여기까지 온다. 진행 중인 라운드 상태를 복원한다.
        await restorePlayState(room.id);
      } else {
        setStatus('WAITING');
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

      connectWebSocket(room.id);
      window.history.pushState({ room: room.id }, '');
    },
    [nickname, connectWebSocket, clearLogs, addLog, restorePlayState],
  );

  const acceptInvite = useCallback(
    async (invite: RoomInvite) => {
      try {
        const response = await axios.post(`/api/invites/${invite.inviteId}/accept`);
        if (response.data.result === 'SUCCESS') {
          const { room, playerSequence } = response.data.data;
          await enterRoom(
            {
              id: String(room.roomId),
              name: room.title,
              hostMemberId: room.hostMemberId,
              hostName: room.hostNickname,
              playerCount: room.currentPlayers,
              maxPlayers: room.maxPlayers,
              status: room.status,
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
          await enterRoom(room, slotIndex);
        }
      } catch (error: any) {
        console.error('Join room failed:', error);
        const httpStatus = error?.response?.status;
        const redirectRoomId = error?.response?.data?.data?.redirectRoomId ?? error?.response?.data?.redirectRoomId;
        if (httpStatus === 409 && redirectRoomId) {
          setRoomId(redirectRoomId);
          setRoomName('');
          setIsHost(false);
          setStatus('PLAYING');
          setPlayers([{ memberId: myMemberId ?? 0, name: nickname, isHost: false, isReady: false, score: 0 }]);
          connectWebSocket(redirectRoomId);
          return;
        }
        const message = error?.response?.data?.error?.message || '방에 입장할 수 없습니다.';
        window.alert(message);
      }
    },
    [nickname, connectWebSocket, enterRoom],
  );

  const createRoom = useCallback(
    async (
      title: string,
      maxPlayers: number,
      category: string,
      songCount: number,
      gameType: string,
      difficulty?: number,
    ) => {
      setIsCreatingRoom(true);
      try {
        const response = await axios.post('/game/rooms', {
          title,
          maxPlayers,
          category,
          totalRound: songCount,
          gameType,
          difficulty,
        });
        if (response.data.result === 'SUCCESS') {
          const newRoomId = response.data.data;
          setRoomMaxPlayers(maxPlayers);
          setRoomName(title);
          localStorage.setItem(PLAYER_COLOR_INDEX_KEY, '0');
          setMyColorIndex(0);
          clearLogs();
          localStorage.removeItem('ums_logs');
          setRoomId(newRoomId);
          setIsHost(true);
          setStatus('WAITING');
          setCurrentVideoId(''); // 이전 비디오 아이디 초기화
          setHint('');
          localStorage.removeItem('ums_currentVideoId');
          setPlayers([
            { memberId: myMemberId ?? 0, name: nickname, isHost: true, isReady: true, score: 0, colorIndex: 0 },
          ]);
          connectWebSocket(newRoomId);
          window.history.pushState({ room: newRoomId }, '');
        }
      } catch (error) {
        console.error('Create room failed:', error);
        addLog('[오류] 방 생성에 실패했습니다.');
      } finally {
        setIsCreatingRoom(false);
      }
    },
    [nickname, addLog, connectWebSocket, clearLogs],
  );

  const toggleReady = useCallback(async () => {
    if (!roomId) return;
    try {
      const response = await axios.post(`/game/rooms/${roomId}/ready`);
      if (response.data.result === 'SUCCESS') {
        setPlayers((prev) => prev.map((p) => (p.name === nickname ? { ...p, isReady: !p.isReady } : p)));
      }
    } catch (error: any) {
      console.error('Toggle ready failed:', error);
      const message = error?.response?.data?.error?.message || '준비 상태 변경에 실패했습니다.';
      window.alert(message);
    }
  }, [roomId, nickname]);

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
      if (!roomId || !stompClient.current || !stompClient.current.connected) return;
      stompClient.current.publish({
        destination: roomChat(roomId),
        body: JSON.stringify({ message }),
      });
    },
    [roomId],
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
    enterLobby,
    joinRoom,
    acceptInvite,
    createRoom,
    leaveRoom,
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
