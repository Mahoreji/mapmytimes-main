export const ASSETS = {
  logos: {
    primary: "/assets/logos/mapmytimes-logo.png",
  },
  og: {
    default: "/assets/og/og-default.svg",
  },
  icons: {
    favicon: "/assets/icons/favicon.svg",
  },
  placeholders: {
    postCover: "/assets/placeholders/post-cover.svg",
    avatar: "/assets/placeholders/avatar.svg",
    categoryHero: "/assets/placeholders/category-hero.svg",
  },
  categories: {
    india: "/assets/categories/india.svg",
    world: "/assets/categories/world.svg",
    business: "/assets/categories/business.svg",
    technology: "/assets/categories/technology.svg",
    sports: "/assets/categories/sports.svg",
    politics: "/assets/categories/politics.svg",
    culture: "/assets/categories/culture.svg",
    opinion: "/assets/categories/opinion.svg",
  } as Record<string, string>,
} as const;

export function postCoverOrDefault(url?: string | null) {
  return url && url.trim().length > 0 ? url : ASSETS.placeholders.postCover;
}

export function avatarOrDefault(url?: string | null) {
  return url && url.trim().length > 0 ? url : ASSETS.placeholders.avatar;
}

export function categoryImageOrDefault(slug?: string | null) {
  if (!slug) return ASSETS.placeholders.categoryHero;
  const key = slug.toLowerCase();
  return ASSETS.categories[key] ?? ASSETS.placeholders.categoryHero;
}
