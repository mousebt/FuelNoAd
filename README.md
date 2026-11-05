# 油耗管家 (FuelTracker Android App)

一款注重隐私、纯本地存储、现代化 UI 的 Android 原生油耗记录应用。

## 🌟 核心特性
- **纯本地离线优先**：使用 Android 官方推荐的 **Room Database (SQLite)**，所有数据均保留在手机本地，不依赖任何第三方服务器。
- **严谨的油耗算法**：
  - 加满到加满物理算法（标准百公里油耗推算）
  - 支持中途“未加满”场景的油量自动累计与延迟统算
  - 支持“漏记”智能重置基准点，保护历史数据不受脏数据污染
- **现代 Android 架构**：
  - **Jetpack Compose + Material 3**（单向数据流与响应式 UI）
  - **Coroutines & Flow**（响应式数据管道）
  - **MVI / MVVM 架构**（视图与业务逻辑解耦）
- **数据安全与备份**：
  - 采用 Android Storage Access Framework (SAF)，无需任何危险外部存储权限
  - 支持一键导出/导入 **JSON 完整备份** 与 **CSV 表格**
  - 自适应智能解析导入格式（同时兼容 .csv 与 .json）

---

## 📝 开发日志
详细的每日开发进展与技术演进请参考 [DEV_LOG.md](DEV_LOG.md)。

---

## 📁 项目代码结构

```
app/src/main/java/com/fueltracker/app/
├── FuelApplication.kt               # 全局 Application，初始化本地数据库与仓储
├── MainActivity.kt                  # 承载 Compose 的入口 Activity
├── model/                           # 核心领域实体模型 (Vehicle, FuelRecord, DashboardStats)
├── domain/
│   └── FuelCalculator.kt            # 核心业务算法引擎 (百公里油耗、区间推算、数据看板聚合)
├── data/
│   ├── local/
│   │   ├── FuelDatabase.kt          # Room 数据库与初始化种子数据回调
│   │   ├── dao/                     # VehicleDao, FuelRecordDao 数据访问层
│   │   └── entity/                  # 数据库表结构实体
│   ├── repository/
│   │   └── FuelRepository.kt        # 统一数据管理仓储，封装计算与持久化
│   └── backup/
│       └── DataBackupManager.kt     # JSON / CSV 导出与恢复管理器
└── ui/
    ├── theme/                       # M3 主题、翡翠绿调色板与字体规范
    ├── viewmodel/                   # FuelViewModel (StateFlow 状态管理)
    ├── components/
    │   ├── MetricCard.kt            # 仪表盘主能耗渐变卡片
    │   ├── FuelTrendChart.kt        # Compose Canvas 动态平滑油耗折线图
    │   ├── RecordItemCard.kt        # 加油记录卡片 (含未加满/已加满/基准状态徽章)
    │   └── AddRecordSheet.kt        # 记一笔 M3 ModalBottomSheet 表单
    └── screens/
        ├── MainScreen.kt            # 底部导航栏与主 Scaffold 容器
        ├── DashboardScreen.kt       # 首页看板
        ├── HistoryScreen.kt         # 全部明细流 (支持加满/未加满筛选)
        ├── AnalyticsScreen.kt       # 费用统计与月度柱状图
        └── SettingsScreen.kt        # 车辆车库管理与本地文件备份
```

---

## 🚀 如何运行与编译

### 1. 使用 Android Studio 打开
1. 打开 **Android Studio (推荐 2024.1+ Iguana / Jellyfish / Koala 或更新版本)**。
2. 选择 **Open**，导航到本项目根目录 `e:\pythonProjects\Fuel` 并打开。
3. 等待 Gradle 自动完成依赖下载与项目同步（Sync）。
4. 连接 Android 手机或启动虚拟模拟器（Min SDK 26，Android 8.0 及以上）。
5. 点击上方绿色的 **Run 'app'** 按钮，即可在手机上启动运行。

### 2. 运行自动化单元测试
在 Android Studio 内打开 `app/src/test/java/com/fueltracker/app/domain/FuelCalculatorTest.kt`，点击类名前的绿色运行箭头，即可一键验证包括：
- 首次加油基准
- 加满到加满标准油耗
- 未加满多次累加
- 漏记防污染机制
- 看板聚合统算
全部单元测试用例。
