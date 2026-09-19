# AE2 Byproduct Remover

使 AE2 处理样板的每一种产物都可下单，并控制合成计划是否复用其他预计产物。

项目代码采用 **LGPL-3.0-or-later**，见 [LICENSE](LICENSE)。编码逻辑改写自 Applied Energistics 2，计算语义参考 GTNH 的 AE2 Lite／V2 实现；具体版本、上游作者、源码位置和兼容接口来源见 [代码来源与第三方声明](THIRD_PARTY_NOTICES.md)。Gradle Wrapper 保留其 Apache-2.0 许可证。

| 安装文件 | Minecraft / 加载器 | AE2 |
| --- | --- | --- |
| `ae2byproductremover-forge-0.1.2.jar` | 1.20.1 / Forge 47.4.16 | 15.4.10 |
| `ae2byproductremover-neoforge-0.1.2.jar` | 1.21.1 / NeoForge 21.1.241–21.1.x | 19.2.17 |

将对应平台的 JAR 放入客户端与服务端的 `mods` 目录，同时安装对应 AE2。Forge 版还需要 AE2 的前置 GuideME 20.1.7；NeoForge 的 AE2 已内置 GuideME。已有有效处理样板保留原存储格式，无须重新编码。

## 计算规则

默认采用独立计算：同一加工中，只有当前所需种类的预计产物参与计算。其他种类不会抵扣后续原料；已有网络库存与同种目标产物的批量余量仍可使用。

例如 `A → B + C`、`B + C → D`，网络只有 A 时，下单 D 会规划两次加工，消耗 2A。即使第一次实际返回的 B、C 已足够制作 D，也仍派发第二次加工，不动态减单。额外真实产物正常入库。

世界配置位于 `<世界目录>/serverconfig/ae2byproductremover-server.toml`：

```toml
useByproducts = false
```

设为 `true` 后允许复用其他预计产物，上述例子只需 1A。修改配置后重新加载世界或重启服务器；新计划采用新设置，已提交任务保留原计划与进度。这两个选项对应 Lite／产物复用的计算语义。

两种模式都将无用途的其他产物排除在任务的“已计划”“合成中”状态及完成条件之外。参与后续加工的产物正常显示；数量按样板的整批产量记账。请求产物到齐且全部计划加工已派发后完成任务。

## 编码界面

处理样板界面移除主副产物标识，原按钮循环轮换产物顺序。编码允许首个输出槽为空，并稳定压紧输出空槽，例如 `[空,B,空,C,空,D] → [B,C,D]`，保留顺序和数量。全空输出仍不能编码。

## NeoForge 附属模组兼容

以下兼容会在对应模组存在时自动启用：

| 模组 | 验证版本 | 接入行为 |
| --- | --- | --- |
| AdvancedAE | 1.6.12 | 独立 CPU 的样板派发、完成条件和任务存档 |
| Neo ECO AE Extension | 21.1.1 | 独立 CPU 的样板派发、完成条件和任务存档 |
| Thunderbolt Core | 1.0.6，NAST HARD 0.9.6 所带版本 | 快速规划中的 Lite／复用计算，以及预览和执行计划的产物过滤 |
| Data Energistics | 3.1.3 | 重编码时压紧输出，允许首槽为空，保留自定义资源及数量处理 |

AdvancedAE 与 Neo ECO 共用 AE2／Thunderbolt 的规划入口，兼容同时覆盖它们后续执行和存档恢复的独立实现。

## 构建

需要 JDK 17 和 JDK 21；Gradle 用 JDK 21 启动。可分别设置 `JAVA17_HOME`、`JAVA21_HOME` 供工具链定位。

```powershell
.\gradlew.bat build
```

产物分别位于 `forge/build/libs/` 与 `neoforge/build/libs/`。

构建时仅编译引用 AdvancedAE 与 Neo ECO，不将它们打包进本模组。测试上述附属兼容时，指定含对应版本及其前置模组的 `mods` 目录：

```powershell
.\gradlew.bat :neoforge:test -PcompatTests=true "-PcompatModsDir=D:\path\to\instance\mods"
```

该测试直接加载整合包中的模组 JAR，包含 Thunderbolt 快速规划、Data Energistics 重编码，以及两个附属 CPU 的实际执行与存档入口。

`build` 包含两个平台的输出压紧测试，以及 NeoForge 加载真实 AE2 和 Mixin 后的编码、规划、执行与存档测试。Forge 的运行测试使用 GameTest：

```powershell
.\gradlew.bat :forge:runGameTestServer
```

验证两个客户端的界面与编码 Mixin 加载后自动退出：

```powershell
.\gradlew.bat :forge:runClientSmoke :neoforge:runClientSmoke
```

设计及验收场景见 [.scratch/processing-outputs/spec.md](.scratch/processing-outputs/spec.md)。
