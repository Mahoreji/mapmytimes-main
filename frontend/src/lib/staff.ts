import type { Department } from "@/types/blog";

export const DEPARTMENT_LABELS: Record<Department | string, string> = {
  GROUND_REPORTER: "Ground Reporter",
  CAMERAMAN: "Cameraman",
  VIDEOGRAPHER: "Videographer",
  EDITOR: "Editor",
  SUB_EDITOR: "Sub Editor",
  COPY_EDITOR: "Copy Editor",
  ADMIN_STAFF: "Admin Staff",
  FRANCHISE_HEAD: "Franchise Head",
  HR: "Human Resources",
  DESIGNER: "Designer",
  PHOTOGRAPHER: "Photographer",
  JOURNALIST: "Journalist",
  SENIOR_EDITOR: "Senior Editor",
  CHIEF_EDITOR: "Chief Editor",
  NEWS_ANCHOR: "News Anchor",
  PRODUCER: "Producer",
  ASSOCIATE_PRODUCER: "Associate Producer",
  CONTENT_WRITER: "Content Writer",
  DIGITAL_MARKETING: "Digital Marketing",
  SEO_SPECIALIST: "SEO Specialist",
  GRAPHIC_DESIGNER: "Graphic Designer",
  VIDEO_EDITOR: "Video Editor",
  SOUND_ENGINEER: "Sound Engineer",
  TECHNICIAN: "Technician",
  RESEARCHER: "Researcher",
  CORRESPONDENT: "Correspondent",
  BUREAU_CHIEF: "Bureau Chief",
  NEWS_DIRECTOR: "News Director",
  MANAGING_EDITOR: "Managing Editor",
  FEATURE_WRITER: "Feature Writer",
  COLUMNIST: "Columnist",
  CARTOONIST: "Cartoonist",
  LIBRARIAN: "Librarian",
  ARCHIVIST: "Archivist",
  TRANSLATOR: "Translator",
  PROOFREADER: "Proofreader",
  FRONT_DESK: "Front Desk",
  ACCOUNTS: "Accounts",
  LEGAL: "Legal",
  IT_SUPPORT: "IT Support",
  SALES_EXECUTIVE: "Sales Executive",
  MARKETING_EXECUTIVE: "Marketing Executive",
  PUBLIC_RELATIONS: "Public Relations",
  EVENT_MANAGER: "Event Manager",
  TRAINEE: "Trainee",
  INTERN: "Intern",
};

export const DEPARTMENT_OPTIONS: { value: Department | string; label: string }[] =
  Object.entries(DEPARTMENT_LABELS)
    .map(([value, label]) => ({ value, label }))
    .sort((a, b) => a.label.localeCompare(b.label));

export function departmentLabel(d: Department | string | null | undefined): string {
  if (!d) return "—";
  return DEPARTMENT_LABELS[d] || String(d).replace(/_/g, " ");
}

export const STATUS_COLOR: Record<string, { bg: string; text: string; ring: string }> = {
  ACTIVE: { bg: "bg-emerald-50", text: "text-emerald-700", ring: "ring-emerald-600/20" },
  SUSPENDED: { bg: "bg-amber-50", text: "text-amber-700", ring: "ring-amber-600/20" },
  EXPIRED: { bg: "bg-rose-50", text: "text-rose-700", ring: "ring-rose-600/20" },
  REVOKED: { bg: "bg-ink-900/10", text: "text-ink-800", ring: "ring-ink-900/20" },
  UNDER_REVIEW: { bg: "bg-sky-50", text: "text-sky-700", ring: "ring-sky-600/20" },
  PENDING_APPROVAL: { bg: "bg-violet-50", text: "text-violet-700", ring: "ring-violet-600/20" },
  TRANSFERRED: { bg: "bg-indigo-50", text: "text-indigo-700", ring: "ring-indigo-600/20" },
  RESIGNED: { bg: "bg-zinc-100", text: "text-zinc-700", ring: "ring-zinc-600/20" },
  RETIRED: { bg: "bg-stone-100", text: "text-stone-700", ring: "ring-stone-600/20" },
};

export function statusPill(s: string) {
  const c = STATUS_COLOR[s] ?? {
    bg: "bg-ink-50",
    text: "text-ink-700",
    ring: "ring-ink-600/20",
  };
  return c;
}

export function formatDateCompact(s: string | null | undefined): string {
  if (!s) return "—";
  try {
    const d = new Date(s);
    if (Number.isNaN(d.getTime())) return s;
    return d.toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric",
    });
  } catch {
    return s;
  }
}
