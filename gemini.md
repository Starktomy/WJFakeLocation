# WJFakeLocation 代码库调查报告

## 1. 项目概述
**WJFakeLocation** 是一款高级的 Android 应用程序，作为 Xposed 模块（兼容 LSPosed）运行，用于在系统全局范围内伪造设备位置、网络元数据和传感器数据。该应用使用现代 Android 开发框架提供了健壮的用户界面，并深度集成了原生地图 SDK 与云同步服务，旨在提供无缝的虚拟定位体验。

## 2. 技术栈与架构
该项目遵循 Clean Architecture（整洁架构）原则，并结合了 **MVVM (Model-View-ViewModel)** 设计模式。
*   **UI 框架:** Jetpack Compose (现代声明式 UI)
*   **开发语言:** Kotlin (使用 Coroutines 和 Flows 处理异步数据流)
*   **依赖注入:** Hilt / Dagger
*   **本地数据库:** Room (SQLite 抽象层，用于存储收藏和配置)
*   **云端同步:** Supabase (使用 `supabase-kt` 2.0.0 版本连接 PostgreSQL 后端)
*   **地图引擎:** AMap (高德 3D 地图 SDK) & 百度地图 SDK
*   **Hook 框架:** Xposed API (拦截系统级方法调用)
*   **代码质量:** 由 `ktlint` (格式化) 和 `detekt` (静态分析) 统一管理

## 3. 核心架构与目录结构

```text
app/src/main/java/com/steadywj/wjfakelocation/
├── common/         # 工具类 (例如基于 Timber 封装的 WJLogger)
├── data/           # 数据层 (本地与远程)
│   ├── local/      # Room 数据库设置 (AppDatabase, DAOs)
│   ├── model/      # 数据实体类 (FavoriteLocation, FakeCellInfo, FakeWifiInfo, LocationSettings)
│   └── repository/ # 单一数据源 (FavoritesRepository, PreferencesRepository)
├── di/             # Hilt 依赖注入模块 (AppModule, DatabaseModule)
├── domain/         # 领域层 (业务逻辑与服务)
│   └── service/    # CloudSyncService (Supabase 同步), AIService, PluginManager (Lua 脚本), GPX/KML 解析, PathPlayer
├── manager/        # 表现层 (Compose UI 与 ViewModels)
│   ├── about/      # 关于页面
│   ├── favorites/  # 收藏管理 UI
│   ├── map/        # 核心地图交互 (AMap/Baidu 地图封装, ViewModel, MapScreen)
│   ├── navigation/ # Compose 导航图
│   ├── search/     # POI 搜索管理
│   └── settings/   # 应用配置与 API Key 设置
└── xposed/         # Xposed Hook 核心引擎
    ├── MainHook.kt # Xposed 模块入口点
    ├── common/     # Hook 工具类 (LocationUtil, PreferencesUtil)
    └── hooks/      # 具体的 API 拦截器 (LocationApiHooks, TelephonyHook, WifiHook, SystemServicesHooks)
```

## 4. 关键子系统与工作流

### 4.1 Xposed Hook 引擎 (`xposed/`)
应用的核心功能依赖于拦截系统 API 从而注入伪造数据。
*   **`LocationApiHooks`:** Hook `LocationManager` 和 `FusedLocationProviderClient`，使用用户选择的坐标替换真实的 GPS 坐标。
*   **`TelephonyHook`:** Hook `TelephonyManager` 以伪造基站数据（`CellInfo`, `CellLocation`），防止其他应用通过基站三角定位获取真实位置。
*   **`WifiHook`:** Hook `WifiManager` 以伪造附近 Wi-Fi 网络的 BSSID/MAC 地址，缓解基于 Wi-Fi 的位置追踪。
*   **`LocationUtil`:** 处理 WGS84、GCJ02 和 BD09 坐标系之间的数学转换，并应用海拔、精度和速度等逼真的修饰符。

### 4.2 地图引擎 (`manager/map/`)
应用支持双地图引擎，以适应不同区域的需求和 API 的可用性。
*   **高德 (AMap) & 百度:** 作为 Compose 组件（`AMapView`, `BaiduMapView`）进行封装。
*   **生命周期管理:** 通过 `AMapManager` 和 `BaiduMapManager` 清晰地管理生命周期事件，避免在 Compose 中发生内存泄漏。
*   **轨迹回放:** 支持解析 GPX/KML 文件，并使用 `PathPlayer` 动态回放运动轨迹。

### 4.3 数据与云同步 (`data/` & `domain/service/`)
*   **本地存储:** 用户设置安全地存储在 `SharedPreferences` 中（由 `PreferencesRepository` 管理，对于 `Double` 序列化使用了位移转换），而收藏的位置则存储在 Room 数据库中。
*   **Supabase 集成:** `CloudSyncService` 连接到 Supabase Postgres 实例，将 `FavoriteLocation` DTO 同步到云端，允许用户跨设备持久化保存其伪装配置。

## 5. 近期改进与稳定化
项目最近经历了一次重大的稳定化重构阶段：
*   **跨进程通信 (IPC) 闭环:** 解决了 Hook 层中长期存在的配置硬编码问题。通过在主应用中实现基于 `ContentProvider` 的 `SettingsProvider`，并在 Hook 模块内通过 `ProviderHelper` （带 2 秒级别的内存缓存）进行安全高效的跨进程读取，彻底打通了主应用 UI 控制目标应用内位置伪装、基站伪装及 WiFi 伪装的功能。
*   **地图 SDK 迁移:** 成功稳定了 AMap 3D SDK 的实现，并移除了已废弃的 2D 依赖。
*   **Supabase-kt 升级:** 重构了查询语句以匹配 `postgrest-kt` 2.0.0 的语法（例如 `supabase.from("table").select(...) { filter { eq(...) } }`），并通过将数据流获取改写为直接的 DAO 同步查询，解决了 Coroutine `Flow` 的解析问题。
*   **Android 14 兼容性:** 通过安全地包装 SDK 版本检查，处理了有关 `mslAltitudeMeters` 的严格 `NewApi` lint 错误（API 34）。
*   **代码质量与构建稳定性:** 移除了错误的 `useJUnitPlatform()` 配置以修复 Gradle 测试执行器的崩溃，使得测试套件正常运行；更新了 `Mockito` 的参数验证（`any()`）。实现了 100% 符合严格的 `ktlint` 代码风格，解决了所有 `detekt` 的性能或历史遗留问题并强制执行整洁架构规则。

## 6. 下一步计划与潜在增强
1.  **AI 服务扩展:** `AIService` 目前包含集成的基础设施。添加具体的 Moonshot/Kimi AI 提示词可以允许用户根据自然语言请求动态生成伪造的“旅行轨迹”。
2.  **插件系统:** `PluginManager` 提到了 Lua 脚本。扩展此功能可以允许用户编写和动态加载自定义的 Xposed Hook 脚本。
3.  **UI 打磨:** Compose UI 的功能非常完善。实施广泛的 Material 3 主题（动态配色）和微动画将大幅提升高级感。
