import { NextRequest, NextResponse } from "next/server";

export const dynamic = "force-dynamic";
export const runtime = "nodejs";

type StaffStatus =
  | "ACTIVE"
  | "VERIFIED"
  | "PENDING"
  | "SUSPENDED"
  | "REVOKED"
  | "EXPIRED"
  | "RETIRED";

type Department =
  | "FOUNDER"
  | "EDITOR_IN_CHIEF"
  | "MANAGING_EDITOR"
  | "ASSOCIATE_EDITOR"
  | "CHIEF_OF_BUREAU"
  | "BUREAU_CHIEF"
  | "SENIOR_GROUND_REPORTER"
  | "GROUND_REPORTER"
  | "CRIME_REPORTER"
  | "POLITICAL_REPORTER"
  | "BUSINESS_REPORTER"
  | "SPORTS_REPORTER"
  | "TECH_REPORTER"
  | "CULTURE_REPORTER"
  | "ENTERTAINMENT_REPORTER"
  | "WEATHER_REPORTER"
  | "HEALTH_REPORTER"
  | "SCIENCE_REPORTER"
  | "ENVIRONMENT_REPORTER"
  | "EDUCATION_REPORTER"
  | "AGRICULTURE_REPORTER"
  | "LIFESTYLE_REPORTER"
  | "TRAVEL_REPORTER"
  | "WOMEN_AND_CHILD_REPORTER"
  | "RURAL_AFFAIRS_REPORTER"
  | "URBAN_AFFAIRS_REPORTER"
  | "PARLIAMENTARY_REPORTER"
  | "LEGAL_REPORTER"
  | "DEFENCE_REPORTER"
  | "INTERNATIONAL_REPORTER"
  | "INVESTIGATIVE_REPORTER"
  | "FEATURE_WRITER"
  | "COLUMNIST"
  | "CARTOONIST"
  | "PHOTOGRAPHER"
  | "CINEMATOGRAPHER"
  | "CAMERAMAN"
  | "VIDEO_EDITOR"
  | "COPY_EDITOR"
  | "PROOFREADER"
  | "PAGE_DESIGNER"
  | "GRAPHIC_DESIGNER"
  | "WEB_DESIGNER"
  | "ARCHIVIST"
  | "LIBRARIAN"
  | "FACT_CHECKER"
  | "ASSOCIATE_PRODUCER"
  | "PRODUCER"
  | "EXECUTIVE_PRODUCER"
  | "CHIEF_OPERATING_OFFICER"
  | "CHIEF_FINANCIAL_OFFICER"
  | "HUMAN_RESOURCES"
  | "SALES"
  | "MARKETING"
  | "DIGITAL_MARKETING"
  | "SOCIAL_MEDIA"
  | "SEO"
  | "PRODUCT"
  | "ENGINEERING"
  | "CUSTOMER_SUPPORT"
  | "LEGAL"
  | "ADMIN_STAFF"
  | "ACCOUNTS"
  | "INTERN"
  | "OTHER";

type StaffList = {
  idNumber: string;
  fullName: string;
  designation?: string | null;
  department: Department;
  photoUrl?: string | null;
  city?: string | null;
  state?: string | null;
  validTill?: string | null;
  status: StaffStatus;
  qrCodeUrl?: string | null;
};

const DEMO_STAFF: (StaffList & { id: string; issueDate?: string | null; district?: string | null })[] = [];

function env<T>(v: T | undefined, fallback: T) {
  return v ?? fallback;
}

function notFound<T>(data: T, message = "Not found") {
  return NextResponse.json({
    data,
    message,
    errors: [message],
  });
}

function ok<T>(data: T, message = "OK") {
  return NextResponse.json({
    data,
    message,
    errors: [],
  });
}

function paginated<T>(content: T[], page = 0, size = 10) {
  const totalElements = content.length;
  const start = page * size;
  const sliced = content.slice(start, start + size);
  return {
    content: sliced,
    totalElements,
    number: page,
    size,
    totalPages: Math.max(1, Math.ceil(totalElements / size)),
    first: page === 0,
    last: start + size >= totalElements,
    empty: sliced.length === 0,
  };
}

