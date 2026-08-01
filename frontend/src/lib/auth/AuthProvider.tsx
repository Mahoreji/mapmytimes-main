"use client";

import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { authApi, oauth2 as oauth2Api } from "@/lib/api/authApi";
import {
  authUserFromResponse,
  tokenStorage,
  type AuthSession,
  type AuthUser,
} from "@/lib/auth/token-storage";
import type {
  ChangePasswordRequest,
  DeleteAccountRequest,
  ForgotPasswordRequest,
  ForgotPasswordResponse,
  ForgotPasswordStep2Request,
  LoginRequest,
  LoginWithOtpRequest,
  RegisterRequest,
  RegisterResponse,
  ResendVerificationRequest,
  ResetPasswordRequest,
  ResetPasswordResponse,
  SendOtpRequest,
  TwoFactorEnableRequest,
  TwoFactorStatusResponse,
  UpdateUserProfileRequest,
  UserProfileResponse,
  VerifyEmailRequest,
  VerifyEmailResponse,
  ImageUploadResponse,
  ImageDeleteResponse,
  NotificationSettings,
  SessionResponse,
  AccountStatusResponse,
  OAuth2LoginRequest,
} from "@/types/auth";

type AuthStatus = "loading" | "authenticated" | "anonymous";

interface AuthContextValue {
  user: AuthUser | null;
  status: AuthStatus;
  isAuthenticated: boolean;
  accessToken: string | null;
  session: AuthSession;

  register: (req: RegisterRequest) => Promise<RegisterResponse>;
  verifyEmail: (req: VerifyEmailRequest) => Promise<VerifyEmailResponse>;
  verifyOtp: (req: VerifyEmailRequest) => Promise<VerifyEmailResponse>;
  sendVerificationOtp: (req: ResendVerificationRequest) => Promise<string>;
  resendVerification: (req: ResendVerificationRequest) => Promise<string>;
  resendOtp: (req: ResendVerificationRequest) => Promise<string>;
  sendLoginOtp: (req: SendOtpRequest) => Promise<string>;
  login: (req: LoginRequest) => Promise<void>;
  loginWithOtp: (req: LoginWithOtpRequest) => Promise<void>;
  oauth2Callback: (req: OAuth2LoginRequest) => Promise<void>;
  logout: (opts?: { sessionId?: string; all?: boolean }) => Promise<void>;
  forgotPassword: (req: ForgotPasswordRequest) => Promise<ForgotPasswordResponse>;
  forgotPasswordStep2: (req: ForgotPasswordStep2Request) => Promise<ForgotPasswordResponse>;
  resetPassword: (req: ResetPasswordRequest) => Promise<ResetPasswordResponse>;
  changePassword: (req: ChangePasswordRequest) => Promise<string>;
  deleteAccount: (req: DeleteAccountRequest) => Promise<string>;
  checkEmail: (email: string) => Promise<{ available: boolean; exists: boolean; email: string }>;
  accountStatus: () => Promise<AccountStatusResponse>;

  refreshProfile: () => Promise<UserProfileResponse>;
  updateProfile: (req: UpdateUserProfileRequest) => Promise<UserProfileResponse>;
  getPreferences: () => Promise<Record<string, any>>;
  updatePreferences: (body: Record<string, any>) => Promise<Record<string, any>>;
  getNotificationSettings: () => Promise<NotificationSettings>;
  updateNotificationSettings: (body: NotificationSettings) => Promise<NotificationSettings>;
  uploadProfileImage: (file: File) => Promise<ImageUploadResponse>;
  deleteProfileImage: () => Promise<ImageDeleteResponse>;

  sessions: () => Promise<SessionResponse[]>;
  terminateSession: (sessionId: string) => Promise<string>;
  twoFactorStatus: () => Promise<TwoFactorStatusResponse>;
  twoFactorSetup: () => Promise<TwoFactorStatusResponse>;
  twoFactorEnable: (req: TwoFactorEnableRequest) => Promise<TwoFactorStatusResponse>;
  twoFactorDisable: (code: string) => Promise<TwoFactorStatusResponse>;
  twoFactorBackupCodes: () => Promise<{ codes: string[]; remaining: number }>;
}

const AuthContext = createContext<AuthContextValue | undefined>(undefined);

