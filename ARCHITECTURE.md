# Noteds 架构说明

## 1. 项目概览（Project Overview）
Noteds 是一款基于 Kotlin、Jetpack Compose、MVVM 与 Room 的本地记账/催收工具，支持按客户或分组管理赊账与回款，并提供基础报表与仪表盘视图。【F:app/src/main/java/com/example/noteds/ui/AppRoot.kt†L26-L170】【F:app/src/main/java/com/example/noteds/ui/home/DashboardScreen.kt†L1-L120】
典型用户是需要在手机本地记录赊账的家庭小店或小生意经营者，后续可通过云端同步/Web 仪表盘让家人协同查看（此部分在本文中标为“未来规划”）。当前版本聚焦本地数据：客户/分组管理、账务录入、报表统计、备份导入导出等。【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L135-L217】【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L76-L191】

## 2. 整体架构（System Architecture）
### 2.1 分层结构
* **UI 层**：Compose Screen 与自定义导航栈，负责页面布局、交互与页面切换。【F:app/src/main/java/com/example/noteds/ui/AppRoot.kt†L26-L170】
* **ViewModel 层**：`CustomerViewModel` 与 `ReportsViewModel` 负责状态管理、业务聚合（层级汇总、统计、备份导入导出）。【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L44-L217】【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L65-L191】
* **Repository 层**：`CustomerRepository`、`LedgerRepository`、`BackupRepository` 封装 DAO、事务与备份恢复逻辑。【F:app/src/main/java/com/example/noteds/data/repository/CustomerRepository.kt†L8-L41】【F:app/src/main/java/com/example/noteds/data/repository/LedgerRepository.kt†L8-L22】【F:app/src/main/java/com/example/noteds/data/repository/BackupRepository.kt†L10-L71】
* **Data 层**：Room 实体、DAO、数据库与迁移，以及备份数据模型。【F:app/src/main/java/com/example/noteds/data/entity/CustomerEntity.kt†L7-L31】【F:app/src/main/java/com/example/noteds/data/entity/LedgerEntryEntity.kt†L8-L27】【F:app/src/main/java/com/example/noteds/data/db/AppDatabase.kt†L12-L216】【F:app/src/main/java/com/example/noteds/data/model/BackupData.kt†L1-L10】

层间依赖：UI → ViewModel → Repository → DAO/Room，所有数据源目前均为本地 Room。

### 2.2 主要模块与包结构
* `ui/`：包含 `AppRoot` 导航、首页 `home/`、客户相关 `customers/`、报表 `reports/` 以及主题等，负责 Compose 视图与交互。
* `ui/customers/`：列表、详情、表单组件及 `CustomerViewModel`，实现分组层级、客户编辑与账务操作。【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L65-L217】
* `ui/reports/`：统计计算与备份导入导出逻辑集中在 `ReportsViewModel`，报表界面消费其状态。【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L65-L474】
* `data/entity`、`data/dao`、`data/db`、`data/repository`、`data/model`：Room 实体、DAO、数据库/迁移、仓库以及备份/统计用模型。【F:app/src/main/java/com/example/noteds/data/db/AppDatabase.kt†L12-L216】【F:app/src/main/java/com/example/noteds/data/model/CustomerWithBalance.kt†L1-L12】
* `di/`：`AppContainer` 负责数据库与仓库的创建与注入。【F:app/src/main/java/com/example/noteds/di/AppContainer.kt†L1-L34】

### 2.3 系统视角图（文字）
* **当前实现**：Android 本地应用，UI 通过 ViewModel 使用 Repository 访问 Room（customers、ledger_entries）。【F:app/src/main/java/com/example/noteds/data/db/AppDatabase.kt†L12-L216】【F:app/src/main/java/com/example/noteds/ui/AppRoot.kt†L26-L170】
* **未来规划/推测**：在现有备份 JSON 基础上扩展云端 API 上传快照，并提供 Web 仪表盘读取同一数据模型（见第 6 节）。目前代码未包含云端实现，仅预留 JSON 备份格式与导入导出流程。【F:app/src/main/java/com/example/noteds/data/model/BackupData.kt†L1-L10】【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L348-L474】

## 3. 领域模型与数据结构（Domain Model & Data Schema）
### 3.1 核心实体
* **CustomerEntity**：单表同时表示客户与分组。`isGroup` 与 `parentId` 实现层级，包含姓名、电话、备注、还款日、删除标记以及多张头像/证件照 URI；`initialTransactionDone` 标记是否已生成初始账目。【F:app/src/main/java/com/example/noteds/data/entity/CustomerEntity.kt†L7-L31】
* **LedgerEntryEntity**：账务流水，`customerId` 外键指向客户，`type` 区分欠款/回款，包含金额、时间戳与备注。【F:app/src/main/java/com/example/noteds/data/entity/LedgerEntryEntity.kt†L8-L27】
* **CustomerWithBalance**：Room 投影结果，附带客户累积欠款与回款，用于列表与汇总。【F:app/src/main/java/com/example/noteds/data/model/CustomerWithBalance.kt†L1-L12】
* **BackupData**：备份文件的根对象，包含版本、时间戳以及客户与流水列表。【F:app/src/main/java/com/example/noteds/data/model/BackupData.kt†L1-L10】

### 3.2 Room 数据库设计
* 数据库版本 6，表 `customers` 与 `ledger_entries`，均启用主键自增与关键索引（parentId、customerId）。【F:app/src/main/java/com/example/noteds/data/db/AppDatabase.kt†L12-L126】
* 迁移：v1–v5 通过重建表确保旧列兼容；v5→v6 重建 `customers` 表以加入 `parentId`/`isGroup` 并创建索引，复制旧数据后删除旧表，确保层级字段正确初始化。【F:app/src/main/java/com/example/noteds/data/db/AppDatabase.kt†L22-L216】

