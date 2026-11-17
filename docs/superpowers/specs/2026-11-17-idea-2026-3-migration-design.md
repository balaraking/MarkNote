# IntelliJ IDEA 2026.3 迁移设计

## 意图

让 MarkNote 使用 IntelliJ IDEA 2026.3 对应的 IntelliJ Platform 263 构建和运行，同时保留现有扁平代码笔记功能。

## 目标

- 将旧 `org.jetbrains.intellij` 1.x 构建迁移到 `org.jetbrains.intellij.platform` 2.x。
- 目标平台固定为 IntelliJ IDEA 2026.3，插件最低兼容构建号为 `263`，允许后续平台版本继续安装。
- 使用 Java 25 工具链，并升级到适配 2026.3 的 Kotlin 2.x。
- 添加可复现的 Gradle Wrapper。
- 配置 IntelliJ Platform 测试框架、JUnit 5 单测和现有 `BasePlatformTestCase` 集成测试。
- 保留现有 plugin ID、独立 `CodeReadingNoteLite.xml` 存储和旧 XML 隔离行为。

## 非目标

- 不改动笔记领域行为、锚点算法、编辑器 UI 或持久化模型。
- 不迁移旧插件数据。
- 不在本次迁移中同时支持 2023.2 或其他旧 IDE 分支。

## 约束

- IntelliJ IDEA 2026.3 对应 Platform branch 263，运行时 Java 25。
- IntelliJ Platform Gradle Plugin 2.x 使用新插件 ID 和新 `intellijPlatform {}` DSL，最低 Gradle 9.0。
- Gradle 使用 Foojay resolver 自动供应缺失的 Java 25 toolchain，避免依赖开发机的固定 JDK 路径。
- 2026.3 目标平台需要显式声明 Java bundled plugin 和 Platform test framework。
- Kotlin 标准库使用 IntelliJ 平台自带版本，避免把外部 stdlib 重复打包进插件。

## 方案

### 方案 A：完整迁移到 Platform Gradle Plugin 2.x（采用）

使用 `org.jetbrains.intellij.platform` 2.20.1，目标 `intellijIdea("2026.3")`，在 `dependencies` 中声明 `bundledPlugin("com.intellij.java")` 和 Platform test framework。优点是符合 2026.3 官方工具链，能继续使用 Gradle 9.9.1；代价是需要改写旧 Gradle DSL 和测试配置。

### 方案 B：保留 Gradle IntelliJ Plugin 1.x

只调整平台版本和兼容范围。该方案不能可靠支持 2026.3，且旧插件在 Gradle 9 上已经因内部 API 移除而失败，放弃。

### 方案 C：只修改 plugin.xml 兼容范围

不改构建平台，只把 `until-build` 改成 263。该方案无法验证 2026.3 API 和 Java 25 兼容性，放弃。

## 迁移边界

- 修改 `build.gradle.kts`、`settings.gradle.kts`、`gradle.properties`，新增 `gradle/wrapper` 文件。
- 修改 `plugin.xml` 的平台依赖/兼容范围相关声明，但保留 `soulx.MarkNote`。
- 为测试增加 JUnit 5 API/Engine、Platform test framework 和 `useJUnitPlatform()` 配置。
- 必要时只修复由 2026.3 API 变更暴露的编译错误，不扩展产品功能。

## 验收

- `./gradlew compileKotlin` 通过。
- `./gradlew test` 编译并运行领域、路径、持久化和编辑器集成测试。
- `./gradlew build` 通过，插件产物的 `since-build` 为 `263`，目标平台为 2026.3；不设置 `until-build`，保持后续版本兼容性。
- 旧 `CodeReadingNote.xml` 不被读取或覆盖。