export function AuthProvider({ children }: { children: ReactNode }) {
  const [user, setUser] = useState<AuthUser | null>(tokenStorage.user);
  const [session, setSession] = useState<AuthSession>(tokenStorage.session);
  const [status, setStatus] = useState<AuthStatus>(() =>
    tokenStorage.access ? "loading" : "anonymous",
  );
  const [accessToken, setAccessToken] = useState<string | null>(tokenStorage.access);

  useEffect(() => {
    setAccessToken(tokenStorage.access);
    setUser(tokenStorage.user);
    setSession(tokenStorage.session);
    setStatus(tokenStorage.access ? "authenticated" : "anonymous");
  }, []);

  const hydrateFromAuthResponse = useCallback((login: any) => {
    tokenStorage.setLogin(login);
    setAccessToken(login.accessToken ?? tokenStorage.access);
    setUser(tokenStorage.user);
    setSession(tokenStorage.session);
    setStatus("authenticated");
  }, []);

  const clearAuth = useCallback(() => {
    tokenStorage.clear();
    setAccessToken(null);
    setUser(null);
    setSession({});
    setStatus("anonymous");
  }, []);

  const register = useCallback(async (req: RegisterRequest) => {
    return authApi.register(req);
  }, []);

  const verifyEmail = useCallback(async (req: VerifyEmailRequest) => {
    const res = await authApi.verifyEmail(req);
    if (res.verified && res.accessToken) {
      hydrateFromAuthResponse({
        isAuthenticated: true,
        accessToken: res.accessToken,
        refreshToken: res.refreshToken,
        tokenType: res.tokenType || "Bearer",
        user: res.user,
        email: req.email,
      } as any);
    }
    return res;
  }, [hydrateFromAuthResponse]);

  const verifyOtp = useCallback(async (req: VerifyEmailRequest) => verifyEmail(req), [verifyEmail]);

  const sendVerificationOtp = useCallback(async (req: ResendVerificationRequest) => {
    const r = await authApi.sendVerificationOtp(req);
    return (r as any)?.message ?? "";
  }, []);

  const resendVerification = useCallback(async (req: ResendVerificationRequest) => {
    const r = await authApi.resendVerification(req);
    return (r as any)?.message ?? "";
  }, []);

  const resendOtp = useCallback(
    async (req: ResendVerificationRequest) => sendVerificationOtp(req),
    [sendVerificationOtp],
  );

  const sendLoginOtp = useCallback(async (req: SendOtpRequest) => {
    const r = await authApi.sendLoginOtp(req);
    return (r as any)?.message ?? "";
  }, []);

  const login = useCallback(
    async (req: LoginRequest) => {
      const res = await authApi.login(req);
      hydrateFromAuthResponse(res);
    },
    [hydrateFromAuthResponse],
  );

  const loginWithOtp = useCallback(
    async (req: LoginWithOtpRequest) => {
      const res = await authApi.loginWithOtp(req);
      hydrateFromAuthResponse(res);
    },
    [hydrateFromAuthResponse],
  );

  const oauth2Callback = useCallback(
    async (req: OAuth2LoginRequest) => {
      const res = await oauth2Api.callback(req);
      hydrateFromAuthResponse(res);
    },
    [hydrateFromAuthResponse],
  );

  const logout = useCallback(async (opts?: { sessionId?: string; all?: boolean }) => {
    try {
      if (opts?.all) await authApi.logoutAll();
      else await authApi.logout(opts?.sessionId);
    } finally {
      if (!opts?.sessionId || opts?.all) {
        clearAuth();
      }
    }
  }, [clearAuth]);

  const forgotPassword = useCallback(async (req: ForgotPasswordRequest) => {
    return authApi.forgotPasswordStep1(req);
  }, []);

  const forgotPasswordStep2 = useCallback(async (req: ForgotPasswordStep2Request) => {
    return authApi.forgotPasswordStep2(req);
  }, []);

  const resetPassword = useCallback(async (req: ResetPasswordRequest) => {
    return authApi.resetPassword(req);
  }, []);

  const changePassword = useCallback(async (req: ChangePasswordRequest) => {
    const r = await authApi.changePassword(req);
    return (r as any)?.message ?? "";
  }, []);

  const deleteAccount = useCallback(async (req: DeleteAccountRequest) => {
    const r = await authApi.deleteAccount(req);
    clearAuth();
    return (r as any)?.message ?? "";
  }, [clearAuth]);

  const checkEmail = useCallback(async (email: string) => {
    const res = (await authApi.checkEmail(email)) as any;
    return {
      available: Boolean(res.available ?? !res.exists),
      exists: Boolean(res.exists ?? !res.available),
      email: res.email ?? email,
    };
  }, []);

  const accountStatus = useCallback(async () => authApi.accountStatus(), []);

  const refreshProfile = useCallback(async () => {
    const profile = await authApi.profile();
    tokenStorage.updateProfile(profile);
    setUser(tokenStorage.user);
    return profile;
  }, []);

  const updateProfile = useCallback(async (req: UpdateUserProfileRequest) => {
    const profile = await authApi.updateProfile(req);
    tokenStorage.updateProfile(profile);
    setUser(tokenStorage.user);
    return profile;
  }, []);

  const getPreferences = useCallback(async () => authApi.preferences(), []);
  const updatePreferences = useCallback(
    async (body: Record<string, any>) => authApi.updatePreferences(body),
    [],
  );

  const getNotificationSettings = useCallback(
    async () => authApi.notificationSettings(),
    [],
  );
  const updateNotificationSettings = useCallback(
    async (body: NotificationSettings) => authApi.updateNotificationSettings(body),
    [],
  );

  const uploadProfileImage = useCallback(async (file: File) => {
    const res = await authApi.uploadProfileImage(file);
    const url = res.profileImageUrl ?? res.avatarUrl ?? res.imageUrl;
    if (url && user) {
      const updated = { ...user, profileImageUrl: url, avatarUrl: url };
      tokenStorage.updateUser(updated);
      setUser(updated);
    }
    return res;
  }, [user]);

  const deleteProfileImage = useCallback(async () => {
    const res = await authApi.deleteProfileImage();
    if (user) {
      const updated: AuthUser = { ...user, profileImageUrl: undefined, avatarUrl: undefined };
      tokenStorage.updateUser(updated);
      setUser(updated);
    }
    return res;
  }, [user]);

  const sessions = useCallback(async () => authApi.listSessions(), []);
  const terminateSession = useCallback(async (sessionId: string) => {
    const r = await authApi.terminateSession(sessionId);
    return (r as any)?.message ?? "";
  }, []);

  const twoFactorStatus = useCallback(async () => authApi.twoFactorStatus(), []);
  const twoFactorSetup = useCallback(async () => authApi.twoFactorSetup(), []);
  const twoFactorEnable = useCallback(
    async (req: TwoFactorEnableRequest) => authApi.twoFactorEnable(req),
    [],
  );
  const twoFactorDisable = useCallback(
    async (code: string) => authApi.twoFactorDisable(code),
    [],
  );
  const twoFactorBackupCodes = useCallback(async () => authApi.twoFactorBackupCodes(), []);

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      status,
      isAuthenticated: status === "authenticated",
      accessToken,
      session,
      register,
      verifyEmail,
      verifyOtp,
      sendVerificationOtp,
      resendVerification,
      resendOtp,
      sendLoginOtp,
      login,
      loginWithOtp,
      oauth2Callback,
      logout,
      forgotPassword,
      forgotPasswordStep2,
      resetPassword,
      changePassword,
      deleteAccount,
      checkEmail,
      accountStatus,
      refreshProfile,
      updateProfile,
      getPreferences,
      updatePreferences,
      getNotificationSettings,
      updateNotificationSettings,
      uploadProfileImage,
      deleteProfileImage,
      sessions,
      terminateSession,
      twoFactorStatus,
      twoFactorSetup,
      twoFactorEnable,
      twoFactorDisable,
      twoFactorBackupCodes,
    }),
    [
      user,
      status,
      accessToken,
      session,
      register,
      verifyEmail,
      verifyOtp,
      sendVerificationOtp,
      resendVerification,
      resendOtp,
      sendLoginOtp,
      login,
      loginWithOtp,
      oauth2Callback,
      logout,
      forgotPassword,
      forgotPasswordStep2,
      resetPassword,
      changePassword,
      deleteAccount,
      checkEmail,
      accountStatus,
      refreshProfile,
      updateProfile,
      getPreferences,
      updatePreferences,
      getNotificationSettings,
      updateNotificationSettings,
      uploadProfileImage,
      deleteProfileImage,
      sessions,
      terminateSession,
      twoFactorStatus,
      twoFactorSetup,
      twoFactorEnable,
      twoFactorDisable,
      twoFactorBackupCodes,
    ],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used inside <AuthProvider/>");
  return ctx;
}
