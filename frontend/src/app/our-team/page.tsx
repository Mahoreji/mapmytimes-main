"use client";

import Link from "next/link";
import { useEffect, useMemo, useState } from "react";
import {
  Search as SearchIcon,
  Users,
  X,
  Building2,
  MapPin,
  ShieldCheck,
  Mail,
  Linkedin,
  Shield,
  Globe2,
  Target,
  Users2,
} from "lucide-react";
import { blogApi } from "@/lib/api/blogApi";
import type { StaffListCardDTO, Department } from "@/types/blog";
import { DEPARTMENT_OPTIONS, departmentLabel, formatDateCompact, statusPill } from "@/lib/staff";
import { Input } from "@/components/ui/Input";
import { Button } from "@/components/ui/Button";
import { getApiError } from "@/lib/api/client";
import { cn } from "@/lib/utils";

const DEPT_HINDI_BIO: Record<string, string> = {
  FOUNDER: "मापमाईटाइम्स की स्थापना और संपादकीय नेतृत्व।",
  EDITOR_IN_CHIEF: "संपादकीय नीति, निष्पक्षता और गुणवत्ता की ज़िम्मेदारी।",
  MANAGING_DIRECTOR: "रणनीति, प्रबंधन और विकास की ज़िम्मेदारी।",
  CHIEF_MARKETING_OFFICER: "ब्रांड, मार्केटिंग और डिजिटल रणनीति की अगुवाई।",
  CHIEF_TECHNOLOGY_OFFICER: "टेक्नोलॉजी, प्रोडक्ट डेवलपमेंट और इनोवेशन।",
  CHIEF_OPERATING_OFFICER: "ऑपरेशन्स, टीम मैनेजमेंट और प्रोसेस की ज़िम्मेदारी।",
  OPERATIONS_HEAD: "ऑपरेशन्स, टीम मैनेजमेंट और प्रोसेस की ज़िम्मेदारी।",
  BUREAU_CHIEF: "ब्यूरो शहर/क्षेत्र की खबरों और टीम का नेतृत्व।",
  GROUND_REPORTER: "ग्राउंड रिपोर्टिंग और ज़मीनी खबरों की कवरेज।",
  SENIOR_GROUND_REPORTER: "ग्राउंड रिपोर्टिंग और ब्रेकिंग न्यूज़ कवरेज।",
  INVESTIGATIONS_EDITOR: "अनुसंधान, जाँचपड़ताल और गहरी रिपोर्टिंग।",
  PRINCIPAL_CORRESPONDENT: "महत्वपूर्ण बीट, रिपोर्टिंग और विशेष कवरेज।",
  FEATURES_JOURNALIST: "फीचर, कल्चर, लाइफस्टाइल और कहानी लेखन।",
  CITY_BEAT_REPORTER: "शहर की हर छोटी-बड़ी खबर ताज़ा और सटीक।",
  VIDEO_JOURNALIST: "वीडियो खबरें, पैकेज और ऑन-ग्राउंड शूटिंग।",
  CAMERAMAN: "वीडियोग्राफी और कैमरा ऑपरेशन्स।",
  PHOTO_EDITOR: "फोटोग्राफी और इमेज एडिटिंग डेस्क।",
  COPY_EDITOR: "कॉपी, भाषा, सच्चाई और फैक्ट चेकिंग।",
  NEWS_DESK: "न्यूज़ डेस्क, बुलेटिन और संपादन।",
  SUB_EDITOR: "समाचारों का संपादन, फैक्ट-चेक और भाषा शुद्धता।",
  CHIEF_EDITOR: "संपादकीय नीति और पूरी न्यूज़रूम का नेतृत्व।",
  COLUMNIST: "विशेष स्तंभ, विश्लेषण और राय लेखन।",
  CARTOONIST: "कार्टून, इलस्ट्रेशन और विज़ुअल स्टोरीटेलिंग।",
  PRODUCER: "वीडियो, शो और बुलेटिन प्रोडक्शन।",
  ASSOCIATE_PRODUCER: "शो और वीडियो प्रोडक्शन की सहायता।",
  ARCHIVIST: "संग्रह, अभिलेख और डिजिटल लाइब्रेरी प्रबंधन।",
  ADMIN_STAFF: "प्रशासनिक सहायता और ऑफिस ऑपरेशन्स।",
  ACCOUNTS: "वित्त, लेखा और भुगतान प्रबंधन।",
  HR: "मानव संसाधन, भर्ती और कर्मचारी विकास।",
  PUBLIC_RELATIONS: "सार्वजनिक संबंध और मीडिया आउटरीच।",
  EVENT_MANAGER: "इवेंट, कार्यक्रम और आयोजन प्रबंधन।",
  TRAINEE: "प्रशिक्षणार्थी — शिक्षण और क्षेत्र अनुभव।",
  INTERN: "इंटर्न — समाचार शिक्षण और प्रारंभिक अनुभव।",
  DIGITAL_MARKETING: "डिजिटल मार्केटिंग और सोशल मीडिया रणनीति।",
  AUDIENCE_ENGAGEMENT: "पाठकों और दर्शकों से जुड़ाव और समुदाय निर्माण।",
  UX_DESIGNER: "यूजर एक्सपीरियंस और इंटरफ़ेस डिज़ाइन।",
  PRODUCT_MANAGER: "प्रोडक्ट रोडमैप और फीचर्स का नेतृत्व।",
  SOFTWARE_ENGINEER: "सॉफ्टवेयर डेवलपमेंट और प्लेटफ़ॉर्म इंजीनियरिंग।",
  DATA_ANALYST: "डेटा विश्लेषण और दर्शक अंतर्दृष्टि।",
  LEGAL_COUNSEL: "कानूनी सलाह और मीडिया कानून अनुपालन।",
  FACT_CHECKER: "फ़ैक्ट-चेकिंग, पड़ताल और सत्यापन।",
  DESIGNATION_DEFAULT: "पत्रकारिता और समाचार विभाग में योगदान।",
};

