import { useState, useCallback, useEffect, useRef } from 'react';
import axios from 'axios';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { Player, GameStatus, Room, GameStartInfo, RoundEndInfo } from '../types/game';
import { stripTag } from '../utils/stringUtils';
import { PLAYER_COLOR_INDEX_KEY } from '../utils/playerColor';

// Configure axios base URL
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL;

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
  const [logs, setLogs] = useState<string[]>([]);
  const [currentVideoId, setCurrentVideoId] = useState(() => localStorage.getItem('ums_currentVideoId') || '');
  const [isHost, setIsHost] = useState(false);
  const [playerIndex, setPlayerIndex] = useState<number | null>(null);
  const [gameStartInfo, setGameStartInfo] = useState<GameStartInfo | null>(null);
  const [gameType, setGameType] = useState<string | null>(() => localStorage.getItem('ums_gameType'));
  const [roundEndInfo, setRoundEndInfo] = useState<RoundEndInfo | null>(null);
  const [roundIndex, setRoundIndex] = useState<number>(0);
  const [currentRound, setCurrentRound] = useState<number>(0);
  const [totalRound, setTotalRound] = useState<number>(0);
  const [isBootstrapping, setIsBootstrapping] = useState(true);
  const [isCreatingRoom, setIsCreatingRoom] = useState(false);
  const [myColorIndex, setMyColorIndex] = useState<number | null>(() => {
    const saved = localStorage.getItem(PLAYER_COLOR_INDEX_KEY);
    return saved !== null ? Number(saved) : null;
  });

  const stompClient = useRef<Client | null>(null);
  const fetchRankRef = useRef<() => Promise<void>>(async () => { });

  const addLog = useCallback((msg: string) => {
    setLogs(prev => [...prev.slice(-49), msg]);
  }, []);

  const clearLogs = useCallback(() => {
    setLogs([]);
  }, []);

  const fetchRoomUsers = useCallback(
    async (targetRoomId: string) => {
      try {
        const response = await axios.get(`/game/rooms/${targetRoomId}/users`, {
          headers: nickname ? { playerName: encodeURIComponent(nickname) } : undefined,
        });

        if (response.data?.result === 'SUCCESS' && response.data.data) {
          const playersData: any[] = response.data.data.players ?? [];
          const host: string = response.data.data.host ?? '';
          setPlayers(prev => {
            const prevMap = new Map(prev.map(p => [p.name, p]));
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
      } catch (error) {
        console.error('Failed to fetch room users:', error);
      }
    },
    [nickname],
  );

  const handleEvent = useCallback((event: any) => {
    console.log("Processing WebSocket Event:", event);
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
        setPlayers(prev => prev.map(p => ({
          ...p,
          isHost: p.name === event.newHost,
          isReady: p.name === event.newHost ? true : p.isReady
        })));
        setIsHost(event.newHost === nickname);
        break;

      case 'PLAYER_READY':
        setPlayers(prev => prev.map(p =>
          p.name === event.player ? { ...p, isReady: true } : p
        ));
        break;

      case 'CHAT':
        addLog(`${stripTag(event.playerName)}: ${event.message}`);
        break;

      case 'GAME_START': {
        setStatus('PLAYING');
        const normalizedGameType = event.gameType === 'CS 문제 맞추기' || event.gameType === 'CS' ? 'CS' : 'SONG';
        setGameType(normalizedGameType);
        setGameStartInfo({
          gameType: normalizedGameType,
          category: event.category,
          songCount: event.songCount,
          message: event.message,
        });
        setLogs([]);
        break;
      }

      case 'ROUND_START':
        setStatus('PLAYING');
        setCurrentVideoId(event.content);
        setRoundEndInfo(null);
        setGameStartInfo(null);
        setRoundIndex(event.round);
        setCurrentRound(event.round);
        setTotalRound(event.totalRound);

        // 라운드 시작 시 구분선 강화
        addLog(`================================================================================`);

        setPlayers(prev => {
          const idx = prev.findIndex(p => p.name === nickname);
          setPlayerIndex(idx !== -1 ? idx + 1 : null);
          return prev;
        });
        break;

      case 'TIMER_TICK':
        setTimeLeft(event.remainingSeconds);
        setTotalTime(30);
        break;

      case 'CORRECT_ANSWER':
        setPlayers(prev => prev.map(p =>
          p.name === event.playerName ? { ...p, score: event.score } : p
        ));
        break;

      case 'ROUND_SKIP':
        break;

      case 'ROUND_END':
        setRoundEndInfo({ answer: event.answer, winner: event.winner });
        fetchRankRef.current();
        break;

      case 'GAME_RESULT': {
        setStatus('RESULT');
        setPlayerIndex(null);
        setGameStartInfo(null);
        setGameType(null);
        setRoundEndInfo(null);
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
        break;
      }

      case 'GAME_END':
        break;
    }
  }, [addLog, nickname, roomId, fetchRoomUsers]);

  const handleEventRef = useRef(handleEvent);

  useEffect(() => {
    handleEventRef.current = handleEvent;
  }, [handleEvent]);

  const connectWebSocket = useCallback((targetRoomId: string) => {
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
      }
    });

    client.activate();
    stompClient.current = client;
  }, [addLog]);

  const leaveRoom = useCallback(async () => {
    if (roomId) {
      try {
        await axios.post(`/game/rooms/${roomId}/leave`, null, {
          headers: { playerName: encodeURIComponent(nickname) }
        });
      } catch (error) {
        console.error('Leave room failed:', error);
      }
    }
    if (stompClient.current) {
      stompClient.current.deactivate();
    }
    setRoomId(null);
    setStatus('ROOM_LIST');
    setPlayers([]);
    setIsHost(false);
    setPlayerIndex(null);
    setGameStartInfo(null);
    setRoundEndInfo(null);
    localStorage.removeItem(PLAYER_COLOR_INDEX_KEY);
    setMyColorIndex(null);
  }, [roomId, nickname]);

  const returnToLobby = useCallback(() => {
    if (stompClient.current) {
      stompClient.current.deactivate();
    }
    setRoomId(null);
    setStatus('ROOM_LIST');
    setPlayers([]);
    setIsHost(false);
    setPlayerIndex(null);
    setGameStartInfo(null);
    setRoundEndInfo(null);
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
  }, [status, roomId]);

  useEffect(() => {
    const bootstrap = async () => {
      if (status === 'WAITING' || status === 'PLAYING') {
        if (roomId) {
          connectWebSocket(roomId);
          if (nickname) {
            setPlayers(prev => {
              if (prev.length === 0) {
                return [{ id: nickname, name: nickname, isHost, isReady: isHost, score: 0 }];
              }
              return prev;
            });
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
          status: r.status || 'WAITING'
        }));
        setRooms(mappedRooms);
      }
    } catch (error) {
      console.error('Failed to fetch rooms:', error);
    }
  }, []);

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

  const joinRoom = useCallback(async (room: Room) => {
    try {
      const response = await axios.post(`/game/rooms/${room.id}/join`, null, {
        headers: { playerName: encodeURIComponent(nickname) }
      });
      if (response.data.result === 'SUCCESS') {
        const slotIndex = typeof response.data.data === 'number' ? response.data.data : null;
        if (slotIndex !== null) {
          localStorage.setItem(PLAYER_COLOR_INDEX_KEY, String(slotIndex));
          setMyColorIndex(slotIndex);
        }
        clearLogs();
        setRoomId(room.id);
        setIsHost(room.hostName === nickname);
        setStatus('WAITING');
        setPlayers([{ id: nickname, name: nickname, isHost: room.hostName === nickname, isReady: room.hostName === nickname, score: 0, colorIndex: slotIndex ?? undefined }]);
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
        setIsHost(false);
        setStatus('PLAYING');
        setPlayers([{ id: nickname, name: nickname, isHost: false, isReady: false, score: 0 }]);
        connectWebSocket(redirectRoomId);
        return;
      }
      const message = error?.response?.data?.error?.message || '방에 입장할 수 없습니다.';
      window.alert(message);
    }
  }, [nickname, connectWebSocket, clearLogs, addLog]);

  const createRoom = useCallback(async (title: string, maxPlayers: number, category: string, songCount: number, gameType: string) => {
    setIsCreatingRoom(true);
    try {
      const response = await axios.post('/game/rooms', {
        title,
        maxPlayers,
        hostName: nickname,
        category,
        totalRound: songCount,
        gameType
      });
      if (response.data.result === 'SUCCESS') {
        const newRoomId = response.data.data;
        localStorage.setItem(PLAYER_COLOR_INDEX_KEY, '0');
        setMyColorIndex(0);
        clearLogs();
        setRoomId(newRoomId);
        setIsHost(true);
        setStatus('WAITING');
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
  }, [nickname, addLog, connectWebSocket, clearLogs]);

  const toggleReady = useCallback(async () => {
    if (!roomId) return;
    try {
      const response = await axios.post(`/game/rooms/${roomId}/ready`, null, {
        headers: { playerName: encodeURIComponent(nickname) }
      });
      if (response.data.result === 'SUCCESS') {
        setPlayers(prev => prev.map(p =>
          p.name === nickname ? { ...p, isReady: !p.isReady } : p
        ));
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
      const response = await axios.post(`/game/rooms/${roomId}/start`, null, {
        headers: { playerName: encodeURIComponent(nickname) }
      });
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
      await axios.post(`/game/rooms/${roomId}/skip`, null, {
        headers: { playerName: encodeURIComponent(nickname) }
      });
    } catch (error) {
      console.error('Skip vote failed:', error);
    }
  }, [roomId, nickname]);

  const fetchRank = useCallback(async () => {
    if (!roomId) return;
    try {
      const response = await axios.get(`/game/rooms/${roomId}/play/rank`, {
        headers: nickname ? { playerName: encodeURIComponent(nickname) } : undefined,
      });
      if (response.data?.result === 'SUCCESS' && Array.isArray(response.data.data)) {
        const rankData: { player: string; score: number }[] = response.data.data;
        setPlayers(prev => {
          const prevMap = new Map(prev.map(p => [p.name, p]));
          return rankData.map(({ player, score }) => ({
            id: player,
            name: player,
            isHost: prevMap.get(player)?.isHost ?? false,
            isReady: prevMap.get(player)?.isReady ?? false,
            score,
          }));
        });
      }
    } catch (error) {
      console.error('Failed to fetch rank:', error);
    }
  }, [roomId, nickname]);

  useEffect(() => {
    fetchRankRef.current = fetchRank;
  }, [fetchRank]);

  const changeNickname = useCallback((newName: string) => {
    localStorage.setItem('ums_nickname', newName);
    setNickname(newName);
  }, []);

  const sendMessage = useCallback((message: string) => {
    if (!roomId || !stompClient.current || !stompClient.current.connected) return;
    stompClient.current.publish({
      destination: `/publish/room/${roomId}/chat`,
      headers: { playerName: nickname },
      body: message
    });
  }, [roomId, nickname]);

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
    isBootstrapping,
    isCreatingRoom,
    myColorIndex,
    enterLobby,
    joinRoom,
    createRoom,
    leaveRoom,
    returnToLobby,
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
  };
};
