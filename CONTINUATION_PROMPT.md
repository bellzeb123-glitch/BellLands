# BellLands — Prompt kontynuacyjny do nowego wątku

Wklej poniższy tekst jako pierwszą wiadomość w nowym wątku Claude Code:

---

Kontynuujemy prace nad **BellLands** — pluginem claim/land protection dla Paper 1.21.4, Java 21, Maven.

## Lokalizacja projektu
- Katalog: `F:\Projekty\BellLands`
- Build: PowerShell only (NIE Bash): `& "C:\Users\user\.m2\wrapper\dists\apache-maven-3.9.9-bin\33b4b2b4\apache-maven-3.9.9\bin\mvn.cmd" -f "F:\Projekty\BellLands\pom.xml" clean package -q`
- Wersja: 1.26.1.3

## Stan projektu — FREE version GOTOWA
Plugin ma pełną funkcjonalność wersji Free:

### Struktura klas (15 plików Java)
- `BellLands.java` — main plugin class
- `model/Land.java` — model działki, 13 flag ochrony (ALL_FLAGS) + 3 flagi gości (GUEST_FLAGS)
- `manager/LandManager.java` — CRUD claimów, outline system (corner1/corner2/players maps)
- `manager/WarpManager.java` — warpy graczy
- `manager/TPAManager.java` — system teleportacji
- `config/LangManager.java` — PL/EN z MERGE pattern (base z JAR + custom z dysku)
- `command/ClaimCommand.java` — /claim + subkomendy (outline, fill, particles, admin, auto, etc.)
- `command/ClaimTabCompleter.java` — tab completion
- `command/TpaCommand.java` — /tpa /tpaccept /tpdeny
- `command/BellLandsCommand.java` — /belllands language
- `gui/ClaimGui.java` — GUI gracza: MainMenu→Flags/GuestFlags/Members/AddTrusted/Warps/Map
- `gui/AdminGui.java` — Admin GUI: Main→PlayerClaims→ClaimDetail, Settings→LockedFlags/Defaults
- `gui/ClaimGuiListener.java` — GuiType enum (14 typów), handlery kliknięć, AdminGuiContext static maps
- `listener/LandListener.java` — ochrona terenu, particle borders, outline particles, ActionBar
- `integration/Pl3xMapHook.java` — Pl3xMap: fills + outer edge polylines, BFS zone grouping

### Zaimplementowane funkcje
- Claiming: claim/unclaim/auto-claim/auto-unclaim/outline+fill
- 13 flag ochrony + 3 flagi gości (guest-doors/guest-use/guest-chest)
- GUI wielostronicowe z nawigacją (mapa chunków 7x5, flagi, członkowie, warpy)
- Admin GUI: zarządzanie graczami, strefami (BFS), claimami, flagami, warpami
- Admin Settings: particles toggle, language toggle, locked flags (per-flag locking), default flags
- OP bypass na działkach graczy
- Particle borders (fioletowe, toggleable) + outline particles (złote)
- Pl3xMap integracja (contiguous zones, bold outer borders only)
- TPA system z timeout/cooldown
- Warpy na działkach
- Wielojęzyczność PL/EN (lang/*.yml)

### Pliki konfiguracji
- `config.yml` — limity, default-flags (16 flag), locked-flags list, particle-borders
- `lang/pl.yml` i `lang/en.yml` — ~280 kluczy językowych
- `plugin.yml` — komendy, permisje, softdepend Pl3xMap

### Ważne szczegóły techniczne
- GUI tracking: UUID→GuiType map (nie po tytule inventory)
- AdminGuiContext: static maps (targetOwners, targetLands, currentLand, zonesMap) — czyszczone selektywnie (nie przy przejściu admin→admin)
- Flag slot mapping: Row 2 slots 10-16 = flags 0-6, Row 3 slots 19-24 = flags 7-12
- Land constructor inicjalizuje ALL_FLAGS + GUEST_FLAGS z config defaults
- isFlagLocked() sprawdza config locked-flags list
- Pl3xMap drawAll() czyści i rysuje od nowa (fills strokeWeight(0) + polyline outer edges)

## Co zostało do zrobienia
1. **Testy na serwerze** — sprawdzenie wszystkich funkcji po ostatnim buildzie
2. **Import z LandClaimPlugin** — migracja danych (odłożone "na końcu")
3. **Pro addon** — rozbudowa (named claims, zaawansowany admin, dodatkowe funkcje)
4. **MiniMessage gradient** na ActionBar branding (odłożone)
5. **Publikacja** na Modrinth/SpigotMC/Hangar (dokumentacja w `PLUGIN_PAGE.md`)

## Zasady pracy
- Komunikacja po polsku
- Minimalne zmiany w repo, nie rekonstruować z bajtkodu
- Dawać kompletne instrukcje
- Nie zastępować gigantów, ułatwiać ich użycie
- BellSuite branding (purple/blue/gold)

---
