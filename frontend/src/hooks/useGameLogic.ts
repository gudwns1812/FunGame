import { useState, useCallback, useEffect, useRef } from 'react';
import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { Player, GameStatus, Room, GameStartInfo, RoundEndInfo, HangmanStatus } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { PLAYER_COLOR_INDEX_KEY } from '../utils/playerColor';

// Configure axios base URL
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL;
axios.defaults.withCredentials = true; // 세션 인증을 위해 추가

export const useGameLogic = () => {
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
  const [haliGaliStatus, setHaliGaliStatus] = useState<string[]>([]);
  const [hangmanStatus, setHangmanStatus] = useState<HangmanStatus | null>(null);
  const [lastHaliGaliAction, setLastHaliGaliAction] = useState<any>(null);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [isCreatingRoom, setIsCreatingRoom] = useState(false);
  const [myColorIndex, setMyColorIndex] = useState<number | null>(() => {
    const saved = localStorage.getItem(PLAYER_COLOR_INDEX_KEY);
    return saved !== null ? Number(saved) : null;
  });

  // 제목없는 음원으로 미디어 플레이어 제목 가리기

  const stompClient = useRef<Client | null>(null);
  const fetchRankRef = useRef<() => Promise<void>>(async () => { });

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
          const host: string = response.data.data.host ?? '';
          setPlayers((prev) => {
            const prevMap = new Map(prev.map((p) => [p.name, p]));
            return playersData.map((pData, idx) => {
              const name = pData.name;
              const isReady = pData.isReady;
              const prevPlayer = prevMap.get(name);
              return {
                id: name,
                name,
                isHost: name === host,
                isReady: isReady,
                score: prevPlayer?.score ?? 0,
                colorIndex: idx,
              };
            });
          });
          setIsHost(host === nickname);
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
    [nickname],
  );

  const handleEvent = useCallback(
    (event: any) => {
      switch (event.type) {
        case 'PLAYER_JOIN':
        case 'PLAYER_LEAVE':
          if (roomId) {
            if (event.player === nickname && event.type === 'PLAYER_JOIN') break;
            const action = event.type === 'PLAYER_JOIN' ? '입장' : '퇴장';
            addLog(`[시스템] ${stripTag(event.player)}님이 ${action}하셨습니다.`);
            fetchRoomUsers(roomId);
          }
          break;

        case 'HOST_CHANGE':
          setPlayers((prev) =>
            prev.map((p) => ({
              ...p,
              isHost: p.name === event.newHost,
              isReady: p.name === event.newHost ? true : p.isReady,
            })),
          );
          setIsHost(event.newHost === nickname);
          break;

        case 'PLAYER_READY':
          if (roomId) {
            fetchRoomUsers(roomId);
          }
          break;

        case 'CHAT': {
          const sender = event.playerName || event.player || '알 수 없음';
          const msg = event.message || '';
          addLog(`${stripTag(sender)}: ${msg}`);
          break;
        }

        case 'GAME_START': {
          setStatus('PLAYING');
          setHint('');
          const normalizedGameType =
            event.gameType === 'CS'
              ? 'CS'
              : event.gameType === 'HALLIGALLI'
                ? 'HALLIGALLI'
                : event.gameType === 'HANGMAN'
                  ? 'HANGMAN'
                  : 'SONG';
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
              isGameOver: false,
              isWin: false,
            });
          }

          setLogs([]);
          break;
        }

        case 'HALIGALI_ACTION':
          setHaliGaliStatus(event.status);
          setLastHaliGaliAction({
            playerName: event.playerName,
            actionType: event.actionType,
            result: event.result,
          });
          if (event.actionType === 'PRESS_BELL' && event.result === 'CORRECT') {
            addLog(`[알림] ${stripTag(event.playerName)}님이 종을 울려 카드를 획득했습니다!`);
          } else if (event.actionType === 'PRESS_BELL' && event.result === 'WRONG') {
            addLog(`[실패] ${stripTag(event.playerName)}님이 종을 잘못 울려 패널티를 받았습니다.`);
          }
          break;

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
          setPlayers((prev) => prev.map((p) => (p.name === event.playerName ? { ...p, score: event.score } : p)));
          break;

        case 'ROUND_SKIP':
          break;

        case 'ROUND_END':
          setHint('');
          const isCsRound = gameTypeRef.current === 'CS';
          setRoundEndInfo({
            answer: event.answer,
            explanation: isCsRound && event.explanation?.trim() ? event.explanation : null,
            winner: event.winner,
          });

          fetchRankRef.current();
          break;

        case 'HANGMAN_ACTION': {
          const s = event.status;
          setHangmanStatus({
            currentDisplay: s[0],
            wrongLetters: s[1] ? s[1].split(',') : [],
            remainingTries: parseInt(s[2], 10),
            currentTurnPlayer: s[3],
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
            setPlayers([{ id: nickname, name: nickname, score: event.score, isHost: false, isReady: false }]);
          } else if (event.rankings) {
            // 기존 퀴즈 결과 처리
            const finalRankings: Player[] = event.rankings
              .split('\n')
              .filter((line: string) => line.trim() !== '')
              .map((line: string) => {
                const colonIdx = line.lastIndexOf(':');
                const name = line.substring(0, colonIdx).trim();
                const score = parseInt(line.substring(colonIdx + 1).trim(), 10) || 0;
                return { id: name, name, score, isHost: false, isReady: false };
              });
            setPlayers(finalRankings);
          }
          break;
        }
      }
    },
    [addLog, nickname, roomId, fetchRoomUsers, gameType, status],
  );

  const handleEventRef = useRef(handleEvent);

  useEffect(() => {
    handleEventRef.current = handleEvent;
  }, [handleEvent]);

  useEffect(() => {
    localStorage.setItem('ums_logs', JSON.stringify(logs));
  }, [logs]);

  const connectWebSocket = useCallback(
    (targetRoomId: string) => {
      if (stompClient.current) {
        stompClient.current.deactivate();
      }

      const client = new Client({
        webSocketFactory: () => new SockJS(import.meta.env.VITE_WS_URL),
        reconnectDelay: 5000,
        onConnect: () => {
          client.subscribe(`/subscribe/room/${targetRoomId}`, (message) => {
            const response = JSON.parse(message.body);
            if (response.result === 'SUCCESS' && response.data) {
              handleEventRef.current(response.data);
            }
          });
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

  const returnToLobby = useCallback(() => {
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
  }, []);

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
              connectWebSocket(roomId);
              await fetchRoomUsers(roomId);
              if (nickname) {
                setPlayers((prev) => {
                  if (prev.length === 0) {
                    return [{ id: nickname, name: nickname, isHost, isReady: isHost, score: 0 }];
                  }
                  return prev;
                });
              }
            } else {
              throw new Error('Room health check failed');
            }
          } catch (error) {
            console.warn('Health check failed, returning to lobby:', error);
            returnToLobby();
          }
        } else {
          setStatus('ROOM_LIST');
        }
      }
      setIsBootstrapping(false);
    };
    bootstrap();
  }, []); // Run once on mount

  useEffect(() => {
    if (status === 'WAITING' && roomId) {
      fetchRoomUsers(roomId);
    }
  }, [status, roomId, fetchRoomUsers]);

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
          hostName: r.hostName,
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

  // SSE 연결 및 방 목록 실시간 업데이트 로직
  useEffect(() => {
    if (status !== 'ROOM_LIST') return;

    let debounceTimer: ReturnType<typeof setTimeout>;
    const debouncedFetchRooms = () => {
      clearTimeout(debounceTimer);
      debounceTimer = setTimeout(() => {
        fetchRooms();
      }, 300);
    };

    const sseUrl = `${import.meta.env.VITE_API_BASE_URL}/api/sse/rooms/subscribe`;
    const eventSource = new EventSource(sseUrl, { withCredentials: true });

    eventSource.addEventListener('room-update', (event) => {
      if (event.data === 'REFRESH') {
        debouncedFetchRooms();
      }
    });

    eventSource.onopen = () => {
      console.log('SSE connection opened');
    };

    eventSource.onerror = (error) => {
      console.error('SSE connection failed:', error);
      eventSource.close();
    };

    return () => {
      eventSource.close();
      clearTimeout(debounceTimer);
    };
  }, [status, fetchRooms]);

  useEffect(() => {
    if (status === 'ROOM_LIST') {
      fetchRooms();
    }
  }, [status, fetchRooms]);

  const enterLobby = useCallback((name: string) => {
    localStorage.setItem('ums_nickname', name);
    setNickname(name);
    setStatus('ROOM_LIST');
  }, []);

  const joinRoom = useCallback(
    async (room: Room) => {
      try {
        const response = await axios.post(`/game/rooms/${room.id}/join`);
        if (response.data.result === 'SUCCESS') {
          setRoomMaxPlayers(room.maxPlayers);
          setRoomName(room.name);
          const slotIndex = typeof response.data.data === 'number' ? response.data.data : null;
          if (slotIndex !== null) {
            localStorage.setItem(PLAYER_COLOR_INDEX_KEY, String(slotIndex));
            setMyColorIndex(slotIndex);
          }
          clearLogs();
          localStorage.removeItem('ums_logs');
          setRoomId(room.id);
          setIsHost(room.hostName === nickname);
          setStatus('WAITING');
          setCurrentVideoId(''); // 이전 비디오 아이디 초기화
          setHint('');
          localStorage.removeItem('ums_currentVideoId');
          setPlayers([
            {
              id: nickname,
              name: nickname,
              isHost: room.hostName === nickname,
              isReady: room.hostName === nickname,
              score: 0,
              colorIndex: slotIndex ?? undefined,
            },
          ]);
          connectWebSocket(room.id);
          addLog(`[시스템] ${room.name} 방에 입장했습니다.`);
          window.history.pushState({ room: room.id }, '');
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
          setPlayers([{ id: nickname, name: nickname, isHost: false, isReady: false, score: 0 }]);
          connectWebSocket(redirectRoomId);
          return;
        }
        const message = error?.response?.data?.error?.message || '방에 입장할 수 없습니다.';
        window.alert(message);
      }
    },
    [nickname, connectWebSocket, clearLogs, addLog],
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
          setPlayers([{ id: nickname, name: nickname, isHost: true, isReady: true, score: 0, colorIndex: 0 }]);
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
        const rankData: { player: string; score: number }[] = response.data.data;
        setPlayers((prev) => {
          const prevMap = new Map(prev.map((p) => [p.name, p]));
          return rankData.map(({ player, score }) => ({
            id: player,
            name: player,
            isHost: prevMap.get(player)?.isHost ?? false,
            isReady: prevMap.get(player)?.isReady ?? false,
            colorIndex: prevMap.get(player)?.colorIndex,
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
        destination: `/publish/room/${roomId}/chat`,
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
          playerName: nickname,
          type: 'SUBMIT_ANSWER',
          value: letter,
        });
      } catch (error) {
        console.error('Hangman action failed:', error);
      }
    },
    [roomId, nickname],
  );

  const sendHaliGaliAction = useCallback(
    async (actionType: 'FLIP_CARD' | 'PRESS_BELL') => {
      if (!roomId) return;
      try {
        await axios.post(`/game/rooms/${roomId}/action`, {
          playerName: nickname,
          type: actionType,
          value: '',
        });
      } catch (error) {
        console.error('HaliGali action failed:', error);
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
    haliGaliStatus,
    lastHaliGaliAction,
    isBootstrapping,
    isCreatingRoom,
    myColorIndex,
    isMusicStart,
    enterLobby,
    joinRoom,
    createRoom,
    leaveRoom,
    returnToLobby,
    startGame,
    toggleReady,
    skipRound,
    sendMessage,
    sendHaliGaliAction,
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
