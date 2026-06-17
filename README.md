# BellLands

Darmowy plugin claim/land protection dla **Paper/Purpur 1.21.x** (Java 21).

- Claimowanie chunków, auto-claim, outline/fill
- 16 flag ochrony + 3 flagi gości
- GUI wielostronicowe, panel admina
- Warpy, TPA, particle borders
- Integracja Pl3xMap (opcjonalnie)
- PL/EN z systemem MERGE

> Wersja: **1.26.1.3** | Autor: Bellzeb

## Wymagania

- Paper lub Purpur 1.21.x
- Java 21+
- (Opcjonalnie) Pl3xMap — mapa webowa
- (Opcjonalnie) LuckPerms — override limitów chunków

## Build

```powershell
mvn -f pom.xml clean package
```

Wynik: `target/BellLands-1.26.1.3.jar`

## Instalacja

1. Wrzuć JAR do `plugins/`
2. Restart serwera (tworzy `config.yml`, `lang/`, `data.db`)
3. (Opcjonalnie) Zainstaluj Pl3xMap

## Rozbudowa — BellLands Pro

Płatny addon: nazwane działki, regiony admina, grupy limitów, przejęcie mapy.
Repo: `BellLandsPro` (wymaga tego pluginu).

## Dokumentacja

- [`docs/architecture.md`](docs/architecture.md) — struktura pakietów i hooki Pro
- [`CONTINUATION_PROMPT.md`](CONTINUATION_PROMPT.md) — stan projektu
- [`PLUGIN_PAGE.md`](PLUGIN_PAGE.md) — opis pod publikację
- [`../Bell-Ecosystem/belllands/`](../Bell-Ecosystem/belllands/) — instrukcje i promo

## Migracja z LandClaimPlugin

Narzędzie: [`../MigrateLCP/`](../MigrateLCP/) — generuje `lands.yml` / `warps.yml`.
