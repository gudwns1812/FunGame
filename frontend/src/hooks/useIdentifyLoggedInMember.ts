import { useEffect } from 'react';
import type { MemberInfo } from '../types/auth';

interface IdentifyParams {
  isAuthenticated: boolean;
  user: MemberInfo | null;
  nickname: string;
  identify: (memberId: number, nickname: string) => void;
  enterLobby: (memberId: number, nickname: string) => void;
}

export const useIdentifyLoggedInMember = ({
  isAuthenticated,
  user,
  nickname,
  identify,
  enterLobby,
}: IdentifyParams) => {
  useEffect(() => {
    if (isAuthenticated && user) {
      identify(user.id, user.nickname);
    }
  }, [isAuthenticated, user, identify]);

  useEffect(() => {
    if (isAuthenticated && user && nickname === '') {
      enterLobby(user.id, user.nickname);
    }
  }, [isAuthenticated, user, nickname, enterLobby]);
};
