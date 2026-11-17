# MarkNote

IntelliJ 平台的轻量代码阅读笔记插件：在代码行上就地添加扁平笔记，随锚点自动跟踪，不打断阅读节奏。

## 功能

- **行内笔记**：在编辑器中选中文本，右键 `添加笔记`，笔记挂在锚点文本上
- **行号区标记**：有笔记的行在 gutter 显示图标，点击可查看
- **行尾短预览**：笔记首行以行尾提示（inlay）形式展示
- **锚点跟踪**：锚点文本移动或编辑时，笔记自动跟随（`NoteAnchorService` / `NoteDocumentTracker`）
- **Code Notes 工具窗**：底部面板集中浏览、删除所有笔记
- **重绑锚点**：笔记失锚后可重新绑定到其他文本

## 使用

1. 编辑器里选中一段代码 → 右键 → `添加笔记`（Add Note）
2. 在弹窗里写笔记内容并确认
3. 行号区图标 / 行尾预览随时查看；工具窗 `Code Notes` 管理全部笔记

## 开发

```bash
./gradlew buildPlugin      # 构建插件
./gradlew runIde           # 启动沙盒 IDE 调试
./gradlew test             # 运行测试
```

技术栈：Kotlin + IntelliJ Platform Plugin SDK（仅依赖 `com.intellij.modules.platform`），笔记持久化于 `.idea` 目录下的项目级存储。

## 许可

balaraking 版权所有。
