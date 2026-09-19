# Infinity Legacy II 与 Creative Drawers Producer 2 的 AE2 版本

核查日期：2026-09-19。以作者在 CurseForge 发布的客户端包内 `manifest.json` 为版本依据；通过 HTTP Range 只读取 ZIP 目录和清单。

## 结论

| 整合包 | 本次参考的发布版本 | Minecraft / 加载器 | 清单指定的 AE2 |
| --- | --- | --- | --- |
| Infinity Legacy II | 最新正式版 **4.4**，2026-09-03，CurseForge 文件 **8796289** | **1.21.1 / NeoForge 21.1.248** | 官方 **19.2.17**，项目 **223794** / 文件 **7027323** |
| Creative Drawers Producer 2 | 最新发布 **Beta v1.7**，2026-09-02，CurseForge 文件 **8793011**；尚无正式版 | **1.21.1 / NeoForge 21.1.241** | 官方 **19.2.17**，项目 **223794** / 文件 **7027323** |

两者清单都引用官方 Applied Energistics 2 项目的同一个发布文件 `appliedenergistics2-19.2.17.jar`，不是另外一个 AE2 fork 项目。因此这两个整合包共同支持将 NeoForge 1.21.1 的 AE2 实现基线定为 **19.2.17**。这只确认基础 AE2 的来源与版本，不代替各附属模组对合成逻辑的兼容审查。

## Infinity Legacy II

- 正确项目是作者 **WesleyJBA** 的 [Infinity Legacy II](https://www.curseforge.com/minecraft/modpacks/infinity-legacy-2)，项目 ID **1264960**；slug 为 `infinity-legacy-2`。同作者另有 `Infinity Legacy` 和 `Infinity Legacy II Lite`，本次没有将它们混入结果。
- [最新文件 API](https://www.curseforge.com/api/v1/mods/1264960/files?pageIndex=0&pageSize=5&sort=dateCreated&sortDescending=true) 第一项为 `Infinity Legacy II 4.4`，`releaseType: 1`，对应 [4.4 发布页](https://www.curseforge.com/minecraft/modpacks/infinity-legacy-2/files/8796289)。文件名为 `Infinity Legacy II-4.4.zip`。发布时间 API 为 `2026-09-02T23:59:51.897Z`，网页标示日期为 2026-09-03。
- [发布文件的官方 CDN](https://mediafilez.forgecdn.net/files/8796/289/Infinity%20Legacy%20II-4.4.zip) 中 `manifest.json` 明确写出：

```json
{
  "name": "Infinity Legacy II",
  "version": "4.4",
  "minecraft": {
    "version": "1.21.1",
    "modLoaders": [{ "id": "neoforge-21.1.248", "primary": true }]
  }
}
```

其中 `files` 的 AE2 条目为：

```json
{ "projectID": 223794, "fileID": 7027323, "required": true, "isLocked": false }
```

本次未确认作者公开发布整合包清单的 GitHub 仓库；版本结论直接来自正式发布包，不依赖搜索到的同名核心模组仓库。

## Creative Drawers Producer 2

- 正确项目是作者 **y_xiao233** 的 [Creative Drawers Producer 2](https://www.curseforge.com/minecraft/modpacks/creative-drawers-producer-2)，项目 ID **1665072**；不同于 Minecraft 1.18.2 的一代整合包，也不同于同名核心模组。
- [完整发布文件 API](https://www.curseforge.com/api/v1/mods/1665072/files?pageIndex=0&pageSize=50&sort=dateCreated&sortDescending=true) 共返回 **8** 个文件，从 Beta v1.0 到 Beta v1.7，全部为 `releaseType: 2`。因此截至核查日没有正式版；最新为 [Beta v1.7 发布页](https://www.curseforge.com/minecraft/modpacks/creative-drawers-producer-2/files/8793011)，API 发布时间为 `2026-09-02T14:25:38.107Z`。
- [发布文件的官方 CDN](https://mediafilez.forgecdn.net/files/8793/11/Creative%20Drawers%20Producer%202-Beta%20v1.7.zip) 中 `manifest.json` 明确写出：

```json
{
  "name": "Creative Drawers Producer 2",
  "version": "Beta v1.7",
  "minecraft": {
    "version": "1.21.1",
    "modLoaders": [{ "id": "neoforge-21.1.241", "primary": true }]
  }
}
```

其中 `files` 的 AE2 条目为：

```json
{ "projectID": 223794, "fileID": 7027323, "required": true, "isLocked": false }
```

搜索可见 [Y-Xiao233/Creative-Drawers-Producer-2](https://github.com/Y-Xiao233/Creative-Drawers-Producer-2) 仓库，描述为 `Minecraft Modpacks`，`master` 中有 `MODLIST.md`、`CHANGELOG.md`、`config`、`kubejs`，没有根目录版本锁定清单或 GitHub Releases。其 [MODLIST.md](https://github.com/Y-Xiao233/Creative-Drawers-Producer-2/blob/master/MODLIST.md) 只列模组名称与项目链接，不含 AE2 版本；本次版本结论采用上述发布包清单，不将开发分支列表当作发布清单。

## AE2 文件身份闭环

[AE2 文件 7027323 的官方 API](https://www.curseforge.com/api/v1/mods/223794/files/7027323) 返回：

```json
{
  "id": 7027323,
  "displayName": "AE2 19.2.17 [NEOFORGE]",
  "fileName": "appliedenergistics2-19.2.17.jar",
  "projectId": 223794,
  "gameVersions": ["NeoForge", "1.21.1"],
  "releaseType": 1
}
```

对应 [AE2 19.2.17 发布页](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2/files/7027323)。`isLocked: false` 是整合包清单中的原值；此处所称“清单指定版本”指下载该发布包时清单引用的文件，并不意味着用户之后不能更新它。
