# BellLands — Architecture

## Overview

BellLands is a free, chunk-based land claiming plugin for Minecraft (API 1.21+). It lets players claim chunks, manage trusted members, set warps, and use TPA teleportation. Data is persisted in SQLite and the plugin ships optional hooks for Pl3xMap and LuckPerms. An extension point exists for the upcoming **BellLandsPro** add-on.

**Main class:** `pl.bell.lands.BellLands` (extends `JavaPlugin`)

---

## Core Systems

### Land Claiming (`LandManager`)

- Chunk-granularity claims — each claim maps to a single Minecraft chunk.
- `LandManager.init()` loads all claims from the database at startup; `saveAll()` persists them on shutdown.
- Players interact through `/claim` with subcommands: `unclaim`, `trust`, `untrust`, `flag`, `flags`, `info`, `menu`, `help`.
- A `ClaimGuiListener` provides an inventory-based GUI for managing claims (`/claim menu`).
- Visual feedback via `LandListener` — action-bar messages, particle borders, and outline particles run as repeating Bukkit tasks.

### Protection (`LandListener`)

- Registered as a Bukkit event listener, `LandListener` intercepts world events (block break/place, entity interact, etc.) and denies them in claimed chunks where the actor is not trusted.
- Flags per-claim let owners toggle specific protections (PvP, mob-spawning, explosions, etc.).

### Warp System (`WarpManager`)

- Each claim can have named warps set with `/setwarp <name>`, listed with `/warps`, teleported to with `/warp <name>`, and removed with `/delwarp <name>`.
- `WarpManager.init()` loads warps; `flushAll()` batch-writes dirty entries on shutdown.
- All four warp commands are handled by a single `WarpCommand` executor with tab completion.

### TPA System (`TPAManager`)

- Request-based teleportation: `/tpa <player>`, `/tpaccept`, `/tpdeny`.
- Managed by `TPAManager`, which tracks pending requests and expiry.
- Config-reloadable via `tpaManager.reloadConfig()`.

---

## Database (SQLite)

The plugin bundles `sqlite-jdbc 3.46.0.0` (declared in `plugin.yml` `libraries`). All claim and warp data is persisted to a local SQLite file managed through `LandManager` and `WarpManager`.

---

## Configuration & Localisation

- `config.yml` — general settings, loaded with `saveDefaultConfig()`.
- `LangManager` — multi-language message files (Polish `pl` and English `en`), switchable at runtime via `/belllands language <en|pl>`.
- Hot-reload with `/belllands reload` — triggers `LangManager.reload()`, `TPAManager.reloadConfig()`, and any registered reload hooks.

---

## Optional Integrations

| Integration | Hook class | Purpose |
|---|---|---|
| **Pl3xMap** | `Pl3xMapHook` | Renders claimed chunks on the web map. |
| **LuckPerms** | `LuckPermsWarpHook` | Integrates permission-based warp access. |

Both are declared as `softdepend` — the plugin functions fully without them.

---

## Commands

| Command | Description | Permission |
|---|---|---|
| `/claim [sub]` | Claim/manage land (aliases: `/dzialka`, `/land`) | `belllands.claim` |
| `/warp <name>` | Teleport to a claim warp | `belllands.claim` |
| `/setwarp <name>` | Set a warp on your claim | `belllands.claim` |
| `/delwarp <name>` | Delete a claim warp | `belllands.claim` |
| `/warps` | List claim warps | `belllands.claim` |
| `/tpa <player>` | Send a teleport request | `belllands.tpa` |
| `/tpaccept` | Accept a pending TPA | `belllands.tpa` |
| `/tpdeny` | Deny a pending TPA | `belllands.tpa` |
| `/belllands [reload\|language]` | Admin / plugin management | *(none / op)* |

### Permissions

- `belllands.claim` — claim land & warps (default: all players).
- `belllands.tpa` — use TPA commands (default: all players).
- `belllands.admin` — admin operations such as removing other players' claims (default: op).
- `belllands.*` — grants all of the above.

---

## API for BellLandsPro

BellLands exposes two static extension points so the **BellLandsPro** add-on (or any third-party plugin) can hook in without modifying the core:

- **`setClaimLimitResolver(Function<Player, Integer>)`** — lets an add-on override the maximum number of claims per player (e.g. VIP tiers, permission groups).
- **`addReloadHook(Runnable)`** — registers a callback that fires whenever `/belllands reload` is executed, so the add-on can reload its own config in sync.

---

## Package Layout

```
pl.bell.lands
├── BellLands              # Plugin entry point
├── command/               # Command executors & tab completers
├── config/                # LangManager, config helpers
├── gui/                   # Inventory GUI (ClaimGuiListener)
├── integration/           # Pl3xMapHook, LuckPermsWarpHook
├── listener/              # LandListener (protection + visuals)
└── manager/               # LandManager, WarpManager, TPAManager
```
