# 处理样板产物平等化

状态：已完成实现，并通过指定 AE2 基线的自动化测试。

## 范围

本 `master` 分支以 GTNH 2.9.0beta3 中相关功能为参考，在 Minecraft 1.21.1 / NeoForge 的 AE2 中实现以下行为。处理样板的所有产物均可下单；调整合成计算逻辑，并删除编码界面的主副产物区分。Forge 1.20.1 独立维护于 [`1.20.1` 分支](https://github.com/Misaka2592/AE2ByproductRemover/tree/1.20.1)。

## 已确定的行为

- 每一种处理样板产物均可作为下单目标。
- 默认采用独立计算模式（Lite 语义），同时提供配置切换到产物复用模式（V2 语义）的能力。
- 对于 `1A → 1B + 1C`、`1B + 1C → 1D`，初始只有 A 时，下单 1D 在独立计算模式消耗 2A，在产物复用模式消耗 1A；初始已有 1C 时，两种模式均只需 1A。
- 执行阶段保留已提交计划的加工次数，不动态减单。上述独立计算场景中，即使第一次加工返回的 B、C 已足够制作 D，仍执行第二次加工；额外真实产物正常入库。
- 同种目标产物的批量余量可在同一计划中复用。对于 `1A → 2B + 1C`，两条分支各需 1B 时，一次加工即可满足，共消耗 1A。
- 对于 `1A → 1B + 1C`，仅调用 B 的合成时，足量 B 返回即可完成任务；C 不得出现在合成界面的“合成中”状态。
- 编码界面删除输出槽及相关提示中的主产物、副产物标识。
- 输出轮换按钮用于轮换产物排列；排列不赋予产物不同身份。
- 编码允许第一输出槽为空；存在有效产物即可满足输出非空条件。
- 编码时压紧全部输出空槽，保持有效产物的相对顺序。例如 `[空, B, 空, C, 空, 空, D]` 编码为 `[B, C, D]`。
- 模式由世界的服务端配置统一控制，默认独立计算；重新加载世界或重启服务器后，后续新计划使用新值。已生成计划与运行中任务继续按原计划执行。
- 配置只切换预计产物是否参与原料计算。两种模式都保留任意产物可下单、输出平等的界面，以及下述完成与状态规则。
- 只下单 B 且 C 在任务中无用途时，C 不列为“已计划”或“合成中”，实际回收仍按网络正常存储；如果 C 被复用为中间原料，显示任务实际需要的 C。

## 条件决定

- 已编码处理样板在不存在兼容问题的前提下直接获得新行为，无须重新编码。AE2 19.2.17 的存储与解析路径已核查，未发现要求迁移格式的障碍；旧有效样板的实际行为列入实现验收。

## 实现与验收版本

“V2 语义”在本项目中指预计产物可复用，不自动包含 GTNH 整套 V2 规划器。

以指定整合包的实际发布清单为依据，本分支基线为官方 AE2 **19.2.17**；构建使用 NeoForge **21.1.250**，声明支持 21.1.241–21.1.x。

| 参考整合包 | 核查的发布版本 | 加载器 | AE2 |
| --- | --- | --- | --- |
| New Age Science and Technology | 2.0，正式版 | NeoForge 21.1.243 | 19.2.17 |
| All the Mods 10 | 8.1，正式版 | NeoForge 21.1.249 | 19.2.17 |
| All the Mods 10 To the Sky | 2.0.4，正式版 | NeoForge 21.1.250 | 19.2.17 |
| Infinity Legacy II | 4.4，正式版 | NeoForge 21.1.248 | 19.2.17 |
| Creative Drawers Producer 2 | Beta v1.7，当前公开版本均为 Beta | NeoForge 21.1.241 | 19.2.17 |

对应发布清单及来源：[NAST](references/pack-nast.md)、[ATM10 与 ATM10Sky](references/pack-atm.md)、[Infinity Legacy II 与 CDP2](references/pack-infinity-drawers.md)。这些整合包用于确定 AE2 版本参考。

源码基线：`neoforge/v19.2.17` → `79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a`。

## 验收场景

P 表示处理样板，所有未特别注明的数量均为 1。

| 场景 | 预期 |
| --- | --- |
| P：A→B+C，分别请求 B 与 C | 两种产物均可下单，输出轮换不改变这一资格 |
| 原生 AE2 规划：P：A→B+C，高优先级 Q：B→10C，库存 1A，请求 10C；轮换 P 的两个输出 | 独立计算下两种排列均可用 P、Q 各一次完成，消耗 1A；复用模式保留完整输出的递归检查，两种排列均无法用现有库存完成 |
| P：A→B+C，Q：B→A，无启动库存，请求 B | 两种模式、两种输出排列均不能凭空启动循环 |
| P：A→B+C，Q：B+C→D，初始只有 A | 独立计算模式计划消耗 2A；复用模式计划消耗 1A |
| 上述独立计算计划中，第一次加工已返回 B、C | 仍执行已规划的第二次加工，不因 D 提前可制作而动态减单；额外真实产物正常入库 |
| P：A→B+C，Q：B+C→D，初始已有 A 和 1C | 两种模式均只需 1A |
| P：A→B+C，仅请求 B，C 不参与任何后续需求 | C 不显示已计划或合成中；B 到齐即完成；实际返回的 C 正常入库 |
| P：A→B+C，复用模式用 C 继续加工 D | 状态显示实际需要的 C，D 的加工正常等待并使用它 |
| P：A→2B+3C，仅请求 5B | 按 B 的产量规划三次加工；C 不抵扣其他原料、不成为任务完成条件 |
| P：A→2B+C，同一计划两条分支各需 1B | 同种产物余量复用，一次加工、消耗 1A 即可满足两条分支 |
| 有效输出含物品与流体 | 每一种均可下单，并遵守相同的模式、完成及状态规则 |
| 输出为 [空,B,空,C,空,空,D] | 编码成功；重新读取样板显示 [B,C,D]，数量与相对顺序保留 |
| 输出全空 | 无有效产物，不能编码 |
| 原版本已编码的有效多输出处理样板 | 保持原存储格式，直接取得新下单资格，无须重新编码 |
| 任务执行中保存退出，修改模式后重新加载 | 旧任务恢复原计划与进度，新计划采用新的配置值 |

## 实现涉及的行为边界

产物注册、原料规划、CPU 预计回收与任务状态、编码及界面必须表达同一套规则。仅改变主输出访问方法或隐藏界面标识，无法满足全部验收场景。实际机器加工仍执行完整配方，未被当前任务使用的真实产物走正常存储流程。

目标产物提前到齐不能导致已规划但尚未发送的加工被自动取消；同时，额外产物不成为任务的完成条件。保存与恢复任务时也应保持已确认的加工次数与进度。该取舍记录于 [ADR-0001](adr/0001-preserve-planned-processing.md)。

本项目使处理样板中保留的各项产物均具备主产物的下单能力，并按配置控制预计产物是否参与原料计算；不负责消除副产物带来的所有调度影响。涉及容器返还、副产物回流及循环样板的加工链，需要由玩家合理设计样板与物流。因此，返还容器造成中间产物等待或派发受阻的问题不纳入修复范围。

原生 AE2 的候选样板在递归检查时使用与加工节点一致的输出视图：独立计算仅包含目标产物，复用模式包含完整输出。保留原生的输入与祖先递归检查，不因输出位于首槽或其他槽而改变检查范围。参考依据见 [GTNH 的目标输出与防环策略](references/gtnh-reference.md#目标输出与防环策略)。

## 后续计划

- GTNH V2 防环策略兼容：覆盖 `master`（NeoForge 1.21.1）及 `1.20.1`（Forge 1.20.1）。以祖先样板判重等成熟实现为参考，研究复用模式下如何允许上述 P→Q 路径，同时保留真正循环的防护。当前仅支持预计产物复用，仍沿用现代 AE2 的保守防环规则；后续设计需单独评估和验收，不包含上述容器返还问题。

## 实现验证

- NeoForge 21.1.250 / AE2 19.2.17：26 项测试通过，覆盖输出压紧、真实编码菜单、真实 AE2 规划、CPU 派发与存档恢复；包含输出排列一致性、复用模式保守检查及真正输入循环的回归场景。
- NeoForge 客户端已验证编码界面与菜单 Mixin 的加载和注入。

构建、运行测试与配置方法见 [README](../README.md)。

## 0.1.1 附属兼容

用户在 NAST HARD 0.9.6 中确认了两个问题：首产物请求仍列出其他产物，以及首槽为空无法编码、编码后仍保留空槽。实际调用分别经过 Thunderbolt 快速规划和 Data Energistics 外层重编码，原生 AE2 测试未覆盖这些入口。

- Thunderbolt Core 1.0.6：在真实快速规划建图时应用请求目标与模式快照，在生成计划时过滤无用途产物。首产物与其他产物下单保持相同语义，保留快速规划器。
- AdvancedAE 1.6.12、Neo ECO AE Extension 21.1.1：使用共用规划入口；接入各自独立 CPU 的派发、完成判断和存档恢复，保留输出视图与已确认加工次数。
- Data Energistics 3.1.3：在其自定义资源解包后压紧输出，使首槽检查和最终写入使用同一份有效输出序列。
- AdvancedAE 高级处理样板的编码保护：在处理模式内重新编码时保留高级类型和仍存在的输入资源的方向；数量变化、翻倍和槽位移动不改变方向归属。删除输入移除对应方向，新增输入未指定方向；全部原输入被替换后仍生成高级样板。输出空槽照常压紧。主动切换到其他编码模式时遵循所选模式。此保护不包含高级样板的任意产物下单或 Lite／复用计算支持。
- 上述兼容只在对应模组存在时启用。附属模组不包含在发布 JAR 中。

加载这些真实附属 JAR 后，共 60 项测试通过：基础测试 26 项、Thunderbolt 7 项、Data Energistics 6 项、AdvancedAE／Neo ECO CPU 共 12 项、AdvancedAE 类型与方向编码保护 9 项。修复前的回归测试分别复现了规划与重编码缺陷。

## GTNH 参考结论

详见 [GTNH 固定版本研究](references/gtnh-reference.md)。官方 2.9.0-beta-3 清单锁定 AE2 `rv3-beta-1050-GTNH`，commit `f9f49159899bdcdbf88a341e22b159cd58a61585`。

- 固定源码的普通 V2 规划路径可用预计的另一产物抵扣需求，上述例子消耗 1A；Lite 只展开当前所需产物，上述例子消耗 2A。
- 初始网络已有 1C 时，两种模式均只需 1A。
- 两种模式检查到的普通 CPU 执行路径均将 C 纳入等待、状态展示及最终样板完成目标。因此本项目已确定的“仅请求 B 时，B 到齐即完成且 C 不显示合成中”并非照搬上述路径。
- 已核查 AE2 核心与清单内 AE2FluidCraft 的普通流体样板；尚未定位到与最初描述完全相同的功能入口。

## 已核查的现代 AE2 事实

以下证据固定于 NeoForge commit `fd8b717a405672ce4f65ba540f1db8c91317daa4`。AE2 19.2.17 与该 commit 之间的[三次后续提交](https://github.com/AppliedEnergistics/Applied-Energistics-2/compare/79ee2c704ad62941a426c26b1cb1f76ef5b2ee5a...fd8b717a405672ce4f65ba540f1db8c91317daa4)未改变这里涉及的代码，因此下列结论适用于本分支基线。

- 样板保存完整输出序列，未单独保存主副身份，旧有效处理样板无须迁移格式。[数据组件](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/fd8b717a405672ce4f65ba540f1db8c91317daa4/src/main/java/appeng/crafting/pattern/EncodedProcessingPattern.java#L17)。
- 运行时去空并合并同种产物，编辑与持久化序列仍保留空槽。编码压紧只删除空槽，保留数量与相对顺序。[运行时整理](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/fd8b717a405672ce4f65ba540f1db8c91317daa4/src/main/java/appeng/crafting/pattern/AEPatternHelper.java#L21)。
- 菜单与编码 API 要求首个输出非空，压紧必须在检查前完成；全空输出仍无效。[编码菜单](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/fd8b717a405672ce4f65ba540f1db8c91317daa4/src/main/java/appeng/menu/me/items/PatternEncodingTermMenu.java#L372)。
- “合成中”来自 CPU 输出等待清单，“已计划”包含待执行样板输出，必须同时过滤无用途产物。[等待清单](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/fd8b717a405672ce4f65ba540f1db8c91317daa4/src/main/java/appeng/crafting/execution/CraftingCpuLogic.java#L222)、[状态数据](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/fd8b717a405672ce4f65ba540f1db8c91317daa4/src/main/java/appeng/menu/me/crafting/CraftingStatus.java#L135)、[待执行输出](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/fd8b717a405672ce4f65ba540f1db8c91317daa4/src/main/java/appeng/crafting/execution/CraftingCpuLogic.java#L492)。
- 存档恢复会重新解码样板并恢复剩余次数与等待清单，不重新规划；本项目需保存原输出视图，避免配置变化重新解释旧任务。[任务恢复](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/fd8b717a405672ce4f65ba540f1db8c91317daa4/src/main/java/appeng/crafting/execution/ExecutingCraftingJob.java#L93)。
