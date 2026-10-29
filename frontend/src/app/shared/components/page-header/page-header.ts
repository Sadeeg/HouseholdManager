import { ChangeDetectionStrategy, Component, input } from '@angular/core';

/**
 * Gemeinsam genutzter Kopfbereich einer Seite.
 *
 * Gehört nach `shared/`, weil er von mehreren Features benutzt wird und
 * keinerlei Fachwissen über sie besitzt.
 */
@Component({
  selector: 'app-page-header',
  templateUrl: './page-header.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PageHeader {
  readonly title = input.required<string>();
  readonly subtitle = input<string>();
}
