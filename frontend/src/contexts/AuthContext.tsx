import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import axios from 'axios';
import type { MemberInfo, AuthState } from '../types/auth';

interface AuthContextType extends AuthState {
  login: (loginId: string, password: string) => Promise<void>;
  signup: (loginId: string, password: string, nickname: string) => Promise<void>;
  checkId: (loginId: string) => Promise<boolean>;
  logout: () => Promise<void>;
  updateNickname: (newNickname: string) => Promise<void>;
  refreshUser: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

// Axios default 설정 (Cookie 기반 인증을 위해 credentials 포함)
axios.defaults.withCredentials = true;
axios.defaults.baseURL = import.meta.env.VITE_API_BASE_URL;

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [authState, setAuthState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isInitialLoading: true,
  });

  const refreshUser = useCallback(async () => {
    try {
      const response = await axios.get('/api/auth/me');
      if (response.data.result === 'SUCCESS' && response.data.data) {
        setAuthState({
          user: response.data.data,
          isAuthenticated: true,
          isInitialLoading: false,
        });
      } else {
        setAuthState({
          user: null,
          isAuthenticated: false,
          isInitialLoading: false,
        });
      }
    } catch (error) {
      console.error(error);
      setAuthState({
        user: null,
        isAuthenticated: false,
        isInitialLoading: false,
      });
    }
  }, []);

  // Axios 인터셉터 설정 (401 에러 처리)
  useEffect(() => {
    const interceptor = axios.interceptors.response.use(
      (response) => response,
      (error) => {
        if (error.response?.status === 401) {
          setAuthState({
            user: null,
            isAuthenticated: false,
            isInitialLoading: false,
          });
        }
        return Promise.reject(error);
      },
    );

    return () => axios.interceptors.response.eject(interceptor);
  }, []);

  useEffect(() => {
    refreshUser();
  }, [refreshUser]);

  const login = async (loginId: string, password: string) => {
    try {
      const response = await axios.post('/api/auth/login', { loginId, password });
      if (response.data.result === 'SUCCESS' && response.data.data) {
        // 로그인 성공 시 서버가 준 유저 정보로 즉시 상태 업데이트 (추가 API 호출 없음)
        setAuthState({
          user: response.data.data,
          isAuthenticated: true,
          isInitialLoading: false,
        });
      } else {
        throw new Error(response.data.error.message || '로그인에 실패했습니다.');
      }
    } catch (error: any) {
      throw new Error(error.response?.data?.error?.message || '로그인에 실패했습니다.');
    }
  };

  const signup = async (loginId: string, password: string, nickname: string) => {
    try {
      const response = await axios.post('/api/auth/signup', { loginId, password, nickname });
      if (response.data.result !== 'SUCCESS') {
        throw new Error(response.data.error.message || '회원가입에 실패했습니다.');
      }
    } catch (error: any) {
      throw new Error(error.response?.data?.error?.message || '회원가입에 실패했습니다.');
    }
  };

  const checkId = async (loginId: string): Promise<boolean> => {
    try {
      const response = await axios.get(`/api/auth/check-id?loginId=${encodeURIComponent(loginId)}`);
      if (response.data.result === 'SUCCESS') {
        return response.data.data; // true면 중복, false면 사용 가능
      }
      throw new Error('중복 확인에 실패했습니다.');
    } catch (error: any) {
      throw new Error(error.response?.data?.error?.message || '중복 확인에 실패했습니다.');
    }
  };

  const updateNickname = async (newNickname: string) => {
    try {
      const response = await axios.patch('/api/auth/nickname', { nickname: newNickname });
      if (response.data.result === 'SUCCESS') {
        // 닉네임 변경 성공 시 유저 정보만 업데이트
        setAuthState((prev) => ({
          ...prev,
          user: prev.user ? { ...prev.user, nickname: newNickname } : null,
        }));
      } else {
        throw new Error(response.data.error.message || '닉네임 변경에 실패했습니다.');
      }
    } catch (error: any) {
      throw new Error(error.response?.data?.error?.message || '닉네임 변경에 실패했습니다.');
    }
  };

  const logout = async () => {
    try {
      await axios.post('/api/auth/logout');
      setAuthState({
        user: null,
        isAuthenticated: false,
        isInitialLoading: false,
      });
      localStorage.removeItem('ums_nickname');
    } catch (error) {
      setAuthState({
        user: null,
        isAuthenticated: false,
        isInitialLoading: false,
      });
    }
  };

  return (
    <AuthContext.Provider value={{ ...authState, login, signup, checkId, logout, updateNickname, refreshUser }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