type ReqCtx = {
  segs: string[];
  path: string;
  q: URLSearchParams;
};

function normalizeId(raw: string | undefined | null): string {
  if (!raw) return "";
  return raw
    .toUpperCase()
    .replace(/\s+/g, "")
    .replace(/[-_]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function staffPressIdFromCard(s: StaffList) {
  return {
    idNumber: s.idNumber,
    fullName: s.fullName,
    designation: s.designation,
    department: s.department,
    photoUrl: s.photoUrl,
    signatureUrl: null,
    city: s.city,
    state: s.state,
    district: s.city,
    dateOfBirth: null,
    issueDate: "2026-01-15",
    validTill: s.validTill,
    status: s.status,
    validityStatusText:
      s.status === "ACTIVE" || s.status === "VERIFIED"
        ? "Valid & Active"
        : "Not Active",
    qrCodeUrl: s.qrCodeUrl,
    workEmailMasked: null,
    mobileMasked: null,
    bloodGroupMasked: null,
    reporterBatchId: `MMT-${s.idNumber.split("-").slice(-1)[0]}`,
  };
}

function handleStaff({ segs, q }: ReqCtx) {
  const base = segs.slice(1);
  if (base.length === 0) {
    const dept = q.get("department");
    const qkw = (q.get("q") || q.get("query") || q.get("search") || "").trim().toLowerCase();
    const loc = (q.get("location") || q.get("city") || "").trim().toLowerCase();
    let list = DEMO_STAFF.slice();
    if (dept) list = list.filter((s) => s.department === dept.toUpperCase());
    if (qkw)
      list = list.filter(
        (s) =>
          s.fullName.toLowerCase().includes(qkw) ||
          s.idNumber.toLowerCase().includes(qkw) ||
          (s.designation ?? "").toLowerCase().includes(qkw),
      );
    if (loc)
      list = list.filter(
        (s) =>
          (s.city ?? "").toLowerCase().includes(loc) ||
          (s.state ?? "").toLowerCase().includes(loc),
      );
    return ok(list);
  }
  if (base[0] === "search") {
    const qkw = (q.get("q") || "").trim().toLowerCase();
    const list = DEMO_STAFF.filter(
      (s) =>
        !qkw ||
        s.fullName.toLowerCase().includes(qkw) ||
        s.idNumber.toLowerCase().includes(qkw),
    );
    return ok(list);
  }
  if (base[0] === "department" && base[1]) {
    const deptName = base[1].toUpperCase();
    const list = DEMO_STAFF.filter((s) => s.department === deptName);
    return ok(list);
  }
  if (base[0] === "verify" && base[1]) {
    const id = normalizeId(base.slice(1).join("/"));
    const s =
      DEMO_STAFF.find((x) => normalizeId(x.idNumber) === id) ??
      DEMO_STAFF.find((x) => normalizeId(x.idNumber).includes(id)) ??
      null;
    type StaffT = (typeof DEMO_STAFF)[number];
    const shapeOf = (s: StaffT) => ({
      id: s.id,
      idNumber: s.idNumber,
      fullName: s.fullName,
      designation: s.designation,
      department: s.department,
      photoUrl: s.photoUrl,
      city: s.city,
      state: s.state,
      validTill: s.validTill,
      status: s.status,
      qrCodeUrl: s.qrCodeUrl,
      isValid: s.status === "ACTIVE" || s.status === "VERIFIED",
      verificationMessage:
        s.status === "ACTIVE" || s.status === "VERIFIED"
          ? `Verified ${s.fullName} — ${s.designation ?? s.department}`
          : `ID ${id} status: ${s.status}. Not currently active.`,
      verifyTimestamp: new Date().toISOString(),
    });
    if (!s) return notFound(null, `No credential found for ID: ${id}`);
    return ok(shapeOf(s));
  }
  // /api/v1/staff/<idNumber> → quick reference public detail
  const id = normalizeId(base.join("/"));
  const s =
    DEMO_STAFF.find((x) => normalizeId(x.idNumber) === id) ??
    DEMO_STAFF.find((x) => normalizeId(x.idNumber).includes(id)) ??
    null;
  if (!s) return notFound(null, `No credential found for ID: ${id}`);
  return ok(staffPressIdFromCard(s));
}

function handleBlogPosts({ q }: ReqCtx) {
  const page = Math.max(0, Number(q.get("page") ?? "0"));
  const size = Math.min(100, Math.max(1, Number(q.get("size") ?? "10")));
  const demo: any[] = [];
  for (let i = 1; i <= 12; i++) {
    demo.push({
      id: `post-${1000 + i}`,
      slug: `story-${i}`,
      title:
        i % 3 === 0
          ? "Indore में हुई City Beat की मीटिंग"
          : i % 3 === 1
            ? "Breaking: Ground Report Live Updates"
            : "Latest Opinion — Journalism of Integrity",
      excerpt:
        "MapMyTimes brings grassroots reporting from across India. Ground, verified, unfiltered.",
      cover:
        i % 2 === 0
          ? "/og-default.png"
          : `https://images.unsplash.com/photo-1504711434969-e33886168f5c?auto=format&fit=crop&w=800&q=70`,
      status: "PUBLISHED",
      visibility: "PUBLIC",
      postType: "ARTICLE",
      categories: [
        { id: `cat-${i}`, name: i % 2 ? "India" : "City", slug: i % 2 ? "india" : "city" },
      ],
      tags: [{ id: `t-${i}`, name: "MapMyTimes", slug: "mmt" }],
      author: {
        id: "u-1",
        name: "Prakhar Mahore",
        photoUrl: null,
      },
      readingTimeMinutes: 3,
      viewCount: 100 + i * 17,
      likeCount: 5 + i * 2,
      commentCount: 0,
      publishedAt: new Date(Date.now() - i * 3600_000 * 2).toISOString(),
      createdAt: new Date().toISOString(),
      updatedAt: new Date().toISOString(),
    });
  }
  return ok(paginated(demo, page, size));
}

function handleBlogCategories() {
  const items = [
    { id: "cat-1", name: "India", slug: "india", description: null, parentCategoryId: null, postCount: 42, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "cat-2", name: "World", slug: "world", description: null, parentCategoryId: null, postCount: 18, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "cat-3", name: "Business", slug: "business", description: null, parentCategoryId: null, postCount: 29, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "cat-4", name: "Technology", slug: "technology", description: null, parentCategoryId: null, postCount: 15, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "cat-5", name: "Sports", slug: "sports", description: null, parentCategoryId: null, postCount: 22, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "cat-6", name: "Politics", slug: "politics", description: null, parentCategoryId: null, postCount: 31, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "cat-7", name: "Culture", slug: "culture", description: null, parentCategoryId: null, postCount: 13, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "cat-8", name: "Opinion", slug: "opinion", description: null, parentCategoryId: null, postCount: 11, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
  ];
  return ok(paginated(items, 0, 50));
}

function handleTags() {
  const items = [
    { id: "t-1", name: "Breaking", slug: "breaking", postCount: 44, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "t-2", name: "Indore", slug: "indore", postCount: 19, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "t-3", name: "MP", slug: "mp", postCount: 25, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "t-4", name: "Ground Report", slug: "ground-report", postCount: 36, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
    { id: "t-5", name: "Verified", slug: "verified", postCount: 68, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() },
  ];
  return ok(paginated(items, 0, 50));
}

function handleJobs({ q }: ReqCtx) {
  const dept = q.get("department");
  const demo = [
    {
      id: "job-1",
      title: "Ground Reporter — City Beat (Indore)",
      slug: "ground-reporter-indore",
      department: "GROUND_REPORTER",
      employmentType: "FULL_TIME",
      experienceLevel: "MID",
      locationCity: "Indore",
      locationState: "Madhya Pradesh",
      deadline: new Date(Date.now() + 30 * 86400_000).toISOString(),
      postedAt: new Date(Date.now() - 2 * 86400_000).toISOString(),
      description: "Report on hyper-local beats across Indore. Must have RTO press accreditation locally.",
      ctcMin: 480000,
      ctcMax: 720000,
    },
    {
      id: "job-2",
      title: "Fact-Checker — Remote (Hindi / English)",
      slug: "fact-checker-remote",
      department: "FACT_CHECKER",
      employmentType: "CONTRACT",
      experienceLevel: "ENTRY",
      locationCity: null,
      locationState: null,
      deadline: new Date(Date.now() + 45 * 86400_000).toISOString(),
      postedAt: new Date(Date.now() - 5 * 86400_000).toISOString(),
      description: "Verify viral claims / images / videos. Strict NDA + ethics certification.",
      ctcMin: 300000,
      ctcMax: 480000,
    },
    {
      id: "job-3",
      title: "Associate Video Producer — Mumbai",
      slug: "associate-producer-mumbai",
      department: "ASSOCIATE_PRODUCER",
      employmentType: "FULL_TIME",
      experienceLevel: "MID",
      locationCity: "Mumbai",
      locationState: "Maharashtra",
      deadline: new Date(Date.now() + 25 * 86400_000).toISOString(),
      postedAt: new Date(Date.now() - 1 * 86400_000).toISOString(),
      description: "Package desk, scripting, field shoot coordination for daily YouTube / Shorts output.",
      ctcMin: 600000,
      ctcMax: 960000,
    },
  ];
  const list = dept ? demo.filter((j) => j.department === dept.toUpperCase()) : demo;
  return ok(paginated(list, 0, 20));
}

function handleNotifications() {
  return ok({ unread: 2 });
}

function handleReadingProgress() {
  return ok({ content: [], totalElements: 0, number: 0, size: 20, totalPages: 1 });
}

function handleAuthRefresh() {
  return ok({
    accessToken: "demo.access.token." + Date.now(),
    refreshToken: "demo.refresh.token." + Date.now(),
  });
}

export async function GET(
  req: NextRequest,
  ctx: { params: Promise<{ path?: string[] }> },
) {
  const params = await ctx.params;
  const segs: string[] = params?.path?.filter(Boolean) ?? [];
  const path = "/" + segs.join("/");
  const q = req.nextUrl.searchParams;
  const ctx2: ReqCtx = { segs, path, q };
  // /api/v1/health
  if (segs.length === 0 || (segs.length === 1 && segs[0] === "health"))
    return ok({ status: "OK", service: "mapmytimes-api", env: env(process.env.NODE_ENV, "development"), timestamp: new Date().toISOString() });
  // /api/v1/staff/**
  if (segs[0] === "staff") return handleStaff(ctx2);
  // /api/v1/blog/posts/**
  if (segs[0] === "blog" && segs[1] === "posts") return handleBlogPosts(ctx2);
  // /api/v1/blog/categories
  if (segs[0] === "blog" && segs[1] === "categories") return handleBlogCategories();
  // /api/v1/blog/settings or /api/v1/blog/settings/map
  if (segs[0] === "blog" && segs[1] === "settings") {
    if (segs[2] === "map") return ok({ BRAND_NAME: "MapMyTimes", BRAND_TAGLINE: "JOURNALISM OF INTEGRITY" });
    return ok(paginated([{ id: "s-1", key: "BRAND_NAME", value: "MapMyTimes" }], 0, 20));
  }
  // /api/v1/blog/tags
  if (segs[0] === "blog" && segs[1] === "tags") return handleTags();
  // /api/v1/social/feed | explore
  if (segs[0] === "social") return handleBlogPosts(ctx2);
  // /api/v1/jobs/**
  if (segs[0] === "jobs") return handleJobs(ctx2);
  // /api/v1/applications/my
  if (segs[0] === "applications") return ok({ content: [], totalElements: 0, number: 0, size: 20, totalPages: 1 });
  // /api/v1/notification/**
  if (segs[0] === "notification") {
    if (segs[1] === "unread-count") return handleNotifications();
    return ok({ content: [], totalElements: 0, number: 0, size: 20, totalPages: 1 });
  }
  // /api/v1/reading-progress/**
  if (segs[0] === "reading-progress") return handleReadingProgress();
  // /api/v1/auth/refresh → demo token generator
  if (segs[0] === "auth" && segs[1] === "refresh") return handleAuthRefresh();
  // /api/v1/auth/profile → 401 unless token provided (returns demo user)
  if (segs[0] === "auth" && segs[1] === "profile") {
    return ok({
      id: "demo-user-1",
      email: "demo@mapmytimes.com",
      firstName: "Demo",
      lastName: "User",
      fullName: "Demo User",
      phone: null,
      language: "en",
      photoUrl: null,
      createdAt: new Date(Date.now() - 86400_000 * 45).toISOString(),
      emailVerified: true,
      accountStatus: "ACTIVE",
    });
  }
  // catch-all 200 empty paginated so nothing breaks on unknown list calls
  return ok({
    content: [],
    totalElements: 0,
    number: 0,
    size: Math.min(100, Math.max(1, Number(q.get("size") ?? "10"))),
    totalPages: 1,
    first: true,
    last: true,
    empty: true,
    __path: path,
    __message: "MapMyTimes mock API v1 (local dev fallback) — no data available for this endpoint yet",
  });
}

export async function POST(
  req: NextRequest,
  ctx: { params: Promise<{ path?: string[] }> },
) {
  const params = await ctx.params;
  const segs: string[] = params?.path?.filter(Boolean) ?? [];
  const path = "/" + segs.join("/");
  // /api/v1/auth/refresh → demo refresh
  if (segs[0] === "auth" && segs[1] === "refresh") return handleAuthRefresh();
  // /api/v1/notification/contact-form → 200
  if (segs[0] === "notification" && segs[1] === "contact-form") {
    try { await req.json(); } catch {}
    return ok({ received: true, ticket: `T-${Date.now()}` }, "Contact form received. We will reach out within 2 business days.");
  }
  // /api/v1/applications → job apply
  if (segs[0] === "applications") {
    try { await req.formData(); } catch {}
    return ok({ applicationId: `APP-${Date.now()}`, status: "SUBMITTED" }, "Application submitted successfully.");
  }
  // /api/v1/reading-progress/me
  if (segs[0] === "reading-progress" && segs[1] === "me") {
    try { await req.json(); } catch {}
    return ok({ saved: true });
  }
  // /api/v1/highlights/me
  if (segs[0] === "highlights" && segs[1] === "me") {
    try { await req.json(); } catch {}
    return ok({ highlightId: `hl-${Date.now()}` });
  }
  // /api/v1/auth/* login/register etc → demo user
  if (segs[0] === "auth") {
    try { await req.json(); } catch {}
    return ok({
      accessToken: "demo.access.token." + Date.now(),
      refreshToken: "demo.refresh.token." + Date.now(),
      user: { id: "demo-user-1", fullName: "Demo User", email: "demo@mapmytimes.com" },
    }, "OK");
  }
  return ok({ ok: true, path, method: "POST" });
}

export async function PUT(
  req: NextRequest,
  ctx: { params: Promise<{ path?: string[] }> },
) {
  const params = await ctx.params;
  const segs: string[] = params?.path?.filter(Boolean) ?? [];
  try { await req.json().catch(() => null); } catch {}
  return ok({ ok: true, path: "/" + segs.join("/"), method: "PUT", updated: true });
}

export async function PATCH(
  req: NextRequest,
  ctx: { params: Promise<{ path?: string[] }> },
) {
  const params = await ctx.params;
  const segs: string[] = params?.path?.filter(Boolean) ?? [];
  try { await req.json().catch(() => null); } catch {}
  if (segs[0] === "notification") return ok({ read: true });
  return ok({ ok: true, path: "/" + segs.join("/"), method: "PATCH", patched: true });
}

export async function DELETE(
  _req: NextRequest,
  ctx: { params: Promise<{ path?: string[] }> },
) {
  const params = await ctx.params;
  const segs: string[] = params?.path?.filter(Boolean) ?? [];
  return ok({ ok: true, path: "/" + segs.join("/"), method: "DELETE", deleted: true });
}
