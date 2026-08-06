// -----------------------------------------------------------------------------
// MapMyTimes — Legal & Policy Page Content
// -----------------------------------------------------------------------------
// Non-developers: edit the text below freely. Each page has:
//   slug       = URL segment
//   title      = H1 page title
//   description= SEO meta description
//   lastUpdated= ISO date (YYYY-MM-DD)
//   sections[] = Array of { heading, paragraphs?, list? }
// -----------------------------------------------------------------------------

export type LegalSection = {
  heading: string;
  paragraphs?: string[];
  list?: string[];
  midParagraphs?: string[];
  secondList?: string[];
  postListParagraphs?: string[];
};

export type LegalPage = {
  slug: string;
  title: string;
  description: string;
  lastUpdated: string;
  eyebrow: string;
  sections: LegalSection[];
};

const BRAND = {
  site: "MapMyTimes",
  url: "mapmytimes.com",
  email: "admin@mapmytimes.com",
  phone: "+91 80859 27274",
  operator: "MAPMYTOUR LLP",
  city: "Chhindwara",
  state: "Madhya Pradesh",
  country: "India",
  mapmypr:
    "MapMyPR is a separate public relations and communications brand operated under the same parent organisation, MAPMYTOUR LLP. MapMyPR services are completely independent of the MapMyTimes newsroom.",
  year: new Date().getFullYear(),
};

// -----------------------------------------------------------------------------
// 1. Editorial Policy
// -----------------------------------------------------------------------------
export const editorialPolicy: LegalPage = {
  slug: "editorial-policy",
  title: "Editorial Policy",
  description:
    "The MapMyTimes Editorial Policy — our mission, standards, sourcing, review process, correction policy, sponsored content disclosure, and editorial independence statement.",
  lastUpdated: "2026-08-04",
  eyebrow: "Newsroom Standards",
  sections: [
    {
      heading: "1. Our Mission & Editorial Values",
      paragraphs: [
        `${BRAND.site} is an independent digital newsroom operated by ${BRAND.operator}, ${BRAND.city}, ${BRAND.state}, ${BRAND.country}. Our mission is to deliver journalism that serves citizens — holding power to account, amplifying voices on the margins, and giving readers the verified, contextual facts they need to form their own opinions.`,
        "Our reporting is guided by four non-negotiable values: Accuracy (every claim verified before publication), Independence (free from commercial or political interference), Fairness (we seek the relevant sides, not false balance), and Transparency (we disclose our process and correct publicly).",
      ],
    },
    {
      heading: "2. How Stories Are Sourced, Written & Reviewed",
      paragraphs: [
        "Every story published on MapMyTimes passes through a multi-stage editorial workflow before readers see it:",
      ],
      list: [
        "Sourcing: Reporters are required to name sources wherever possible. Anonymous sourcing is treated as a last resort — never the default — and must be approved by a senior editor, with a clear on-the-record justification to the reader included in the story.",
        "Writing: Copy is drafted by the beat reporter with full attribution and links to primary documents, datasets, or relevant public records wherever available.",
        "Desk review: Each story is reviewed by a section editor for accuracy, balance, clarity, and adherence to our house style.",
        "Copy edit + legal review: Every non-breaking story passes through a copy editor. Stories involving legal risk, serious allegations, or identifiable vulnerable individuals receive a secondary legal/standards review.",
        "Final sign-off: The section editor or Managing Editor gives final sign-off before the story goes live.",
      ],
    },
    {
      heading: "3. Separation of News & Editorial Content from Sponsored / PR Content",
      paragraphs: [
        "MapMyTimes maintains a strict, institutional separation between our independent news/editorial output and any commercial, sponsored, or public-relations content.",
        BRAND.mapmypr,
        "Where content is paid for, sponsored, commissioned by a brand partner, or produced as part of a PR/commercial arrangement (even if created by our in-house teams), it will ALWAYS be:",
      ],
      list: [
        "Clearly and conspicuously labeled at the top of the article (e.g., Sponsored, Partner Content, Presented by, Advertisement, Brand Feature) in a style distinct from standard story labels.",
        "Separated visually from organic news and opinion content on page layouts and in section listings.",
        "Kept out of the editorial decision-making pipeline — no sponsor, advertiser, client, or partner ever dictates what MapMyTimes does or does not cover as news.",
      ],
    },
    {
      heading: "4. Correction & Retraction Process",
      paragraphs: [
        "We make mistakes. When we do, we correct them openly, promptly, and in a way that is visible to readers — not buried or deleted silently.",
        "Any reader can report an error or concern by:",
      ],
      list: [
        `Writing to ${BRAND.email} with the subject line "Correction Request: [Headline or URL of the story]"`,
        `Using our Contact form at ${BRAND.url}/contact and selecting "Correction" as the category`,
        `Contacting our newsroom desk at ${BRAND.phone} (10am–7pm IST, working days)`,
      ],
      postListParagraphs: [
        "On receipt of a correction request, the section editor reviews the original story, sourcing, and the reader's evidence within two working days.",
        "For factual errors: a Correction notice is added to the TOP of the story (below the headline) describing what was wrong, what was changed, and the date. A permanent correction log is retained by the desk.",
        "For significant errors that fundamentally undermine a story's conclusion: the story may be retracted with a prominent Retraction notice explaining the reason, and the original preserved (where feasible) for the record.",
        "Typos, style fixes, and minor formatting changes that do not affect the factual substance of a story are corrected inline without a formal notice, as is industry standard.",
      ],
    },
    {
      heading: "5. Editorial Independence Statement",
      paragraphs: [
        `${BRAND.site} is editorially independent. No owner, investor, advertiser, sponsor, donor, business partner, or political entity of any kind has the right to influence what MapMyTimes publishes, suppresses, or delays as news.`,
        "Our newsroom budget, commercial partnerships, and sponsored revenue streams are managed by separate business teams who are walled off from the editorial desk. Editorial staff salaries and reporting budgets are never contingent on advertiser outcomes.",
        "All editorial personnel with bylines, sign-off authority, or story assignment responsibilities are required to disclose personal, financial, and professional conflicts of interest to the Managing Editor. Where a conflict exists and cannot be managed, the reporter or editor is recused from the story.",
      ],
    },
    {
      heading: "6. Contact for Editorial Queries",
      paragraphs: [
        "For editorial queries, correction requests, story pitches, and standards concerns, please contact:",
      ],
      list: [
        `Email: ${BRAND.email}`,
        `Phone: ${BRAND.phone}`,
        `Contact form: ${BRAND.url}/contact`,
        `Address: ${BRAND.operator}, ${BRAND.city}, ${BRAND.state}, ${BRAND.country}`,
      ],
    },
  ],
};

