# ATM10 与 ATM10 To the Sky 的 AE2 版本基线

核查日期：2026-09-19。下表以 CurseForge 已发布的正式包及其 `manifest.json` 为依据；默认分支只用于交叉核对。

| 整合包身份 | 最新正式发布 | Minecraft / 加载器 | 发布 manifest 锁定的 AE2 | AE2 身份 |
| --- | --- | --- | --- | --- |
| All the Mods 10 - ATM10；CurseForge project `925200`；官方仓库 `AllTheMods/ATM-10` | **8.1**，2026-08-29；file `8764211` | **1.21.1 / NeoForge 21.1.249** | project `223794` / file `7027323` → **Applied Energistics 2 19.2.17 [NEOFORGE]**，`appliedenergistics2-19.2.17.jar` | 官方 AppliedEnergistics 上游发行物，并非另一个 AE2 fork |
| All the Mods 10 - To the Sky - ATM10Sky；CurseForge project `1298402`；官方仓库 `AllTheMods/All-the-mods-10-Sky` | **2.0.4**，2026-09-11；file `8854298`，显示名 `ATM10SKY-2.0.4` | **1.21.1 / NeoForge 21.1.250** | project `223794` / file `7027323` → **Applied Energistics 2 19.2.17 [NEOFORGE]**，`appliedenergistics2-19.2.17.jar` | 与 ATM10 完全相同的官方 AE2 文件 |

因此，以这两个包作为 NeoForge 1.21.1 参考时，共同的 AE2 基线是 **19.2.17**，不是此前为行为核查临时选用的 `1.21.1` 分支头。

## 发布与 manifest 证据

### ATM10 8.1

- [官方 README](https://github.com/AllTheMods/ATM-10/blob/ab6f65e07b88423cdae1724864ba42a573ba758a/README.md) 自称官方仓库，并链接到 CurseForge 的 ATM10 与 ATM10Sky 项目。
- [官方发布列表 API，按创建日期倒序](https://www.curseforge.com/api/v1/mods/925200/files?pageIndex=0&pageSize=20&sort=dateCreated&sortDescending=true) 最新项为 `8764211`，`displayName: All the Mods 10-8.1`，`releaseType: 1`（Release），日期 `2026-08-29T20:15:05.273Z`。
- [发布文件页](https://www.curseforge.com/minecraft/modpacks/all-the-mods-10/files/8764211)；[精确文件元数据 API](https://www.curseforge.com/api/v1/mods/925200/files/8764211)。
- [该正式包的官方 CDN 文件](https://mediafilez.forgecdn.net/files/8764/211/All%20the%20Mods%2010-8.1.zip) 内 `manifest.json` 的相关内容：

```json
{
  "name": "All the Mods 10",
  "version": "8.1",
  "minecraft": {
    "version": "1.21.1",
    "modLoaders": [{ "id": "neoforge-21.1.249", "primary": true }]
  },
  "ae2_file_entry": {
    "projectID": 223794,
    "fileID": 7027323,
    "required": true,
    "isLocked": false
  }
}
```

`ae2_file_entry` 是从原 manifest 的 `files` 数组摘出的条目，名称仅为本报告便于阅读所加；`isLocked: false` 不改变该发布 manifest 指向的精确 `fileID`。

### ATM10Sky 2.0.4

- [官方 README](https://github.com/AllTheMods/All-the-mods-10-Sky/blob/3174cbf54358d6dfc19f0ee66d3eaf5b387a8482/README.md) 将该项目定义为 Minecraft 1.21.1 NeoForge 空岛包，并链接对应 CurseForge 项目。
- [官方发布列表 API，按创建日期倒序](https://www.curseforge.com/api/v1/mods/1298402/files?pageIndex=0&pageSize=20&sort=dateCreated&sortDescending=true) 最新项为 `8854298`，`displayName: ATM10SKY-2.0.4`，`releaseType: 1`（Release），日期 `2026-09-11T00:10:41.113Z`。
- [发布文件页](https://www.curseforge.com/minecraft/modpacks/all-the-mods-10-sky/files/8854298)；[精确文件元数据 API](https://www.curseforge.com/api/v1/mods/1298402/files/8854298)。
- [该正式包的官方 CDN 文件](https://mediafilez.forgecdn.net/files/8854/298/ATM10SKY-2.0.4.zip) 内 `manifest.json` 的相关内容：

```json
{
  "name": "ATM10SKY",
  "version": "2.0.4",
  "minecraft": {
    "version": "1.21.1",
    "modLoaders": [{ "id": "neoforge-21.1.250", "primary": true }]
  },
  "ae2_file_entry": {
    "projectID": 223794,
    "fileID": 7027323,
    "required": true,
    "isLocked": false
  }
}
```

## AE2 文件身份与源码

- [CurseForge project 223794 / file 7027323 API](https://www.curseforge.com/api/v1/mods/223794/files/7027323) 返回 `AE2 19.2.17 [NEOFORGE]`、`appliedenergistics2-19.2.17.jar`、MC `1.21.1`、加载器 `NeoForge`、`releaseType: 1`。[对应公开文件页](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2/files/7027323)。
- 两包仓库记录的 AE2 文件与官方 Modrinth 文件一致：[官方版本元数据](https://api.modrinth.com/v2/version_file/49c18d6a4af487957d7e5a6ad5dcbf71090b8e14) 指向 `AE2 19.2.17 [NEOFORGE]`；[官方项目元数据](https://api.modrinth.com/v2/project/XxWD5pD3) 的 `source_url` 是 `https://github.com/AppliedEnergistics/Applied-Energistics-2`。这里使用仓库已有的文件标识查询元数据，没有下载或重新计算 jar 校验值。
- 官方源码 tag 为 [`neoforge/v19.2.17`](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/neoforge/v19.2.17)，[tag API](https://api.github.com/repos/AppliedEnergistics/Applied-Energistics-2/git/ref/tags/neoforge/v19.2.17) 精确指向 commit **`79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a`**。

## 默认分支与正式发布的区别

- ATM10 `main` 在核查时为 [`ab6f65e07b88423cdae1724864ba42a573ba758a`](https://github.com/AllTheMods/ATM-10/commit/ab6f65e07b88423cdae1724864ba42a573ba758a)。[CHANGELOG](https://github.com/AllTheMods/ATM-10/blob/ab6f65e07b88423cdae1724864ba42a573ba758a/CHANGELOG.md) 最新为 8.1；[modlist](https://github.com/AllTheMods/ATM-10/blob/ab6f65e07b88423cdae1724864ba42a573ba758a/config/crash_assistant/modlist.json) 记录 AE2 19.2.17 与 NeoForge 21.1.249，与正式 manifest 相符。
- ATM10Sky `main` 在核查时为 [`3174cbf54358d6dfc19f0ee66d3eaf5b387a8482`](https://github.com/AllTheMods/All-the-mods-10-Sky/commit/3174cbf54358d6dfc19f0ee66d3eaf5b387a8482)，提交说明为 `2.0.4`；[modlist](https://github.com/AllTheMods/All-the-mods-10-Sky/blob/3174cbf54358d6dfc19f0ee66d3eaf5b387a8482/config/crash_assistant/modlist.json) 记录 AE2 19.2.17 与 NeoForge 21.1.250。该仓库 `CHANGELOG.md` 开头仍是 Beta 5，不能据此判定它没有正式版；正式版以 CurseForge 发布列表和 manifest 为准。

为核对发布 manifest，读取了官方 ZIP 的 HTTP Range 片段（目录和 `manifest.json`），未下载整包或 jar。上述版本结论来自已发布包的精确清单。
