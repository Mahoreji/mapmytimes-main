import * as React from "react";
import { cn } from "@/lib/utils";

type Variant =
  | "primary"
  | "outline"
  | "ghost"
  | "destructive"
  | "news"
  | "ink";
type Size = "sm" | "md" | "lg" | "xl" | "icon";

export interface ButtonProps
  extends React.ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant;
  size?: Size;
  block?: boolean;
  asChild?: boolean;
}

const VARIANT: Record<Variant, string> = {
  primary:
    "bg-ink-950 text-white hover:bg-ink-800 border-2 border-ink-950 shadow-hard-sm hover:shadow-hard",
  news:
    "bg-news text-white hover:bg-news-600 border-2 border-ink-950 shadow-hard-sm hover:shadow-hard",
  ink: "bg-ink-950 text-white hover:bg-ink-800 border-2 border-ink-950",
  outline:
    "bg-white text-ink-950 border-2 border-ink-950 hover:bg-ink-950 hover:text-white",
  ghost: "bg-transparent text-ink-950 hover:bg-ink-900 hover:text-white border-2 border-transparent",
  destructive:
    "bg-news text-white hover:bg-news-700 border-2 border-ink-950",
};

const SIZE: Record<Size, string> = {
  sm: "h-8 px-3 text-sm font-semibold",
  md: "h-10 px-4 text-sm font-semibold",
  lg: "h-12 px-6 text-base font-bold",
  xl: "h-14 px-8 text-lg font-bold",
  icon: "h-10 w-10 p-0 justify-center",
};

export const Button = React.forwardRef<HTMLButtonElement, ButtonProps>(
  (
    { className, variant = "primary", size = "md", block, type = "button", ...rest },
    ref,
  ) => {
    return (
      <button
        ref={ref}
        type={type}
        className={cn(
          "inline-flex items-center justify-center gap-2 select-none transition-all disabled:opacity-50 disabled:cursor-not-allowed font-sans uppercase tracking-wider",
          VARIANT[variant],
          SIZE[size],
          block && "w-full",
          className,
        )}
        {...rest}
      />
    );
  },
);
Button.displayName = "Button";

export function IconButton(props: ButtonProps) {
  return <Button {...props} size="icon" variant={props.variant ?? "ghost"} />;
}
