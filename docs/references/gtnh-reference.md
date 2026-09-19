# GTNH 2.9.0-beta-3：多输出处理样板参考

## 结论

固定版本的 **AE2 普通处理样板**已允许任意输出下单，但仍计算其他产物，并等待最终样板的所有输出；这与本项目要求的“只规划、等待、显示当前请求输出”不同。可选 Lite 规划器忽略其他输出带来的原料抵扣，但仍沿用完整输出的 CPU 执行与显示逻辑。不能把 Lite 的规划行为等同于完整的“去除副产物”功能。

以下是固定源码的静态推导，针对普通 ME 接口上的正常处理样板、常规合成请求；不适用于 fake crafting 等特殊执行模式。本次有界检查了 AE2 核心及清单内 AE2FluidCraft 的普通流体样板路径，未定位到与用户描述完全相同的功能；这不等于证明整合包其他组件不存在该功能。

## 版本来源

- 官方整合包 tag 为 [`2.9.0-beta-3`](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/tree/27e61fbea6e245df80839885460994aa0ef9da9f)，指向 `27e61fbea6e245df80839885460994aa0ef9da9f`。
- 官方发布清单 [`releases/manifests/2.9.0-beta-3.json`](https://github.com/GTNewHorizons/DreamAssemblerXXL/blob/2b7c6aec620860748110e56640e4b1ee6f0274fd/releases/manifests/2.9.0-beta-3.json#L27-L29) 将 AE2 锁定为 `rv3-beta-1050-GTNH`；[该 tag](https://api.github.com/repos/GTNewHorizons/Applied-Energistics-2-Unofficial/git/ref/tags/rv3-beta-1050-GTNH) 指向 `f9f49159899bdcdbf88a341e22b159cd58a61585`。下文 AE2 引用均使用这个 commit。
- [同一清单第 11–13 行](https://github.com/GTNewHorizons/DreamAssemblerXXL/blob/2b7c6aec620860748110e56640e4b1ee6f0274fd/releases/manifests/2.9.0-beta-3.json#L11-L13) 锁定 `AE2FluidCraft-Rework 1.5.106-gtnh`；[该 tag](https://api.github.com/repos/GTNewHorizons/AE2FluidCraft-Rework/git/ref/tags/1.5.106-gtnh) 指向 `31746024ee5482959f882fdadfe050d56427bd28`。

## Q1：A → B + C；B + C → D

假设每个数量均为 1，只有这两个样板，网络初始无 B、C，A 足够：

| 模式 | 下单 1D 的规划 | 原因 |
| --- | --- | --- |
| 普通 V2 | 执行 A→B+C 一次，消耗 **1A** | 先解析 B 时，把 C 注入规划用副产物库存；后续 C 需求从该库存提取。先解析 C 也对称。 |
| Lite | 执行 A→B+C 两次，消耗 **2A** | B、C 各自独立展开输入；对其他产物不建立可抵扣库存。若初始只有 1A，计划会缺少 1A。 |

普通 V2 的证据链：

1. [`CraftingGridCache.setPatternsFromCraftingMethods()` 第 337–361 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cache/CraftingGridCache.java#L337-L361) 对每一个输出登记同一个样板，因此 B、C 均可下单。
2. [`CraftableItemResolver` 第 117–142 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/v2/resolvers/CraftableItemResolver.java#L117-L142) 从完整输出列表找匹配当前请求的输出；[第 376–383 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/v2/resolvers/CraftableItemResolver.java#L376-L383) 把所有其他输出加入 `byproductsInventory`。
3. [`CraftingContext.doWork()` 第 341–358 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/v2/CraftingContext.java#L341-L358) 及[队列插入第 410–423 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/v2/CraftingContext.java#L410-L423) 保持输入顺序，先完整解析前一个子请求。
4. [`ExtractItemResolver` 第 56–70 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/v2/resolvers/ExtractItemResolver.java#L56-L70) 先从副产物库存提取，再从真实库存模型提取，因此第二种中间产物不用再启动一次 P。

Lite 的证据链：[`CraftingJobFast` 第 149–185 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/fast/CraftingJobFast.java#L149-L185) 先提取当前物品的现有库存；不足时按当前输出数量计算倍率，累加该样板输入及执行次数，只消除当前物品缺口。不会用同次预计产出的另一种物品抵扣需求。

**如果网络初始已有 1C：**两种规划器都只需 1A。Lite 会使用现有 C；普通 V2 优先使用预计副产物，因此若先处理 B，会用新增预计 C 而保留现有 C；若先处理 C，则会使用现有 C，并留下加工 B 所产生的额外 C。这一区别来自上述提取顺序。

### Lite 不是整合包强制默认

[`CraftingGridCache` 第 151–152 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cache/CraftingGridCache.java#L151-L152) 的网络默认值为 `false`，它可随网络存档恢复；[第 628–646 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cache/CraftingGridCache.java#L628-L646) 根据 `liteMode` 选择 `CraftingJobFast` 或 `CraftingJobV2`。[`AEConfig` 的客户端初值也为 false](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/core/AEConfig.java#L106)，读取键为 [`Client/isLiteCraftingEnabled`](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/core/AEConfig.java#L431-L432)。[该整合包的 AE2 配置](https://github.com/GTNewHorizons/GT-New-Horizons-Modpack/blob/27e61fbea6e245df80839885460994aa0ef9da9f/config/AppliedEnergistics2/AppliedEnergistics2.cfg) 未设置这个 Lite 键。玩家的客户端设置及既有网络存档可能不同，不能把任一模式外推为所有存档的行为。

## Q2：只下单 B，C 是否进入状态与完成条件

**在检查的普通路径中，C 会显示合成状态，也影响完成；这部分与用户要求不一致。**

- CPU 推送样板后，[第 986–996 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cluster/implementations/CraftingCPUCluster.java#L986-L996) 把完整输出列表加入 `waitingFor` 并发送状态变化。
- [第 1460–1473 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cluster/implementations/CraftingCPUCluster.java#L1460-L1473) 从 `waitingFor` 构造 `ACTIVE` 状态，待执行列表也枚举所有输出。因此 C 会出现在合成中／待合成清单。
- [`finalOutput.addOutputs()` 第 2323–2354 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cluster/implementations/CraftingCPUCluster.java#L2323-L2354) 找到最终输出 B 对应的样板后，把该样板的所有输出加入完成目标。收回 B 后，[第 443–457 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cluster/implementations/CraftingCPUCluster.java#L443-L457) 仅在该目标列表为空时完成，所以 C 未返回会阻止完成。
- Lite 的[第 259–262 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/fast/CraftingJobFast.java#L259-L262) 仍把原样板交给相同 CPU；其计划界面[第 294–298 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/fast/CraftingJobFast.java#L294-L298) 也枚举原样板的所有输出。故 Lite 并未满足“C 不显示”的要求。

### 真实 C 返回

普通规划中，C 如果是中间原料，会由 CPU 接收进内部库存，见 [`injectItems()` 第 465–467 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cluster/implementations/CraftingCPUCluster.java#L465-L467)。只下单 B 时，C 被当作最终样板输出处理；玩家独立请求没有机器接收者时，[`CraftingLink.injectItems()` 第 145–150 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/crafting/CraftingLink.java#L145-L150) 返回该物品，让插入继续交给存储路径。没有相应等待项时，[CPU 第 519 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cluster/implementations/CraftingCPUCluster.java#L519) 原样返回输入；[CraftingGridCache 第 500–507 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/me/cache/CraftingGridCache.java#L500-L507) 继续返回未接收部分。此逻辑不会删除真实产物；最终是否存入网络仍取决于实际导入及可用存储。

## 样板是否被拆分或包装

普通 ME 接口[读取样板第 528–537 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/helpers/DualityInterface.java#L528-L537)，并在[第 1460–1465 行](https://github.com/GTNewHorizons/Applied-Energistics-2-Unofficial/blob/f9f49159899bdcdbf88a341e22b159cd58a61585/src/main/java/appeng/helpers/DualityInterface.java#L1460-L1465) 原样注册该 details。上述核心路径没有为各输出创建独立样板视图。

固定版本 AE2FluidCraft 的 [`FluidPatternDetails` 第 131–170 行](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/31746024ee5482959f882fdadfe050d56427bd28/src/main/java/com/glodblock/github/util/FluidPatternDetails.java#L131-L170) 也返回完整输出；`setOutputs()` 过滤空输出但保留全部非空类型。[第 248–259 行](https://github.com/GTNewHorizons/AE2FluidCraft-Rework/blob/31746024ee5482959f882fdadfe050d56427bd28/src/main/java/com/glodblock/github/util/FluidPatternDetails.java#L248-L259) 读取的附加设置为 `beSubstitute` 与 `combine`，未在这条普通样板解码路径发现去副产物的开关或按请求过滤。

## 对本项目大纲的影响

- 项目已采用默认 2A 的 Lite 计算语义，并提供配置切换到 1A 的预计产物复用语义；具体行为见 [规格](../specification.md)。
- 用户已要求 Q2 中 B 足量返回即完成、C 不显示合成中；这是明确的项目要求，但不能声称上述 GTNH 普通／Lite 路径已经实现它。规划、CPU 等待清单、状态展示及最终输出完成条件必须一致地排除本次未请求的输出。
- 要进一步对应用户所指的小功能，需要其在 GTNH 中的具体名称或入口；现有证据足够说明上述两种已定位模式，暂不扩大到其他附属模组。
