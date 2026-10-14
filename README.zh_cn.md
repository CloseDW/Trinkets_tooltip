# Trinkets Tooltip

[English](README.md) | 中文

让 [Trinkets](https://modrinth.com/mod/5aaWibi9) 拥有
[Curios](https://www.curseforge.com/minecraft/mc-mods/curios) 的外观和手感，同时不改变 Trinkets 的工作方式。

## 功能

* 有着 Curios 类似风格的面板。
* 左键点击**空的**饰品槽会打开 JEI，列出所有能装备到该槽位的物品。 在 JEI 里查询物品，也会显示它能放进哪些饰品槽。
* 每个饰品槽右上角都有 Curios 的**饰品渲染开关**。关掉某个槽位后，那件饰品就不会再画在
  **你自己**的模型上 —— 第三人称视角里的自己，以及背包界面上的玩家预览都会跟着变。
  本模组是纯客户端模组，所以它只改你能看到的东西：别人眼里的你照旧，别人的饰品也完全不受影响。
  开关状态存在 `config/trinkets-tooltip.properties` 的 `render_disabled_slots` 里，重登之后仍然有效。

## 前置

* [Fabric API](https://modrinth.com/mod/P7dR8mSH)
* [Trinkets](https://modrinth.com/mod/5aaWibi9) 1.20.1 的 3.7.2 或更新版本

## 可选联动

* [Configured](https://www.curseforge.com/minecraft/mc-mods/configured)。
* [JEI](https://modrinth.com/mod/jei)。

## 配置

| 选项 | 默认值 | 范围 | 作用 |
|---|---|---|---|
| `min_width` | `1` | 1–4 | 每页的列数；开了滚动条时就是每一屏的列数。 |
| `max_height` | `7` | 2–7 | 每页的行数；开了滚动条时就是面板一次显示的行数。 |
| `scrollbar` | `false` | — | 用面板左侧的 Baubles 风格滚动条取代翻页箭头：不再分页，面板固定 `max_height` 行高，滚轮每次滚动一行。 |
| `jei_slot_lookup` | `true` | — | 左键点击空的饰品槽会打开 JEI，列出所有能装进该槽位的物品。 |
| `render_disabled_slots` | （空） | — | 被关掉渲染的饰品槽，由面板上的渲染开关写入。 |

面板有两种互斥的排版：

* **分页**（`scrollbar = false`，默认）：一页是 `min_width` 列 × `max_height` 行，多出来的槽位靠箭头或滚轮翻页。
* **滚动条**（`scrollbar = true`）：没有翻页箭头，面板左侧多一条 Baubles 风格的滚动条；
  面板固定显示 `max_height` 行，内容按行滚动 —— 滚轮一次一行，也可以按住滑块拖动（在轨道任意位置按下会直接跳过去）。

两种模式下，滚轮往上都是往前看（露出更靠前的槽位）。

## 鸣谢与许可

* 感谢 jptrzy 的[trinkets-curios-theme](https://github.com/jptrzy/trinkets-curios-theme-mod)和**C4 (TheIllusiveC4)** 的 [Curios](https://github.com/TheIllusiveC4/Curios)提供的灵感， 采用 **LGPL-3.0** 许可。

滚动条贴图（`assets/trinkets-tooltip/textures/gui/baubles_scroller.png`）裁自
[BaublesEX](https://github.com/darktire/BaublesEX) / [Baubles](https://github.com/Azanor/Baubles) 的
`assets/baubles/textures/gui/baubles_container.png`：18×166 的轨道和 6×89 的滑块，并排拼进一张 24×166 的图。
**注意**：这两个仓库都没有 LICENSE 文件（GitHub API 两边的 `license` 都是 `null`），所以这一张素材是在
没有明确授权声明的情况下使用的 —— 商业再分发前请先把它换掉。

由于本作品派生自 LGPL-3.0 项目，整个模组以 **LGPL-3.0** 分发。见 [LICENSE](LICENSE)。
