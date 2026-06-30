# Holiday Management Plugin

Version: **1.0.0**

Jenkins 节假日管理插件 - 管理官方节假日和调休工作日，控制定时任务的节假日执行策略。

支持 Jenkins 2.277.4 及后续版本。

## 功能特性

- **节假日数据管理** - 按年份导入/管理中国法定节假日及调休工作日
- **在线导入** - 从 timor.tech API 自动获取中国法定节假日数据
- **离线导入** - 上传 JSON 文件导入节假日数据
- **导出** - 按年份或全量导出节假日数据为 JSON 文件
- **手动维护** - 支持手动添加/删除节假日条目
- **定时构建节假日控制** - 替代原生"Build periodically"，支持三种策略：
  - **排除节假日**（默认）- 仅在工作日执行（含调休工作日，跳过节假日和周末）
  - **包含节假日** - 正常定时执行，不进行节假日过滤
  - **仅节假日执行** - 仅在节假日执行，跳过工作日
- **中英双语** - 支持中文和英文界面

## 安装

1. 下载 `holiday-management-1.0.0.hpi`
2. 进入 **Manage Jenkins → Plugins → Advanced → Deploy Plugin**
3. 上传 HPI 文件并重启 Jenkins

## 使用方式

### 1. 导入节假日数据

进入 **Manage Jenkins → 节假日管理 / Holiday Management**：
- **在线导入**：输入年份，点击"从API导入"
- **离线导入**：上传 JSON 文件（格式见下方）
- **手动添加**：填写日期、名称、类型后添加

JSON 文件格式：
```json
[
  {"date": "2026-01-01", "name": "元旦", "type": "HOLIDAY"},
  {"date": "2026-02-07", "name": "春节调休", "type": "WORKDAY"}
]
```

或按年份分组：
```json
{
  "2026": [
    {"date": "2026-01-01", "name": "元旦", "type": "HOLIDAY"}
  ]
}
```

### 2. 配置定时构建

在 Job 配置页面：
- 勾选 **"定时构建（节假日管理） / Build periodically with holiday management"**
- 填写 Cron 表达式（如 `H H * * *`）
- 选择节假日策略

## 构建

```bash
mvn clean package -DskipTests
```

构建产物：`target/holiday-management-1.0.0-SNAPSHOT.hpi`

## 节假日判断逻辑

| 日期标记 | 排除节假日策略 | 仅节假日策略 |
|----------|---------------|-------------|
| HOLIDAY（节假日） | 跳过 | 执行 |
| WORKDAY（调休工作日） | 执行 | 跳过 |
| 未标记 + 周末 | 跳过 | 跳过 |
| 未标记 + 工作日 | 执行 | 跳过 |

## 技术栈

- Jenkins 2.277.4+
- Java 11
- Jenkins Plugin Parent POM 4.40
- Jenkins BOM bom-2.277.x
- json-lib (net.sf.json, Jenkins/Stapler 内置)