function hindiBioFor(dept: Department, designation?: string | null): string {
  const d = dept as string;
  if (designation) {
    const key = designation
      .toUpperCase()
      .replace(/[^A-Z0-9]+/g, "_")
      .replace(/^_|_$/g, "");
    if (DEPT_HINDI_BIO[key]) return DEPT_HINDI_BIO[key];
  }
  return DEPT_HINDI_BIO[d] ?? DEPT_HINDI_BIO.DESIGNATION_DEFAULT;
}

const FEATURES = [
  {
    icon: <Shield className="h-9 w-9" />,
    title: "सच के प्रति प्रतिबद्ध",
    body: "हमारी पहली प्राथमिकता है सच्ची और निष्पक्ष पत्रकारिता।",
  },
  {
    icon: <Users2 className="h-9 w-9" />,
    title: "टीम वर्क",
    body: "एकजुट टीम जो आपके लिए दिन-रात काम करती है।",
  },
  {
    icon: <Globe2 className="h-9 w-9" />,
    title: "ज़मीनी जुड़ाव",
    body: "देश के हर कोने से जुड़कर लाते हैं आपके लिए असली खबरें।",
  },
  {
    icon: <Target className="h-9 w-9" />,
    title: "हमारा लक्ष्य",
    body: "जनता की आवाज़ को बुलंदी देना और बदलाव की मिसाल बनना।",
  },
];

