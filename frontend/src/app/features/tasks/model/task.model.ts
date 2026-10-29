/**
 * Datenmodell des Features „Aufgaben" (clientseitige Sicht).
 *
 * Spaltenreihenfolge ist Teil des Modells: TODO → DOING → DONE, Position
 * innerhalb der Spalte lückenlos bei 0 (Backend-Vertrag, AC24/AC25).
 */
export type TaskStatus = 'TODO' | 'DOING' | 'DONE';

export const TASK_COLUMNS: readonly TaskStatus[] = ['TODO', 'DOING', 'DONE'];

export interface Task {
  readonly id: number;
  readonly title: string;
  readonly content: string | null;
  readonly status: TaskStatus;
  readonly position: number;
  readonly assigneeUserId: string | null;
  readonly assigneeDisplayName: string | null;
  /** False, wenn die zugewiesene Person nicht mehr im Haushalt ist (AC14). */
  readonly assigneeInHousehold: boolean;
  /** Frist als ISO-Datum (YYYY-MM-DD) oder null. */
  readonly dueDate: string | null;
  /** Labels in gespeicherter Schreibweise (AC17/AC20). */
  readonly labels: readonly string[];
}

/** Formulardaten aus dem Anlegen-/Bearbeiten-Dialog. */
export interface TaskFormData {
  readonly title: string;
  readonly content: string | null;
  readonly assigneeUserId: string | null;
  /** ISO-Datum (YYYY-MM-DD) oder null. */
  readonly dueDate: string | null;
  readonly labels: readonly string[];
}

/** Normalisierung wie im Backend: trim + lowercase — Basis für Farbe und Duplikatschutz. */
export function normalizeLabel(label: string): string {
  return label.trim().toLowerCase();
}

/**
 * Deterministische Label-Farbe: Index = Hash(normalisierter Name) mod 8,
 * derselbe Name erhält boardweit dieselbe Farbe (AC17/AC20). Genutzt als
 * `var(--hm-label-<index + 1>)`.
 */
export function labelColorIndex(label: string): number {
  const normalized = normalizeLabel(label);
  if (normalized.length === 0) {
    return 8; // Fallback --hm-label-8 für namenlose Labels
  }
  let hash = 0;
  for (const char of normalized) {
    hash = (hash * 31 + char.charCodeAt(0)) | 0;
  }
  return (Math.abs(hash) % 8) + 1;
}

/**
 * Überfällig, wenn die Frist vor dem heutigen Tag liegt und die Aufgabe
 * nicht in DONE steht (AC22). Datumsvergleich auf ISO-Strings.
 */
export function isOverdue(task: Pick<Task, 'dueDate' | 'status'>, today: Date = new Date()): boolean {
  if (task.dueDate === null || task.status === 'DONE') {
    return false;
  }
  return task.dueDate < isoDate(today);
}

/** Datum als ISO-String (YYYY-MM-DD), lokal — identische Basis wie der Backend-Vergleich. */
export function isoDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
}

/**
 * Frist-Eingabe TT.MM.JJJJ → ISO (YYYY-MM-DD). Null bei ungültigem Datum
 * oder leerer Eingabe.
 */
export function parseDueDate(input: string): string | null {
  const trimmed = input.trim();
  if (trimmed === '') {
    return null;
  }
  const match = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(trimmed);
  if (match === null) {
    return null;
  }
  const [, day, month, year] = match;
  const date = new Date(Number(year), Number(month) - 1, Number(day));
  if (
    date.getFullYear() !== Number(year)
    || date.getMonth() !== Number(month) - 1
    || date.getDate() !== Number(day)
  ) {
    return null;
  }
  return `${year}-${month}-${day}`;
}

/** ISO (YYYY-MM-DD) → Anzeige TT.MM.JJJJ. */
export function formatDueDate(iso: string): string {
  const [year, month, day] = iso.split('-');
  return `${day}.${month}.${year}`;
}
