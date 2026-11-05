# 油耗管家 (FuelNoAd) 开发日志

## 📅 日期：2026-11-05 ~ 2026-11-06

---

### 一、 今日成果概述
今天完成了从**功能规划**、**高保真交互原型设计**、**Android 原生工程落地**、**云端 CI/CD 自动化构建 APK** 到 **88 笔真实历史数据解析迁移** 的完整全流程。实现了电脑零开发环境依赖、云端自动打包与离线优先体验。

---

### 二、 核心开发里程碑

#### 1. 交互原型与算法推演
* **前端原型**：构建基于 TailwindCSS 与 HTML5 Canvas 的高保真可交互原型 [`fuel_tracker_preview.html`](file:///C:/Users/XuJianhao/.gemini/antigravity/brain/75636ae3-6bc2-4883-9b1d-d92b91097586/fuel_tracker_preview.html)，验证了仪表盘主视觉、能耗卡片与滑动记账抽屉。
* **物理油耗核心算法**：
  * **加满到加满标准算法**：$\text{百公里油耗} = \frac{\Delta \text{加油量}}{\Delta \text{行驶里程}} \times 100$
  * **中途未加满累计机制**：中途多次未加满时，加油量自动累加至下一次加满统一核算。
  * **漏记断点保护机制**：用户勾选漏记后，重置基准锚点，防止历史数据污染。

#### 2. Android 原生工程搭建 (Modern Android Stack)
* **技术选型**：
  * 语言与环境：Kotlin 2.0.0, Min SDK 26 (Android 8.0+), Target SDK 34 (Android 14)
  * UI 框架：Jetpack Compose + Material 3 (Material You)
  * 本地持久化：Room Database 2.6.1 (SQLite) + KSP 2.0
  * 异步与状态：Kotlin Coroutines & Flow (StateFlow MVI 架构)
  * 图表可视化：Compose Canvas 硬件加速贝塞尔平滑油耗走势折线图 + 月度柱状图
* **主要模块**：
  * **领域与数据层**：`FuelCalculator.kt`（配全套 JUnit 单元测试）、`FuelDatabase.kt`、`VehicleEntity`、`FuelRecordEntity`、`FuelRepository`。
  * **数据安全**：`DataBackupManager.kt`（基于 Storage Access Framework，无任何敏感隐私权限）。
  * **界面层**：`DashboardScreen`、`HistoryScreen`、`AnalyticsScreen`、`SettingsScreen`、`AddRecordSheet`。

#### 3. 自动化云端打包 (GitHub Actions CI/CD)
* **零本地环境负担**：编写 `.github/workflows/build-apk.yml`，在微软云端 Ubuntu 容器自动化完成代码拉取、环境配置、测试执行与编译。
* **排查与修复的关键问题**：
  * 移除了 `settings.gradle.kts` 中限制性的 `includeGroupByRegex` 规则，修复了 Maven Central 上 KSP 插件的解析阻断。
  * 补齐了标准 `gradlew` 与官方 `gradle-wrapper.jar` 二进制包。
  * 优化了 Material 3 废弃的 `Divider`，全面升级为 `HorizontalDivider`。
  * 简化状态订阅为 `collectAsState()`，消除了额外的生命周期扩展依赖。
* **编译成果**：
  * GitHub Actions 流水线全绿通过（Run #42058536938 与 #42064533925），成功生成并归档了大小约 **15MB** 的 `app-debug.apk`。

#### 4. 88 笔真实历史数据清洗与迁移
* 成功清洗并转换用户自 2024 年 9 月至 2026 年 11 月以来的 **88 笔真实加油数据**：
  * **总行驶里程**：46,548 km (从 150 km 至 46,698 km)
  * **燃油总花费**：¥22,736.81
  * **累计加注油量**：3,234.7 L
  * **综合百公里油耗**：**6.98 L/100km**
  * **最低单箱油耗**：**5.44 L/100km**
  * **最高单箱油耗**：**8.83 L/100km**
  * **每公里综合成本**：**¥0.49 / km**
* **产出文件**：
  * `fuel_records.csv`（标准 CSV 导入格式）
  * `fuel_backup.json`（全量数据库备份还原格式）
* **引擎升级**：在 App 中实现了自适应智能解析，既支持标准 CSV 导入，也支持 JSON 恢复。

---

### 三、 当前工程状态
* 远程代码仓库：`https://github.com/mousebt/FuelNoAd` (分支：`main`)
* 状态：代码全部提交并同步至 GitHub，工作区干净，APK 构建绿色通过。

---

### 四、 明天规划（Next Steps）
1. **手机端实际导入效果复核**：观察 88 笔真实数据导入后的首页卡片、折线图与月度柱状图渲染表现。
2. **扩展费用记录规划**：增加保养（机油/滤芯）、保险、洗车、停车费等全生命周期用车成本功能。
3. **图表手势交互优化**：为 Compose Canvas 折线图增加滑动十字线瞄准、点位气泡悬浮数值提示。
4. **多车辆切换与管理完善**。
