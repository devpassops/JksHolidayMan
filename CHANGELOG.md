# Changelog

## 1.0.0

### Features

- 节假日数据管理 - 按年份导入/管理中国法定节假日及调休工作日
- 在线导入 - 从 timor.tech API 自动获取中国法定节假日数据
- 离线导入 - 上传 JSON 文件导入节假日数据
- 导出 - 按年份或全量导出节假日数据为 JSON 文件
- 手动维护 - 支持手动添加/删除节假日条目
- 定时构建节假日控制触发器（HolidayTimerTrigger）- 替代原生"Build periodically"，支持三种策略：
  - 排除节假日（默认）- 仅在工作日执行（含调休工作日）
  - 包含节假日 - 正常定时执行，不进行节假日过滤
  - 仅节假日执行 - 仅在节假日执行，跳过工作日
- 节假日管理页面（Manage Jenkins → 节假日管理）
- 权限控制 - 仅管理员可访问和操作
- 中英双语支持

### Technical

- 兼容 Jenkins 2.277.4 及后续版本
- Java 11+
- Jenkins Plugin Parent POM 4.40
- 使用 net.sf.json (Jenkins/Stapler 内置) 替代 Jackson
- 使用 StaplerProxy 强制权限校验
