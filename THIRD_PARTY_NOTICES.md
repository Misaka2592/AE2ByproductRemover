# 代码来源与第三方声明

本项目代码 Copyright (c) 2026 Misaka2592 and contributors，采用 **LGPL-3.0-or-later**，允许按 GNU Lesser General Public License 第 3 版或任意后续版本使用、修改和分发。完整条款见 [LICENSE](LICENSE) 及其包含的 [GPL v3 条款](licenses/GPL-3.0.txt)。

以下说明本 Forge 1.20.1 分支直接改写的代码及行为参考。上游项目的代码、资源和商标仍归各自权利人所有；本项目的许可证不重新授权这些上游内容。发布 JAR 不捆绑 AE2、GTNH 或其他模组。

## Applied Energistics 2：直接改写及接口参考

上游：[AppliedEnergistics/Applied-Energistics-2](https://github.com/AppliedEnergistics/Applied-Energistics-2)，作者 AlgorithmX2、TeamAppliedEnergistics 及贡献者，相关 Java 文件采用 **LGPL-3.0-or-later**。

| 基线 | 固定源码 | 许可证 |
| --- | --- | --- |
| Forge 15.4.10 / Minecraft 1.20.1 | [`b4b08d9941e3faecb520d76be617629bb56661e1`](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/b4b08d9941e3faecb520d76be617629bb56661e1) | [LICENSE](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/LICENSE) |

本项目 `PatternEncodingTermMenuMixin` 的输入采集、至少一个输入的校验流程改写自 AE2 的 `PatternEncodingTermMenu.encodeProcessingPattern`：[Forge 原方法](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/menu/me/items/PatternEncodingTermMenu.java)。

原版权声明为 `Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.`；对应文件明确授予 LGPL 第 3 版或任意后续版本。该版权声明已保留在本项目改写文件中。2026-09-20 的修改加入了输出空槽压紧，并以存在有效输出作为编码条件。

产物索引、规划树、CPU 执行与存档注入参考上述版本中的 `NetworkCraftingProviders`、`CraftingCalculation`、`CraftingTreeNode`、`CraftingSimulationState`、`CraftingCpuLogic` 和 `ExecutingCraftingJob`。这些部分由本项目实现新增视图和 Mixin，调用上游接口、遵循其存档格式；未移植整个规划器或 CPU。

## GTNH：多输出与 Lite／V2 行为参考

- [GTNewHorizons/Applied-Energistics-2-Unofficial](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial)，AlgorithmX2、TeamAppliedEnergistics、GTNH 维护者及贡献者；参考 GTNH 2.9.0-beta-3 锁定的 `rv3-beta-1050-GTNH`，commit [`f9f49159899bdcdbf88a341e22b159cd58a61585`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/tree/f9f49159899bdcdbf88a341e22b159cd58a61585)，[LGPL v3 许可证](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/LICENSE.txt)。
- 具体行为来源：[`CraftingJobFast`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/fast/CraftingJobFast.java)、[`CraftableItemResolver`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/v2/resolvers/CraftableItemResolver.java) 和 [`CraftingGridCache`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cache/CraftingGridCache.java)。参考的是任意输出可下单及预计产物是否参与原料抵扣的行为，未复制 GTNH 的 Lite／V2 规划器代码。
- 流体行为调查另参考 [GTNewHorizons/AE2FluidCraft-Rework](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/tree/31746024ee5482959f882fdadfe050d56427bd28) `1.5.106-gtnh` 中的 `FluidPatternDetails`，commit `31746024ee5482959f882fdadfe050d56427bd28`，[LGPL v3 许可证](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/31746024ee5482959f882fdadfe050d56427bd28/LICENSE.txt)。本项目未分发其代码或资源。

## Gradle Wrapper：随源码分发的构建工具

`gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar` 来自 [Gradle 8.14.3](https://github.com/gradle/gradle/tree/v8.14.3)，采用 [Apache License 2.0](licenses/Apache-2.0.txt)。脚本保留原作者版权和许可头，Wrapper JAR 保留其 `META-INF/LICENSE`。这三个文件继续按 Apache-2.0 分发，不改为本项目的 LGPL。

Gradle 完整发行版和各构建依赖由构建工具另行下载；本仓库不捆绑这些发行版。对应依赖仍受各自许可证约束。

## 源码取得与分发

本分支对应源码与构建脚本公开于 <https://github.com/Misaka2592/AE2ByproductRemover/tree/1.20.1>。分发修改版或二进制时，应遵守 LGPL-3.0-or-later 对许可证、版权声明、修改说明及对应源码的要求；不能用本项目许可证替代上述第三方内容自身的条款。发布 JAR 随附本文件、LGPL/GPL 全文和第三方许可证副本。
