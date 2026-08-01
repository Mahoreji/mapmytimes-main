import axios, { AxiosError } from "axios";
import { SITE } from "@/lib/utils";
import { tokenStorage, isTokenInvalidMsg } from "@/lib/auth/token-storage";
import type { APIResponse } from "@/types/common";
import type { RefreshTokenRequest, RefreshTokenResponse } from "@/types/auth";

const baseURL = SITE.apiBase.replace(/\/$/, "");

export const http = axios.create({
  baseURL,
  timeout: 60_000,
  headers: {
    Accept: "application/json",
  },
});

let isRefreshing = false;
let refreshQueue: Array<(token: string | null) => void> = [];

function flushQueue(token: string | null) {
  refreshQueue.forEach((resolve) => resolve(token));
  refreshQueue = [];
}

function attachAuth(config: any) {
  const access = tokenStorage.access;
  if (access && !config.headers?.Authorization) {
    config.headers = { ...(config.headers || {}), Authorization: `Bearer ${access}` };
  }
  return config;
}

http.interceptors.request.use(
  (config) => attachAuth(config),
  (error) => Promise.reject(error),
);

http.interceptors.response.use(
  (response) => response,
  async (err: AxiosError<APIResponse<any>>) => {
    const config = err.config as any;
    const status = err.response?.status ?? 0;
    const message =
      (typeof err.response?.data?.message === "string" ? err.response.data.message : undefined) ||
      err.message;

    const is401 = status === 401;
    const isAuth = is401 || (status >= 400 && status < 500 && isTokenInvalidMsg(message));

    if (!isAuth || config._retried) {
      return Promise.reject(err);
    }

    if (typeof window === "undefined") {
      return Promise.reject(err);
    }

    if (isRefreshing) {
      return new Promise<string | null>((resolve) => refreshQueue.push(resolve)).then((tok) => {
        if (!tok) return Promise.reject(err);
        config.headers = { ...(config.headers || {}), Authorization: `Bearer ${tok}` };
        config._retried = true;
        return http.request(config);
      });
    }

    isRefreshing = true;
    try {
      const refresh = tokenStorage.refresh;
      if (!refresh) {
        tokenStorage.clear();
        flushQueue(null);
        return Promise.reject(err);
      }
      const payload: RefreshTokenRequest = { refreshToken: refresh };
      const { data } = await axios.post<RefreshTokenResponse>(
        `${baseURL}/api/v1/auth/refresh`,
        payload,
        { headers: { "Content-Type": "application/json" } },
      );
      const next = data.accessToken ?? (data as any).data?.accessToken;
      if (!next) {
        tokenStorage.clear();
        flushQueue(null);
        return Promise.reject(err);
      }
      tokenStorage.setAccessToken(next);
      if ((data as any).refreshToken) {
        try {
          window.localStorage.setItem("mmt.auth.refresh", (data as any).refreshToken);
        } catch {}
      }
      flushQueue(next);
      config.headers = { ...(config.headers || {}), Authorization: `Bearer ${next}` };
      config._retried = true;
      return http.request(config);
    } catch {
      tokenStorage.clear();
      flushQueue(null);
      return Promise.reject(err);
    } finally {
      isRefreshing = false;
    }
  },
);

export function unwrap<T>(response: { data: APIResponse<T> }): T {
  const envelope = response.data;
  if (envelope && typeof envelope === "object" && "data" in envelope) {
    return envelope.data as T;
  }
  return envelope as unknown as T;
}

export function getApiError(error: unknown): string {
  if (!error) return "Something went wrong.";
  if (axios.isAxiosError(error)) {
    const d = error.response?.data as APIResponse<any> | undefined;
    const msgs = d?.errors && d.errors.length ? d.errors : [];
    const m = d?.message;
    if (m) msgs.unshift(m);
    if (msgs.length) return msgs.join(" ");
    return error.message || "Request failed.";
  }
  if (error instanceof Error) return error.message;
  return String(error);
}
