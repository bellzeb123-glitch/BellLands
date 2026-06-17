# BellLands — Land Claiming & Teleportation Plugin

**Version:** 1.26.1.3  
**API:** Paper 1.21.4+  
**Java:** 21+  
**Author:** Bellzeb  

---

## Description

BellLands is a lightweight yet feature-complete land claiming plugin for Paper servers. Protect your builds with chunk-based claims, manage permissions with an intuitive GUI, and teleport between your lands with built-in warps and TPA system.

Part of the **BellSuite** plugin ecosystem by Bellzeb.

---

## Features

### Land Claiming
- **Chunk-based claiming** — claim the chunk you're standing on with `/claim`
- **Auto-claim / Auto-unclaim** — walk around to mass claim or unclaim chunks
- **Outline & Fill** — select a rectangular area and fill it with claims in one command
- **Visual particle borders** — see purple particle outlines on your claim boundaries (toggleable per-player)
- **Claim limit** — configurable max claims per player

### GUI Management
- **Full GUI system** — manage everything from `/claim menu`
  - **Main Menu** — central hub with navigation to all features
  - **Protection Flags** — toggle 13 protection flags with click
  - **Guest Permissions** — control what non-trusted players can do (doors, interaction, chests)
  - **Members** — add/remove trusted players via GUI with player head selection
  - **Chunk Map** — visual 7×5 grid showing surrounding chunks, click to claim/unclaim
  - **Warps** — click to teleport, shift+click to delete

### Protection Flags (13)
| Flag | Description |
|------|-------------|
| PVP | Player vs Player combat |
| Explosions | TNT/creeper explosions |
| Explosion Damage | Block damage from explosions |
| Fire Spread | Fire spreading between blocks |
| Mob Spawning | Hostile mob spawning |
| Mob Damage | Damage from mobs |
| Lava Flow | Lava flow |
| Lava/Fire Damage | Player damage from lava/fire |
| Water Flow | Water flow |
| Pistons | Piston movement |
| Leaf Decay | Leaf block decay |
| Use (Interaction) | Levers, buttons, pressure plates |
| Doors | Door interaction |

### Guest Permissions (3)
Control what **non-trusted** visitors can do on your land:
- **Guest Doors** — allow/block door usage
- **Guest Interaction** — allow/block levers, buttons, etc.
- **Guest Chests** — allow/block chest access

### Admin System
- **Admin Panel** (`/claim admin`) — full server management GUI
  - View all players with claims, manage individual claims or entire zones
  - Change any player's flags, guest permissions, and trusted members
  - Delete individual claims, zones, or all claims of a player
  - **Zone management** — adjacent chunks grouped into zones for bulk operations
  - **Settings panel:**
    - Toggle particle borders globally
    - Change server language (PL/EN)
    - **Lock flags globally** — prevent players from changing specific flags
    - **Set default flag values** — applies instantly to all regular claims (named claims excluded with Pro) and new claims
- **OP bypass** — operators bypass all land protections

### Warps
- Set named warp points on your claimed land
- Teleport to warps from anywhere
- Configurable warp limit per player

### TPA System
- `/tpa <player>` — send teleport request
- `/tpaccept` / `/tpdeny` — accept or deny requests
- Configurable timeout and cooldown

### Integrations
- **Pl3xMap** — automatic web map overlay showing all claims with contiguous zone rendering and bold outer borders

### Multilingual
- Full **Polish** and **English** language support
- Easy to add new languages (YAML-based, merge system preserves custom edits on update)

---

## Commands

| Command | Description | Permission |
|---------|-------------|------------|
| `/claim` | Claim current chunk | `belllands.claim` |
| `/claim unclaim` | Remove claim | `belllands.claim` |
| `/claim menu` | Open management GUI | `belllands.claim` |
| `/claim info` | Show land info | `belllands.claim` |
| `/claim flags` | List all flags | `belllands.claim` |
| `/claim flag <flag> <true\|false>` | Change flag | `belllands.claim` |
| `/claim trust <player>` | Add trusted player | `belllands.claim` |
| `/claim untrust <player>` | Remove trusted player | `belllands.claim` |
| `/claim auto` | Toggle auto-claim | `belllands.claim` |
| `/claim unclaim auto` | Toggle auto-unclaim | `belllands.claim` |
| `/claim outline` | Start outline selection | `belllands.claim` |
| `/claim fill` | Fill outlined area | `belllands.claim` |
| `/claim setwarp <name>` | Set warp on your land | `belllands.claim` |
| `/claim warp <name>` | Teleport to warp | `belllands.claim` |
| `/claim delwarp <name>` | Delete warp | `belllands.claim` |
| `/claim warps` | List warps | `belllands.claim` |
| `/claim particles` | Toggle border particles | `belllands.claim` |
| `/claim admin` | Open admin panel | `belllands.admin` |
| `/tpa <player>` | Send teleport request | `belllands.tpa` |
| `/tpaccept` | Accept TP request | `belllands.tpa` |
| `/tpdeny` | Deny TP request | `belllands.tpa` |
| `/belllands language <en\|pl>` | Change language | — |

**Aliases:** `/claim`, `/dzialka`, `/land`

---

## Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `belllands.*` | All permissions | OP |
| `belllands.claim` | Claiming & land management | Everyone |
| `belllands.tpa` | TPA teleportation | Everyone |
| `belllands.admin` | Admin panel access | OP |

---

## Configuration

```yaml
language: pl  # pl or en

claims:
  max-per-player: 5
  max-warps: 3
  default-flags:
    pvp: false
    explosions: false
    fire-spread: false
    mob-spawning: true
    # ... all 13 flags + 3 guest flags configurable
  locked-flags: []        # Flags players cannot change
  particle-borders: true  # Global particle border toggle

tpa:
  timeout-seconds: 60
  cooldown-seconds: 30
```

---

## Installation

1. Drop `BellLands-1.26.1.3.jar` into your server's `plugins/` folder
2. Restart the server
3. Edit `plugins/BellLands/config.yml` to your needs
4. Customize language files in `plugins/BellLands/lang/` (optional)

**Requirements:** Paper 1.21.4+, Java 21+  
**Optional:** Pl3xMap (for web map integration)

---

## Support

- Author: **Bellzeb**
- Part of **BellSuite** ecosystem
- Pro addon with advanced features coming soon
