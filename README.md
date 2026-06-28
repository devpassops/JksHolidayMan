# Holiday Management Plugin

Version: **1.0.0**

Jenkins 节假日管理插件 - 管理官方节假日和调休工作日，控制定时任务的节假日执行策略。

基于 Jenkins 2.479.2 开发。

## 功能特性

- **节假日数据管理** - 按年份导入/管理中国法定节假日及调休工作日
- **官方API导入** - 从 timor.tech API 自动获取中国法定节假日数据
- **手动维护** - 支持手动添加/删除节假日条目
- **定时构建节假日控制** - 替代原生"Build periodically"，支持三种策略：
  - **排除节假日**（默认）- 仅在工作日执行（含调休工作日，跳过节假日和周末）
  - **包含节假日** - 所有日期均执行
  - **仅节假日执行** - 仅在节假日执行，跳过工作日
- **Job级策略覆盖** - 每个Job可独立配置节假日策略，优先级高于触发器级配置
- **全局默认策略** - 系统级默认节假日策略配置
- **中英双语** - 支持中文和英文界面

## 安装

1. 下载 `holiday-management-1.0.0.hpi`
2. 进入 **Manage Jenkins → Plugins → Advanced → Deploy Plugin**
3. 上传 HPI 文件并重启 Jenkins

## 使用方式

### 1. 导入节假日数据

进入 **Manage Jenkins → 节假日管理 / Holiday Management**：
- 输入年份，点击 **Import** 从官方API导入该年度节假日数据
- 也可手动添加单个节假日/调休工作日条目

### 2. 配置定时构建

在 Job 配置页面：
- 勾选 **"定时构建（节假日管理） / Build periodically with holiday management"**
- 填写 Cron 表达式（如 `H H * * *`）
- 选择节假日策略

### 3. Job级策略覆盖

在 Job 配置页面添加 **"节假日管理 / Holiday Management"** 属性：
- 启用节假日控制
- 选择该Job专用的节假日策略（覆盖触发器级配置）

## 构建

```bash
# 使用构建脚本
./build.sh package

# 或直接使用 Maven
mvn hpi:hpi -DskipTests
```

构建产物：`target/holiday-management-1.0.0.hpi`

## 节假日判断逻辑

| 日期标记 | 是否执行（排除节假日策略） | 是否执行（仅节假日策略） |
|----------|---------------------------|-------------------------|
| HOLIDAY（节假日） | 跳过 | 执行 |
| WORKDAY（调休工作日） | 执行 | 跳过 |
| 未标记 + 周末 | 跳过 | 跳过 |
| 未标记 + 工作日 | 执行 | 跳过 |

## 技术栈

- Jenkins Plugin Parent POM 5.7
- Jenkins BOM 2.479.x
- Java 17
- Jackson 2.17.0