### 3.3 层级结构与分组汇总
`parentId + isGroup` 定义文件夹/分组与子客户关系。`CustomerViewModel.getCustomers` 按 parentId 分组，在内存中递归计算分组下所有子项的欠款与回款总额，以便列表显示汇总余额。【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L65-L106】

## 4. 功能架构（Feature Architecture）
### 4.1 导航与页面结构
底部导航包含“首頁/客戶/報表”，使用自维护 `screenStack` 管理分层页面（主页→分组列表→客户详情→编辑/新增），自定义返回栈与 BackHandler 控制返回逻辑。【F:app/src/main/java/com/example/noteds/ui/AppRoot.kt†L31-L170】

### 4.2 客户管理与账务录入
* 创建分组/客户：`addCustomer` 可指定 `isGroup`、父级 `parentId`，保存头像并写入 `customers`；若提供初始欠款则自动生成一条 `DEBT` 账目记录。【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L135-L185】
* 编辑：`updateCustomer` 支持更新信息与照片并清理替换文件；单独的照片更新、账目更新/删除接口用于详情页操作。【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L264-L323】
* 删除：`deleteCustomerRecursive` 递归删除分组下所有子客户及账目，同时标记客户为删除并清理本地文件，账目通过 `ledgerRepository.deleteEntriesForCustomer` 清空。【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L189-L217】
* 账务录入：`addLedgerEntry` 追加欠款/回款流水，`getTransactionsForCustomer` 提供流式数据给详情页。【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L53-L55】【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L300-L315】

### 4.3 报表与统计逻辑
`ReportsViewModel` 汇总所有客户与流水，计算：
* 总待收款与 Top 欠款人（按余额排序取前 10）。【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L65-L98】
* 本月新增欠款/回款与近 6 个月月度趋势。【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L99-L227】
* 账龄分布（0–30/31–60/61–90/90+），交易次数，平均回款周期（以最近 30 天欠款与当前欠款比值估算）。【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L139-L191】
* 总欠款趋势序列：按月份累积欠款-回款并归一化，用于首页折线图。【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L183-L303】

### 4.4 首页仪表盘（DashboardScreen）
仪表盘展示总待收款、趋势图、本月新增欠款/回款与 Top 欠款人列表；点击欠款人回调到客户详情，数据来源于 `ReportsViewModel` 的状态流。【F:app/src/main/java/com/example/noteds/ui/home/DashboardScreen.kt†L1-L120】

## 5. 备份与恢复（Backup & Restore）
* 备份：`ReportsViewModel.exportBackup` 与 `CustomerViewModel.exportBackup` 将客户与账目序列化为 JSON，并将照片打包进 zip，保留原始 ID/时间戳。【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L348-L405】【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L219-L237】
* 恢复：`ReportsViewModel.importBackup` 解析 zip/JSON 并调用 `BackupRepository.replaceAllData` 覆盖数据库，`BackupRepository` 先清空表，再插入客户与流水并重置自增序列，保留 parentId/isGroup 以维持层级关系并过滤无效引用账目。【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L407-L474】【F:app/src/main/java/com/example/noteds/data/repository/BackupRepository.kt†L10-L71】
* 文件位置：备份使用应用缓存目录临时生成 zip/照片，导入前先清理临时目录；照片持久化在应用内部 filesDir/customer_photos，下次导入会在解析时重建。【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L348-L474】【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L154-L173】

## 6. 未来的云端同步与 Web 仪表盘规划（未来规划/推测）
> 代码目前无云端实现；以下为基于现有备份格式的自然扩展设想。

### 6.1 同步策略（Cloud Sync Strategy）
* 采用现有 JSON/zip 备份作为全量 snapshot，通过 `POST /api/snapshot` 上传，服务器按 owner/device 维度清库重建，保证单设备场景下的一致性。

### 6.2 云端存储与自动备份
* 服务器可沿用 `customers`/`ledger_entries` 结构并新增 ownerId/deviceId；定期生成数据库备份文件，便于灾备。

### 6.3 Web 仪表盘（Dashboard Web App）
* 复用移动端统计模型：总待收款、月度趋势、账龄分布、Top 欠款人；Web 端补充客户搜索、筛选、时间线视图等，统计可在服务端或客户端按相同公式计算。

## 7. 非功能性考量（Non-functional Concerns）
* 适用规模：面向小型本地账本，Room + 内存递归汇总能满足小数据量场景。【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L65-L106】
* 数据一致性：备份/恢复在事务中执行并重置自增，过滤已删除客户或孤儿账目，降低层级丢失风险。【F:app/src/main/java/com/example/noteds/data/repository/BackupRepository.kt†L10-L71】
* 扩展云端/多设备时需考虑用户/权限、冲突解决与分片同步；可在现有备份全量策略上增加版本号与时间戳元数据以支持增量或合并。

## 8. 总结（Summary）
当前版本提供完整的本地账本闭环：层级化客户/分组模型、账务流水、统计报表、仪表盘与备份恢复，结构清晰且易于扩展。【F:app/src/main/java/com/example/noteds/ui/customers/CustomerViewModel.kt†L65-L217】【F:app/src/main/java/com/example/noteds/ui/reports/ReportsViewModel.kt†L65-L191】
基于统一的数据模型与备份格式，未来可平滑扩展到云端同步与 Web 仪表盘，适用于家庭店铺或线下小生意的赊账管理。
