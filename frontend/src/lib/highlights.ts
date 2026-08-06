import { tokenStorage } from "@/lib/auth/token-storage";
import { SITE } from "@/lib/utils";

export type CreateHighlightRequest = {
  postId: string | number;
  paragraphIndex: number;
  charStart: number;
  charEnd: number;
  excerpt?: string;
};

export type HighlightResponse = {
  id: string;
  userId?: string;
  postId?: string;
  paragraphIndex: number;
  charStart: number;
  charEnd: number;
  excerpt: string;
  createdAt?: string;
};

function getBaseUrl(): string {
  return SITE.apiBase.replace(/\/$/, "");
}

function authHeaders(): Record<string, string> {
  const token = tokenStorage.access;
  return {
    "Content-Type": "application/json",
    ...(token ? { Authorization: `Bearer ${token}` } : {}),
  };
}

export async function createHighlight(
  req: CreateHighlightRequest,
): Promise<HighlightResponse> {
  const res = await fetch(`${getBaseUrl()}/api/v1/highlights/me`, {
    method: "POST",
    headers: authHeaders(),
    body: JSON.stringify(req),
  });
  if (!res.ok) throw new Error(`Failed to create highlight: ${res.status}`);
  const data = await res.json();
  return (data?.data ?? data) as HighlightResponse;
}

export async function listHighlights(
  postId: string | number,
): Promise<HighlightResponse[]> {
  const res = await fetch(
    `${getBaseUrl()}/api/v1/highlights/me/post/${postId}`,
    {
      method: "GET",
      headers: authHeaders(),
    },
  );
  if (!res.ok) {
    if (res.status === 401 || res.status === 403) return [];
    throw new Error(`Failed to list highlights: ${res.status}`);
  }
  const data = await res.json();
  return (data?.data ?? data ?? []) as HighlightResponse[];
}

export async function deleteHighlight(id: string | number): Promise<void> {
  const res = await fetch(
    `${getBaseUrl()}/api/v1/highlights/me/${String(id)}`,
    {
      method: "DELETE",
      headers: authHeaders(),
    },
  );
  if (!res.ok) throw new Error(`Failed to delete highlight: ${res.status}`);
}

export const HIGHLIGHT_BG = "rgba(227, 30, 36, 0.18)";

export type ParagraphBlock = {
  index: number;
  element: HTMLElement;
  text: string;
};

/**
 * Find all block-level elements that should be treated as paragraphs in reader body.
 * Order matters — paragraphIndex is computed from this list.
 */
export function getReaderParagraphBlocks(root: HTMLElement | null): ParagraphBlock[] {
  if (!root) return [];
  const tags = new Set([
    "P",
    "H1",
    "H2",
    "H3",
    "H4",
    "H5",
    "H6",
    "LI",
    "BLOCKQUOTE",
    "FIGCAPTION",
    "PRE",
    "TD",
    "TH",
  ]);
  const out: ParagraphBlock[] = [];
  const walk = (node: Node) => {
    if (node.nodeType === Node.ELEMENT_NODE) {
      const el = node as HTMLElement;
      if (tags.has(el.tagName)) {
        const text = el.textContent ?? "";
        if (text.trim().length > 0) {
          out.push({ index: out.length, element: el, text });
          return;
        }
      }
      el.childNodes.forEach(walk);
    }
  };
  root.childNodes.forEach(walk);
  return out;
}

export type SelectionPoint = {
  paragraphIndex: number;
  charStart: number;
  charEnd: number;
  excerpt: string;
  block: ParagraphBlock;
  rect: DOMRect;
};

/**
 * From a Selection inside reader body, derive the paragraph index and char offsets within the stripped paragraph text.
 */
export function resolveSelection(
  root: HTMLElement | null,
  selection: Selection | null,
): SelectionPoint | null {
  if (!root || !selection || selection.rangeCount === 0) return null;
  const range = selection.getRangeAt(0);
  if (range.collapsed) return null;
  const blocks = getReaderParagraphBlocks(root);
  if (blocks.length === 0) return null;
  const commonAncestor =
    range.commonAncestorContainer.nodeType === Node.ELEMENT_NODE
      ? (range.commonAncestorContainer as HTMLElement)
      : (range.commonAncestorContainer.parentElement as HTMLElement | null);
  if (!commonAncestor) return null;
  let containing: ParagraphBlock | null = null;
  for (const b of blocks) {
    if (b.element.contains(range.startContainer) && b.element.contains(range.endContainer)) {
      containing = b;
      break;
    }
  }
  if (!containing) {
    for (const b of blocks) {
      if (
        b.element.contains(range.startContainer) ||
        b.element.contains(range.endContainer)
      ) {
        containing = b;
        break;
      }
    }
  }
  if (!containing) return null;

  const fullText = containing.text;
  const preRange = document.createRange();
  preRange.selectNodeContents(containing.element);
  preRange.setEnd(range.startContainer, range.startOffset);
  const start = preRange.toString().length;
  const end = start + range.toString().length;
  if (start < 0 || end <= start || start >= fullText.length) return null;

  const s = Math.max(0, start);
  const e = Math.min(fullText.length, end);
  const excerpt = fullText.slice(s, Math.min(s + 200, e));
  const rect = range.getBoundingClientRect();
  return {
    paragraphIndex: containing.index,
    charStart: s,
    charEnd: e,
    excerpt,
    block: containing,
    rect,
  };
}

