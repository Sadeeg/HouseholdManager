import { ChangeDetectionStrategy, Component, inject } from '@angular/core';

import { PageHeader } from '../../shared/components/page-header/page-header';
import { HouseholdsData } from './data/households-data';
import { HouseholdItem } from './ui/household-item';

/**
 * Smart Component (Feature-Schicht) des Features „Haushalte".
 *
 * Holt die Daten über den Data-Service und verteilt sie an die dumb
 * Components der UI-Schicht.
 */
@Component({
  selector: 'app-households-page',
  imports: [HouseholdItem, PageHeader],
  templateUrl: './households-page.html',
  styleUrl: './households-page.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HouseholdsPage {
  private readonly householdsData = inject(HouseholdsData);

  protected readonly households = this.householdsData.households;
}
