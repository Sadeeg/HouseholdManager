/**
 * Vitest-Setup für alle Testdateien (registriert über `test.options.setupFiles`
 * in `angular.json`).
 *
 * jsdom kennt keine Geometrie: jedes Element hat die Größe 0 und gilt damit als
 * „unsichtbar". Die Fokusfalle des CDK fokussiert unsichtbare Elemente nicht —
 * im Test bleibt der Fokus dort, wo er war, und der CDK warnt bei jedem
 * Element, das er nicht erreichen kann.
 *
 * Das Setup gibt den Elementen eine Größe, damit die Testumgebung dieselbe
 * Aussage trifft wie der Browser. Es ist eine Einrichtung der Testumgebung,
 * kein Produktivverhalten: im Browser haben die Elemente ihre Größe. Früher
 * stand das als Helfer, den jede Fokus-Datei selbst aufrufen musste — das
 * machte dieselbe Komponente in zwei Umgebungen testbar (mit und ohne
 * Sichtbarkeit). Jetzt gilt die Umgebung für alle Testdateien gleich.
 */
Object.defineProperty(HTMLElement.prototype, 'getClientRects', {
  configurable: true,
  value: () => [{ width: 100, height: 20 }] as unknown as DOMRectList,
});
