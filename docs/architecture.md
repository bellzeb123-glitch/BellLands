# BellLands — Architektura

## Wersja

`1.26.1.3` — wersja Free kompletna. Pro w osobnym repo `BellLandsPro`.

## Struktura pakietów

```text
pl.bell.lands/
├── BellLands.java                 Główna klasa, reload hooks dla addonów
├── command/
│   ├── ClaimCommand.java          /claim + subkomendy
│   ├── ClaimTabCompleter.java
│   ├── TpaCommand.java            /tpa /tpaccept /tpdeny
│   └── BellLandsCommand.java      /belllands language
├── config/
│   └── LangManager.java           PL/EN — MERGE pattern
├── gui/
│   ├── ClaimGui.java              GUI gracza (mapa, flagi, członkowie, warpy)
│   ├── AdminGui.java              Panel admina
│   └── ClaimGuiListener.java      Handlery kliknięć + hooki dla Pro
├── integration/
│   └── Pl3xMapHook.java           Warstwa mapy + delegate dla Pro
├── listener/
│   └── LandListener.java          Ochrona terenu, particles, ActionBar + hook trust
├── manager/
│   ├── LandManager.java           CRUD claimów, outline/fill
│   ├── WarpManager.java           Warpy na działkach
│   └── TPAManager.java            Teleportacja z timeout/cooldown
├── model/
│   ├── Land.java                  Model chunka (flagi, trusted, współrzędne)
│   └── ClaimAction.java           Enum akcji ochrony (dla resolvera trust)
└── storage/
    └── Database.java              SQLite (WAL, single-thread writer)
```

## Kluczowe decyzje

### SQLite zamiast YAML

Claimy i warpy w `data.db`. Zapis asynchroniczny przez jeden wątek — nie blokuje main thread.
Paper `libraries` w `plugin.yml` dostarcza sterownik SQLite na serwerze.

### GUI tracking po UUID, nie po tytule

`ClaimGuiListener.GuiType` + mapa `openGuis` — stabilne przy zmianie języka i addonach.

### Integracja Pro przez hooki (nie osobne API)

BellLandsPro wymaga głębokiej integracji z GUI i mapą. Zamiast `BellLandsAPI` Free
eksponuje statyczne hooki:

| Hook | Klasa | Cel |
|------|-------|-----|
| `setProtectionGate` | `ClaimGuiListener` | Blokada per-claim flag (globalne flagi w Pro) |
| `setDefaultFlagPropagator` | `ClaimGuiListener` | Propagacja globalnych defaultów (Pro pomija named) |
| `addGuiAddonHook` | `ClaimGuiListener` | Wstrzykiwanie przycisków Pro bez title matching |
| `setGuestFlagPropagator` | `ClaimGuiListener` | Flood-fill flag gości |
| `setTrustedResolver` | `LandListener` | Per-trust na named claims |
| `setDelegate` | `Pl3xMapHook` | Przejęcie renderowania mapy przez Pro |
| `addReloadHook` | `BellLands` | Reload języka Pro przy zmianie języka core |

To świadome odstępstwo od ogólnej reguły „Pro importuje tylko api/” — patrz
`Bell-Ecosystem/shared/conventions.md` (sekcja BellLands).

### Pl3xMap

Free rysuje claimy (fill + outer edge). Gdy Pro jest załadowany, ustawia `RenderDelegate`
i przejmuje całe rysowanie (named claims, regiony, kolory).

## Zależności runtime

| Plugin | Typ | Opis |
|--------|-----|------|
| Paper/Purpur 1.21.x | wymagany | `api-version: '1.21'` |
| Pl3xMap | softdepend | Wizualizacja na mapie webowej |
| LuckPerms | opcjonalny | Override limitów (`belllands.claims.<N>`) |
| BellLandsPro | osobny JAR | `depend: [BellLands]` w plugin.yml Pro |

## Pliki konfiguracyjne

- `config.yml` — limity, default-flags, locked-flags, particle-borders
- `lang/pl.yml`, `lang/en.yml` — ~280 kluczy, MERGE z JAR

## Powiązana dokumentacja

- [`../CONTINUATION_PROMPT.md`](../CONTINUATION_PROMPT.md) — stan projektu i plan
- [`../../Bell-Ecosystem/belllands/promo-free.md`](../../Bell-Ecosystem/belllands/promo-free.md) — opis marketingowy Free
- [`../../Bell-Ecosystem/belllands/promo-pro.md`](../../Bell-Ecosystem/belllands/promo-pro.md) — opis Pro
