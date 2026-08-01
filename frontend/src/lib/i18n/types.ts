export type LanguageCode = "en" | "hi";

export type NavLabel =
  | "home"
  | "news"
  | "videos"
  | "shorts"
  | "india"
  | "world"
  | "business"
  | "tech"
  | "technology"
  | "sports"
  | "politics"
  | "culture"
  | "opinion"
  | "sections"
  | "explore"
  | "about"
  | "contact"
  | "careers"
  | "signin"
  | "join"
  | "login"
  | "dashboard"
  | "my_posts"
  | "profile_settings"
  | "signout"
  | "my_apps"
  | "alerts"
  | "moderation";

export interface Dict {
  brand: {
    tagline: string;
  };
  header: {
    searchPlaceholder: string;
    signIn: string;
    join: string;
    language: string;
    languageShort: Record<LanguageCode, string>;
    menu: {
      open: string;
      close: string;
      signedInAs: string;
      dashboard: string;
      myPosts: string;
      profileSettings: string;
      signOut: string;
      searchPlaceholder: string;
      myApps: string;
      alerts: string;
      moderation: string;
    };
  };
  nav: Record<NavLabel, string> & {
    sections_subscribe: string;
    sections_company: string;
    sections_newsroom: string;
  };
  common: {
    loading: string;
    errorGeneric: string;
    retry: string;
    back: string;
    next: string;
    prev: string;
    viewAll: string;
    exploreMore: string;
    readMore: string;
    seeMore: string;
    watchMore: string;
    search: string;
    subscribe: string;
    subscribePlaceholder: string;
    subscribeSuccess: string;
    postedOn: string;
    byAuthor: string;
    minRead: (min: number | string) => string;
    newsletterTitle: string;
    newsletterBody: string;
    footer: {
      mission: string;
      about: string;
      contactNewsroom: string;
      joinAsJournalist: string;
      copyright: (year: number) => string;
      followUs: string;
    };
  };
  home: {
    featured: string;
    trendingNow: string;
    trendingVideo: string;
    latestStories: string;
    categories: string;
    videoNews: string;
    exploreAll: string;
    watchMore: string;
    allVideos: string;
    heroTitle: string;
    heroBody: string;
  };
  sections: {
    title: string;
    description: string;
  };
  explore: {
    title: string;
    description: string;
  };
  search: {
    title: string;
    description: string;
    placeholder: string;
    empty: string;
    noResults: string;
    tryOther: string;
  };
  category: {
    storiesIn: string;
    noStories: string;
  };
  tag: {
    tagLabel: string;
    storiesTagged: string;
    noStories: string;
  };
  careers: {
    openRoles: (n: number) => string;
    noRoles: string;
    filters: string;
    department: string;
    allDepartments: string;
    employmentType: string;
    experienceLevel: string;
    heroEyebrow: string;
    heroTitle: string;
    heroBody: string;
    searchPlaceholder: string;
    search: string;
    clearAll: string;
    backToAllRoles: string;
    aboutRole: string;
    whatYouDo: string;
    whatLookingFor: string;
    readyToJoin: string;
    deadlinePassed: string;
    applyNow: string;
    signInToApply: string;
    createAccountToApply: string;
    applyingAs: string;
    myApplications: string;
    browseOtherRoles: string;
  };
  applications: {
    title: string;
    description: string;
    browseRoles: string;
    noApplications: string;
    noApplicationsBody: string;
    status: string;
    details: string;
    withdraw: string;
    withdrawConfirmTitle: string;
    withdrawConfirmBody: string;
    withdrawConfirmYes: string;
    withdrawConfirmNo: string;
    applicant: string;
    contact: string;
    experience: string;
    coverLetter: string;
    resume: string;
    interview: string;
    rejection: string;
  };
  auth: {
    signIn: string;
    signInCta: string;
    email: string;
    password: string;
    remember: string;
    forgot: string;
    publishTitle: string;
    publishBody: string;
    perks: {
      composer: string;
      moderation: string;
      notifications: string;
    };
    welcome: string;
    welcomeBody: string;
    noAccount: string;
    joinNow: string;
  };
}
