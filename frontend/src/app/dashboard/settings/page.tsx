"use client";

import { useEffect, useState } from "react";
import { PageHeader, Card } from "@/components/dashboard/Panels";
import { useAuth } from "@/lib/auth/AuthProvider";
import { Button } from "@/components/ui/Button";
import { Input, Textarea, Checkbox } from "@/components/ui/Input";
import {
  User as UserIcon,
  Upload,
  ShieldCheck,
  Lock,
  Trash2,
  AlertTriangle,
  Save,
} from "lucide-react";
import { useForm } from "react-hook-form";
import { cn, initials, formatRelative } from "@/lib/utils";
import AuthShell, { AuthAlert } from "@/components/auth/AuthShell";
import { avatarOrDefault } from "@/lib/assets";
import { getApiError } from "@/lib/api/client";
import type { TwoFactorStatusResponse, SessionResponse } from "@/types/auth";

type ProfileForm = { firstName: string; lastName: string; phone: string; location: string; bio: string; website: string };
type PasswordForm = { currentPassword: string; newPassword: string; confirmPassword: string };
type NotifForm = {
  emailEnabled: boolean;
  pushEnabled: boolean;
  smsEnabled: boolean;
  newsAlerts: boolean;
  replyAlerts: boolean;
  marketing: boolean;
};

