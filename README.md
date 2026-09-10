<div align="center">

# SF_Magic8Ball — Slimefun Legacy
### The classic Magic 8 Ball addon, preserved for modern servers

[![Build](https://github.com/wickidcow/SF_Magic8Ball/actions/workflows/maven.yml/badge.svg)](https://github.com/wickidcow/SF_Magic8Ball/actions/workflows/maven.yml)
[![License](https://img.shields.io/badge/License-GPLv3-blue)](https://github.com/wickidcow/Slimefun-Legacy/blob/master/LICENSE)

</div>

> [!IMPORTANT]
> SF_Magic8Ball is an unofficial, independently maintained downstream fork. **NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.**

## Preserved gameplay

The Magic 8 Ball Fragment, Magic 8 Ball item, recipes, research, particles, sounds and familiar affirmative/noncommittal/negative responses remain the same. This project stays a tiny novelty addon rather than becoming a general chat or prediction plugin.

## 1.0.1 modernization

- Action-bar output now uses Paper's Adventure API instead of the older Bungee compatibility bridge.
- Edited configurations with an empty response category no longer risk a random-selection exception; empty categories are skipped and a classic-style fallback is available if all response lists are empty.
- A small per-player use cooldown is configurable with `options.cooldown-ms`; the shipped default is 500 ms and `0` disables it.
- Cooldown state is cleared when players leave.

Release JAR: `SF_Magic8Ball1.0.1.jar`

Built with Java 25 targeting Java 21 bytecode. Slimefun Legacy is the primary target, with shared API compatibility retained for Slimefun United, SlimefunGuguProject/Slimefun4 and original Slimefun4-compatible implementations. Paper is primary; Purpur, Folia and Leaf are compatibility targets.

No direct GuizhanLib dependency is used and no Chinese localization is distributed.

## Credits, trademarks and license

Original project authorship remains with **xMoonCorp** and contributors. Magic 8 Ball is a trademark associated with Mattel; this independent Minecraft addon is not affiliated with or endorsed by Mattel.

**NOT AN OFFICIAL MINECRAFT PRODUCT. NOT APPROVED BY OR ASSOCIATED WITH MOJANG OR MICROSOFT.** Minecraft and other third-party trademarks remain the property of their respective owners.

This maintained distribution is released under the [GNU General Public License v3.0](https://github.com/wickidcow/Slimefun-Legacy/blob/master/LICENSE). Upstream authorship and copyright remain with their original owners.