/**
 * Clear previously painted highlights under this root and re-apply the provided list.
 * Returns a map of highlight id -> elements added (for click handler attachment).
 */
export function applyHighlightSpans(
  root: HTMLElement | null,
  highlights: HighlightResponse[],
  onClick?: (hl: HighlightResponse, ev: MouseEvent) => void,
): Map<string, HTMLSpanElement[]> {
  const added = new Map<string, HTMLSpanElement[]>();
  if (!root) return added;
  // Remove any previous highlight spans we created (data-mmt-highlight)
  root.querySelectorAll<HTMLSpanElement>("span[data-mmt-highlight]").forEach((s) => {
    const parent = s.parentNode;
    if (!parent) return;
    while (s.firstChild) parent.insertBefore(s.firstChild, s);
    parent.removeChild(s);
    parent.normalize();
  });
  const blocks = getReaderParagraphBlocks(root);
  if (blocks.length === 0 || highlights.length === 0) return added;

  for (const hl of highlights) {
    const block = blocks[hl.paragraphIndex];
    if (!block) continue;
    const fullText = block.text;
    const s = Math.max(0, Number(hl.charStart) || 0);
    const e = Math.min(fullText.length, Number(hl.charEnd) || 0);
    if (e <= s) continue;
    try {
      const spans = wrapTextRange(block.element, s, e, {
        "data-mmt-highlight": hl.id,
        style: `background-color: ${HIGHLIGHT_BG}; border-radius: 2px; cursor: pointer;`,
        title: "Click to delete highlight",
      });
      if (spans.length > 0) {
        added.set(hl.id, spans);
        if (onClick) {
          const handler = (ev: Event) => {
            ev.preventDefault();
            onClick(hl, ev as MouseEvent);
          };
          spans.forEach((sp) => sp.addEventListener("click", handler));
        }
      }
    } catch {
      /* ignore */
    }
  }
  return added;
}

/**
 * Wrap the text content range [start, end) within an element using TreeWalker,
 * splitting text nodes as needed. Returns the new wrapper spans created.
 */
function wrapTextRange(
  container: HTMLElement,
  start: number,
  end: number,
  spanAttrs: Record<string, string>,
): HTMLSpanElement[] {
  const spans: HTMLSpanElement[] = [];
  const walker = document.createTreeWalker(container, NodeFilter.SHOW_TEXT, {
    acceptNode: (n) =>
      n.nodeValue && (n.parentElement as HTMLElement | null)?.tagName !== "SCRIPT"
        ? NodeFilter.FILTER_ACCEPT
        : NodeFilter.FILTER_REJECT,
  });
  const textNodes: Text[] = [];
  let node: Node | null;
  while ((node = walker.nextNode())) textNodes.push(node as Text);

  let walked = 0;
  for (const tn of textNodes) {
    const len = tn.nodeValue?.length ?? 0;
    const nodeStart = walked;
    const nodeEnd = walked + len;
    walked = nodeEnd;
    if (nodeEnd <= start) continue;
    if (nodeStart >= end) break;
    const segStart = Math.max(0, start - nodeStart);
    const segEnd = Math.min(len, end - nodeStart);
    if (segEnd <= segStart) continue;
    const fullText = tn.nodeValue ?? "";
    const beforeText = fullText.slice(0, segStart);
    const midText = fullText.slice(segStart, segEnd);
    const afterText = fullText.slice(segEnd);
    const parent = tn.parentNode;
    if (!parent) continue;
    const before = beforeText ? document.createTextNode(beforeText) : null;
    const span = document.createElement("span");
    for (const [k, v] of Object.entries(spanAttrs)) span.setAttribute(k, v);
    span.textContent = midText;
    const after = afterText ? document.createTextNode(afterText) : null;
    parent.insertBefore(document.createComment(""), tn);
    const anchor = tn.previousSibling!;
    parent.removeChild(tn);
    if (before) parent.insertBefore(before, anchor);
    parent.insertBefore(span, anchor);
    if (after) parent.insertBefore(after, anchor);
    parent.removeChild(anchor);
    spans.push(span);
  }
  return spans;
}
