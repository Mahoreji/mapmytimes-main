import type { Config } from "tailwindcss";

const config: Config = {
  content: [
    "./src/app/**/*.{ts,tsx}",
    "./src/components/**/*.{ts,tsx}",
    "./src/**/*.{ts,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        ink: {
          950: "#0A0A0A",
          900: "#121212",
          800: "#1A1A1A",
          700: "#242424",
          600: "#2E2E2E",
        },
        news: {
          DEFAULT: "#E31E24",
          50:  "#FDECEE",
          100: "#FBD8DC",
          200: "#F5AFB6",
          300: "#EE858F",
          400: "#E85B69",
          500: "#E31E24",
          600: "#D0121A",
          700: "#A80D14",
          800: "#800A10",
          900: "#58070A",
        },
      },
      fontFamily: {
        headline: ["var(--font-archivo-black)", "Anton", "Impact", "sans-serif"],
        sans: ["var(--font-inter)", "Manrope", "system-ui", "sans-serif"],
      },
      boxShadow: {
        ribbon: "0 4px 0 0 #D0121A, 0 8px 24px rgba(227,30,36,0.25)",
        hard: "8px 8px 0 0 #0A0A0A",
        "hard-sm": "4px 4px 0 0 #0A0A0A",
      },
      keyframes: {
        ticker: {
          "0%":   { transform: "translateX(0%)" },
          "100%": { transform: "translateX(-50%)" },
        },
        pulseDot: {
          "0%,100%": { opacity: "1", transform: "scale(1)" },
          "50%":     { opacity: "0.5", transform: "scale(1.2)" },
        },
      },
      animation: {
        ticker:    "ticker 45s linear infinite",
        pulseDot:  "pulseDot 1.2s ease-in-out infinite",
      },
    },
  },
  plugins: [],
};

export default config;
