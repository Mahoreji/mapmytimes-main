"use client";

import * as React from "react";
import type { ReactNode } from "react";
import { cn } from "@/lib/utils";

export function PageHeader({
  eyebrow,
  title,
  description,
  action,
}: {
  eyebrow?: string;
  title: string;
  description?: string;
  action?: ReactNode;
}) {
  return (
    <div className="border-2 border-ink-950 bg-white p-5 sm:p-6 shadow-hard-sm flex flex-wrap items-end justify-between gap-4">
      <div className="space-y-1.5">
        {eyebrow ? <div className="ribbon text-xs">{eyebrow}</div> : null}
        <h1 className="font-headline text-2xl sm:text-3xl uppercase leading-none">{title}</h1>
        {description ? <p className="text-sm text-ink-700 max-w-2xl">{description}</p> : null}
      </div>
      {action}
    </div>
  );
}

export function Card({
  children,
  className,
}: {
  children: ReactNode;
  className?: string;
}) {
  return (
    <div className={cn("border-2 border-ink-950 bg-white shadow-hard-sm p-5 sm:p-6", className)}>
      {children}
    </div>
  );
}

export function StatCard({
  label,
  value,
  icon,
  tone = "ink",
}: {
  label: string;
  value: ReactNode;
  icon?: ReactNode;
  tone?: "ink" | "news" | "outline";
}) {
  const toneMap = {
    ink: "bg-ink-950 text-white",
    news: "bg-news text-white",
    outline: "bg-white text-ink-950 border-ink-950",
  } as const;
  return (
    <div className={cn("border-2 border-ink-950 p-5 shadow-hard-sm", toneMap[tone])}>
      <div className="flex items-center justify-between gap-3">
        <div className="text-[11px] uppercase tracking-[0.25em] font-bold opacity-85">
          {label}
        </div>
        {icon}
      </div>
      <div className="mt-3 font-headline text-3xl sm:text-4xl uppercase leading-none tabular-nums">
        {value}
      </div>
    </div>
  );
}
