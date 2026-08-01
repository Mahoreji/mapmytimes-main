export type MainSectionSlug =
  | "india"
  | "world"
  | "business"
  | "technology"
  | "sports"
  | "politics"
  | "culture"
  | "opinion";

export interface MainSectionMeta {
  name: string;
  short: string;
  navLabel?: string;
  tagline: string;
  icon: string;
  accent?: string;
}

export const MAIN_SECTION_SLUGS: MainSectionSlug[] = [
  "india",
  "world",
  "business",
  "technology",
  "sports",
  "politics",
  "culture",
  "opinion",
];

export const MAIN_SECTION_META: Record<MainSectionSlug, MainSectionMeta> = {
  india: {
    name: "India",
    short: "IN",
    navLabel: "India",
    tagline:
      "National news, politics, cities, policy, economy, and public-interest reporting from across India.",
    icon: "/assets/categories/india.svg",
    accent: "india",
  },
  world: {
    name: "World",
    short: "WRL",
    navLabel: "World",
    tagline:
      "Breaking global coverage, world politics, conflict, diplomacy and worldwide story investigations.",
    icon: "/assets/categories/world.svg",
    accent: "world",
  },
  business: {
    name: "Business",
    short: "BIZ",
    navLabel: "Business",
    tagline:
      "Markets, economy, startups, companies, finance, industry, and analysis of the India business story.",
    icon: "/assets/categories/business.svg",
    accent: "business",
  },
  technology: {
    name: "Technology",
    short: "TECH",
    navLabel: "Tech",
    tagline:
      "Tech news, AI, gadgets, policy, internet economy, software, hardware and innovation from India and the world.",
    icon: "/assets/categories/technology.svg",
    accent: "technology",
  },
  sports: {
    name: "Sports",
    short: "SP",
    navLabel: "Sports",
    tagline:
      "Cricket, football, Olympics, athletics, leagues, match analysis and on-ground sports journalism.",
    icon: "/assets/categories/sports.svg",
    accent: "sports",
  },
  politics: {
    name: "Politics",
    short: "POL",
    navLabel: "Politics",
    tagline:
      "Parliament, state assemblies, elections, parties, leadership and the politics behind the headlines.",
    icon: "/assets/categories/politics.svg",
    accent: "politics",
  },
  culture: {
    name: "Culture",
    short: "CUL",
    navLabel: "Culture",
    tagline:
      "Arts, cinema, music, literature, heritage, food, fashion and the stories that shape modern India.",
    icon: "/assets/categories/culture.svg",
    accent: "culture",
  },
  opinion: {
    name: "Opinion",
    short: "OP",
    navLabel: "Opinion",
    tagline:
      "Editorials, columns, guest essays and thoughtful commentary on the issues that matter today.",
    icon: "/assets/categories/opinion.svg",
    accent: "opinion",
  },
};

export interface MainSectionEntry {
  slug: MainSectionSlug;
  order: number;
  meta: MainSectionMeta;
}

export const MAIN_SECTIONS: MainSectionEntry[] = MAIN_SECTION_SLUGS.map((slug, i) => ({
  slug,
  order: i,
  meta: MAIN_SECTION_META[slug],
}));

export const MAIN_SECTION_HREF = (slug: string) =>
  `/category/${encodeURIComponent(String(slug).toLowerCase())}`;

export const MAIN_SECTION_NAV_LINKS: Array<{ href: string; label: string; slug?: MainSectionSlug }> = [
  { href: "/", label: "Home" },
  ...MAIN_SECTIONS.filter((s) => s.slug !== "politics" && s.slug !== "culture" && s.slug !== "opinion").map((s) => ({
    href: MAIN_SECTION_HREF(s.slug),
    label: s.meta.navLabel || s.meta.name,
    slug: s.slug,
  })),
];
