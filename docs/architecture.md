# BellLands — Architektura

## Wersja
`1.26.1.2` — Faza początkowa.

## Struktura pakietów
```text
pl.bell.lands/
├── BellLands.java                 Główna klasa pluginu
├── api/
│   └── BellLandsAPI.java          Singleton dla innych pluginów i wersji PRO
├── model/
│   ├── Claim.java                 Model działki (współrzędne, właściciel, zaufani)
│   └── AdminRegion.java           Model strefy chronionej (WorldGuard replacement)
├── manager/
│   ├── ClaimManager.java          Obsługa tworzenia, kasowania, edycji
│   └── TPAManager.java            Obsługa próśb o teleportację z wygasaniem
├── gui/
│   ├── ClaimGUI.java              Zarządzanie działką dla gracza
│   └── AdminGuardGUI.java         Zarządzanie strefami admina i uprawnieniami
├── command/
│   └── (Logika komend /tpa, /tpaccept, /blands)
└── event/
    └── ProtectionListener.java    Blokowanie niszczenia/stawiania/interakcji na cudzym terenie