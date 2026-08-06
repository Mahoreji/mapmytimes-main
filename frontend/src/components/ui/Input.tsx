import * as React from "react";
import { cn } from "@/lib/utils";

export const Input = React.forwardRef<
  HTMLInputElement,
  React.InputHTMLAttributes<HTMLInputElement> & {
    label?: string;
    error?: string;
    hint?: string;
    trailingIcon?: React.ReactNode;
  }
>(({ className, id, label, error, hint, trailingIcon, type = "text", ...rest }, ref) => {
  const iid = id ?? React.useId();
  return (
    <div className="flex flex-col gap-1.5">
      {label ? (
        <label
          htmlFor={iid}
          className="text-xs font-bold uppercase tracking-widest text-ink-800"
        >
          {label}
        </label>
      ) : null}
      <div className="relative">
        <input
          id={iid}
          ref={ref}
          type={type}
          className={cn(
            "h-11 px-3 w-full border-2 border-ink-950 bg-white text-ink-950 placeholder:text-ink-600 focus:outline-none focus:ring-2 focus:ring-news focus:border-news transition-colors font-sans",
            trailingIcon && "pr-14",
            error && "border-news focus:ring-news",
            className,
          )}
          aria-invalid={!!error}
          {...rest}
        />
        {trailingIcon ? (
          <div className="pointer-events-none absolute right-2.5 top-1/2 -translate-y-1/2">
            {trailingIcon}
          </div>
        ) : null}
      </div>
      {error ? (
        <span className="text-xs font-medium text-news-700">{error}</span>
      ) : hint ? (
        <span className="text-xs text-ink-600">{hint}</span>
      ) : null}
    </div>
  );
});
Input.displayName = "Input";

export const Textarea = React.forwardRef<
  HTMLTextAreaElement,
  React.TextareaHTMLAttributes<HTMLTextAreaElement> & {
    label?: string;
    error?: string;
    hint?: string;
  }
>(({ className, id, label, error, hint, ...rest }, ref) => {
  const iid = id ?? React.useId();
  return (
    <div className="flex flex-col gap-1.5">
      {label ? (
        <label
          htmlFor={iid}
          className="text-xs font-bold uppercase tracking-widest text-ink-800"
        >
          {label}
        </label>
      ) : null}
      <textarea
        id={iid}
        ref={ref}
        className={cn(
          "min-h-[140px] px-3 py-2 w-full border-2 border-ink-950 bg-white text-ink-950 placeholder:text-ink-600 focus:outline-none focus:ring-2 focus:ring-news focus:border-news transition-colors font-sans",
          error && "border-news focus:ring-news",
          className,
        )}
        aria-invalid={!!error}
        {...rest}
      />
      {error ? (
        <span className="text-xs font-medium text-news-700">{error}</span>
      ) : hint ? (
        <span className="text-xs text-ink-600">{hint}</span>
      ) : null}
    </div>
  );
});
Textarea.displayName = "Textarea";

export function Checkbox({
  id,
  label,
  error,
  hint,
  ...rest
}: React.InputHTMLAttributes<HTMLInputElement> & {
  label: React.ReactNode;
  error?: string;
  hint?: string;
}) {
  const iid = id ?? React.useId();
  return (
    <div className="flex flex-col gap-1.5">
      <label htmlFor={iid} className="flex items-start gap-2 cursor-pointer select-none">
        <input
          id={iid}
          type="checkbox"
          className="mt-1 h-4 w-4 accent-news border-2 border-ink-950"
          aria-invalid={!!error}
          {...rest}
        />
        <span className="text-sm text-ink-800">{label}</span>
      </label>
      {error ? (
        <span className="text-xs font-medium text-news-700 pl-6">{error}</span>
      ) : hint ? (
        <span className="text-xs text-ink-600 pl-6">{hint}</span>
      ) : null}
    </div>
  );
}