// -----------------------------------------------------------------------------
// 2. Fact-Check Policy
// -----------------------------------------------------------------------------
export const factCheckPolicy: LegalPage = {
  slug: "fact-check-policy",
  title: "Fact-Check Policy",
  description:
    "MapMyTimes Fact-Check Policy — source verification standards, how misinformation is flagged and corrected, and how fact-checks differ from news and opinion.",
  lastUpdated: "2026-08-04",
  eyebrow: "Verification Standards",
  sections: [
    {
      heading: "1. Our Commitment to Verification Before Publication",
      paragraphs: [
        "At MapMyTimes, verification is not an add-on step — it is the foundation of every story.",
        "Every factual claim made in any published piece (news report, analysis, feature, or investigation) is checked against at least one primary source or two independent corroborating secondary sources before the story goes live. No claim, statistic, or quote is published on hearsay alone.",
      ],
    },
    {
      heading: "2. Source Verification Standards",
      paragraphs: ["We follow the following hierarchy for sources:"],
      list: [
        "Primary sources preferred: Original documents, on-the-record interviews with direct participants, public records, official datasets, verifiable photographs / video footage, and direct observation by our journalists are the gold standard.",
        "Multiple independent sources required: Where a claim is controversial, sensitive, or could cause tangible harm, at least two independent and unrelated sources must corroborate it on the record.",
        "Attribution requirements: Every direct quote, every statistic, and every document referenced must be attributed to its source (by name wherever possible; with a clear description of the source's position/access when anonymity is a justified last resort).",
        "Digital forensics: Viral images, screenshots, and videos are verified using reverse image search, metadata examination, geolocation / chronolocation, and cross-reference with authoritative archives before publication.",
        "Government and official sources: Press releases, government statements, and official spokespersons are treated as one source — never as the sole source — for any sensitive or contested claim.",
      ],
    },
    {
      heading: "3. How Readers Can Flag Suspected Misinformation",
      paragraphs: [
        "Readers are our most important partners in accuracy. If you believe a story, graphic, headline, or social post by MapMyTimes contains incorrect, misleading, or unsubstantiated information, please raise it with us via any of the channels below — every submission is read and logged by a senior editor:",
      ],
      list: [
        `Email ${BRAND.email} with subject line "Fact-Check Flag: [story URL / headline]"`,
        `Submit via the Contact form at ${BRAND.url}/contact and select "Fact-check / Misinformation"`,
        "Direct message our verified social accounts (@mapmytimes) with a link to the content and a one-line summary of your concern.",
      ],
      postListParagraphs: [
        "For third-party content (reader comments, user-submitted posts, third-party embeds) that you believe contains harmful misinformation, please use the same channels with the location of the content clearly described — we review and act on such flags within 24 working hours.",
      ],
    },
    {
      heading: "4. How Corrections & Updates to Fact-Checked Stories Are Handled",
      paragraphs: [
        "When a fact-check, correction, or post-publication update is applied to a story:",
      ],
      list: [
        "A clearly labeled CORRECTION, UPDATE, or CLARIFICATION box is placed conspicuously at the TOP of the story (immediately below the headline), dated, and written in plain language.",
        "The box describes: what was originally incorrect / incomplete, what the corrected / updated fact is, and what evidence was used to make the change.",
        "The story's update timestamp is revised. Where feasible, the original incorrect sentence(s) are preserved with strikethrough or in an editor's note so the change is transparent, not invisible.",
        "If a story is fundamentally undermined by a factual error and cannot be repaired in place, it is formally retracted with a RETRACTED banner and accompanying explanation, rather than silently deleted.",
      ],
    },
    {
      heading: "5. Distinction Between News Reporting, Opinion / Analysis & Fact-Check Content",
      paragraphs: [
        "MapMyTimes publishes several types of content — each with a clear visual label so readers know what they are getting:",
      ],
      list: [
        "News Reporting: Straight, factual account of events. Claims are attributed, multiple sources corroborated, and language is neutral.",
        "Analysis / Opinion: Written by columnists, guest contributors, or editors. Contains the author's judgment and perspective, clearly labeled OPINION, COLUMN, or ANALYSIS. Opinion writers' views do not necessarily represent the institutional position of MapMyTimes.",
        "Fact-Check: A dedicated FACT CHECK piece systematically evaluates a specific claim circulating in the public domain. Each Fact Check includes our methodology, sources consulted, and a clear verdict (e.g., True, Mostly True, False, Misleading, Out of Context, Unverifiable).",
      ],
    },
    {
      heading: "6. Contact for Fact-Check Submissions",
      paragraphs: [
        "To submit a claim for fact-checking or to contact our Fact-Check desk:",
      ],
      list: [
        `Email: ${BRAND.email} (subject: "Fact-Check Submission")`,
        `Contact form: ${BRAND.url}/contact`,
        `Desk phone: ${BRAND.phone} (10am–7pm IST, Mon–Sat)`,
      ],
    },
  ],
};

