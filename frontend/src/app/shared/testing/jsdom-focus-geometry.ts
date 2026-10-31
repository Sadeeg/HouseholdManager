/**
 * jsdom kennt keine Geometrie: jedes Element hat die Größe 0 und gilt
 * deshalb als „unsichtbar". Die Fokusfalle des CDK fokussiert unsichtbare
 * Elemente nicht — im Test bleibt der Fokus also immer dort, wo er war.
 *
 * Der Test gibt den Elementen deshalb eine Größe. Das ist eine Einrichtung
 * der Testumgebung, kein Produktivverhalten: im Browser haben die Elemente
 * ihre Größe.
 */
export function giveElementsGeometry(): void {
  Object.defineProperty(HTMLElement.prototype, 'getClientRects', {
    configurable: true,
    value: () => [{ width: 100, height: 20 }] as unknown as DOMRectList,
  });
}
