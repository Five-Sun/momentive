import { apiFetch } from "./client";

export type UserRole = "USER" | "ADMIN";

export interface AuthUser {
  id: number;
  email: string;
  nickname: string;
  role: UserRole;
}

export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export function signup(request: SignupRequest): Promise<AuthUser> {
  return apiFetch<AuthUser>("/auth/signup", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function login(request: LoginRequest): Promise<AuthUser> {
  return apiFetch<AuthUser>("/auth/login", {
    method: "POST",
    body: JSON.stringify(request),
  });
}

export function logout(): Promise<void> {
  return apiFetch<void>("/auth/logout", { method: "POST" });
}

export function refresh(): Promise<AuthUser> {
  return apiFetch<AuthUser>("/auth/refresh", { method: "POST" });
}

export function me(): Promise<AuthUser> {
  return apiFetch<AuthUser>("/auth/me");
}
