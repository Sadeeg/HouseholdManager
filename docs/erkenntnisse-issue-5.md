# Erkenntnisse — Issue #5 „UserRefResponse in den DTO-Ordner verschieben“

Aufgeschrieben am 31.10.2026, nach Abschluss von Issue #5 (Branch
`feature/5-dto-ordner`). Zweck: die Regel „alle DTOs liegen in
`adapters/inbound/rest/dto/`" an zwei Stellen durchsetzen, die sie gebrochen
hatten.

---

## 1. Behobenes Problem

### 1.1 Zwei Controller trugen ihre DTOs als verschachteltes Record

- **Symptom:** `UserSearchController.UserRefResponse` und
  `AuthController.CurrentUserResponse` waren als innere Records definiert.
- **Ursache:** Die DTOs entstanden mit dem Controller und wurden nicht in den
  DTO-Ordner verschoben — die Projektregel lautet aber: alle ankommenden und
  ausgehenden DTOs liegen in `adapters/inbound/rest/dto/`.
- **Nachbesserung:** Beide Records liegen jetzt als eigenständige Records in
  `adapters/inbound/rest/dto/` (`UserRefResponse`, `CurrentUserResponse`) und
  werden per `from(...)` aus dem Port bzw. dem Modell befüllt — wie die
  übrigen DTOs (`MemberResponse`, `TaskResponse`, …).
- **Lehre:** Ein verschachteltes Record im Controller ist bequem, aber es
  verstreut den Vertrag. Der DTO-Ordner ist die einzige Stelle, an der ein
  Client den Vertrag findet. Regel im Skill `backend-ports-adapters`.

### 1.2 Der Vertrag blieb unverändert

- **Beleg:** Die JSON-Schlüssel (`id`, `displayName`, `email`) sind durch die
  bestehenden Controller-Tests (`UserSearchControllerTest`,
  `AuthControllerTest`) gepinnt — die Tests brachen nicht. Die DTOs wurden
  verschoben, nicht verändert.

## 2. Überführte Regeln

| Lehre | Regel |
|---|---|
| DTOs liegen im DTO-Ordner | `backend-ports-adapters` — „DTOs als Records in `adapters/inbound/rest/dto/`"; der Controller enthält keine DTO-Definition |