export default function OurTeamPage() {
  const [list, setList] = useState<StaffListCardDTO[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  const [query, setQuery] = useState("");
  const [queryBuf, setQueryBuf] = useState("");
  const [dept, setDept] = useState<string>("");
  const [city, setCity] = useState("");
  const [cityBuf, setCityBuf] = useState("");

  useEffect(() => {
    let active = true;
    setLoading(true);
    (async () => {
      try {
        const r = await blogApi.staff.public.list();
        if (!active) return;
        setList(Array.isArray(r) ? r : []);
      } catch (e) {
        if (!active) return;
        setErr(getApiError(e) || "Could not load our team. Please try again later.");
      } finally {
        if (active) setLoading(false);
      }
    })();
    return () => {
      active = false;
    };
  }, []);

  const cities = useMemo(() => {
    const s = new Set<string>();
    list.forEach((x) => {
      if (x.city) s.add(x.city);
      if (x.state) s.add(x.state);
    });
    return Array.from(s).sort();
  }, [list]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return list.filter((s) => {
      if (dept && s.department !== dept) return false;
      if (city) {
        const hay = `${s.city ?? ""} ${s.state ?? ""}`.toLowerCase();
        if (!hay.includes(city.toLowerCase())) return false;
      }
      if (q) {
        const hay = `${s.fullName} ${s.idNumber} ${s.designation ?? ""} ${s.city ?? ""} ${s.state ?? ""}`.toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });
  }, [list, query, dept, city]);

  const hasAny = dept || city || query;

  return (
    <main className="min-h-[calc(100vh-180px)] bg-white relative overflow-hidden">
      {/* Decorative background layer */}
      <div className="pointer-events-none absolute inset-0 z-0">
        {/* Left top dot grid */}
        <div
          className="absolute left-0 top-10 h-40 w-40 opacity-40"
          style={{
            backgroundImage:
              "radial-gradient(circle, rgba(227,30,36,0.35) 1.4px, transparent 1.4px)",
            backgroundSize: "14px 14px",
          }}
        />
        {/* Soft pink left blob */}
        <div
          className="absolute -left-24 top-0 h-[520px] w-[520px] rounded-full opacity-60 blur-3xl"
          style={{
            background:
              "radial-gradient(closest-side, rgba(254,202,202,0.55), rgba(255,241,242,0.15) 60%, transparent 70%)",
          }}
        />
        {/* Right big MM watermark */}
        <div className="absolute right-0 top-10 h-[520px] w-[520px] opacity-[0.04] flex items-center justify-center select-none overflow-hidden">
          <span
            className="font-headline tracking-tighter leading-none"
            style={{ fontSize: "420px", fontWeight: 900, color: "#0A0A0A" }}
          >
            MM
          </span>
        </div>
      </div>

      <section className="relative z-10">
        <div className="mx-auto max-w-7xl px-4 pt-12 pb-10 sm:pt-16 sm:pb-14">
          <div className="flex flex-col items-center gap-4 text-center">
            <div className="inline-flex items-center gap-2">
              <span className="text-[13px] font-bold uppercase tracking-[0.28em] text-news">
                Our Strength
              </span>
            </div>
            <h1 className="font-headline text-[2.6rem] sm:text-[3.6rem] leading-[1.05] tracking-tight text-ink-950">
              Meet Our{" "}
              <span className="text-news relative inline-block">
                Team
                <span className="absolute -bottom-3 left-1/2 -translate-x-1/2 block h-[4px] w-20 sm:w-28 rounded-full bg-news" />
              </span>
            </h1>
            <p className="mt-5 max-w-3xl text-lg sm:text-xl leading-[1.7] text-ink-700">
              MapMyTimes की मज़बूत टीम जो आपके लिए दिन-रात काम करती है,
              <br className="hidden sm:block" />
              ताकि आपको मिले सच्ची, निष्पक्ष और ज़मीनी खबरें।
            </p>

            <div className="w-full grid grid-cols-1 md:grid-cols-12 gap-3 mt-6">
              <div className="md:col-span-5">
                <Input
                  label="Search by name, ID or city"
                  leadingIcon={<SearchIcon className="h-4 w-4" />}
                  placeholder="Type Rakesh, MP-28, Indore…"
                  value={queryBuf}
                  onChange={(e) => setQueryBuf(e.target.value)}
                  onKeyDown={(e) => {
                    if (e.key === "Enter") setQuery(queryBuf);
                  }}
                  trailingIcon={
                    queryBuf ? (
                      <button
                        type="button"
                        onClick={() => {
                          setQueryBuf("");
                          setQuery("");
                        }}
                        className="text-ink-600 hover:text-ink-950 pointer-events-auto"
                        aria-label="Clear"
                      >
                        <X className="h-4 w-4" />
                      </button>
                    ) : null
                  }
                />
              </div>
              <div className="md:col-span-3">
                <label className="text-[10px] uppercase tracking-[0.2em] font-bold text-ink-600 mb-1 block">
                  Department
                </label>
                <select
                  value={dept}
                  onChange={(e) => setDept(e.target.value)}
                  className="h-11 w-full border-2 border-ink-950 bg-white px-3 text-sm font-bold text-ink-950 focus:outline-none focus:ring-2 focus:ring-news/50"
                >
                  <option value="">All departments</option>
                  {DEPARTMENT_OPTIONS.map((d) => (
                    <option key={d.value} value={d.value}>
                      {d.label}
                    </option>
                  ))}
                </select>
              </div>
              <div className="md:col-span-3">
                <label className="text-[10px] uppercase tracking-[0.2em] font-bold text-ink-600 mb-1 block">
                  City / State
                </label>
                <select
                  value={city}
                  onChange={(e) => setCity(e.target.value)}
                  className="h-11 w-full border-2 border-ink-950 bg-white px-3 text-sm font-bold text-ink-950 focus:outline-none focus:ring-2 focus:ring-news/50"
                >
                  <option value="">All locations</option>
                  {cities.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </div>
              <div className="md:col-span-1 flex items-end">
                <Button variant="news" block onClick={() => setQuery(queryBuf)}>
                  GO
                </Button>
              </div>
            </div>

            {hasAny ? (
              <div className="flex flex-wrap items-center justify-center gap-2 mt-2">
                <span className="text-[11px] uppercase tracking-widest font-bold text-ink-600">
                  Filters:
                </span>
                {query ? (
                  <span className="inline-flex items-center gap-2 border-2 border-ink-950 px-3 py-1 text-xs font-bold bg-white">
                    <SearchIcon className="h-3 w-3" /> “{query}”
                    <button onClick={() => setQuery("")} className="hover:text-news">
                      <X className="h-3 w-3" />
                    </button>
                  </span>
                ) : null}
                {dept ? (
                  <span className="inline-flex items-center gap-2 border-2 border-ink-950 px-3 py-1 text-xs font-bold bg-white">
                    <Building2 className="h-3 w-3" /> {departmentLabel(dept as Department)}
                    <button onClick={() => setDept("")} className="hover:text-news">
                      <X className="h-3 w-3" />
                    </button>
                  </span>
                ) : null}
                {city ? (
                  <span className="inline-flex items-center gap-2 border-2 border-ink-950 px-3 py-1 text-xs font-bold bg-white">
                    <MapPin className="h-3 w-3" /> {city}
                    <button onClick={() => setCity("")} className="hover:text-news">
                      <X className="h-3 w-3" />
                    </button>
                  </span>
                ) : null}
              </div>
            ) : null}
          </div>
        </div>
      </section>

      <section className="relative z-10 mx-auto max-w-7xl px-4 pb-12 sm:pb-16">
        {loading ? (
          <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-5">
            {Array.from({ length: 8 }).map((_, i) => (
              <div
                key={i}
                className="w-full flex flex-col animate-pulse bg-white rounded-2xl border border-ink-100 shadow-[0_6px_20px_rgb(0,0,0,0.06)] p-3"
              >
                <div className="w-full aspect-[3/4] rounded-xl bg-ink-100" />
                <div className="mt-3 h-4 w-32 bg-ink-200 rounded" />
                <div className="mt-1.5 h-3 w-36 bg-ink-100 rounded" />
                <div className="mt-2 h-2.5 w-full bg-ink-50 rounded" />
                <div className="mt-1 h-2.5 w-4/5 bg-ink-50 rounded" />
                <div className="mt-3 h-3.5 w-16 bg-ink-100 rounded" />
              </div>
            ))}
          </div>
        ) : err ? (
          <div className="border-2 border-news bg-news-50 text-news-800 p-4 font-bold text-sm">
            {err}
          </div>
        ) : filtered.length === 0 ? (
          <div className="border-2 border-ink-950/20 border-dashed p-12 text-center">
            <Users className="h-10 w-10 mx-auto text-ink-400" />
            <div className="font-headline uppercase text-2xl mt-3 tracking-wide">
              No team members match
            </div>
            <p className="text-ink-700 mt-2 text-sm">
              {list.length === 0
                ? "Our team list is being prepared. Check back soon."
                : "Try removing filters or searching another name."}
            </p>
          </div>
        ) : (
          <>
            <div className="flex items-center justify-between mb-5">
              <div className="text-[11px] uppercase tracking-[0.25em] font-bold text-ink-600">
                Showing <span className="text-ink-950">{filtered.length}</span> of{" "}
                <span className="text-ink-950">{list.length}</span> credentials
              </div>
              <Link
                href="/verify-press"
                className="inline-flex items-center gap-2 text-[11px] uppercase tracking-widest font-bold hover:text-news"
              >
                <ShieldCheck className="h-3.5 w-3.5" /> Verify an ID directly
              </Link>
            </div>
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-5">
              {filtered.map((s) => {
                const initials = s.fullName
                  .split(/\s+/)
                  .filter(Boolean)
                  .slice(0, 2)
                  .map((w) => w[0]?.toUpperCase() ?? "")
                  .join("");
                const hasPhoto = s.photoUrl && s.photoUrl.trim().length > 0;
                const bio = hindiBioFor(s.department, s.designation);
                const deptStr = s.designation || departmentLabel(s.department);
                return (
                  <Link
                    key={s.idNumber}
                    href={`/our-team/${encodeURIComponent(s.idNumber)}`}
                    className="group bg-white rounded-2xl border border-ink-100 shadow-[0_6px_20px_rgb(0,0,0,0.06)] hover:shadow-[0_16px_40px_-10px_rgb(0,0,0,0.12)] transition-all duration-300 p-3 -translate-y-0 hover:-translate-y-1 flex flex-col"
                  >
                    {/* Photo 3:4 portrait */}
                    <div className="relative w-full mx-auto aspect-[3/4] rounded-xl overflow-hidden bg-ink-100">
                      {hasPhoto ? (
                        <img
                          src={s.photoUrl!}
                          alt={s.fullName}
                          className="absolute inset-0 h-full w-full object-cover transition-transform duration-500 ease-out group-hover:scale-105"
                        />
                      ) : (
                        <div className="absolute inset-0 flex items-center justify-center overflow-hidden">
                          <div
                            className="absolute inset-0 opacity-90"
                            style={{
                              backgroundImage:
                                "radial-gradient(circle at 20% 20%, #fff1f1 0, transparent 45%), radial-gradient(circle at 80% 80%, #fff5ea 0, transparent 50%), linear-gradient(135deg, #fef2f2 0%, #fff7ed 100%)",
                            }}
                          />
                          <div
                            aria-hidden
                            className="absolute right-[-28%] top-[-28%] h-[88%] w-[88%] rounded-full opacity-50 blur-2xl"
                            style={{
                              background:
                                "radial-gradient(circle, rgba(227,30,36,0.16) 0%, transparent 60%)",
                            }}
                          />
                          <div className="relative font-headline tracking-wider text-5xl sm:text-6xl leading-none select-none text-news/80">
                            {initials || "MM"}
                          </div>
                        </div>
                      )}
                      {/* Hover overlay subtle */}
                      <div className="absolute inset-0 bg-gradient-to-t from-black/30 via-transparent to-transparent opacity-0 group-hover:opacity-100 transition-opacity duration-300" />
                    </div>

                    {/* Text body */}
                    <div className="mt-3.5 flex flex-col flex-1">
                      <h3 className="font-headline text-[1.05rem] leading-tight tracking-[0.01em] text-ink-950">
                        {s.fullName}
                      </h3>
                      <p className="mt-1.5 text-[0.82rem] font-bold leading-tight text-news">
                        {deptStr}
                      </p>
                      <p className="mt-2.5 text-[0.82rem] leading-[1.5] text-ink-600 flex-1">
                        {bio}
                      </p>
                    </div>

                    {/* Icons row */}
                    <div className="mt-3.5 flex items-center gap-2.5">
                      <span
                        className={cn(
                          "inline-flex h-8 w-8 items-center justify-center rounded-md border border-ink-200 text-ink-500"
                        )}
                        aria-hidden
                      >
                        <Mail className="h-3.5 w-3.5" />
                      </span>
                      <span
                        className={cn(
                          "inline-flex h-8 w-8 items-center justify-center rounded-md border border-ink-200 text-ink-500"
                        )}
                        aria-hidden
                      >
                        <Linkedin className="h-3.5 w-3.5" />
                      </span>
                    </div>
                  </Link>
                );
              })}
            </div>
          </>
        )}
      </section>

      {/* Bottom 4-feature strip */}
      <section className="relative z-10 mx-auto max-w-7xl px-4 pb-16">
        <div className="rounded-3xl bg-ink-950/[0.03] border border-ink-100 grid grid-cols-1 md:grid-cols-2 xl:grid-cols-4 divide-y md:divide-y-0 md:divide-x divide-ink-200/80 overflow-hidden">
          {FEATURES.map((f) => (
            <div key={f.title} className="flex gap-4 items-start px-7 py-7 sm:px-8 sm:py-8">
              <div className="shrink-0 h-14 w-14 rounded-2xl flex items-center justify-center text-news ring-2 ring-news/20 bg-news/[0.04]">
                {f.icon}
              </div>
              <div className="flex-1">
                <h4 className="font-headline text-xl leading-tight text-ink-950">
                  {f.title}
                </h4>
                <p className="mt-2 text-[0.93rem] leading-[1.55] text-ink-600">{f.body}</p>
              </div>
            </div>
          ))}
        </div>
      </section>
    </main>
  );
}
