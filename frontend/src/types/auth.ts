export type UserRole = 'MASTER' | 'ADMIN' | 'USER';

export interface MemberInfo {
  id: number;
  loginId: string;
  nickname: string;
  email: string;
  role: UserRole;
}

export interface AuthState {
  user: MemberInfo | null;
  isAuthenticated: boolean;
  isInitialLoading: boolean;
}
