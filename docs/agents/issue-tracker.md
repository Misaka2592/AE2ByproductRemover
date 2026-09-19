# Issue tracker: Local Markdown

事项与规格存放于 `.scratch/`。

- 每个功能一个目录：`.scratch/<feature-slug>/`。
- 规格文件：`.scratch/<feature-slug>/spec.md`。
- 每个实施事项一个文件：`.scratch/<feature-slug>/issues/<NN>-<slug>.md`，从 01 编号。
- 分诊状态写在文件顶部的 `Status:` 行；取值见 `triage-labels.md`。
- 讨论记录追加至文件底部的 `## Comments`。

技能要求发布事项时，在上述位置创建对应文件。
技能要求读取事项时，读取用户引用的文件；使用编号时先定位对应功能目录。
