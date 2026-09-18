# Trinkets Tooltip

[English](README.md) | 中文

让 [Trinkets](https://modrinth.com/mod/5aaWibi9) 拥有
[Curios](https://www.curseforge.com/minecraft/mc-mods/curios) 的外观和手感，同时不改变 Trinkets 的工作方式。

## 功能

* 有着 Curios 类似风格的面板。
* 左键点击**空的**饰品槽会打开 JEI，列出所有能装备到该槽位的物品。 在 JEI 里查询物品，也会显示它能放进哪些饰品槽。

## 前置

* [Fabric API](https://modrinth.com/mod/P7dR8mSH)
* [Trinkets](https://modrinth.com/mod/5aaWibi9) 1.20.1 的 3.7.2 或更新版本

## 可选联动

* [Configured](https://www.curseforge.com/minecraft/mc-mods/configured)。
* [JEI](https://modrinth.com/mod/jei)。

## 配置

| 选项 | 默认值 | 范围 | 作用 |
|---|---|---|---|
| `pagination` | `true` | — | 像 Curios 那样把面板分成若干页并显示上/下一页箭头。关闭则一次性铺开所有槽位。 |
| `min_width` | `1` | 1–4 | 每页的列数（仅在分页开启时生效）。 |
| `max_height` | `7` | 2–7 | 每页的行数。 |
| `scrolling_outside_boundary` | `false` | — | 在背包内任意位置滚轮都能翻页，而不只是在面板上。 |
| `jei_slot_lookup` | `true` | — | 左键点击空的饰品槽会打开 JEI，列出所有能装进该槽位的物品。 |

## 鸣谢与许可

* 感谢 jptrzy 的[trinkets-curios-theme](https://github.com/jptrzy/trinkets-curios-theme-mod)和**C4 (TheIllusiveC4)** 的 [Curios](https://github.com/TheIllusiveC4/Curios)提供的灵感， 采用 **LGPL-3.0** 许可。

由于本作品派生自 LGPL-3.0 项目，整个模组以 **LGPL-3.0** 分发。见 [LICENSE](LICENSE)。
