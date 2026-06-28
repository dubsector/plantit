# plantit

[![Build](https://github.com/dubsector/plantit/actions/workflows/build.yml/badge.svg)](https://github.com/dubsector/plantit/actions/workflows/build.yml)
[![CodeQL](https://github.com/dubsector/plantit/actions/workflows/codeql.yml/badge.svg)](https://github.com/dubsector/plantit/actions/workflows/codeql.yml)
[![Dependency Check](https://github.com/dubsector/plantit/actions/workflows/dependency-check.yml/badge.svg)](https://github.com/dubsector/plantit/actions/workflows/dependency-check.yml)
[![Zizmor](https://github.com/dubsector/plantit/actions/workflows/zizmor.yml/badge.svg)](https://github.com/dubsector/plantit/actions/workflows/zizmor.yml)
[![OpenSSF Scorecard](https://api.securityscorecards.dev/projects/github.com/dubsector/plantit/badge)](https://securityscorecards.dev/viewer/?uri=github.com/dubsector/plantit)
[![Security Policy](https://img.shields.io/badge/Security-Policy-green)](SECURITY.md)
[![Dependabot](https://img.shields.io/badge/Dependabot-enabled-025E8C?logo=dependabot)](https://github.com/dubsector/plantit/network/updates)

Tactical bomb-defuse minigame for Minecraft, running on [Paper](https://papermc.io/software/paper) 1.21.4.

Players split into two teams. One team plants a bomb at a designated site; the other defuses it before time runs out. Built with original pixel-art assets and a custom game engine — not a port of any existing game.

Works alongside [plantit-queue](https://github.com/dubsector/plantit-queue), a Velocity proxy plugin that manages the player queue and dispatches players to game servers automatically.

## Requirements

- Paper 1.21.4
- Java 21
- [WorldEdit](https://enginehub.org/worldedit/) 7.4.x
- [WorldGuard](https://enginehub.org/worldguard/) 7.0.x
- [plantit-queue](https://github.com/dubsector/plantit-queue) on your Velocity proxy

## Installation

1. Download the latest JAR from [Releases](https://github.com/dubsector/plantit/releases)
2. Drop it into your Paper server's `plugins/` folder alongside WorldEdit and WorldGuard
3. Restart the server — a default `config.yml` is generated in `plugins/PlantIt/`
4. Configure your game settings and map regions
5. The server will automatically signal the Velocity queue when a match ends and slots open

## Configuration

```yaml
game:
  min-players: 2
  max-rounds: 24
  overtime-rounds: 6

phases:
  freeze-duration: 15      # seconds of buy phase before round goes live
  round-duration: 115      # seconds per round
  round-end-delay: 5       # seconds between rounds
```

## How it works

### Round flow

1. Players join — assigned to **T** (attackers) or **CT** (defenders) automatically
2. **Freeze phase** — movement locked, players buy equipment
3. **Live phase** — round timer counts down
4. Round ends when: all of one team is eliminated, the bomb explodes, the bomb is defused, or time expires
5. Teams swap at halftime
6. First team to win `max-rounds / 2 + 1` rounds wins the match

### Win conditions

| Outcome | Winner |
|---|---|
| All Ts eliminated | CT |
| All CTs eliminated | T |
| Bomb exploded | T |
| Bomb defused | CT |
| Time expired (no bomb) | CT |

### Queue integration

When a match ends, the plugin signals the Velocity proxy via the `plantit:queue` plugin messaging channel (`SLOT_OPEN:<count>`). [plantit-queue](https://github.com/dubsector/plantit-queue) then dispatches the next players from the queue automatically.

## Building from source

```bash
git clone https://github.com/dubsector/plantit
cd plantit
mvn clean package -DskipTests
# JAR is in target/
```

Requires Java 21, Maven 3.8+, and internet access for dependency resolution (WorldEdit and WorldGuard are pulled from the EngineHub Maven repo).

## License

[GPL-3.0](LICENSE) — forks must also be open source.
