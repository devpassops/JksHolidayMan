# Changelog

## 1.0.0

### Features

- 节假日数据管理 - 按年份导入/管理中国法定节假日及调休工作日
- 官方API导入 - 从 timor.tech API 自动获取中国法定节假日数据
- 手动维护 - 支持手动添加/删除节假日条目
- 定时构建节假日控制触发器（HolidayTimerTrigger）- 替代原生"Build periodically"，支持三种策略：
  - 排除节假日（默认）- 仅在工作日执行（含调休工作日）
  - 包含节假日 - 所有日期均执行
  - 仅节假日执行 - 仅在节假日执行，跳过工作日
- Job级节假日属性（HolidayJobProperty）- 每个Job可独立配置节假日策略
- 全局默认策略配置（HolidayGlobalConfig）
- 节假日管理页面（Manage Jenkins → 节假日管理）
- 中英双语支持
