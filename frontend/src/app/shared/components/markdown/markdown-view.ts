import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

import { MarkdownBlock, parseMarkdown } from './markdown-parser';

/**
 * Dumb Component: rendert die Markdown-Teilmenge als sichere Angular-
 * Struktur — kein rohes HTML, keine Skripte (AC3).
 */
@Component({
  selector: 'app-markdown-view',
  templateUrl: './markdown-view.html',
  styleUrl: './markdown-view.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MarkdownView {
  readonly content = input<string>('');

  protected readonly blocks = computed<MarkdownBlock[]>(() => parseMarkdown(this.content()));
}
