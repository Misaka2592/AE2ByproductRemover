# GregTech Leisure 的 Forge / AE2 基线

核查日期：2026-09-19。

**可将本项目的 Forge 参考基线锁定为 AE2 官方版 `15.4.10`，对应 Minecraft `1.20.1`。** 已核实的 GregTech Leisure 发布包版本为 **`1.4.5.0`**，其 manifest 使用 **Forge `47.4.16`**。

## 发布来源与身份

[MC百科的原版 GTL 条目](https://www.mcmod.cn/modpack/769.html) 标注名称“格雷科技休闲版 / GregTech Leisure”、制作成员 `nutant233` 与 `maple197`，并将下载入口指向[这个公开 Google Drive 目录](https://drive.google.com/drive/folders/1Ga_w-TmDKNru0me1kAM_gXyedz_Ne4-x)。此处研究对象是原版 GTL，不是 GregTech Leisure Community Edition，也不是第三方自用整合包。

该目录在核查时列出的完整客户端与服务端均为 `1.4.5.0`，文件名没有 alpha/beta 标记；客户端文件为 [`GregTech-Leisure-1.4.5.0.zip`](https://drive.google.com/file/d/1AkLuw6F5te9gvOGoNaSJlw3uIned_rXZ/view)，Drive 文件 ID 为 `1AkLuw6F5te9gvOGoNaSJlw3uIned_rXZ`，显示修改日期 2026-02-22。因此将它作为**公开关联下载目录中最新、未标记预发布的整包**。目录没有独立的 stable 标签或正式发布日志，无法据此断言 QQ 群、另一网盘等渠道不存在其他版本。

压缩包内 `README.md` 标题也是 `GregTech Leisure / 格雷科技休闲版`，链接回同一 MC百科条目，并引用 `nutant233/GTLCore`；这构成发布内容与该项目条目的相互对应。原历史仓库 `nutant233/GregTech-Leisure` 当前公开 API 返回 404；搜索所得旧分支／副本未用来代替此次发布证据。

## 发布包 manifest：直接证据

上述发布 ZIP 内的 `manifest.json` 包含：

```json
{
  "manifestType": "minecraftModpack",
  "manifestVersion": 1,
  "minecraft": {
    "modLoaders": [{ "id": "forge-47.4.16", "primary": true }],
    "version": "1.20.1"
  },
  "name": "GregTech Leisure",
  "overrides": ".minecraft",
  "version": "1.4.5.0"
}
```

同一个 manifest 的 `files` 数组有 AE2 条目：

```json
{
  "projectID": 223794,
  "fileID": 7148487,
  "required": true,
  "isLocked": false
}
```

`modlist.html` 对应链接为 [Applied Energistics 2（by thetechnici4n）](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2)。ZIP 中 `.minecraft/mods/` 的覆盖文件只有 `gtlcore-1.2.2.8-fix2.jar` 和 `gtmthings-1.3.5.b.jar`，没有以另一个 AE2 JAR 覆盖 manifest 指向的下载。因此清单选择的是**官方 AE2 发行物，不是 nutant233 的 AE2 fork**。这个结论只说明 AE2 发行物来源，不代表整合包核心没有在运行时修改它。

这些条目来自发布压缩包本身，不是当前开发分支。为读取元数据，仅按 HTTP `Range` 获取 ZIP 中央目录及三个小文本条目；没有下载完整整包或任何 JAR。

## AE2 文件与源码版本

| 项目 | 已核实值 | 证据 |
| --- | --- | --- |
| CurseForge project ID | `223794` | 发布 manifest |
| CurseForge file ID | `7148487` | 发布 manifest；[文件页](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2/files/7148487) |
| 文件名 | `appliedenergistics2-forge-15.4.10.jar` | [CurseForge 精确文件元数据 API](https://www.curseforge.com/api/v1/mods/223794/files/7148487) 返回该名称、`AE2 15.4.10 [FORGE]`、MC 1.20.1 与 `releaseType: 1` |
| 官方发行版本 | `15.4.10`，release | [AE2 官方 Modrinth 版本 `7KVs6HMQ`](https://modrinth.com/mod/ae2/version/7KVs6HMQ)；[公开版本 API](https://api.modrinth.com/v2/version/7KVs6HMQ) 列同名、同尺寸文件、Forge 与 1.20.1 |
| GitHub release | `forge/v15.4.10`，非 prerelease | [官方发布页](https://github.com/AppliedEnergistics/Applied-Energistics-2/releases/tag/forge/v15.4.10) |
| 精确源码 commit | `b4b08d9941e3faecb520d76be617629bb56661e1` | [官方 tag API](https://api.github.com/repos/AppliedEnergistics/Applied-Energistics-2/git/ref/tags/forge/v15.4.10) |

MC百科模组列表当前也写 AE2 `15.4.10`，但其中 Forge 仍写 `47.3.7`；加载器基线以已核实的 `1.4.5.0` 发布 manifest 的 `47.4.16` 为准。