// -----------------------------------------------------------------------------
// 3. Privacy Policy
// -----------------------------------------------------------------------------
export const privacyPolicy: LegalPage = {
  slug: "privacy-policy",
  title: "Privacy Policy",
  description:
    "MapMyTimes Privacy Policy — what personal data we collect, how we use it, cookies, third-party services, data retention, user rights, children's privacy, and how to contact us.",
  lastUpdated: "2026-08-04",
  eyebrow: "Your Privacy",
  sections: [
    {
      heading: "1. Introduction",
      paragraphs: [
        `This Privacy Policy applies to all services offered by ${BRAND.site} (operated by ${BRAND.operator}, ${BRAND.city}, ${BRAND.state}, ${BRAND.country}), including our website ${BRAND.url}, mobile applications, newsletters, and related digital properties (collectively, the "Services").`,
        "We take your privacy seriously. This policy describes the personal information we collect, why we collect it, how we use it, who we share it with, and the choices and rights you have.",
      ],
    },
    {
      heading: "2. What Personal Data We Collect",
      paragraphs: [
        "Depending on how you interact with the Services, we may collect the following categories of personal data:",
      ],
      list: [
        "Account & registration information: When you create a MapMyTimes account — name, email address, phone number (optional), username, password hash, and profile details you choose to provide.",
        "Comments & contributions: Comments you post on articles, text of letters to the editor, submissions via Contact, and any other content you submit.",
        "Newsletter & communications: Email address and subscription preferences when you sign up for newsletters, alerts, or marketing communications.",
        "Cookies & device / usage analytics: Standard web server log data (IP address, browser type, referring URL, exit pages, timestamp), and anonymised or pseudonymised usage data via cookies and analytics SDKs (e.g., pages read, scroll depth, session duration, device type, approximate geographic region).",
        "Contact & support: Any information you voluntarily share when writing to us via forms, email, or phone.",
        "Payment data (if applicable): Where paid subscriptions or paid products are offered, payment processing is handled by a PCI-DSS compliant payment processor — MapMyTimes does not store full card numbers on our own servers.",
      ],
    },
    {
      heading: "3. How We Use Your Data",
      paragraphs: ["Personal data is used only for the following legitimate purposes:"] as string[] | undefined,
      list: [
        "Service delivery: To operate the Services, authenticate you as a user, display your personalised reading experience, publish and moderate your comments, and deliver newsletters you have subscribed to.",
        "Personalisation: To surface stories, recommendations, and features we think will interest you (you can opt out of personalisation where technically feasible via your account settings).",
        "Communications: To respond to your support inquiries, send important service announcements (security, policy changes, outage notices), and — only where you have given explicit opt-in consent — marketing / promotional emails.",
        "Analytics & improvement: To understand aggregate reader behaviour, improve story selection, site performance, and product features. Analytics data is used at the aggregate level wherever possible and individual users are not identified in reports.",
        "Legal & security: To comply with applicable law, respond to lawful requests from public authorities, protect the security and integrity of our systems and users, and enforce our Terms & Conditions.",
      ],
    },
    {
      heading: "4. Cookie Policy Summary",
      paragraphs: [
        "We use cookies and similar local-storage technologies for the following purposes:",
      ],
      list: [
        "Strictly necessary cookies: Required for core site functionality (e.g., login sessions, security tokens). Cannot be turned off for the site to work.",
        "Preference cookies: Remember your settings (theme, region, font size, last-read position).",
        "Analytics cookies: Measure aggregate usage to improve the product. You can opt out via your browser settings or our (forthcoming) Cookie Preference banner.",
        "Third-party cookies (embedded content): YouTube videos, social media embeds, and embedded widgets may set their own cookies — these are governed by the third party's own privacy policies, not this one.",
      ],
      postListParagraphs: [
        "You can disable or restrict cookies at any time via your browser's cookie settings. Doing so may affect certain features of the Services (login, saved articles, preference persistence).",
      ],
    },
    {
      heading: "5. Third-Party Services Used",
      paragraphs: [
        "The following categories of third parties may process personal data on our behalf under written data-processing agreements with appropriate safeguards:",
      ],
      list: [
        "Hosting & Infrastructure: Our hosting provider(s) located in India and/or global cloud regions, who process data solely on our instructions to keep the Services online.",
        "[Analytics Provider]: Web and mobile analytics service used to measure aggregate reader usage and performance.",
        "Email / Newsletter provider: Transactional and newsletter email delivery service.",
        "Social platforms: Content embedded from YouTube, Instagram, Facebook/X, and similar platforms is loaded directly from their servers and subject to their privacy policies — we recommend reviewing those policies for information about their data practices.",
        "CDN and image delivery providers: Content Delivery Networks accelerate image and asset delivery for readers worldwide.",
      ],
    },
    {
      heading: "6. Data Retention & Security",
      paragraphs: [
        "We retain personal data only for as long as necessary to fulfil the purposes for which it was collected, or as required by applicable law:",
      ],
      list: [
        "Account data is retained for the lifetime of your account plus a reasonable wind-down period (typically 12 months) after deletion request.",
        "Newsletter email addresses are retained until the subscriber unsubscribes or the address is found to be invalid.",
        "Comments and contributed content are retained indefinitely unless a user requests deletion (see Section 7) — note that published comments may have been shared, indexed, or cached by third parties beyond our control.",
        "Web server / analytics logs are retained for a maximum of 18 months, after which they are aggregated, pseudonymised, or securely deleted.",
      ],
      postListParagraphs: [
        "Security: We employ reasonable industry-standard administrative, technical, and physical safeguards — including encrypted connections (TLS), hashed / salted credentials, role-based access, and regular vulnerability scans — to protect personal data against unauthorised access, alteration, disclosure, or destruction. No internet service can guarantee absolute security, however.",
      ],
    },
    {
      heading: "7. Your Rights & How to Exercise Them",
      paragraphs: [
        "Under applicable Indian law (the Digital Personal Data Protection Act, 2023, and other laws as may be in force) and other applicable regulations, you have the right to:",
      ],
      list: [
        "Access: Request a copy of the personal data we hold about you.",
        "Correction: Ask us to update or correct inaccurate or incomplete personal data.",
        "Deletion / Erasure: Request that we delete your personal data (subject to our legal retention obligations for records, accounting, tax, and enforcement).",
        "Withdrawal of Consent: Where processing is based on your consent, you may withdraw consent at any time (e.g., unsubscribe from newsletters, delete your account).",
        "Grievance: Raise a grievance about any aspect of our handling of your personal data and receive a reasoned response.",
      ],
      postListParagraphs: [
        `To exercise any of these rights, please write to our Grievance Officer at ${BRAND.email} with the subject line "Privacy / DPDP Request: [Your Name]". Please include sufficient information to allow us to identify your account — typically your registered email address and any relevant URLs or timestamps.`,
        "We will acknowledge receipt within 72 working hours and provide a substantive response within 30 calendar days of receipt of a complete request.",
      ],
    },
    {
      heading: "8. Children's Privacy",
      paragraphs: [
        `The Services are not directed at children under the age of 18 ("Minors"). We do not knowingly solicit or knowingly collect personal information from Minors.`,
        "If you are a parent or guardian and believe your child has provided us with personal data without your consent, please contact us at the address in Section 10 and we will take reasonable steps to delete that information promptly.",
      ],
    },
    {
      heading: "9. Policy Updates",
      paragraphs: [
        "We may amend this Privacy Policy from time to time to reflect changes in our practices, products, or legal obligations.",
        "Material changes will be notified via a prominent notice on the website for at least 30 days before they take effect, and the 'Last updated' date at the top of this page will be revised. Please review this page periodically.",
      ],
    },
    {
      heading: "10. Contact for Privacy Queries",
      paragraphs: ["For all privacy, data, and DPDP-related queries, please contact:"],
      list: [
        `Grievance Officer / Data Privacy — ${BRAND.site}`,
        `Email: ${BRAND.email} (subject: "Privacy Query")`,
        `Phone: ${BRAND.phone}`,
        `Address: ${BRAND.operator}, ${BRAND.city}, ${BRAND.state}, ${BRAND.country}`,
      ],
    },
  ],
};

