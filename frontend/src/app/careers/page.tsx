"use client";

import { useEffect, useMemo, useState } from "react";
import { careersApi } from "@/lib/api/careersApi";
import type {
  ExperienceLevel,
  JobPostingSummaryResponse,
  JobType,
} from "@/types/careers";
import { EXPERIENCE_LABELS, JOB_TYPE_LABELS } from "@/types/careers";
import { JobCard, SectionTitle } from "@/components/careers/JobCard";
import { Button } from "@/components/ui/Button";
import { Pagination } from "@/components/ui/Pagination";
import { Input } from "@/components/ui/Input";
import { Briefcase, Search as SearchIcon, SlidersHorizontal, X } from "lucide-react";

type Mode = "browse" | "search";

export default function CareersListingPage() {
  const [mode, setMode] = useState<Mode>("browse");
  const [jobs, setJobs] = useState<JobPostingSummaryResponse[]>([]);
  const [departments, setDepartments] = useState<string[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [totalElements, setTotalElements] = useState(0);
  const [loading, setLoading] = useState(true);
  const [initialLoading, setInitialLoading] = useState(true);

  const [query, setQuery] = useState("");
  const [queryBuffer, setQueryBuffer] = useState("");
  const [department, setDepartment] = useState<string>("");
  const [jobType, setJobType] = useState<JobType | "">("");
  const [experienceLevel, setExperienceLevel] = useState<ExperienceLevel | "">("");

  useEffect(() => {
    careersApi.jobs
      .departments()
      .then((d) => setDepartments(Array.isArray(d) ? d : []))
      .catch(() => setDepartments([]));
  }, []);

  useEffect(() => {
    let active = true;
    setLoading(true);
    const req =
      mode === "search" && query.trim()
        ? careersApi.jobs.search(query, { page, size: 12 })
        : careersApi.jobs.list({
            page,
            size: 12,
            sortBy: "createdAt",
            sortDir: "DESC",
            department: department || undefined,
            jobType: jobType || undefined,
            experienceLevel: experienceLevel || undefined,
          });

    req
      .then((list) => {
        if (!active) return;
        setJobs((list as any).content ?? []);
        setTotalPages(Math.max(1, (list as any).totalPages ?? 1));
        setTotalElements((list as any).totalElements ?? 0);
        setLoading(false);
        setInitialLoading(false);
      })
      .catch(() => {
        if (!active) return;
        setJobs([]);
        setTotalPages(1);
        setTotalElements(0);
        setLoading(false);
        setInitialLoading(false);
      });
    return () => {
      active = false;
    };
  }, [mode, query, page, department, jobType, experienceLevel]);

  const activeFilters = useMemo(
    () => [
      department ? { key: "department" as const, label: `Dept: ${department}` } : null,
      jobType ? { key: "jobType" as const, label: `Type: ${JOB_TYPE_LABELS[jobType]}` } : null,
      experienceLevel
        ? {
            key: "experienceLevel" as const,
            label: `Exp: ${EXPERIENCE_LABELS[experienceLevel]}`,
          }
        : null,
      query.trim() ? { key: "query" as const, label: `“${query}”` } : null,
    ].filter(Boolean) as { key: "department" | "jobType" | "experienceLevel" | "query"; label: string }[],
    [department, jobType, experienceLevel, query],
  );

  const clearFilter = (k: typeof activeFilters[number]["key"]) => {
    if (k === "department") setDepartment("");
    if (k === "jobType") setJobType("");
    if (k === "experienceLevel") setExperienceLevel("");
    if (k === "query") {
      setQuery("");
      setQueryBuffer("");
      setMode("browse");
    }
    setPage(0);
  };

  const clearAll = () => {
    setDepartment("");
    setJobType("");
    setExperienceLevel("");
    setQuery("");
    setQueryBuffer("");
    setMode("browse");
    setPage(0);
  };

  const submitSearch = (e?: React.FormEvent) => {
    e?.preventDefault();
    setQuery(queryBuffer.trim());
    setMode(queryBuffer.trim() ? "search" : "browse");
    setPage(0);
  };

  const anyFilter = department || jobType || experienceLevel;

  return (
    <main className="mx-auto max-w-7xl px-4 py-8 sm:py-10">
      <section className="border-b-4 border-ink-950 pb-6">
        <div className="ribbon text-xs mb-3 inline-block">Open positions</div>
        <h1 className="font-headline text-4xl sm:text-6xl uppercase leading-none tracking-tight">
          Careers at <span className="text-news">MapMyTimes</span>
        </h1>
        <p className="mt-4 max-w-2xl text-ink-700 text-sm sm:text-base">
          Journalism of Integrity. Build the newsroom of the future with us —
          reporting, product, ops, and more. No fluff, verified bylines.
        </p>

        <form onSubmit={submitSearch} className="mt-6 grid grid-cols-1 lg:grid-cols-[1fr_auto] gap-3">
          <div className="relative">
            <SearchIcon className="h-4 w-4 absolute left-4 top-1/2 -translate-y-1/2 text-ink-600" />
            <Input
              value={queryBuffer}
              onChange={(e) => setQueryBuffer(e.target.value)}
              placeholder="Search jobs — reporter, editor, product, designer…"
              className="!pl-11 h-12 !text-base"
            />
          </div>
          <div className="flex gap-2">
            <Button variant="news" size="lg" type="submit">
              <SearchIcon className="h-4 w-4" /> Search
            </Button>
          </div>
        </form>
      </section>

      <section className="mt-8 grid grid-cols-1 lg:grid-cols-4 gap-6">
        <aside className="lg:col-span-1">
          <div className="border-2 border-ink-950 bg-white p-4 sm:p-5 shadow-hard-sm sticky top-24">
            <div className="flex items-center gap-2 mb-4">
              <SlidersHorizontal className="h-4 w-4 text-news" />
              <h3 className="font-headline uppercase tracking-wide text-sm">
                Filters
              </h3>
            </div>

            <div className="space-y-5">
              <div>
                <label className="block text-[11px] font-bold uppercase tracking-widest text-ink-700 mb-2">
                  Department
                </label>
                <select
                  value={department}
                  onChange={(e) => {
                    setDepartment(e.target.value);
                    setMode("browse");
                    setPage(0);
                  }}
                  className="w-full h-11 border-2 border-ink-950 px-3 bg-white text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-news"
                >
                  <option value="">All departments</option>
                  {departments.map((d) => (
                    <option key={d} value={d}>
                      {d}
                    </option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-bold uppercase tracking-widest text-ink-700 mb-2">
                  Employment type
                </label>
                <div className="grid grid-cols-2 gap-1.5">
                  {(Object.keys(JOB_TYPE_LABELS) as JobType[]).map((j) => (
                    <button
                      key={j}
                      type="button"
                      onClick={() => {
                        setJobType(jobType === j ? "" : j);
                        setMode("browse");
                        setPage(0);
                      }}
                      className={
                        "h-9 text-[11px] font-bold uppercase tracking-widest border-2 border-ink-950 transition-colors " +
                        (jobType === j
                          ? "bg-ink-950 text-white"
                          : "bg-white hover:bg-ink-900 hover:text-white")
                      }
                    >
                      {JOB_TYPE_LABELS[j]}
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-[11px] font-bold uppercase tracking-widest text-ink-700 mb-2">
                  Experience level
                </label>
                <div className="grid grid-cols-1 gap-1.5">
                  {(Object.keys(EXPERIENCE_LABELS) as ExperienceLevel[]).map((lvl) => (
                    <button
                      key={lvl}
                      type="button"
                      onClick={() => {
                        setExperienceLevel(experienceLevel === lvl ? "" : lvl);
                        setMode("browse");
                        setPage(0);
                      }}
                      className={
                        "h-9 px-3 text-[11px] font-bold uppercase tracking-widest border-2 border-ink-950 transition-colors inline-flex items-center justify-between " +
                        (experienceLevel === lvl
                          ? "bg-news text-white"
                          : "bg-white hover:bg-ink-900 hover:text-white")
                      }
                    >
                      <span>{EXPERIENCE_LABELS[lvl]}</span>
                    </button>
                  ))}
                </div>
              </div>
            </div>

            {anyFilter ? (
              <Button
                variant="outline"
                size="sm"
                className="w-full mt-5"
                onClick={clearAll}
              >
                <X className="h-3.5 w-3.5" /> Clear all filters
              </Button>
            ) : null}
          </div>
        </aside>

        <div className="lg:col-span-3 space-y-6">
          <div>
            <SectionTitle
              eyebrow={
                mode === "search" && query
                  ? `Search results · “${query}”`
                  : "All roles"
              }
              title={
                loading
                  ? "Loading jobs…"
                  : jobs.length
                    ? `${totalElements} open role${totalElements === 1 ? "" : "s"}`
                    : "No open roles"
              }
              action={
                activeFilters.length ? (
                  <div className="flex flex-wrap gap-2">
                    {activeFilters.map((f) => (
                      <button
                        key={f.key}
                        onClick={() => clearFilter(f.key)}
                        className="inline-flex items-center gap-1.5 h-8 px-3 border-2 border-ink-950 bg-white text-[10px] font-bold uppercase tracking-widest hover:bg-news hover:text-white transition-colors"
                      >
                        {f.label} <X className="h-3 w-3" />
                      </button>
                    ))}
                  </div>
                ) : undefined
              }
            />
          </div>

          {initialLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {Array.from({ length: 6 }).map((_, i) => (
                <div
                  key={i}
                  className="animate-pulse border-2 border-ink-950 p-5 sm:p-6 space-y-4 bg-white"
                >
                  <div className="h-5 w-3/4 bg-ink-900/20" />
                  <div className="h-4 w-1/2 bg-ink-900/10" />
                  <div className="flex gap-2 pt-2">
                    <div className="h-6 w-20 bg-ink-900/15" />
                    <div className="h-6 w-20 bg-ink-900/15" />
                  </div>
                  <div className="h-3 w-full bg-ink-900/10 mt-2" />
                </div>
              ))}
            </div>
          ) : jobs.length === 0 ? (
            <div className="border-2 border-ink-950 p-10 bg-white shadow-hard-sm text-center">
              <Briefcase className="h-10 w-10 text-news mx-auto mb-4" />
              <h3 className="font-headline text-2xl uppercase mb-2">
                No jobs match your filters
              </h3>
              <p className="text-sm text-ink-700 mb-6 max-w-md mx-auto">
                Try clearing filters, searching for a different keyword, or
                check back later for new openings.
              </p>
              <div className="flex flex-wrap items-center justify-center gap-2">
                <Button variant="news" size="sm" onClick={clearAll}>
                  <X className="h-3.5 w-3.5" /> Reset filters
                </Button>
              </div>
            </div>
          ) : (
            <>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4 md:auto-rows-fr">
                {jobs.map((j) => (
                  <JobCard key={j.id} job={j} />
                ))}
              </div>
              <Pagination page={page} setPage={setPage} totalPages={totalPages} />
            </>
          )}
        </div>
      </section>
    </main>
  );
}
