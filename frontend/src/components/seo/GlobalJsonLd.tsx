import Script from "next/script";
import { SITE } from "@/lib/utils";

export function GlobalJsonLd() {
  const base = SITE.url.replace(/\/$/, "");

  const org = {
    "@context": "https://schema.org",
    "@type": "NewsMediaOrganization",
    "@id": `${base}/#organization`,
    name: SITE.name,
    alternateName: "Map My Times",
    legalName: "MAPMYTOUR LLP",
    url: base,
    logo: {
      "@type": "ImageObject",
      url: `${base}/assets/icons/favicon.svg`,
      width: 512,
      height: 512,
    },
    image: `${base}/assets/og/og-default.svg`,
    email: SITE.email,
    telephone: SITE.phone,
    foundingDate: "2024",
    foundingLocation: {
      "@type": "Place",
      name: "India",
      address: { "@type": "PostalAddress", addressCountry: "IN" },
    },
    address: {
      "@type": "PostalAddress",
      addressCountry: "IN",
      addressRegion: "India",
    },
    areaServed: [
      { "@type": "Country", name: "India" },
      { "@type": "Country", name: "Worldwide" },
    ],
    sameAs: [
      "https://mapmytimes.com",
      "https://www.mapmytour.in",
    ],
    slogan: SITE.tagline,
    missionStatement:
      "MapMyTimes delivers independent, verified journalism — unflinching news coverage, investigations, destination storytelling and stories that serve the public good.",
    ethicsPolicy: `${base}/about`,
    correctionsPolicy: `${base}/contact`,
    diversityPolicy: `${base}/about`,
    masthead: `${base}/about`,
    actionableFeedbackPolicy: `${base}/contact`,
    unnamedSourcesPolicy: `${base}/about`,
    ownershipFundingInfo: `${base}/about`,
    knowsAbout: [
      "India News",
      "Breaking News",
      "Investigative Journalism",
      "Travel and Tourism",
      "Destination Guides",
      "Business",
      "Technology",
      "Sports",
      "Politics",
      "Culture",
      "Opinion",
      "World News",
      "Kerala Backwaters",
      "Himalayan Travel",
      "Golden Triangle",
      "Luxury Hotels",
      "Solo Travel Tips",
    ],
    contactPoint: [
      {
        "@type": "ContactPoint",
        email: SITE.email,
        telephone: SITE.phone,
        contactType: "newsroom",
        areaServed: "IN",
        availableLanguage: ["English", "Hindi"],
      },
      {
        "@type": "ContactPoint",
        email: SITE.email,
        contactType: "customer support",
        areaServed: "Worldwide",
        availableLanguage: ["English"],
      },
    ],
  };

  const website = {
    "@context": "https://schema.org",
    "@type": "WebSite",
    "@id": `${base}/#website`,
    url: base,
    name: SITE.name,
    inLanguage: ["en-IN", "hi-IN", "en"],
    publisher: { "@id": `${base}/#organization` },
    potentialAction: [
      {
        "@type": "SearchAction",
        target: `${base}/search?q={search_term_string}`,
        "query-input": "required name=search_term_string",
      },
    ],
  };

  const nav = {
    "@context": "https://schema.org",
    "@type": "ItemList",
    "@id": `${base}/#sitenavigation`,
    itemListElement: [
      { "@type": "SiteNavigationElement", position: 1, name: "Home", url: `${base}/` },
      { "@type": "SiteNavigationElement", position: 2, name: "India", url: `${base}/category/india` },
      { "@type": "SiteNavigationElement", position: 3, name: "World", url: `${base}/category/world` },
      { "@type": "SiteNavigationElement", position: 4, name: "Business", url: `${base}/category/business` },
      { "@type": "SiteNavigationElement", position: 5, name: "Technology", url: `${base}/category/technology` },
      { "@type": "SiteNavigationElement", position: 6, name: "Sports", url: `${base}/category/sports` },
      { "@type": "SiteNavigationElement", position: 7, name: "Politics", url: `${base}/category/politics` },
      { "@type": "SiteNavigationElement", position: 8, name: "Culture", url: `${base}/category/culture` },
      { "@type": "SiteNavigationElement", position: 9, name: "Opinion", url: `${base}/category/opinion` },
      { "@type": "SiteNavigationElement", position: 10, name: "Explore", url: `${base}/explore` },
      { "@type": "SiteNavigationElement", position: 11, name: "About", url: `${base}/about` },
      { "@type": "SiteNavigationElement", position: 12, name: "Contact", url: `${base}/contact` },
    ],
  };

  const faq = {
    "@context": "https://schema.org",
    "@type": "FAQPage",
    "@id": `${base}/#faq`,
    mainEntity: [
      {
        "@type": "Question",
        name: "What is MapMyTimes?",
        acceptedAnswer: {
          "@type": "Answer",
          text: "MapMyTimes is an independent news platform committed to verified, unflinching journalism — reports, investigations, destination guides and storytelling that serves the public good. It is powered by MAPMYTOUR LLP, India.",
        },
      },
      {
        "@type": "Question",
        name: "How often is MapMyTimes updated?",
        acceptedAnswer: {
          "@type": "Answer",
          text: "MapMyTimes publishes breaking news, features, travel and destination stories throughout the day, seven days a week. Our editors update the homepage hourly with the latest verified journalism.",
        },
      },
      {
        "@type": "Question",
        name: "How do I contact the MapMyTimes newsroom?",
        acceptedAnswer: {
          "@type": "Answer",
          text: `Write to the newsroom at ${SITE.email} or call the editors at ${SITE.phone}. You can also use the Contact page to send tips, corrections or story ideas securely.`,
        },
      },
      {
        "@type": "Question",
        name: "Can journalists join MapMyTimes?",
        acceptedAnswer: {
          "@type": "Answer",
          text: "Yes. MapMyTimes welcomes independent journalists, writers, photographers and editors. Use the Join as Journalist link in the footer to apply and get access to the newsroom dashboard, composer and story tools.",
        },
      },
      {
        "@type": "Question",
        name: "Does MapMyTimes send a newsletter?",
        acceptedAnswer: {
          "@type": "Answer",
          text: "Yes. Subscribe to the free MapMyTimes newsletter on the homepage or the Join panel to receive breaking-news alerts, editor's picks and top stories in your inbox every day.",
        },
      },
      {
        "@type": "Question",
        name: "Where can I read the best travel stories on MapMyTimes?",
        acceptedAnswer: {
          "@type": "Answer",
          text: "Browse the Explore page and categories for destination guides, luxury travel tips, offbeat Himalayan stories, Kerala backwater guides, Golden Triangle itineraries and more — written by on-ground journalists and verified with on-location reporting.",
        },
      },
    ],
  };

  const aeoActions = {
    "@context": "https://schema.org",
    "@type": "WebApplication",
    "@id": `${base}/#app`,
    name: SITE.name,
    url: base,
    applicationCategory: "NewsApplication",
    operatingSystem: "Any",
    browserRequirements: "Requires JavaScript and modern HTML5 browser",
    offers: { "@type": "Offer", price: "0", priceCurrency: "INR" },
    featureList: [
      "Breaking India news and worldwide coverage",
      "Destination and travel guides for India",
      "Personalised search across stories, categories, tags, authors",
      "Free daily newsletter",
      "Journalist dashboard for story publishing",
      "Share, save and comment on stories (logged in)",
    ],
    aggregateRating: {
      "@type": "AggregateRating",
      ratingValue: "4.7",
      bestRating: "5",
      ratingCount: "1324",
    },
  };

  return (
    <>
      <Script id="jsonld-org" type="application/ld+json" strategy="beforeInteractive" dangerouslySetInnerHTML={{ __html: JSON.stringify(org) }} />
      <Script id="jsonld-website" type="application/ld+json" strategy="beforeInteractive" dangerouslySetInnerHTML={{ __html: JSON.stringify(website) }} />
      <Script id="jsonld-nav" type="application/ld+json" strategy="beforeInteractive" dangerouslySetInnerHTML={{ __html: JSON.stringify(nav) }} />
      <Script id="jsonld-faq" type="application/ld+json" strategy="beforeInteractive" dangerouslySetInnerHTML={{ __html: JSON.stringify(faq) }} />
      <Script id="jsonld-aeo" type="application/ld+json" strategy="beforeInteractive" dangerouslySetInnerHTML={{ __html: JSON.stringify(aeoActions) }} />
    </>
  );
}
