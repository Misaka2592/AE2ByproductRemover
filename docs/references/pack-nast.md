# New Age Science and Technology 的 AE2 版本

核查日期：2026-09-19。

| 项目 | 结果 |
| --- | --- |
| 整合包 | New Age Science and Technology，作者 a999cs；主项目，不是 Easy 或 hard 分支 |
| 当前正式发布 | 2.0，2026-08-29，CurseForge file ID `8762320` |
| Minecraft | 1.21.1 |
| 加载器 | NeoForge 21.1.243 |
| AE2 | 官方 Applied Energistics 2 19.2.17，CurseForge project ID `223794` / file ID `7027323` |

## 发布证据

- [官方项目页面](https://www.curseforge.com/minecraft/modpacks/new-age-science-and-technology) 将 2.0 列为 Main File；[文件详情](https://www.curseforge.com/minecraft/modpacks/new-age-science-and-technology/files/8762320) 明确标记 Release、MC 1.21.1、NeoForge 与发布日期。
- 该[正式发布包](https://www.curseforge.com/minecraft/modpacks/new-age-science-and-technology/download/8762320)中的 `manifest.json` 写有 `name: New Age Science and Technology 2.0`、`version: 2.0`、`minecraft.version: 1.21.1`、`modLoaders[0].id: neoforge-21.1.243`，并锁定 `projectID: 223794, fileID: 7027323, required: true`。
- [AE2 对应文件](https://www.curseforge.com/minecraft/mc-mods/applied-energistics-2/files/7027323)为 AE2 19.2.17 的 NeoForge 版；包内 `modlist.html` 同时将此依赖列为 Applied Energistics 2 (by Team AppliedEnergistics)。

因此，该整合包的版本参考可落在官方 AE2 19.2.17。整合包还含其他 AE 附属模组；这里仅确认用户要求的 AE2 版本，不由此推定附属模组的兼容性。
