# 代码来源与第三方声明

本项目代码 Copyright (c) 2026 Misaka2592 and contributors，采用 **LGPL-3.0-or-later**，允许按 GNU Lesser General Public License 第 3 版或任意后续版本使用、修改和分发。完整条款见 [LICENSE](LICENSE) 及其包含的 [GPL v3 条款](licenses/GPL-3.0.txt)。

以下分别说明直接改写的代码、行为参考和兼容接口参考。上游项目的代码、资源和商标仍归各自权利人所有；本项目的许可证不重新授权这些上游内容。发布 JAR 不包含 AE2 或下列附属模组的实现、资源或 JAR。

## Applied Energistics 2：直接改写及接口参考

上游：[AppliedEnergistics/Applied-Energistics-2](https://github.com/AppliedEnergistics/Applied-Energistics-2)，作者 AlgorithmX2、TeamAppliedEnergistics 及贡献者，相关 Java 文件采用 **LGPL-3.0-or-later**。

| 基线 | 固定源码 | 许可证 |
| --- | --- | --- |
| NeoForge 19.2.17 / Minecraft 1.21.1 | [`79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a`](https://github.com/AppliedEnergistics/Applied-Energistics-2/tree/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a) | [LICENSE](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a/LICENSE) |

本分支 `src/main/java/dev/ae2byproductremover/mixin/encoding/PatternEncodingTermMenuMixin.java` 的输入采集、至少一个输入的校验流程改写自 AE2 的 [`PatternEncodingTermMenu.encodeProcessingPattern`](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a/src/main/java/appeng/menu/me/items/PatternEncodingTermMenu.java)。

原版权声明为 `Copyright (c) 2013 - 2014, AlgorithmX2, All rights reserved.`；对应文件明确授予 LGPL 第 3 版或任意后续版本。该版权声明已保留在本项目改写文件中。2026-09-20 的修改加入了输出空槽压紧，并以存在有效输出作为编码条件。

产物索引、规划树、CPU 执行与存档注入参考上述版本中的 `NetworkCraftingProviders`、`CraftingCalculation`、`CraftingTreeNode`、`CraftingSimulationState`、`CraftingCpuLogic` 和 `ExecutingCraftingJob`。这些部分由本项目实现新增视图和 Mixin，调用上游接口、遵循其存档格式；未移植整个规划器或 CPU。

## GTNH：多输出与 Lite／V2 行为参考

- [GTNewHorizons/Applied-Energistics-2-Unofficial](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial)，AlgorithmX2、TeamAppliedEnergistics、GTNH 维护者及贡献者；参考 GTNH 2.9.0-beta-3 锁定的 `rv3-beta-1050-GTNH`，commit [`f9f49159899bdcdbf88a341e22b159cd58a61585`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/tree/f9f49159899bdcdbf88a341e22b159cd58a61585)，[LGPL v3 许可证](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/LICENSE.txt)。
- 具体行为来源：[`CraftingJobFast`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/fast/CraftingJobFast.java)、[`CraftableItemResolver`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/v2/resolvers/CraftableItemResolver.java) 和 [`CraftingGridCache`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cache/CraftingGridCache.java)。参考的是任意输出可下单及预计产物是否参与原料抵扣的行为，未复制 GTNH 的 Lite／V2 规划器代码。
- 流体行为调查另参考 [GTNewHorizons/AE2FluidCraft-Rework](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/tree/31746024ee5482959f882fdadfe050d56427bd28) `1.5.106-gtnh` 中的 `FluidPatternDetails`，commit `31746024ee5482959f882fdadfe050d56427bd28`，[LGPL v3 许可证](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/31746024ee5482959f882fdadfe050d56427bd28/LICENSE.txt)。本项目未分发其代码或资源。

## NeoForge 兼容接口来源

以下桥接代码是本项目编写的注入、访问器和产物过滤逻辑；引用目标类及方法来实现互操作，没有搬入附属模组的规划器、CPU 或编码器实现。第三方项目分别保留以下许可证。

| 项目与作者 | 实际验证版本与参考源码 | 相关接口与许可 |
| --- | --- | --- |
| [AdvancedAE](https://github.com/pedroksl/AdvancedAE)，Pedroksl 及贡献者 | 1.6.12-1.21.1；参考源码 [`c322dad81e6f2ad94cb1fb861e73a7ba68e5907a`](https://github.com/pedroksl/AdvancedAE/tree/c322dad81e6f2ad94cb1fb861e73a7ba68e5907a)，版本字段匹配，该提交不代表已证明与发布 JAR 字节一致 | `AdvCraftingCPULogic`、`ExecutingCraftingJob`、`AdvProcessingPattern` 和 `AdvPatternDetailsEncoder`（读取输入方向及保留样板类型）；[LGPL v3](https://github.com/pedroksl/AdvancedAE/blob/c322dad81e6f2ad94cb1fb861e73a7ba68e5907a/LICENSE.md)。其中来自 AE2 的执行代码保留 Copyright (c) 2021, TeamAppliedEnergistics 和 LGPL-3.0-or-later 声明 |
| [Neo ECO AE Extension](https://github.com/DancingSnow0517/NeoECOAEExtension)，DancingSnow、ZhuRuoLing 及贡献者 | 21.1.1，commit [`3b955f8b5d25a9dd58892ebbc1517ca7819695cf`](https://github.com/DancingSnow0517/NeoECOAEExtension/tree/3b955f8b5d25a9dd58892ebbc1517ca7819695cf) | `ECOCraftingCPULogic`、`ExecutingCraftingJob`；项目 [GPL v3](https://github.com/DancingSnow0517/NeoECOAEExtension/blob/3b955f8b5d25a9dd58892ebbc1517ca7819695cf/LICENSE)，其中来自 AE2 的执行代码保留 TeamAppliedEnergistics 2021、LGPL-3.0-or-later。上游说明经 Hikari_Nova 授权移植 ECO AE Extension |
| Thunderbolt Core，MOAKIEE、CystrySU、gjmhmm8、_leng、TedXenon、MHanHanBing、QianChang | NAST HARD 0.9.6 所带 1.0.6；原仓库 [ae2lt/Thunderbolt-Core](https://github.com/ae2lt/Thunderbolt-Core) 核查时不可访问，源码参考明确标为**社区备份**的 [bfzds/Thunderbolt-Core，`432ae81f66952670a031e8cd9697ea74f2413926`](https://github.com/bfzds/Thunderbolt-Core/tree/432ae81f66952670a031e8cd9697ea74f2413926)，版本字段为 MC 1.21.1 / 1.0.6 | `FastCraftingPlanner`、`CraftPlan`；[GNU LGPL v3](https://github.com/bfzds/Thunderbolt-Core/blob/432ae81f66952670a031e8cd9697ea74f2413926/LICENSE)。本项目未复制其快速规划算法 |
| [Data Energistics](https://github.com/ModularMCLib/DataEnergistics)，fish_dan、QiuYe、TedXenon 及贡献者 | 3.1.3，tag `v3.1.3-1.21`，commit [`68c5b878f4d6dea3ef6ab4743a7ffbfae43f7c92`](https://github.com/ModularMCLib/DataEnergistics/tree/68c5b878f4d6dea3ef6ab4743a7ffbfae43f7c92) | [`PatternEncodingSourceHelper`](https://github.com/ModularMCLib/DataEnergistics/blob/68c5b878f4d6dea3ef6ab4743a7ffbfae43f7c92/src/main/java/com/fish_dan_/data_energistics/menu/patternencoding/source/PatternEncodingSourceHelper.java)；代码 [AGPL-3.0-or-later](https://github.com/ModularMCLib/DataEnergistics/blob/68c5b878f4d6dea3ef6ab4743a7ffbfae43f7c92/LICENSE)，资源另依 [LICENSE.RESOURCE](https://github.com/ModularMCLib/DataEnergistics/blob/68c5b878f4d6dea3ef6ab4743a7ffbfae43f7c92/LICENSE.RESOURCE) 保留所有权利。没有使用其资源 |

## Gradle Wrapper：随源码分发的构建工具

`gradlew`、`gradlew.bat`、`gradle/wrapper/gradle-wrapper.jar` 来自 [Gradle 8.14.3](https://github.com/gradle/gradle/tree/v8.14.3)，采用 [Apache License 2.0](licenses/Apache-2.0.txt)。脚本保留原作者版权和许可头，Wrapper JAR 保留其 `META-INF/LICENSE`。这三个文件继续按 Apache-2.0 分发，不改为本项目的 LGPL。

Gradle 完整发行版和各构建依赖由构建工具另行下载；本仓库不捆绑这些发行版。对应依赖仍受各自许可证约束。

## 源码取得与分发

本模组对应源码与构建脚本公开于 <https://github.com/Misaka2592/AE2ByproductRemover>。分发修改版或二进制时，应遵守 LGPL-3.0-or-later 对许可证、版权声明、修改说明及对应源码的要求；不能用本项目许可证替代上述第三方内容自身的条款。发布 JAR 随附本文件、LGPL/GPL 全文和第三方许可证副本。