// -----------------------------------------------------------------------------
// 4. Terms & Conditions
// -----------------------------------------------------------------------------
export const termsConditions: LegalPage = {
  slug: "terms-and-conditions",
  title: "Terms & Conditions",
  description:
    "MapMyTimes Terms & Conditions — rules for using our website, accounts, conduct, IP ownership, prohibited uses, liability, governing law (India / Madhya Pradesh jurisdiction) and contact.",
  lastUpdated: "2026-08-04",
  eyebrow: "Use of Service",
  sections: [
    {
      heading: "1. Acceptance of Terms",
      paragraphs: [
        `By accessing, browsing, using, creating an account on, or downloading content from ${BRAND.url} and the MapMyTimes mobile applications (together, the "Services"), you agree to be bound by these Terms & Conditions and the accompanying Privacy Policy, Editorial Policy, and Copyright Notice.`,
        `The Services are owned, operated, and controlled by ${BRAND.operator}, a limited liability partnership registered under the laws of ${BRAND.country}, with registered office at ${BRAND.city}, ${BRAND.state}, ${BRAND.country}.`,
        "If you do not agree to any part of these Terms, please do not use the Services.",
      ],
    },
    {
      heading: "2. Description of Service",
      paragraphs: [
        "MapMyTimes is an independent digital news and media platform offering journalistic content including news reports, investigations, opinion columns, destination features, fact-checks, photography, video reporting, short-form video, newsletters, and related features (the 'Content').",
        "The Content is provided for personal, informational, and non-commercial use. Access to certain features, including comments, saved articles, newsletters, and personalised recommendations, may require registration of a free user account.",
      ],
    },
    {
      heading: "3. User Accounts & Conduct Rules",
      paragraphs: ["Where you create an account or contribute user-generated content (UGC) such as comments, posts, or submissions, you agree that:"] as string[] | undefined,
      list: [
        "You are at least 18 years of age, or (if between 13 and 17) you have obtained verifiable parental or guardian consent to register and use the account.",
        "You will provide accurate, truthful registration information and update it to keep it current.",
        "You are responsible for safeguarding the confidentiality of your account credentials and for all activities conducted under your account.",
        "You will not impersonate any person or entity, misrepresent your affiliation, or use a false, misleading, or unlawful identity.",
        "UGC and conduct rules: You will not post, share, or transmit content or comments that are unlawful, defamatory, hate speech, discriminatory, harassing, sexually explicit (without editorial / legitimate journalistic context), inciting to violence, infringing of third-party rights, spam, malware, or deliberately misleading / fake news.",
        "MapMyTimes reserves the right — without being obliged — to moderate, edit, refuse to publish, remove, or disable access to any UGC, and to suspend or terminate any account, at any time and for any reason, with or without prior notice.",
      ],
    },
    {
      heading: "4. Intellectual Property Ownership",
      paragraphs: [
        `All intellectual property rights in and to the Services — including but not limited to text articles, photography, illustrations, infographics, video reports, short-form content, audio, logos, brand assets ("${BRAND.site}" wordmark, "Journalism of Integrity" tagline), website design, UI code, trademarks, and trade dress — are the exclusive property of ${BRAND.operator} and/or its licensors and contributors, and are protected under the Copyright Act, 1957, the Trade Marks Act, 1999, and all other applicable intellectual property laws of ${BRAND.country} and international treaties.`,
        "Contributors, photographers, and freelance journalists whose bylines appear on content retain moral rights of attribution and integrity as provided by applicable law, and may have written agreements with us covering commercial exploitation and assignment of copyright in their published work.",
      ],
    },
    {
      heading: "5. Permitted vs. Prohibited Uses",
      paragraphs: ["Permitted uses (subject to our Copyright Notice):"],
      list: [
        "Personal, non-commercial reading, viewing, and storage of Content via normal browser or app functionality.",
        "Personal, non-commercial sharing: Sharing links to stories on social media or via direct message, with proper attribution to MapMyTimes and a functioning hyperlink back to the original URL on mapmytimes.com.",
        "Fair dealing / fair use as expressly permitted by the Copyright Act, 1957, for purposes such as criticism, review, research, teaching, or news reporting — with due acknowledgement of the source.",
      ],
      midParagraphs: [
        "Prohibited uses (without our prior, specific, written permission — see Copyright Notice):",
      ],
      secondList: [
        "Republishing, reproducing, reposting, scraping, crawling, archiving, or bulk-downloading MapMyTimes Content — whether in whole or in part, in any medium (text, image, video, audio) — for any commercial purpose, paywalled use, or to create a competing or derivative product, publication, feed, dataset, or service.",
        "Removing or modifying any copyright notice, watermark, byline, attribution, link-back, or DRM protection from Content.",
        "Misrepresenting MapMyTimes Content as your own, or using our trademarks, logos, or brand assets in any way that suggests endorsement, sponsorship, or affiliation without a written agreement.",
        "Using the Services in any manner that disrupts, damages, overburdens, or impairs our infrastructure, security, or other users' access — including DDoS, credential-stuffing, scraping via automated tools without consent, or injection of malicious code.",
      ],
    },
    {
      heading: "6. Third-Party Links & Content Disclaimer",
      paragraphs: [
        "The Services may contain links to, and embeds of, third-party websites, platforms, advertisers, social media posts, video players, and content that are not owned, operated, or moderated by MapMyTimes.",
        "Such third-party links and content are provided solely for reader convenience. MapMyTimes does not endorse, verify, adopt, or assume any responsibility or liability for the accuracy, legality, privacy practices, terms, safety, or quality of any third-party website or content. You access such content at your own risk.",
      ],
    },
    {
      heading: "7. Limitation of Liability & Disclaimers",
      paragraphs: [
        "The Services and all Content are provided on an 'AS IS' and 'AS AVAILABLE' basis, without any warranties, express or implied, including but not limited to implied warranties of merchantability, fitness for a particular purpose, title, and non-infringement — to the maximum extent permissible under applicable law.",
        "MapMyTimes makes reasonable efforts to ensure the accuracy and timeliness of Content, but journalism is inherently iterative and we do not warrant that Content will be error-free, uninterrupted, complete, or current at any particular moment.",
        `In no event shall ${BRAND.operator}, its partners, journalists, contributors, affiliates, officers, employees, agents, or licensors be liable for any indirect, incidental, special, consequential, exemplary, or punitive damages, including (without limitation) loss of profits, data, business, goodwill, or reputation, arising out of or related to your use of, or inability to use, the Services or any Content — even if advised of the possibility of such damages.`,
        "Our aggregate total liability under these Terms (for any direct claim not excluded above) shall be limited to the total amount, if any, paid by you to MapMyTimes for the Services in the 12 calendar months preceding the event giving rise to the claim.",
      ],
    },
    {
      heading: "8. Governing Law & Dispute Resolution",
      paragraphs: [
        `These Terms & Conditions, and any dispute, claim, or controversy arising out of or relating to them or the Services (whether in contract, tort, statute, or otherwise), shall be:`,
      ],
      list: [
        `Governing law: Governed exclusively by the laws of ${BRAND.country}, without regard to conflict-of-law principles.`,
        `Jurisdiction: Subject exclusively to the jurisdiction of the competent courts located in ${BRAND.state}, ${BRAND.country}.`,
        "Dispute resolution: Where feasible, the parties will first attempt to resolve any dispute informally through good-faith negotiation within 30 days of written notice, before commencing formal proceedings.",
      ],
    },
    {
      heading: "9. Changes to Terms",
      paragraphs: [
        "We may update these Terms & Conditions from time to time to reflect changes to the Services, practices, or the law.",
        "Material changes will be notified by a prominent website notice for a reasonable period prior to taking effect, and the 'Last updated' date at the top of this page will be revised. Your continued use of the Services after the effective date of a revision constitutes acceptance of the revised Terms.",
      ],
    },
    {
      heading: "10. Severability & Entire Agreement",
      paragraphs: [
        "If any provision of these Terms is held by a court or tribunal of competent jurisdiction to be invalid, illegal, or unenforceable, the remaining provisions shall remain in full force and effect, and the invalid provision shall be reformed, to the extent permissible, so as to achieve its original intent as closely as possible.",
        "These Terms, together with the Privacy Policy, Editorial Policy, and Copyright Notice referenced herein, constitute the entire agreement between you and MapMyTimes with respect to the use of the Services, and supersede any prior or contemporaneous oral or written understandings.",
      ],
    },
    {
      heading: "11. Contact Information",
      paragraphs: [
        `For any queries, notices, or takedown requests relating to these Terms, please contact ${BRAND.operator} at:`,
      ],
      list: [
        `Email: ${BRAND.email}`,
        `Phone: ${BRAND.phone}`,
        `Address: ${BRAND.operator}, ${BRAND.city}, ${BRAND.state}, ${BRAND.country}`,
      ],
    },
  ],
};

