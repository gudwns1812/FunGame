export type UserRole = 'MASTER' | 'ADMIN' | 'USER';

export interface MemberInfo {
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