export default function SettingsPage() {
  const auth = useAuth();
  const [avatarPreview, setAvatarPreview] = useState<string | null>(null);
  const [avatarFile, setAvatarFile] = useState<File | null>(null);
  const [avatarMsg, setAvatarMsg] = useState<{ kind: "ok" | "err"; text: string } | null>(null);
  const [profileMsg, setProfileMsg] = useState<{ kind: "ok" | "err"; text: string } | null>(null);
  const [pwdMsg, setPwdMsg] = useState<{ kind: "ok" | "err"; text: string } | null>(null);
  const [notifMsg, setNotifMsg] = useState<{ kind: "ok" | "err"; text: string } | null>(null);
  const [deleteOpen, setDeleteOpen] = useState(false);
  const [deleteConfirm, setDeleteConfirm] = useState("");
  const [tab, setTab] = useState<"profile" | "password" | "notifications" | "security" | "danger">(
    "profile",
  );
  const [notifLoaded, setNotifLoaded] = useState(false);

  const {
    register: regProfile,
    handleSubmit: submitProfile,
    reset: resetProfile,
    formState: { errors: profileErrors },
  } = useForm<ProfileForm>({
    defaultValues: {
      firstName: auth.user?.firstName ?? "",
      lastName: auth.user?.lastName ?? "",
      phone: auth.user?.phone ?? auth.user?.phoneNumber ?? "",
      location: "",
      bio: "",
      website: "",
    },
    mode: "onTouched",
  });

  const {
    register: regPwd,
    handleSubmit: submitPwd,
    reset: resetPwd,
    watch,
    formState: { errors: pwdErrors },
  } = useForm<PasswordForm>({
    defaultValues: { currentPassword: "", newPassword: "", confirmPassword: "" },
    mode: "onTouched",
  });
  const newPwd = watch("newPassword");

  const {
    register: regNotif,
    handleSubmit: submitNotif,
    reset: resetNotif,
    setValue: setNotif,
    formState: { errors: notifErrors },
  } = useForm<NotifForm>({
    defaultValues: {
      emailEnabled: true,
      pushEnabled: true,
      smsEnabled: false,
      newsAlerts: true,
      replyAlerts: true,
      marketing: false,
    },
    mode: "onTouched",
  });

  useEffect(() => {
    resetProfile({
      firstName: auth.user?.firstName ?? "",
      lastName: auth.user?.lastName ?? "",
      phone: auth.user?.phone ?? auth.user?.phoneNumber ?? "",
      location: "",
      bio: "",
      website: "",
    });
  }, [auth.user, resetProfile]);

  useEffect(() => {
    if (notifLoaded) return;
    (async () => {
      try {
        const s = await auth.getNotificationSettings();
        resetNotif({
          emailEnabled: s.emailEnabled ?? true,
          pushEnabled: s.pushEnabled ?? true,
          smsEnabled: s.smsEnabled ?? false,
          newsAlerts: s.newsAlerts ?? true,
          replyAlerts: s.replyAlerts ?? true,
          marketing: s.marketing ?? false,
        });
      } catch {}
      setNotifLoaded(true);
    })();
  }, [auth, notifLoaded, resetNotif]);

  useEffect(() => {
    if (avatarFile) {
      const url = URL.createObjectURL(avatarFile);
      setAvatarPreview(url);
      return () => URL.revokeObjectURL(url);
    }
  }, [avatarFile]);

  async function uploadAvatar() {
    if (!avatarFile) return;
    try {
      const r = await auth.uploadProfileImage(avatarFile);
      setAvatarMsg({ kind: "ok", text: r.message || "Avatar updated." });
      setAvatarFile(null);
    } catch (e) {
      setAvatarMsg({ kind: "err", text: getApiError(e) });
    }
    setTimeout(() => setAvatarMsg(null), 4000);
  }

  async function removeAvatar() {
    if (!confirm("Remove your profile image?")) return;
    try {
      const r = await auth.deleteProfileImage();
      setAvatarPreview(null);
      setAvatarMsg({ kind: "ok", text: r.message || "Avatar removed." });
      setTimeout(() => setAvatarMsg(null), 4000);
    } catch (e) {
      setAvatarMsg({ kind: "err", text: getApiError(e) });
    }
  }

  async function saveProfile(form: ProfileForm) {
    try {
      await auth.updateProfile({
        firstName: form.firstName.trim() || undefined,
        lastName: form.lastName.trim() || undefined,
        phone: form.phone.trim() || undefined,
        location: form.location.trim() || undefined,
        bio: form.bio.trim() || undefined,
        website: form.website.trim() || undefined,
      });
      setProfileMsg({ kind: "ok", text: "Profile saved." });
      setTimeout(() => setProfileMsg(null), 4000);
    } catch (e) {
      setProfileMsg({ kind: "err", text: getApiError(e) || "Could not save profile." });
    }
  }

  async function saveNotifications(form: NotifForm) {
    try {
      await auth.updateNotificationSettings(form);
      setNotifMsg({ kind: "ok", text: "Notification preferences saved." });
      setTimeout(() => setNotifMsg(null), 4000);
    } catch (e) {
      setNotifMsg({ kind: "err", text: getApiError(e) || "Could not save notification settings." });
    }
  }

  async function changePassword(form: PasswordForm) {
    try {
      const r = await auth.changePassword({
        currentPassword: form.currentPassword,
        newPassword: form.newPassword,
      });
      setPwdMsg({ kind: "ok", text: r || "Password changed." });
      resetPwd();
    } catch (e) {
      setPwdMsg({ kind: "err", text: getApiError(e) || "Could not change password." });
    }
    setTimeout(() => setPwdMsg(null), 4000);
  }

  async function deleteAccount() {
    if (deleteConfirm.trim() !== "DELETE") return;
    try {
      const r = await auth.deleteAccount({ reason: "User requested via settings." });
      alert(r || "Account deleted.");
      window.location.href = "/";
    } catch (e) {
      alert(getApiError(e) || "Could not process that request.");
    }
  }

  const currentAvatar = avatarPreview ?? auth.user?.profileImageUrl ?? auth.user?.avatarUrl ?? null;
  const displayAvatar = avatarOrDefault(currentAvatar);

  const TABS: Array<{ id: typeof tab; label: string; icon: React.ComponentType<any>; hint: string }> = [
    { id: "profile", label: "Profile", icon: UserIcon, hint: "Name, byline avatar, contact, bio" },
    { id: "password", label: "Password", icon: Lock, hint: "Change your login password" },
    { id: "notifications", label: "Notifications", icon: ShieldCheck, hint: "Email, push, news alerts" },
    { id: "security", label: "2FA & sessions", icon: ShieldCheck, hint: "Two-factor, devices, logout all" },
    { id: "danger", label: "Delete account", icon: AlertTriangle, hint: "Irreversible" },
  ];

  return (
    <>
      <PageHeader
        eyebrow="Account"
        title="Account settings"
        description="Manage your profile, avatar, notifications, password, 2FA, devices, and account."
      />

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6">
        <aside className="lg:col-span-3">
          <Card className="space-y-2 p-2">
            {TABS.map(({ id, label, icon: Icon, hint }) => (
              <button
                key={id}
                type="button"
                onClick={() => setTab(id)}
                className={cn(
                  "w-full text-left px-4 py-3 rounded-sm transition flex items-start gap-3",
                  tab === id
                    ? "bg-ink-950 text-white"
                    : "hover:bg-ink-950/5 text-ink-950",
                )}
              >
                <div
                  className={cn(
                    "h-9 w-9 shrink-0 inline-flex items-center justify-center border-2",
                    tab === id
                      ? "border-white/20 text-white"
                      : "border-ink-950/20 text-ink-950",
                  )}
                >
                  <Icon className="h-4 w-4" />
                </div>
                <div className="min-w-0 flex-1">
                  <div className="font-bold uppercase tracking-widest text-xs leading-none">
                    {label}
                  </div>
                  <div
                    className={cn(
                      "mt-1.5 text-[11px] leading-snug",
                      tab === id ? "text-white/70" : "text-ink-600",
                    )}
                  >
                    {hint}
                  </div>
                </div>
              </button>
            ))}
          </Card>
        </aside>

        <section className="lg:col-span-9 space-y-6">
          {tab === "profile" ? (
            <>
              <Card className="space-y-5">
                <div>
                  <div className="ribbon text-xs mb-2">Byline</div>
                  <h2 className="font-headline text-2xl uppercase leading-none">Your public profile</h2>
                </div>
                <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                  <div className="md:col-span-1 space-y-4">
                    <div className="flex flex-col items-start gap-5">
                      <div className="h-32 w-32 rounded-full bg-ink-950 flex items-center justify-center border-2 border-ink-950 overflow-hidden">
                        <img src={displayAvatar} alt="" className="h-full w-full object-cover" />
                      </div>
                      <div className="w-full space-y-3 border-t-2 border-ink-950/10 pt-4">
                        <div>
                          <div className="font-bold truncate text-lg">
                            {auth.user?.firstName} {auth.user?.lastName}
                          </div>
                          <div className="text-xs text-ink-600 truncate">{auth.user?.email}</div>
                          <div className="mt-2 inline-flex items-center gap-1 text-[10px] font-bold uppercase tracking-widest text-ink-700">
                            <ShieldCheck className="h-3.5 w-3.5 text-news" />
                            {auth.user?.isVerified ? "Email verified" : "Verification pending"}
                          </div>
                        </div>
                        <Input
                          type="file"
                          accept="image/*"
                          label="Upload new avatar"
                          onChange={(e) => setAvatarFile(e.target.files?.[0] ?? null)}
                        />
                        {avatarMsg ? (
                          <div
                            className={cn(
                              "border-2 p-3 text-sm",
                              avatarMsg.kind === "ok"
                                ? "border-ink-950 bg-ink-950 text-white"
                                : "border-news bg-news-50 text-news-700",
                            )}
                          >
                            {avatarMsg.text}
                          </div>
                        ) : null}
                        <div className="flex gap-2 flex-wrap">
                          <Button variant="news" size="sm" onClick={uploadAvatar} disabled={!avatarFile}>
                            <Upload className="h-4 w-4" />
                            Save avatar
                          </Button>
                          <Button variant="outline" size="sm" onClick={removeAvatar}>
                            <Trash2 className="h-4 w-4" />
                            Remove
                          </Button>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div className="md:col-span-2 space-y-5">
                    <div>
                      <div className="ribbon text-xs mb-2">Details</div>
                      <h3 className="font-headline text-xl uppercase leading-none">
                        Name & contact details
                      </h3>
                    </div>
                    {profileMsg ? (
                      <div
                        className={cn(
                          "border-2 p-3 text-sm",
                          profileMsg.kind === "ok"
                            ? "border-ink-950 bg-ink-950 text-white"
                            : "border-news bg-news-50 text-news-700",
                        )}
                      >
                        {profileMsg.text}
                      </div>
                    ) : null}
                    <form onSubmit={submitProfile(saveProfile)} className="grid grid-cols-1 md:grid-cols-2 gap-4">
                      <Input
                        label="First name"
                        required
                        error={profileErrors.firstName?.message as any}
                        {...regProfile("firstName", { required: "Required.", minLength: 2 })}
                      />
                      <Input
                        label="Last name"
                        required
                        error={profileErrors.lastName?.message as any}
                        {...regProfile("lastName", { required: "Required.", minLength: 2 })}
                      />
                      <Input
                        label="Phone (optional)"
                        type="tel"
                        error={profileErrors.phone?.message as any}
                        {...regProfile("phone")}
                      />
                      <Input
                        label="City / location (optional)"
                        error={profileErrors.location?.message as any}
                        {...regProfile("location")}
                      />
                      <Input
                        label="Website (optional)"
                        type="url"
                        error={profileErrors.website?.message as any}
                        {...regProfile("website")}
                      />
                      <div className="space-y-1.5 md:col-span-2">
                        <div className="text-xs font-bold uppercase tracking-widest text-ink-800">
                          Short bio (optional)
                        </div>
                        <Textarea
                          rows={4}
                          placeholder="A short public bio — appears on your byline profile page."
                          error={profileErrors.bio?.message as any}
                          {...regProfile("bio")}
                        />
                      </div>
                      <div className="md:col-span-2 space-y-1.5">
                        <div className="text-xs font-bold uppercase tracking-widest text-ink-800">
                          Email (read-only)
                        </div>
                        <div className="h-11 px-3 border-2 border-ink-950 bg-ink-900/5 flex items-center text-sm text-ink-800">
                          {auth.user?.email}
                        </div>
                        <p className="text-[11px] text-ink-600">
                          Contact the newsroom to change your login email.
                        </p>
                      </div>
                      <div className="md:col-span-2 flex items-center justify-end">
                        <Button type="submit" variant="news" size="md">
                          <Save className="h-4 w-4" />
                          Save profile
                        </Button>
                      </div>
                    </form>
                  </div>
                </div>
              </Card>
            </>
          ) : null}

          {tab === "password" ? (
            <Card className="space-y-5">
              <div className="flex items-center gap-2">
                <Lock className="h-4 w-4 text-ink-700" />
                <div>
                  <div className="ribbon text-xs mb-2">Security</div>
                  <h2 className="font-headline text-xl uppercase leading-none">Change password</h2>
                </div>
              </div>
              {pwdMsg ? (
                <div
                  className={cn(
                    "border-2 p-3 text-sm",
                    pwdMsg.kind === "ok"
                      ? "border-ink-950 bg-ink-950 text-white"
                      : "border-news bg-news-50 text-news-700",
                  )}
                >
                  {pwdMsg.text}
                </div>
              ) : null}
              <form
                onSubmit={submitPwd(changePassword)}
                className="grid grid-cols-1 md:grid-cols-3 gap-4"
              >
                <Input
                  label="Current password"
                  type="password"
                  autoComplete="current-password"
                  required
                  error={pwdErrors.currentPassword?.message as any}
                  {...regPwd("currentPassword", { required: "Enter your current password." })}
                />
                <Input
                  label="New password"
                  type="password"
                  autoComplete="new-password"
                  required
                  error={pwdErrors.newPassword?.message as any}
                  hint="Use 8+ characters with numbers or symbols."
                  {...regPwd("newPassword", {
                    required: "Create a new password.",
                    minLength: { value: 8, message: "Use 8 characters minimum." },
                  })}
                />
                <Input
                  label="Confirm new password"
                  type="password"
                  autoComplete="new-password"
                  required
                  error={pwdErrors.confirmPassword?.message as any}
                  {...regPwd("confirmPassword", {
                    required: "Confirm the new password.",
                    validate: (v) => v === newPwd || "Passwords don't match.",
                  })}
                />
                <div className="md:col-span-3 flex items-center justify-end">
                  <Button type="submit" variant="primary" size="md">
                    <Lock className="h-4 w-4" />
                    Update password
                  </Button>
                </div>
              </form>
            </Card>
          ) : null}

          {tab === "notifications" ? (
            <NotificationSettingsCard
              submitNotif={submitNotif(saveNotifications)}
              regNotif={regNotif}
              notifErrors={notifErrors}
              notifMsg={notifMsg}
            />
          ) : null}
          {tab === "security" ? <SecurityTab /> : null}

          {tab === "danger" ? (
            <Card className="space-y-5 border-news">
              <div className="flex items-start gap-3">
                <div className="h-10 w-10 bg-news text-white inline-flex items-center justify-center border-2 border-ink-950 flex-shrink-0">
                  <AlertTriangle className="h-5 w-5" />
                </div>
                <div>
                  <div className="ribbon text-xs mb-2">Danger zone</div>
                  <h2 className="font-headline text-lg uppercase leading-none">Delete account</h2>
                  <p className="mt-2 text-sm text-ink-700">
                    Permanently remove your MapMyTimes account, login, and byline data. This cannot
                    be undone.
                  </p>
                </div>
              </div>
              {!deleteOpen ? (
                <Button
                  variant="outline"
                  className="w-full text-news-700 hover:bg-news hover:text-white hover:border-news"
                  onClick={() => setDeleteOpen(true)}
                >
                  <Trash2 className="h-4 w-4" />
                  Delete my account
                </Button>
              ) : (
                <div className="border-2 border-news bg-news-50 p-4 space-y-3">
                  <p className="text-xs uppercase tracking-widest font-bold text-news-700">
                    Type <span className="underline">DELETE</span> to confirm.
                  </p>
                  <Input
                    value={deleteConfirm}
                    onChange={(e) => setDeleteConfirm(e.target.value)}
                    placeholder="Type DELETE"
                  />
                  <div className="flex gap-2">
                    <Button
                      variant="news"
                      onClick={deleteAccount}
                      disabled={deleteConfirm.trim() !== "DELETE"}
                    >
                      Yes, delete
                    </Button>
                    <Button
                      variant="outline"
                      onClick={() => {
                        setDeleteOpen(false);
                        setDeleteConfirm("");
                      }}
                    >
                      Cancel
                    </Button>
                  </div>
                </div>
              )}
            </Card>
          ) : null}
        </section>
      </div>
    </>
  );
}

