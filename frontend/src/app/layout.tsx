import type { Metadata, Viewport } from "next";
import { Archivo_Black, Inter, Bebas_Neue, Poppins } from "next/font/google";
import "./globals.css";
import { AppProviders } from "@/lib/providers/AppProviders";
import { SiteHeader } from "@/components/site/SiteHeader";
import { SiteFooter } from "@/components/site/SiteFooter";
import { SITE } from "@/lib/utils";
import { GlobalJsonLd } from "@/components/seo/GlobalJsonLd";

const archivo = Archivo_Black({
  subsets: ["latin"],
  weight: "400",
  variable: "--font-archivo-black",
  display: "swap",
});

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "500", "600", "700", "800"],
  variable: "--font-inter",
  display: "swap",
});

const bebasNeue = Bebas_Neue({
  subsets: ["latin"],
  weight: "400",
  variable: "--font-bebas-neue",
  display: "swap",
});

const poppins = Poppins({
  subsets: ["latin"],
  weight: ["600", "700", "800"],
  variable: "--font-poppins",
  display: "swap",
});

export const viewport: Viewport = {
  themeColor: "#0A0A0A",
  width: "device-width",
  initialScale: 1,
};

export const metadata: Metadata = {
  metadataBase: new URL(SITE.url),
  title: {
    default: `${SITE.name} — ${SITE.tagline}`,
    template: `%s | ${SITE.name}`,
  },
  description:
    "MapMyTimes delivers independent, verified journalism — unflinching news coverage, investigations, destination storytelling and stories that matter. India news, worldwide news, travel and more.",
  keywords: [
    "MapMyTimes",
    "Map My Times",
    "India news",
    "latest news",
    "breaking news",
    "investigative journalism",
    "Journalism of Integrity",
    "news India",
    "Himalayan travel",
    "Kerala backwaters",
    "Golden Triangle tour",
    "Munnar tourist places",
    "Alleppey houseboat",
    "Coorg luxury resorts",
    "solo travel tips",
    "India destination guide",
    "Delhi news",
    "Mumbai news",
    "Bengaluru news",
    "world news",
    "business news India",
    "technology news",
    "sports news",
    "politics India",
    "culture India",
    "opinion journalism",
    "Indian travel news",
    "MAPMYTOUR LLP news",
  ],
  authors: [{ name: SITE.name, url: SITE.url }],
  creator: SITE.name,
  publisher: SITE.name,
  applicationName: SITE.name,
  generator: "Next.js 14 App Router",
  category: "News",
  classification: "General News, Investigative Journalism, Travel & Destination Guides",
  abstract: `${SITE.name} — ${SITE.tagline}. Independent reporting, verified facts, public-service journalism, destination storytelling from India and worldwide.`,
  alternates: {
    canonical: `/`,
    languages: {
      "en-IN": "/",
      "hi-IN": "/",
      "x-default": "/",
    },
  },
  manifest: "/manifest.webmanifest",
  robots: {
    index: true,
    follow: true,
    nocache: false,
    googleBot: {
      index: true,
      follow: true,
      noimageindex: false,
      "max-image-preview": "large",
      "max-video-preview": -1,
      "max-snippet": -1,
    },
  },
  verification: {
    google: "mapmytimes",
  },
  openGraph: {
    type: "website",
    siteName: SITE.name,
    locale: "en_IN",
    alternateLocale: ["en_US", "hi_IN"],
    url: SITE.url,
    title: `${SITE.name} — ${SITE.tagline}`,
    description:
      "MapMyTimes delivers independent, verified journalism — unflinching news coverage, investigations, destination guides and stories that matter.",
    images: [
      { url: "/assets/og/og-default.svg", width: 1200, height: 630, alt: `${SITE.name} — ${SITE.tagline}`, type: "image/svg+xml" },
    ],
    countryName: "India",
    emails: [SITE.email],
    phoneNumbers: [SITE.phone],
    ttl: 3600,
  },
  twitter: {
    card: "summary_large_image",
    site: "@mapmytimes",
    creator: "@mapmytimes",
    title: `${SITE.name} — ${SITE.tagline}`,
    description:
      "MapMyTimes delivers independent, verified journalism — unflinching news coverage, investigations, destination guides and stories that matter.",
    images: ["/assets/og/og-default.svg"],
  },
  appleWebApp: {
    capable: true,
    statusBarStyle: "black-translucent",
    title: SITE.name,
    startupImage: ["/assets/icons/apple-touch-icon.svg"],
  },
  formatDetection: {
    email: false,
    address: true,
    telephone: true,
  },
  appLinks: {
    web: {
      url: SITE.url,
      should_fallback: true,
    },
  },
  other: {
    "geo.country": "IN",
    "geo.region": "IN-DL",
    "geo.placename": "India",
    "geo.position": "20.593684;78.96288",
    ICBM: "20.593684, 78.96288",
    "DC.title": `${SITE.name} — ${SITE.tagline}`,
    "DC.creator": SITE.name,
    "DC.publisher": SITE.name,
    "DC.subject": "News, Journalism, India, Travel, Investigative Reporting",
    "DC.description":
      "MapMyTimes delivers independent, verified journalism — unflinching news coverage, investigations, destination guides and stories that matter.",
    "DC.coverage.spatial": "India; Asia; Worldwide",
    "DC.type": "Text; News; Article; Image",
    "DC.date": new Date().toISOString().slice(0, 10),
    "DC.rights": `© ${new Date().getFullYear()} MAPMYTOUR LLP, All Rights Reserved.`,
    "DC.language": "en-IN, hi-IN",
    "DC.source": SITE.url,
    "DC.format": "text/html",
    "DC.identifier": SITE.url,
    "DC.contributor": "Journalists, Editors, Photographers, Contributors",
    rating: "General",
    distribution: "Global",
    revisit: "After 1 days",
    "googlebot-news": "index, follow, max-image-preview:large, max-snippet:-1",
    "google-news-publisher": SITE.name,
    "news_keywords":
      "India news, breaking news, MapMyTimes, investigative journalism, travel India, Kerala, Munnar, Alleppey, Coorg, Himalaya, Golden Triangle, business, tech, sports, politics, culture, opinion",
    place: "India",
    "article-section": "General News",
    "article-tag": "India News, Breaking News, Destination Guides, Investigative Journalism",
    "og:image:alt": `${SITE.name} — ${SITE.tagline}`,
  },
  icons: {
    icon: [
      { url: "/assets/icons/favicon.svg", type: "image/svg+xml", sizes: "any" },
      { url: "/assets/logos/mapmytimes-logo.png", type: "image/png", sizes: "32x32" },
      { url: "/assets/logos/mapmytimes-logo.png", type: "image/png", sizes: "16x16" },
      { url: "/assets/logos/mapmytimes-logo.png", type: "image/png", sizes: "192x192" },
    ],
    apple: [{ url: "/assets/icons/apple-touch-icon.svg", type: "image/svg+xml", sizes: "180x180" }],
    shortcut: ["/assets/icons/favicon.svg"],
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={`${archivo.variable} ${inter.variable} ${bebasNeue.variable} ${poppins.variable}`}>
      <body className="min-h-screen flex flex-col bg-white text-ink-950 antialiased">
        <AppProviders>
          <GlobalJsonLd />
          <a
            href="#main"
            className="sr-only focus:not-sr-only focus:fixed focus:top-3 focus:left-3 focus:z-50 focus:bg-news focus:text-white focus:px-4 focus:py-2 font-bold"
          >
            Skip to content
          </a>
          <SiteHeader />
          <main id="main" className="flex-1">
            {children}
          </main>
          <SiteFooter />
        </AppProviders>
      </body>
    </html>
  );
}
