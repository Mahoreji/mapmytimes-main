import type { AuthResponse, UserProfileResponse } from "@/types/auth";

const ACCESS_KEY = "mmt.auth.access";
const REFRESH_KEY = "mmt.auth.refresh";
const USER_KEY = "mmt.auth.user";
const SESSION_KEY = "mmt.auth.session";

export type AuthUser = {
  userId: string | number;
  firstName: string;
  lastName: string;
  email: string;
  phone?: string;
  phoneNumber?: string;
  isVerified?: boolean;
  emailVerified?: boolean;
  profileImageUrl?: string;
  avatarUrl?: string;
  roles?: string[];
  role?: string;
  twoFactorEnabled?: boolean;
  preferences?: Record<string, any>;
};

export type AuthSession = {
  sessionId?: string;
  deviceId?: string;
  requiresTwoFactor?: boolean;
  twoFactorToken?: string;
  expiresAt?: number;
};

export function authUserFromResponse(r: AuthResponse | undefined | null): AuthUser | null {
  if (!r) return null;
  const u = r.user;
  const idRaw = u?.id ?? u?.userId ?? (r as any).userId;
  if (!u && !idRaw && !r.email) return null;
  return {
    userId: (idRaw as any) ?? (u?.id || u?.userId || r.email),
    firstName: u?.firstName ?? (r as any).firstName ?? "",
    lastName: u?.lastName ?? (r as any).lastName ?? "",
    email: u?.email ?? r.email ?? "",
    phone: u?.phone ?? u?.phoneNumber ?? (r as any).phone ?? (r as any).phoneNumber,
    phoneNumber: u?.phoneNumber ?? u?.phone ?? (r as any).phoneNumber ?? (r as any).phone,
    isVerified: u?.isVerified ?? u?.emailVerified ?? (r as any).isVerified,
    emailVerified: u?.emailVerified ?? u?.isVerified,
    profileImageUrl: u?.profileImageUrl ?? u?.avatarUrl ?? (r as any).profileImageUrl,
    avatarUrl: u?.avatarUrl ?? u?.profileImageUrl,
    roles: u?.roles ?? (u?.role ? [u.role] : undefined),
    role: u?.role,
    twoFactorEnabled: u?.twoFactorEnabled ?? r.requiresTwoFactor,
    preferences: u?.preferences,
  };
}

export function authSessionFromResponse(r: AuthResponse | undefined | null): AuthSession {
  if (!r) return {};
  const sess: AuthSession = {
    sessionId: r.sessionId,
    deviceId: r.deviceId,
    requiresTwoFactor: r.requiresTwoFactor,
    twoFactorToken: r.twoFactorToken,
  };
  if (r.expiresIn) sess.expiresAt = Date.now() + r.expiresIn * 1000;
  return sess;
}

function safeLS<T = any>(key: string, fallback: T): T {
  if (typeof window === "undefined") return fallback;
  try {
    const raw = window.localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : fallback;
  } catch {
    return fallback;
  }
}

function setLS(key: string, val: any) {
  if (typeof window === "undefined") return;
  try {
    if (val == null) window.localStorage.removeItem(key);
    else window.localStorage.setItem(key, typeof val === "string" ? val : JSON.stringify(val));
  } catch {}
}

export const tokenStorage = {
  get access(): string | null {
    if (typeof window === "undefined") return null;
    try {
      return window.localStorage.getItem(ACCESS_KEY);
    } catch {
      return null;
    }
  },
  get refresh(): string | null {
    if (typeof window === "undefined") return null;
    try {
      return window.localStorage.getItem(REFRESH_KEY);
    } catch {
      return null;
    }
  },
  get user(): AuthUser | null {
    return safeLS<AuthUser | null>(USER_KEY, null);
  },
  get session(): AuthSession {
    return safeLS<AuthSession>(SESSION_KEY, {});
  },

  setLogin(login: AuthResponse & { [k: string]: any }) {
    if (typeof window === "undefined") return;
    window.localStorage.setItem(ACCESS_KEY, login.accessToken ?? "");
    if (login.refreshToken) window.localStorage.setItem(REFRESH_KEY, login.refreshToken);
    const u = authUserFromResponse(login);
    if (u) window.localStorage.setItem(USER_KEY, JSON.stringify(u));
    window.localStorage.setItem(SESSION_KEY, JSON.stringify(authSessionFromResponse(login)));
  },

  updateUser(patch: Partial<AuthUser>) {
    const curr = tokenStorage.user;
    const next = { ...curr, ...patch } as AuthUser;
    setLS(USER_KEY, next);
  },

  updateProfile(profile: UserProfileResponse) {
    const curr = (tokenStorage.user ?? {}) as Partial<AuthUser>;
    const next: AuthUser = {
      userId: (profile.userId ?? profile.id ?? curr.userId) as any,
      firstName: profile.firstName ?? curr.firstName ?? "",
      lastName: profile.lastName ?? curr.lastName ?? "",
      email: profile.email ?? curr.email ?? "",
      phone: profile.phone ?? profile.phoneNumber ?? curr.phone,
      phoneNumber: profile.phoneNumber ?? profile.phone ?? curr.phoneNumber,
      profileImageUrl: profile.profileImageUrl ?? profile.avatarUrl ?? curr.profileImageUrl,
      avatarUrl: profile.avatarUrl ?? profile.profileImageUrl ?? curr.avatarUrl,
      isVerified:
        profile.isVerified ?? profile.emailVerified ?? curr.isVerified ?? false,
      emailVerified: profile.emailVerified ?? profile.isVerified ?? curr.emailVerified ?? false,
      roles: profile.roles ?? curr.roles,
      role: profile.role ?? curr.role,
      twoFactorEnabled: profile.twoFactorEnabled ?? curr.twoFactorEnabled ?? false,
      preferences: profile.preferences ?? curr.preferences,
    };
    setLS(USER_KEY, next);
  },

  setAccessToken(token: string) {
    if (typeof window === "undefined") return;
    window.localStorage.setItem(ACCESS_KEY, token);
  },

  clear() {
    if (typeof window === "undefined") return;
    window.localStorage.removeItem(ACCESS_KEY);
    window.localStorage.removeItem(REFRESH_KEY);
    window.localStorage.removeItem(USER_KEY);
    window.localStorage.removeItem(SESSION_KEY);
  },
};

export function isTokenInvalidMsg(msg?: string) {
  if (!msg) return false;
  return /TOKEN_INVALID|JWT|signature|expired|unauthorized/i.test(msg);
}
