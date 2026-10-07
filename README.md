# Pride Holo

When you look at a chest, machine, tank, battery or villager, a hologram appears in front of it showing **what's inside and what it's doing**. Pride Holo is a maintained fork of [HoloInventory](https://github.com/dries007/HoloInventory) for Minecraft 1.12.2, built on the [UEL update](https://github.com/ChromaPIE/HoloInventory-UEL).

> **Status: beta (3.0.0).**

## Features

- **Items**: any inventory from any mod (vanilla `IInventory` and Forge item handlers, checked on every side). Identical items are merged, and very large storage shows "+N more".
- **Fluids**: every Forge fluid tank, shown as amount/capacity bars in the fluid's colour, with its name.
- **Energy**: stored/maximum Forge Energy (RF/FE).
- **Progress**: "Working 45%" for furnaces, brewing stands and modded machines that save progress in the usual way (`cookTime/totalCookTime`, `progress/maxProgress` and similar, including one level of sub-tags). A fuel bar is also shown.
- **Villagers**: their trades.
- **No peeking at loot**: locked containers (Treasure2-style locks and lock flags) and loot chests whose loot hasn't been generated yet are never shown outside creative mode. This also avoids triggering loot generation early.
- **Settings screen with live preview**. Open it from *Mods → Pride Holo → Config* or *Options → Pride Holo*.

## Fixes compared to HoloInventory

- Blocks and entities are read on the server thread, not the network thread. This fixes random crashes in large packs.
- Requests are only answered for blocks in your dimension, in loaded chunks and within about 12 blocks. You can't peek into distant chests, and no chunks are loaded.
- About 4 requests per second instead of 20, and only for the block or entity you're looking at.
- Items with huge NBT (backpacks, shulker boxes) are sent without it. Item counts above 127 survive the network trip, and "1000" no longer shows as "0M".

## Controls

- Holograms are shown by default.
- **H** turns them on or off. A *hold to show* key is also available but unbound by default. Both are under *Controls → prideholo*.
- Show mode can be always, sneak, sprint, hold or toggle.

### Commands
`/prideholo ban | unban [name] | list`
- `ban` hides the next block type you right-click, and `unban` shows it again.
- On a server this applies server-wide (operators only). In single player it applies to your client.

## Configuration

**Client look and behaviour**: `config/prideholo-client.json`, edited in the settings screen.
- What to show: chests, machines, tanks, entities, ender chest, jukebox, fluids, energy, progress and fuel.
- Show mode, scale and item size, maximum columns/items, and sorting (by slot, count or name).
- Count format (short `1.2k` or exact), spin speed, bob, fade-in, position (front/above) and maximum distance.
- Panel, colour strip, glow, slot tiles, opacity, accent colour (rainbow, trans, pink, blue or purple), title and status badge.

**Common / server**: the Forge config `config/prideholo.cfg` holds the banned block list, show-on-sneak/sprint, rotation speed and name/panel rendering.

## Requirements

- Minecraft **1.12.2**
- **Forge** 14.23.5.2860+ or **Cleanroom**
- Install it on **both client and server**. The server answers the hologram requests.

## Building from source

```sh
./gradlew build
```

The jar is written to `build/libs/`.

## License

[MIT](LICENSE.txt), the same license as the upstream projects.

- **[HoloInventory](https://github.com/dries007/HoloInventory)**: Copyright (c) 2014–2017 Dries K. (Dries007), MIT.
- **[HoloInventory-UEL](https://github.com/ChromaPIE/HoloInventory-UEL)** by ChromaPIE, MIT.

Upstream copyright headers are kept in the inherited source files.

## Credits

Made with [Claude Code](https://claude.com/claude-code) and [Blockbench](https://www.blockbench.net).

- **Dries007**: original author of [HoloInventory](https://github.com/dries007/HoloInventory).
- **ChromaPIE**: [HoloInventory-UEL](https://github.com/ChromaPIE/HoloInventory-UEL) update.
- The HoloInventory contributors (including T145) whose work is preserved in this repository's history.
