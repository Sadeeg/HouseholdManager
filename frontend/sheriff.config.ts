import { SheriffConfig } from '@softarc/sheriff-core';

/**
 * Modulgrenzen nach der Architektur-Matrix von Manfred Steyer.
 *
 * - `domain:<feature>`  Zugehörigkeit zu einem Feature (bounded context)
 * - `type:feature`      smart Component (spricht mit dem Backend)
 * - `type:ui`           dumb Component (nur Darstellung)
 * - `type:data`         Daten-Schicht (Model + HTTP-Zugriff)
 * - `type:core`         app-weit (Konfiguration, HTTP-Basis, Auth)
 * - `type:shared`       von mehreren Features benutzt, fachneutral
 *
 * Regeln (siehe `.opencode/skills/frontend-feature-architecture`):
 * 1. Ein Feature benutzt nur eigene Module — Ausnahme: shared und core.
 * 2. Ein Modul benutzt nur Module tieferer Schichten:
 *    feature → ui, data · ui → (nichts) · data → (nichts).
 */
export const config: SheriffConfig = {
  enableBarrelLess: true,
  autoTagging: false,
  entryFile: 'src/main.ts',

  modules: {
    'src/app/core': ['type:core'],
    'src/app/shared': ['type:shared'],
    'src/app/features/households': ['domain:households', 'type:feature'],
    'src/app/features/households/model': ['domain:households', 'type:model'],
    'src/app/features/households/data': ['domain:households', 'type:data'],
    'src/app/features/households/ui': ['domain:households', 'type:ui'],
    'src/app/features/tasks': ['domain:tasks', 'type:feature'],
    'src/app/features/tasks/model': ['domain:tasks', 'type:model'],
    'src/app/features/tasks/data': ['domain:tasks', 'type:data'],
    'src/app/features/tasks/ui': ['domain:tasks', 'type:ui'],
  },

  depRules: {
    // Die App-Shell darf alles laden (Lazy Routes).
    root: () => true,

    // Regel 1: keine Feature-Grenze überschreiten — Ausnahme shared/core.
    'domain:*': ({ toTags, fromTags }) =>
      toTags.includes('type:shared') ||
      toTags.includes('type:core') ||
      toTags.some((tag) => tag.startsWith('domain:') && fromTags.includes(tag)),

    // Regel 2: nur tiefere Schichten.
    'type:feature': ['type:ui', 'type:data', 'type:model', 'type:shared', 'type:core'],
    'type:data': ['type:model', 'type:shared', 'type:core'],
    'type:ui': ['type:model', 'type:shared', 'type:core'],
    'type:shared': ['type:shared', 'type:core'],
    'type:core': ['type:core', 'type:shared'],
  },
};
