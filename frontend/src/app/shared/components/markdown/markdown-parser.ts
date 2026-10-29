/**
 * Minimaler, sicherer Markdown-Parser (Whitelist).
 *
 * Unterstützt genau die im Issue festgelegte Teilmenge: Überschriften (#, ##,
 * ###), Aufzählungen (-, *, 1.), Code-Blöcke (```), inline **fett**,
 * *kursiv* und `code`. Es entsteht keine Roh-HTML-Ausgabe — der Renderer
 * baut aus diesen Blöcken Angular-Elemente, Skripte und unbekannte Syntax
 * bleiben Text.
 */

export type InlineFormat = 'bold' | 'italic' | 'code';

export interface InlineSegment {
  readonly text: string;
  readonly format?: InlineFormat;
}

export type MarkdownBlock =
  | { readonly type: 'heading'; readonly level: number; readonly segments: InlineSegment[] }
  | { readonly type: 'list'; readonly ordered: boolean; readonly items: InlineSegment[][] }
  | { readonly type: 'code'; readonly lines: string[] }
  | { readonly type: 'paragraph'; readonly segments: InlineSegment[] };

const HEADING = /^(#{1,3})\s+(.*)$/;
const UNORDERED_ITEM = /^\s*[-*]\s+(.*)$/;
const ORDERED_ITEM = /^\s*\d+[.)]\s+(.*)$/;
const FENCE = /^\s*```/;
const INLINE = /\*\*([^*]+)\*\*|\*([^*]+)\*|`([^`]+)`/g;

export function parseMarkdown(source: string): MarkdownBlock[] {
  const blocks: MarkdownBlock[] = [];
  const lines = source.replace(/\r\n?/g, '\n').split('\n');

  let fence: string[] | null = null;
  let list: { ordered: boolean; items: InlineSegment[][] } | null = null;
  let paragraph: string[] = [];

  const flushParagraph = (): void => {
    if (paragraph.length > 0) {
      blocks.push({ type: 'paragraph', segments: parseInline(paragraph.join(' ')) });
      paragraph = [];
    }
  };
  const flushList = (): void => {
    if (list !== null) {
      blocks.push({ type: 'list', ordered: list.ordered, items: list.items });
      list = null;
    }
  };

  for (const line of lines) {
    if (FENCE.test(line)) {
      if (fence === null) {
        flushParagraph();
        flushList();
        fence = [];
      } else {
        blocks.push({ type: 'code', lines: fence });
        fence = null;
      }
      continue;
    }
    if (fence !== null) {
      fence.push(line);
      continue;
    }

    const heading = HEADING.exec(line);
    if (heading !== null) {
      flushParagraph();
      flushList();
      blocks.push({
        type: 'heading',
        level: heading[1].length,
        segments: parseInline(heading[2]),
      });
      continue;
    }

    const unordered = UNORDERED_ITEM.exec(line);
    const ordered = unordered === null ? ORDERED_ITEM.exec(line) : null;
    if (unordered !== null || ordered !== null) {
      flushParagraph();
      const isOrdered = ordered !== null;
      if (list === null || list.ordered !== isOrdered) {
        flushList();
        list = { ordered: isOrdered, items: [] };
      }
      list.items.push(parseInline((unordered ?? ordered)![1]));
      continue;
    }

    if (line.trim() === '') {
      flushParagraph();
      flushList();
      continue;
    }
    paragraph.push(line.trim());
  }

  if (fence !== null) {
    // unverschlossener Code-Block: Rest als Code, nicht als Text verlieren
    blocks.push({ type: 'code', lines: fence });
  }
  flushParagraph();
  flushList();

  return blocks;
}

export function parseInline(text: string): InlineSegment[] {
  const segments: InlineSegment[] = [];
  const inline = new RegExp(INLINE.source, 'g');
  let last = 0;
  let match: RegExpExecArray | null;

  while ((match = inline.exec(text)) !== null) {
    if (match.index > last) {
      segments.push({ text: text.slice(last, match.index) });
    }
    const [, bold, italic, code] = match;
    if (bold !== undefined) {
      segments.push({ text: bold, format: 'bold' });
    } else if (italic !== undefined) {
      segments.push({ text: italic, format: 'italic' });
    } else {
      segments.push({ text: code, format: 'code' });
    }
    last = match.index + match[0].length;
  }
  if (last < text.length) {
    segments.push({ text: text.slice(last) });
  }
  return segments;
}
