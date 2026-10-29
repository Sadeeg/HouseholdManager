import { DatePipe } from '@angular/common';
import { ChangeDetectionStrategy, Component, input } from '@angular/core';

import { Household } from '../model/household.model';

/**
 * Dumb Component (UI-Schicht) des Features „Haushalte".
 *
 * Kennt das Feature, spricht aber nicht mit dem Backend: Daten kommen
 * über Inputs, es gibt keine Outputs und keinen Datenzugriff.
 */
@Component({
  selector: 'app-household-item',
  imports: [DatePipe],
  templateUrl: './household-item.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HouseholdItem {
  readonly household = input.required<Household>();
}
