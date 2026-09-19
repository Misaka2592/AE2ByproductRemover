# AE2 Byproduct Remover — Forge 1.20.1

使 AE2 处理样板的每一种产物都可下单，并控制合成计划是否复用其他预计产物。

项目代码采用 **LGPL-3.0-or-later**，见 [LICENSE](LICENSE)。编码逻辑改写自 Applied Energistics 2，计算语义参考 GTNH 的 AE2 Lite／V2 实现；具体版本、上游作者和源码位置见 [代码来源与第三方声明](THIRD_PARTY_NOTICES.md)。Gradle Wrapper 保留其 Apache-2.0 许可证。

| 安装文件 | Minecraft / 加载器 | AE2 |
| --- | --- | --- |
| `ae2byproductremover-forge-0.1.1.jar` | 1.20.1 / Forge 47.4.16 | 15.4.10 |

将 JAR 放入客户端与服务端的 `mods` 目录，同时安装 AE2 15.4.10 及其前置 GuideME 20.1.7。已有有效处理样板保留原存储格式，无须重新编码。

本分支 `1.20.1` 独立维护 Forge 版；NeoForge 1.21.1 版及其附属模组兼容见 [`master` 分支](https://github.com/Misaka2592/AE2ByproductRemover/tree/master)。两个分支均在根目录独立构建。

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

## 构建与测试

需要 JDK 17 编译和运行 Minecraft。Gradle 可使用 JDK 17 或 JDK 21 启动；使用 JDK 21 时，可设置 `JAVA17_HOME` 指向 JDK 17，供工具链定位。

```powershell
.\gradlew.bat build
```

发布产物为 `build/libs/ae2byproductremover-forge-0.1.1.jar`。构建包含输出压紧的 4 项单元测试；真实 AE2 规划、产物注册、CPU 派发与存档恢复使用 3 项 GameTest：

```powershell
.\gradlew.bat runGameTestServer
```

验证客户端编码界面与菜单 Mixin 加载后自动退出：

```powershell
.\gradlew.bat runClientSmoke
```

代码位于 `src/main/`，单元测试位于 `src/test/`，GameTest 与客户端冒烟入口位于 `src/gametest/`。设计及验收场景见 [规格](docs/specification.md)，版本依据见 [GregTech Leisure 发布清单](docs/references/pack-leisure.md)。