function NotificationSettingsCard({
  submitNotif,
  regNotif,
  notifErrors,
  notifMsg,
}: {
  submitNotif: (e?: any) => Promise<void>;
  regNotif: any;
  notifErrors: any;
  notifMsg: { kind: "ok" | "err"; text: string } | null;
}) {
  return (
    <form onSubmit={submitNotif} className="Card space-y-5 p-0 border-0">
      <Card className="space-y-5">
        <div>
          <div className="ribbon text-xs mb-2">Channels</div>
          <h2 className="font-headline text-xl uppercase leading-none">
            Notification preferences
          </h2>
          <p className="mt-2 text-sm text-ink-700">
            Choose how MapMyTimes reaches you for stories, replies, and product updates.
          </p>
        </div>
        {notifMsg ? (
          <div
            className={cn(
              "border-2 p-3 text-sm",
              notifMsg.kind === "ok"
                ? "border-ink-950 bg-ink-950 text-white"
                : "border-news bg-news-50 text-news-700",
            )}
          >
            {notifMsg.text}
          </div>
        ) : null}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
          <Checkbox
            label="Email notifications"
            hint="Story summaries and important alerts in your inbox."
            {...regNotif("emailEnabled")}
          />
          <Checkbox
            label="Push notifications"
            hint="Browser push for live news and replies."
            {...regNotif("pushEnabled")}
          />
          <Checkbox
            label="SMS alerts"
            hint="Rare SMS only for account security."
            {...regNotif("smsEnabled")}
          />
        </div>
        <div className="border-t-2 border-ink-950/10 pt-5">
          <div className="ribbon text-xs mb-3">Topics</div>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
            <Checkbox
              label="News & top stories"
              hint="Daily top headlines and featured reports."
              {...regNotif("newsAlerts")}
            />
            <Checkbox
              label="Replies & comments"
              hint="When someone replies to or mentions you."
              {...regNotif("replyAlerts")}
            />
            <Checkbox
              label="Marketing"
              hint="Occasional product, event, and newsletter updates."
              {...regNotif("marketing")}
            />
          </div>
        </div>
        <div className="flex items-center justify-end">
          <Button type="submit" variant="news" size="md">
            <Save className="h-4 w-4" />
            Save preferences
          </Button>
        </div>
      </Card>
    </form>
  );
}

