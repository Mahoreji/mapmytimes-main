import type { MetadataRoute } from "next";
import { SITE } from "@/lib/utils";

export default function manifest(): MetadataRoute.Manifest {
  return {
    name: SITE.name,
    short_name: SITE.name,
    description: `${SITE.tagline} — ${SITE.name} delivers independent, verified journalism — unflinching news coverage, investigations, and stories that matter.`,
    start_url: "/",
    scope: "/",
    display: "standalone",
    orientation: "portrait-primary",
    background_color: "#FFFFFF",
    theme_color: "#0A0A0A",
    categories: ["news", "magazines", "lifestyle", "business"],
    lang: "en-IN",
    dir: "ltr",
    icons: [
      {
        src: "/assets/icons/favicon.svg",
        type: "image/svg+xml",
        sizes: "any",
        purpose: "any",
      },
      {
        src: "/assets/icons/apple-touch-icon.svg",
        type: "image/svg+xml",
        sizes: "180x180",
        purpose: "maskable",
      },
      {
        src: "/assets/logos/mapmytimes-logo.png",
        type: "image/png",
        sizes: "512x512",
        purpose: "any",
      },
    ],
    shortcuts: [
      {
        name: "Latest News",
        short_name: "Home",
        description: "Read the latest stories from MapMyTimes",
        url: "/",
      },
      {
        name: "Explore",
        short_name: "Explore",
        description: "Browse sections and categories",
        url: "/explore",
      },
    ],
  };
}
