import { Metadata } from "next";
import { Suspense } from "react";
import ShortsFeed from "@/components/shorts/ShortsFeed";
import { SITE } from "@/lib/utils";

export const metadata: Metadata = {
  title: `Shorts — Latest News Videos · ${SITE.name}`,
  description: `Vertical news Shorts from ${SITE.name}. Swipe through the latest headlines, breaking news, and on-the-ground reporting — one short at a time.`,
  alternates: {
    canonical: `${SITE.url}/shorts`,
  },
  robots: { index: true, follow: true },
  openGraph: {
    title: `Shorts — Latest News Videos · ${SITE.name}`,
    description: `Swipe through the latest news Shorts from ${SITE.name}.`,
    url: `${SITE.url}/shorts`,
    type: "website",
    siteName: SITE.name,
  },
  twitter: {
    card: "summary_large_image",
    title: `Shorts — Latest News Videos · ${SITE.name}`,
    description: `Vertical news Shorts from ${SITE.name}.`,
  },
};

export default function ShortsPage() {
  return (
    <div className="w-full bg-black min-h-screen">
      <Suspense fallback={null}>
        <ShortsFeed pageSize={12} />
      </Suspense>
    </div>
  );
}
