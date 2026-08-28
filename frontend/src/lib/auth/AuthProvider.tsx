"use client";

import { createContext, useCallback, useContext, useState } from "react";
import type { AuthUser, LoginRequest, SignupRequest } from "@/lib/api/auth";
import * as authApi from "@/lib/api/auth";

interface AuthContextValue {
  user: AuthUser | null;
  login: (request: LoginRequest) => Promise<AuthUser>;
  signup: (request: SignupRequest) => Promise<AuthUser>;
  logout: () => Promise<void>;
}

const AuthContext = createContext<AuthContextValue | null>(null);

interface AuthProviderProps {
  initialUser: AuthUser | null;
  children: React.ReactNode;
}

/**
 * 로그인 상태를 Context로 전파한다. 컴포넌트는 `src/lib/api/auth.ts`를 직접 호출하지 않고
 * 이 Provider가 노출하는 `login`/`signup`/`logout`을 통해서만 접근한다.
 */
export function AuthProvider({ initialUser, children }: AuthProviderProps) {
  const [user, setUser] = useState<AuthUser | null>(initialUser);

  const login = useCallback(async (request: LoginRequest) => {
    const loggedInUser = await authApi.login(request);
    setUser(loggedInUser);
    return loggedInUser;
  }, []);

  const signup = useCallback(async (request: SignupRequest) => {
    const createdUser = await authApi.signup(request);
    setUser(createdUser);
    return createdUser;
  }, []);

  const logout = useCallback(async () => {
    await authApi.logout();
    setUser(null);
  }, []);

  return <AuthContext.Provider value={{ user, login, signup, logout }}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error("useAuth는 AuthProvider 내부에서만 사용할 수 있습니다.");
  }
  return context;
}
