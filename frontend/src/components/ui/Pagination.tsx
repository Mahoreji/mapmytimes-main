"use client";

import { ChevronLeft, ChevronRight } from "lucide-react";
import { Button } from "./Button";

export function Pagination({
  page,
  setPage,
  totalPages,
}: {
  page: number;
  setPage: (n: number) => void;
  totalPages: number;
}) {
  if (totalPages <= 1) return null;
  const pages = Array.from({ length: Math.min(5, totalPages) }, (_, i) => {
    const start = Math.max(0, Math.min(totalPages - 5, page - 2));
    return start + i;
  });
  return (
    <div className="mt-10 flex items-center justify-center gap-2">
      <Button
        variant="outline"
        size="sm"
        onClick={() => setPage(Math.max(0, page - 1))}
        disabled={page === 0}
      >
        <ChevronLeft className="h-4 w-4" /> Prev
      </Button>
      {pages.map((p) => (
        <button
          key={p}
          onClick={() => setPage(p)}
          className={
            "h-10 w-10 border-2 border-ink-950 text-sm font-bold " +
            (p === page ? "bg-news text-white" : "bg-white hover:bg-ink-950 hover:text-white")
          }
        >
          {p + 1}
        </button>
      ))}
      <Button
        variant="outline"
        size="sm"
        onClick={() => setPage(Math.min(totalPages - 1, page + 1))}
        disabled={page >= totalPages - 1}
      >
        Next <ChevronRight className="h-4 w-4" />
      </Button>
    </div>
  );
}
