# Changelog

## 1.0.1

### Bug Fixes

- 修复删除节假日数据返回 HTML 而非 JSON 导致解析报错 - 所有 POST 端点支持 AJAX JSON 响应
- 修复 HolidayEnvironmentContributor 注释与实际环境变量名不一致

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

### Bug Fixes

- 修复节假日数据加载 404 错误 - 将 Stapler getter 方法改为 private，消除路由冲突
- 修复删除节假日数据 403 (CSRF) 错误 - POST 请求改用 URLSearchParams (application/x-www-form-urlencoded) 替代 FormData，crumb 同时通过请求头和表单参数双重传递
- 修复文件上传 crumb 403 问题 - 文件上传使用 FormData，crumb 仅通过请求头传递，避免 multipart 解析不可靠
- 修复离线导入未选文件跳转错误页问题 - 前端验证文件选择，未选文件弹窗提示不提交
- 修复 API 导入和离线导入成功/失败无提示问题 - 所有 POST 操作改为 AJAX，成功/失败弹窗提示后刷新页面
- 修复 URL 兼容性问题 - 页面级操作使用相对 URL，crumbIssuer 使用 Jelly 注入的 rootURL，兼容 Java 直启和 Tomcat 两种部署模式

### Technical

- 兼容 Jenkins 2.277.4 及后续版本
- Java 11+
- Jenkins Plugin Parent POM 4.40
- 使用 net.sf.json (Jenkins/Stapler 内置) 替代 Jackson
- 使用 StaplerProxy 强制权限校验
- 所有 POST 端点支持 AJAX (X-Requested-With) 请求返回 JSON，传统表单请求仍返回 redirect