// -----------------------------------------------------------------------------
// 5. Copyright Notice
// -----------------------------------------------------------------------------
export const copyrightNotice: LegalPage = {
  slug: "copyright-notice",
  title: "Copyright Notice",
  description:
    "MapMyTimes Copyright Notice — © content ownership, permitted personal use, prohibited reproduction, reprint permission process, DMCA-style takedown for copyright infringement, and licensing contact.",
  lastUpdated: "2026-08-04",
  eyebrow: "Copyright",
  sections: [
    {
      heading: "1. Copyright Ownership Statement",
      paragraphs: [
        `© ${BRAND.year} ${BRAND.site} / ${BRAND.operator}. All rights reserved.`,
        `Unless otherwise explicitly noted, all Content published or otherwise made available on the Services — including all text articles, news reports, opinion pieces, fact-checks, features, destination guides, photography, photojournalism, illustrations, infographics, design work, logos and trademarks, video reports, short-form video, podcasts, audio content, source code, UI / UX design elements, and the selection, coordination, arrangement and compilation of the foregoing — is the exclusive property of ${BRAND.operator} and/or its respective licensors, freelance contributors, photographers, illustrators, and journalists under written assignment agreements.`,
        "Content is protected under the Copyright Act, 1957 of India, and all applicable copyright laws, treaties, and conventions internationally (including but not limited to the Berne Convention).",
      ],
    },
    {
      heading: "2. Permitted Use (Personal, Non-Commercial Sharing)",
      paragraphs: [
        "Subject to the conditions below, you are free to:",
      ],
      list: [
        "Read, view, stream, listen to, and print a single copy of individual Content items for your own personal, non-commercial, informational use.",
        `Share links to stories on personal social media accounts, via private messaging, or on personal blogs, provided that: (a) you do not excerpt more than a short, reasonable snippet (max 2–3 lines) of text; (b) you clearly attribute the work to MapMyTimes / the bylined journalist; and (c) you include a visible, functioning, direct hyperlink back to the original story URL on https://${BRAND.url}.`,
        "Cite or quote short, fair portions of Content for purposes of criticism, review, teaching, research, parody, or news reporting — in each case with full attribution and link-back, and within the bounds of 'fair dealing' under the Copyright Act, 1957.",
      ],
    },
    {
      heading: "3. Prohibited Use",
      paragraphs: [
        "The following uses are strictly PROHIBITED unless you have obtained prior, specific, WRITTEN permission from MapMyTimes (see Section 4):",
      ],
      list: [
        "Full or substantial republication, reproduction, or reposting of any MapMyTimes article, photograph, video, illustration, infographic, or design asset — in whole or in part — on any website, blog, social media account, print publication, email newsletter, paywalled service, commercial content platform, AI / LLM training dataset, or derivative work.",
        "Modification, adaptation, translation, remix, or creation of derivative works from Content for public or commercial use.",
        "Removal, cropping, covering, or alteration of any byline, credit line, attribution, watermark, MapMyTimes logo, or copyright notice on any Content.",
        "Bulk or systematic scraping, crawling, archival, mirroring, screenshotting, or automated downloading of Content for any purpose, personal or commercial, using scripts, bots, scrapers, AI tools, or any other automated method.",
        "Use of the MapMyTimes name, wordmark, logos, tagline ('Journalism of Integrity'), or distinctive trade dress in any manner that suggests or implies endorsement, sponsorship, partnership, or affiliation by MapMyTimes without a signed, written licence agreement.",
      ],
    },
    {
      heading: "4. How to Request Reprint / Republishing / Licensing Permission",
      paragraphs: [
        "To request permission to reprint, translate, syndicate, republish, embed, or otherwise reuse any MapMyTimes Content beyond the permitted uses in Section 2, please submit a written request to our Copyright & Licensing team with ALL of the following information clearly provided:",
      ],
      list: [
        "Full name, organisation name, and contact details of the person/entity requesting permission.",
        "The EXACT piece(s) of Content you wish to reuse: headline, author, date of publication, and full original URL for each story, image, video, or graphic.",
        "A clear description of the intended use (e.g., print publication, website republication, book reprint, translation, corporate newsletter, documentary, internal training material, LLM / dataset, etc.).",
        "Distribution: The platforms / media, audience, estimated reach or circulation, and territory for the reuse.",
        "Commercial status: Whether the use is commercial (paid, monetised, paywalled, sponsored) or non-commercial / not-for-profit.",
        "Attribution plan: How you propose to credit MapMyTimes and the bylined author(s).",
      ],
      postListParagraphs: [
        `Send your request by email to ${BRAND.email} with the subject line "Reprint / Licensing Request: [Short description]". We review complete requests within 15 working days. Permission — if granted — will be confirmed in writing via email and may be subject to a licence fee, attribution requirements, and additional terms.`,
      ],
    },
    {
      heading: "5. Copyright Infringement Takedown Procedure (DMCA-style Notice)",
      paragraphs: [
        "If you are a copyright owner or an authorised agent thereof and believe in good faith that any Content or material hosted on or made available through the MapMyTimes Services infringes upon your copyright, you may notify us by submitting a written Copyright Takedown Notice ('Notice') containing the information listed below. Notices will be processed in accordance with the Copyright Act, 1957 and applicable interim safe-harbour and information-technology laws of India (as amended from time to time).",
        "Please send your Notice to the address provided at the end of this page, and ensure it includes substantially ALL of the following elements (incomplete Notices may not be actioned):",
      ],
      list: [
        "1. Your (the complaining party's) physical or electronic signature, full name, postal address, email address, and telephone number.",
        "2. Identification of the copyrighted work you claim has been infringed — or, if multiple works are covered by a single Notice, a representative list of such works (with titles and, where possible, original URLs or registration numbers).",
        "3. Identification of the material on MapMyTimes that you claim is infringing, with information sufficient to permit us to locate it — specifically, the EXACT URL on mapmytimes.com where the infringing material appears, plus a clear description of the portion of the content at issue (e.g., 'image 2 of 4' or 'paragraphs 3–5').",
        "4. A statement that you have a good-faith belief that the use of the material in the manner complained of is not authorised by the copyright owner, its agent, or the law.",
        "5. A statement, under penalty of perjury, that the information in the Notice is accurate and that you are the copyright owner, or are authorised to act on behalf of the copyright owner, of an exclusive right that is allegedly infringed.",
      ],
      midParagraphs: [
        "Upon receipt of a complete, valid Notice, MapMyTimes will:",
      ],
      secondList: [
        "Promptly review the Notice and assess the alleged infringement.",
        "Remove or disable access to the material(s) identified if, in our reasonable judgment, infringement appears likely.",
        "Where feasible, notify the user, contributor, or uploader who posted the material that the material has been removed or access disabled, and provide them with a copy of the Notice and the opportunity to submit a Counter-Notice.",
        "For repeat infringers: Terminate access and publishing privileges where appropriate.",
      ],
      postListParagraphs: [
        "Counter-Notice procedure: If you believe material you posted was removed or disabled by mistake or misidentification, you may send a written Counter-Notice to the same address including: your details, identification of the removed material and its former location, a statement under penalty of perjury of your good-faith belief of mistake, and consent to jurisdiction. Upon receipt of a valid Counter-Notice, MapMyTimes may restore the material within 10–14 working days unless the original complainant has filed a court action against the user.",
      ],
    },
    {
      heading: "6. Contact for Copyright / Licensing Queries",
      paragraphs: [
        "All copyright takedown notices, counter-notices, reprint requests, and licensing inquiries should be directed in writing to:",
      ],
      list: [
        `Copyright & Licensing Desk — ${BRAND.site}`,
        `Attn: Legal / Copyright Officer, ${BRAND.operator}`,
        `Email: ${BRAND.email}  (subject: "Copyright Notice" / "Counter-Notice" / "Licensing Request")`,
        `Phone: ${BRAND.phone}`,
        `Registered office: ${BRAND.operator}, ${BRAND.city}, ${BRAND.state}, ${BRAND.country}`,
      ],
    },
  ],
};

// -----------------------------------------------------------------------------
// Combined registry (used by footer, sitemap, navigation)
// -----------------------------------------------------------------------------
export const LEGAL_PAGES: readonly LegalPage[] = [
  editorialPolicy,
  factCheckPolicy,
  privacyPolicy,
  termsConditions,
  copyrightNotice,
] as const;

export function getLegalPage(slug: string): LegalPage | undefined {
  return LEGAL_PAGES.find((p) => p.slug === slug);
}
