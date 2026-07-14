# Holiday Management Plugin

Version: **1.0.1**

Jenkins 节假日管理插件 - 管理官方节假日和调休工作日，控制定时任务的节假日执行策略。

支持 Jenkins 2.277.4 及后续版本，兼容 Java 直启和 Tomcat 部署两种模式。

## 功能特性

- **节假日数据管理** - 按年份导入/管理中国法定节假日及调休工作日
- **在线导入** - 从 timor.tech API 自动获取中国法定节假日数据，导入成功/失败弹窗提示
- **离线导入** - 上传 JSON 文件导入节假日数据，未选文件弹窗提示不跳转错误页
- **导出** - 按年份或全量导出节假日数据为 JSON 文件
- **手动维护** - 支持手动添加/删除节假日条目，操作结果弹窗提示
- **构建环境变量** - 自动向所有构建注入节假日相关环境变量，供 Pipeline 和 Freestyle 任务使用：
  - `DAY_IS_WORKDAY` - 今天是否为工作日（含调休工作日）
  - `DAY_IS_HOLIDAY` - 今天是否为节假日
  - `HOLIDAY_NAME` - 今天节假日名称（仅节假日时设置）
- **定时构建节假日控制** - 替代原生"Build periodically"，支持多行日程表和三种策略：
  - **排除节假日**（默认）- 仅在工作日执行（含调休工作日，跳过节假日和周末）
  - **包含节假日** - 正常定时执行，不进行节假日过滤
  - **仅节假日执行** - 仅在节假日执行，跳过工作日
- **中英双语** - 支持中文和英文界面

## 安装

1. 下载 `holiday-management-1.0.1.hpi`
2. 进入 **Manage Jenkins → Plugins → Advanced → Deploy Plugin**
3. 上传 HPI 文件并重启 Jenkins

## 使用方式

### 1. 导入节假日数据

进入 **Manage Jenkins → 节假日管理 / Holiday Management**：
- **在线导入**：输入年份，点击"从API导入"
- **离线导入**：上传 JSON 文件（格式见下方）
- **手动添加**：填写日期、名称、类型后添加

**快速下载当年节假日 JSON**：点击下方链接直接下载当前年份的节假日数据：
```bash
curl -o holiday-2026.json https://timor.tech/api/holiday/2026
```

或直接访问：[下载 2026 年节假日 JSON](https://timor.tech/api/holiday/2026)

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
- 填写 Cron 表达式，支持多行日程表（每行一个计划，如 `H H * * *`）
- 选择节假日策略

### 3. 在 Pipeline 中使用环境变量

所有构建自动注入以下环境变量（始终可用，非节假日时 HOLIDAY_NAME 为空字符串）：

```groovy
// Pipeline 示例
pipeline {
    agent any
    stages {
        stage('Check Holiday') {
            steps {
                // 推荐使用中括号访问 env 变量，避免 "no such property" 错误
                echo "Today is workday: ${env['DAY_IS_WORKDAY']}"
                echo "Today is holiday: ${env['DAY_IS_HOLIDAY']}"
                echo "Holiday name: ${env['HOLIDAY_NAME'] ?: 'N/A'}"
                
                // 根据节假日状态执行不同逻辑
                if (env['DAY_IS_HOLIDAY'] == 'true') {
                    echo "Skipping build on holiday: ${env['HOLIDAY_NAME']}"
                }
            }
        }
    }
}
```

Freestyle 任务 Shell 示例：
```bash
if [ "$DAY_IS_HOLIDAY" = "true" ]; then
    echo "Today is holiday: $HOLIDAY_NAME"
    exit 0
fi
echo "Today is workday, proceeding with build..."
```

## 构建

```bash
mvn clean package -DskipTests
```

构建产物：`target/holiday-management-1.0.1-SNAPSHOT.hpi`

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
- 兼容 Java 直启 (无 context path) 和 Tomcat (有 context path) 两种部署模式
- AJAX POST 操作支持 JSON 响应，传统表单提交仍支持 redirect

## Star History

[![Star History Chart](https://api.star-history.com/chart?repos=devpassops/JksHolidayMan&type=date&legend=top-left)](https://www.star-history.com/?repos=devpassops%2FJksHolidayMan&type=date&legend=top-left)