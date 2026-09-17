# Trinkets Tooltip

[中文](README.zh_cn.md) | English

Gives [Trinkets](https://modrinth.com/mod/5aaWibi9) the look and feel of
[Curios](https://www.curseforge.com/minecraft/mc-mods/curios), without changing how Trinkets works.

## What it does

* A panel in the Curios style.
* Left clicking an **empty** trinket slot opens JEI, listing every
  item that can be equipped in that slot. Looking an item up in JEI also shows which trinket
  slots accept it.

## Requirements

* [Fabric API](https://modrinth.com/mod/P7dR8mSH)
* [Trinkets](https://modrinth.com/mod/5aaWibi9) 3.7.2 or newer for 1.20.1

## Optional integrations

* [Configured](https://www.curseforge.com/minecraft/mc-mods/configured).
* [JEI](https://modrinth.com/mod/jei).

## Configuration

| Option | Default | Range | Effect |
|---|---|---|---|
| `pagination` | `true` | — | Split the panel into pages with previous/next arrows, like Curios. Off lays every slot out at once. |
| `min_width` | `1` | 1–4 | Columns per page (only used while pagination is on). |
| `max_height` | `7` | 2–7 | Rows per page. |
| `scrolling_outside_boundary` | `false` | — | Let the mouse wheel turn pages anywhere in the inventory, not just over the panel. |
| `jei_slot_lookup` | `true` | — | Left clicking an empty trinket slot opens JEI listing every item that can be equipped there. |

## Credits and license

* Panel layout, textures and behaviour are derived from
  [trinkets-curios-theme](https://github.com/jptrzy/trinkets-curios-theme-mod) by **jptrzy**,
  licensed under **LGPL-3.0**.
* The Curios interface artwork (`inventory_revamp.png` and `inventory.png`) is derived from
  [Curios](https://github.com/TheIllusiveC4/Curios) by **C4 (TheIllusiveC4)**, licensed under
  **LGPL-3.0**.

Because this work is derived from an LGPL-3.0 project, the mod as a whole is distributed
under **LGPL-3.0**. See [LICENSE](LICENSE).