function SecurityTab() {
  const auth = useAuth();
  const [tfStatus, setTfStatus] = useState<TwoFactorStatusResponse | null>(null);
  const [setup, setSetup] = useState<TwoFactorStatusResponse | null>(null);
  const [sessions, setSessions] = useState<SessionResponse[]>([]);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState("");
  const [msg, setMsg] = useState("");
  const [code, setCode] = useState("");
  const [disableCode, setDisableCode] = useState("");
  const [backupCodes, setBackupCodes] = useState<string[]>([]);

  async function reload() {
    try {
      const [s, tf] = await Promise.all([auth.sessions(), auth.twoFactorStatus()]);
      setSessions(Array.isArray(s) ? s : []);
      setTfStatus(tf);
    } catch (e) {
      setErr(getApiError(e) || "Could not load security data.");
    }
  }

  useEffect(() => {
    reload();
  }, [auth]);

  async function startSetup() {
    setBusy(true);
    setErr("");
    try {
      const s = await auth.twoFactorSetup();
      setSetup(s);
    } catch (e) {
      setErr(getApiError(e) || "Could not start 2FA setup.");
    } finally {
      setBusy(false);
    }
  }

  async function enable2fa() {
    if (!code.trim() || !setup?.secret) return;
    setBusy(true);
    setErr("");
    try {
      const r = await auth.twoFactorEnable({ code: code.trim(), secret: setup.secret });
      setTfStatus(r);
      setSetup(null);
      setCode("");
      const bc = await auth.twoFactorBackupCodes();
      setBackupCodes(bc.codes ?? []);
      setMsg(r.message || "Two-factor authentication enabled.");
      setTimeout(() => setMsg(""), 5000);
    } catch (e) {
      setErr(getApiError(e) || "Could not enable 2FA — check your code.");
    } finally {
      setBusy(false);
    }
  }

  async function disable2fa() {
    if (!disableCode.trim()) return;
    setBusy(true);
    setErr("");
    try {
      const r = await auth.twoFactorDisable(disableCode.trim());
      setTfStatus(r);
      setDisableCode("");
      setMsg(r.message || "Two-factor authentication disabled.");
      setTimeout(() => setMsg(""), 5000);
    } catch (e) {
      setErr(getApiError(e) || "Could not disable 2FA.");
    } finally {
      setBusy(false);
    }
  }

  async function logoutAll() {
    if (!confirm("Log out every other device/session?")) return;
    try {
      await auth.logout({ all: true });
      window.location.href = "/login";
    } catch (e) {
      setErr(getApiError(e) || "Could not log out all sessions.");
    }
  }

  async function terminate(sid: string) {
    try {
      const m = await auth.terminateSession(sid);
      setMsg(m || "Session ended.");
      await reload();
      setTimeout(() => setMsg(""), 4000);
    } catch (e) {
      setErr(getApiError(e) || "Could not terminate that session.");
    }
  }

  return (
    <>
      <Card className="space-y-5">
        <div className="flex items-center gap-2">
          <ShieldCheck className="h-4 w-4 text-ink-700" />
          <div>
            <div className="ribbon text-xs mb-2">Two-factor authentication</div>
            <h2 className="font-headline text-xl uppercase leading-none">
              2FA protects your account even if your password leaks.
            </h2>
          </div>
        </div>
        {msg ? <AuthAlert type="success">{msg}</AuthAlert> : null}
        {err ? <AuthAlert type="error">{err}</AuthAlert> : null}

        <div className="border-2 border-ink-950/10 p-5">
          <div className="flex flex-col md:flex-row md:items-start md:justify-between gap-4">
            <div>
              <div className="font-bold uppercase tracking-widest text-sm">
                Status:{" "}
                <span className={tfStatus?.enabled ? "text-green-700" : "text-ink-700"}>
                  {tfStatus?.enabled ? "Enabled" : "Not enabled"}
                </span>
              </div>
              <p className="mt-2 text-sm text-ink-700">
                Use an authenticator app (Google Authenticator, 1Password, Authy) for 6-digit
                time-based one-time codes.
              </p>
            </div>
            {!tfStatus?.enabled ? (
              <Button variant="news" onClick={startSetup} disabled={busy || !!setup}>
                <ShieldCheck className="h-4 w-4" />
                {busy ? "Preparing…" : setup ? "Setup in progress" : "Enable 2FA"}
              </Button>
            ) : (
              <div className="space-y-3 md:w-[340px]">
                <div className="flex items-end gap-2">
                  <Input
                    label="Enter 2FA code to disable"
                    inputMode="numeric"
                    placeholder="123456"
                    value={disableCode}
                    onChange={(e) => setDisableCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                  />
                </div>
                <Button variant="outline" onClick={disable2fa} disabled={busy || !disableCode}>
                  Disable 2FA
                </Button>
              </div>
            )}
          </div>

          {setup && !tfStatus?.enabled ? (
            <div className="mt-6 grid grid-cols-1 md:grid-cols-2 gap-6 border-t-2 border-ink-950/10 pt-5">
              <div className="space-y-3">
                <div className="ribbon text-xs">1. Scan QR code</div>
                <div className="w-full aspect-square border-2 border-ink-950/10 bg-white p-4 flex items-center justify-center">
                  {setup.qrCodeUrl ? (
                    <img src={setup.qrCodeUrl} alt="2FA QR code" className="max-h-full max-w-full" />
                  ) : (
                    <div className="text-xs text-ink-700 text-center p-4">
                      No QR code returned — copy the manual secret below and paste it into your
                      authenticator app.
                    </div>
                  )}
                </div>
                <div className="space-y-1.5">
                  <div className="text-[10px] uppercase tracking-widest font-bold text-ink-700">
                    Manual entry secret
                  </div>
                  <div className="h-11 px-3 border-2 border-ink-950 bg-ink-900/5 flex items-center text-sm font-mono break-all">
                    {setup.secret}
                  </div>
                </div>
              </div>
              <div className="space-y-3">
                <div className="ribbon text-xs">2. Enter the 6-digit code</div>
                <Input
                  label="Authenticator code"
                  inputMode="numeric"
                  placeholder="123456"
                  value={code}
                  onChange={(e) => setCode(e.target.value.replace(/\D/g, "").slice(0, 6))}
                />
                <Button variant="news" onClick={enable2fa} disabled={busy || code.length < 6}>
                  {busy ? "Verifying…" : "Confirm & enable"}
                </Button>
              </div>
            </div>
          ) : null}

          {backupCodes.length ? (
            <div className="mt-6 border-t-2 border-ink-950/10 pt-5">
              <div className="flex items-end justify-between gap-3 mb-3">
                <div>
                  <div className="ribbon text-xs mb-1">Backup codes</div>
                  <p className="text-sm text-ink-700">
                    Save these codes somewhere safe. Use any one of them if you lose access to your
                    authenticator app.
                  </p>
                </div>
                <button
                  type="button"
                  onClick={() => navigator.clipboard?.writeText(backupCodes.join("\n"))}
                  className="text-[11px] uppercase tracking-widest font-bold text-news hover:underline"
                >
                  Copy all
                </button>
              </div>
              <div className="grid grid-cols-2 md:grid-cols-4 gap-2">
                {backupCodes.map((c) => (
                  <div
                    key={c}
                    className="h-10 px-3 border-2 border-ink-950/20 bg-white flex items-center font-mono text-xs"
                  >
                    {c}
                  </div>
                ))}
              </div>
            </div>
          ) : null}
        </div>
      </Card>

      <Card className="space-y-5">
        <div className="flex flex-col md:flex-row md:items-center md:justify-between gap-4">
          <div>
            <div className="ribbon text-xs mb-2">Sessions & devices</div>
            <h2 className="font-headline text-xl uppercase leading-none">
              Signed in devices
            </h2>
          </div>
          <Button variant="outline" onClick={logoutAll}>
            <Lock className="h-4 w-4" />
            Log out all other devices
          </Button>
        </div>
        {sessions.length === 0 ? (
          <div className="border-2 border-ink-950/10 p-5 text-sm text-ink-700">
            No session data returned by the server. You are currently signed in on this device.
          </div>
        ) : (
          <div className="border-2 border-ink-950/10 divide-y-2 divide-ink-950/10">
            {sessions.map((s, i) => {
              const deviceName =
                s.device?.name ||
                (s.device?.os || s.device?.browser
                  ? `${s.device?.browser ?? ""} ${s.device?.os ?? ""}`.trim() || `Session ${i + 1}`
                  : `Session ${i + 1}`);
              const location = s.device?.location || s.location || "—";
              const ip = s.device?.ipAddress || s.ipAddress || "";
              const when = s.lastActiveAt || s.startedAt || "";
              return (
                <div
                  key={(s.id as any) ?? (s.sessionId as any) ?? i}
                  className="p-4 flex flex-col md:flex-row md:items-center gap-4 justify-between"
                >
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2 flex-wrap">
                      <div className="font-bold uppercase tracking-widest text-sm truncate">
                        {deviceName}
                      </div>
                      {s.isCurrent ? (
                        <span className="h-5 px-2 inline-flex items-center text-[10px] font-bold uppercase tracking-widest bg-news text-white">
                          This device
                        </span>
                      ) : null}
                    </div>
                    <div className="mt-1.5 grid grid-cols-1 sm:grid-cols-3 gap-x-4 gap-y-1 text-[11px] text-ink-700">
                      <div>Location: {location}</div>
                      {ip ? <div>IP: {ip}</div> : null}
                      {when ? <div>Last active: {formatRelative(when)}</div> : null}
                    </div>
                  </div>
                  {!s.isCurrent ? (
                    <Button variant="outline" size="sm" onClick={() => terminate((s.sessionId ?? (s.id as any)) as string)}>
                      <Trash2 className="h-4 w-4" />
                      Sign out
                    </Button>
                  ) : null}
                </div>
              );
            })}
          </div>
        )}
      </Card>
    </>
  );
}
