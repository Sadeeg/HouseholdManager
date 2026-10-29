import { TestBed } from '@angular/core/testing';

import { MarkdownView } from './markdown-view';
import { parseInline, parseMarkdown } from './markdown-parser';

function blocks(source: string) {
  return parseMarkdown(source);
}

describe('markdown-parser', () => {
  it('parses headings by level', () => {
    const result = blocks('# Titel\n## Abschnitt\n### Klein');
    expect(result.map((block) => [block.type, (block as { level?: number }).level])).toEqual([
      ['heading', 1],
      ['heading', 2],
      ['heading', 3],
    ]);
    expect(result[0].type === 'heading' && result[0].segments[0].text).toBe('Titel');
  });

  it('parses unordered and ordered lists as separate lists', () => {
    const result = blocks('- a\n- b\n\n1. eins\n2. zwei');
    const lists = result.filter((block) => block.type === 'list');
    expect(lists.length).toBe(2);
    expect(lists[0].type === 'list' && lists[0].ordered).toBe(false);
    expect(lists[0].type === 'list' && lists[0].items.length).toBe(2);
    expect(lists[1].type === 'list' && lists[1].ordered).toBe(true);
  });

  it('parses fenced code blocks verbatim', () => {
    const result = blocks('```\ntext # nicht überschrift\nplain - keine Liste\n```');
    const code = result.find((block) => block.type === 'code');
    expect(code?.type === 'code' && code.lines).toEqual([
      'text # nicht überschrift',
      'plain - keine Liste',
    ]);
  });

  it('parses bold, italic and inline code inside paragraphs', () => {
    const segments = parseInline('vor **fett** *kursiv* `code` nach');
    expect(segments.map((segment) => [segment.text, segment.format ?? null])).toEqual([
      ['vor ', null],
      ['fett', 'bold'],
      [' ' , null],
      ['kursiv', 'italic'],
      [' ' , null],
      ['code', 'code'],
      [' nach', null],
    ]);
  });

  it('keeps unknown syntax as plain text', () => {
    const result = blocks('<b>h4</b> <img onerror="alert(1)">');
    expect(result.every((block) => block.type === 'paragraph')).toBe(true);
    const text = JSON.stringify(result);
    expect(text).toContain('<img onerror');
  });
});

describe('MarkdownView', () => {
  it('renders headings, lists and code as safe elements', async () => {
    const fixture = TestBed.createComponent(MarkdownView);
    fixture.componentRef.setInput('content', '# Überschrift\n- Punkt **fett**\n```\ncode block\n```');
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h3')?.textContent).toContain('Überschrift');
    expect(compiled.querySelector('li strong')?.textContent).toBe('fett');
    expect(compiled.querySelector('pre code')?.textContent).toContain('code block');
  });

  it('never renders raw HTML — script and img-onerror stay text (AC26)', async () => {
    const fixture = TestBed.createComponent(MarkdownView);
    fixture.componentRef.setInput('content', '<script>alert(1)</script>\n<img onerror="alert(2)">');
    await fixture.whenStable();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('script')).toBeNull();
    expect(compiled.querySelector('img')).toBeNull();
    expect(compiled.textContent).toContain('alert(1)');
  });
});
