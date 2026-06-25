# BellLands — Prompt kontynuacyjny do nowego wątku

Wklej poniższy tekst jako pierwszą wiadomość w nowym wątku:

---

Kontynuujemy prace nad **BellLands** (+ **BellLandsPro**) — plugin claim/land protection dla Paper 1.21.4, Java 21, Maven.

## Lokalizacja projektu
- Free: `F:\Projekty\BellLands`
- Pro: `F:\Projekty\BellLandsPro`
- Build Free: `& "C:\Users\user\.m2\wrapper\dists\apache-maven-3.9.9-bin\33b4b2b4\apache-maven-3.9.9\bin\mvn.cmd" -f "F:\Projekty\BellLands\pom.xml" clean package -q`
- Build Pro (wymaga zbudowanego Free): `& "C:\Users\user\.m2\wrapper\dists\apache-maven-3.9.9-bin\33b4b2b4\apache-maven-3.9.9\bin\mvn.cmd" -f "F:\Projekty\BellLandsPro\pom.xml" clean package -q`
- Wersja: 1.26.1.2

## Stan projektu — GOTOWE (kod)
### BellLands Free
- SQLite (`data.db`) + auto-import `lands.yml` / `warps.yml` (MigrateLCP)
- 11 flag ochrony + 5 flag gości, GUI, admin panel, TPA, warpy, Pl3xMap
- Domyślny język: **EN** (`/belllands language`, `/belllands reload`)
- Admin default flags: propagacja na **wszystkie zwykłe claimy** + nowe (goście też)

### BellLandsPro
- Named claims, regiony (27 flag), grupy limitów, przejęcie Pl3xMap
- Hooki w Free: `ClaimGuiListener` (protection gate, propagatory, `GuiAddonHook`)
- Propagacja flag admina **pomija chunki nazwanych działek**
- Resource pack GUI wyłączony

### Integracja Free ↔ Pro
- Pro **nie** używa `BellLandsAPI` — hooki w `gui/`, `listener/`, `integration/`
- `ProGuiHooks` wstrzykuje przyciski Pro przez `GuiAddonHook` (bez dopasowania tytułu inventory)

## Co zostało do zrobienia
1. **Testy na serwerze** — Free solo, Free+Pro, migracja LCP, limity, regiony
2. **Publikacja** Free (Modrinth/Hangar) — `PLUGIN_PAGE.md` gotowy
3. **Dystrybucja Pro** (BBB) + opcjonalnie license validator (poziom 1)
4. MiniMessage ActionBar — odłożone

## Zasady pracy
- Komunikacja po polsku
- Minimalne zmiany w repo
- Domyślny język ekosystemu: angielski
- BellSuite branding (purple/blue/gold)

---
