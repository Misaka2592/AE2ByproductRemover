# 处理样板产物平等化

状态：已完成实现，并通过指定 AE2 基线的自动化测试。

## 范围

以 GTNH 2.9.0beta3 中相关功能为参考，在 Minecraft Forge 1.20.1 的 AE2 中实现以下行为。处理样板的所有产物均可下单；调整合成计算逻辑，并删除编码界面的主副产物区分。

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

- 已编码处理样板在不存在兼容问题的前提下直接获得新行为，无须重新编码。本分支 AE2 基线版本的存储与解析路径已核查，未发现要求迁移格式的障碍；旧有效样板的实际行为列入实现验收。

## 实现与验收版本

“V2 语义”在本项目中指预计产物可复用，不自动包含 GTNH 整套 V2 规划器。

以 GregTech Leisure 的实际发布清单为依据，本分支 Forge 1.20.1 基线为官方 AE2 **15.4.10**。

| 参考整合包 | 核查的发布版本 | 加载器 | AE2 |
| --- | --- | --- | --- |
| GregTech Leisure | 1.4.5.0，公开关联下载目录最新未标记预发布版本 | Forge 47.4.16 | 15.4.10 |

对应发布清单及来源：[GregTech Leisure](references/pack-leisure.md)。该整合包用于确定 AE2 版本参考。

源码基线：`forge/v15.4.10` → `b4b08d9941e3faecb520d76be617629bb56661e1`。

## 验收场景

以下为本分支的验收场景；P 表示处理样板，所有未特别注明的数量均为 1。

| 场景 | 预期 |
| --- | --- |
| P：A→B+C，分别请求 B 与 C | 两种产物均可下单，输出轮换不改变这一资格 |
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

## 实现验证

- Forge 47.4.16 / AE2 15.4.10：4 项输出压紧测试、3 项真实世界 GameTest 通过，覆盖产物注册、2A／1A 规划、无用产物过滤、双目标任务恢复和保留加工次数。
- Forge 版已在真实客户端环境验证编码界面与菜单 Mixin 的加载和注入。

构建、运行测试与配置方法见 [README](../README.md)。

## GTNH 参考结论

详见 [GTNH 固定版本研究](references/gtnh-reference.md)。官方 2.9.0-beta-3 清单锁定 AE2 `rv3-beta-1050-GTNH`，commit `f9f49159899bdcdbf88a341e22b159cd58a61585`。

- 固定源码的普通 V2 规划路径可用预计的另一产物抵扣需求，上述例子消耗 1A；Lite 只展开当前所需产物，上述例子消耗 2A。
- 初始网络已有 1C 时，两种模式均只需 1A。
- 两种模式检查到的普通 CPU 执行路径均将 C 纳入等待、状态展示及最终样板完成目标。因此本项目已确定的“仅请求 B 时，B 到齐即完成且 C 不显示合成中”并非照搬上述路径。
- 已核查 AE2 核心与清单内 AE2FluidCraft 的普通流体样板；尚未定位到与最初描述完全相同的功能入口。

## 已核查的 AE2 15.4.10 事实

以下原始证据固定于 Forge commit `b4b08d9941e3faecb520d76be617629bb56661e1`。

- 存储完整输出序列，未单独保存主副身份。旧有效处理样板可表达所需输出，无须迁移样板格式。[存储格式](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/crafting/pattern/ProcessingPatternEncoding.java)。
- 原版运行时会去空并合并同种产物，但编辑与持久化序列仍保留空槽。编码压紧只删除空槽，保留各有效槽的数量与相对顺序。[运行时整理](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/crafting/pattern/AEPatternHelper.java)。
- 菜单与下层编码 API 都要求首个输出非空；稳定压紧发生在这些检查之前，全空输出仍属无效。[编码菜单](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/menu/me/items/PatternEncodingTermMenu.java)。
- 合成状态界面的“合成中”数量来自 CPU 输出等待清单，待执行样板的全部输出还会计入“已计划”。因此只修改模拟原料计算不足以满足 C 不显示“合成中”的要求。[预计输出](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/crafting/execution/CraftingCpuHelper.java)、[CPU 状态](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/crafting/execution/CraftingCpuLogic.java)。
- 计划保存所需库存及样板次数；CPU 接收该计划，恢复存档时重新解码样板并恢复剩余次数与等待清单，不重新规划，也没有原生的本项目模式字段。配置变更不能使旧计划或恢复中的任务按新模式重新解释。[计划](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/crafting/CraftingPlan.java)、[任务恢复](https://github.com/AppliedEnergistics/Applied-Energistics-2/blob/b4b08d9941e3faecb520d76be617629bb56661e1/src/main/java/appeng/crafting/execution/ExecutingCraftingJob.java)。
