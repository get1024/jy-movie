# jy-movie

基于 Vue 2 + Spring Boot 的电影购票教学项目，包含用户端、管理端和客服端。

![用户端首页](readme_assets/app_home_vue.png)

## 目录

| 目录 | 用途 |
| --- | --- |
| `api` | Java 后端、业务逻辑和接口 |
| `vue-app` | 用户端：电影、选座、购物车、订单、评价 |
| `vue-admin` | 管理端：影片、排片、海报、用户、员工 |
| `vue-worker` | 客服端：任务、留言、活动、统计 |
| `sql_script` | 历史示例数据和数据库结构 |
| `docs` | 维护说明与历史记录 |

## 本次维护的边界

保留现有数据库结构、三个独立前端和主要接口，优先修复订单与登录相关问题、减少重复查询、补充回归测试并恢复可复现构建。本次没有直接迁移 Vue 3 或 Spring Boot 3。

**这是教学/演示项目，不是可直接上线的支付系统。** Vue 2、Vue CLI 和 Spring Boot 2.7 均是旧技术栈。订单支付仍为模拟操作；权限细化、真实支付验证、历史数据与部署安全仍需专项改造。详见 [维护与迁移说明](docs/MAINTENANCE.md)。

## 环境要求

- Node.js 24 + npm（根目录提供 `.nvmrc`）
- JDK 11、Maven 3.9+
- MySQL 8；数据库字符集建议 `utf8mb4`
- Redis 相关依赖保留，但当前没有启用 Spring Cache

## 本地启动

### 1. 创建数据库

创建独立的本地开发数据库后，按需要选择一个 SQL 文件导入：

- `sql_script/structure.sql`：结构参考/空数据库；需要自行初始化业务数据
- `sql_script/jy-movie.sql`：原项目的演示数据
- `sql_script/movie.sql`：更早的历史数据备份

这些是历史快照，不是自动迁移脚本。**不要导入到现有生产数据库覆盖数据**，也不要把演示账号或历史数据直接用于生产。

### 2. 配置并启动后端

后端读取环境变量，不再把数据库密码和 JWT 签名密钥写入源码。

```sh
export DB_URL='jdbc:mysql://localhost:3306/movie?characterEncoding=utf8&serverTimezone=UTC'
export DB_USERNAME='你的数据库用户'
export DB_PASSWORD='你的本地数据库密码'
export JWT_SECRET='替换为独立随机的至少32字节密钥'
mvn -f api/pom.xml spring-boot:run
```

上面的 JWT 文本只是占位提示，请自行设置足够长的随机值，不要复制作为实际密钥。环境变量只在当前终端生效；生产环境应使用部署平台的密钥管理。不要提交真实 `.env`、密码或密钥。

默认后端地址：`http://localhost:8888/api`。可用 `SERVER_PORT` 修改端口；远程部署时用 `APP_PUBLIC_URL` 设置图片链接的外部服务根地址（例如 `https://movies.example.com`，不含 `/api`）。密钥配置或变更后，已有登录状态需清除并重新登录。不要使用原代码中的历史密钥。

上传默认限制为单文件 20 MB、整次请求 21 MB，可通过 `MAX_UPLOAD_FILE_SIZE` 和 `MAX_UPLOAD_REQUEST_SIZE` 调整。

### 3. 启动三个前端

在对应目录使用锁文件安装依赖：

```sh
cd vue-app
npm ci
npm run serve -- --host 127.0.0.1 --port 8080
```

管理端和客服端分别在 `vue-admin`、`vue-worker` 执行同样命令，使用 `8081`、`8082` 避免端口冲突。

默认接口地址为 `http://localhost:8888/api`。如需修改，在各前端目录的本地环境文件中配置 `VUE_APP_API_URL`，然后重启开发服务或重新构建。环境文件示例见各前端目录。

## 测试与构建

```sh
# 后端单元测试和 JAR 构建，不需要真实数据库
mvn --batch-mode --no-transfer-progress -f api/pom.xml clean verify

# 在每个前端目录分别运行
npm ci
npm test
npm run build
```

GitHub Actions 会对 `main` 推送和 Pull Request 执行上述检查。CI 不部署、不支付、不上传构建产物，也不使用生产数据。单元测试通过并不等于已验证真实 MySQL 多实例并发和完整浏览器购票流程，部署前仍应进行集成验收。

## 进一步阅读

- [维护说明、行为变化与待办](docs/MAINTENANCE.md)
- [原项目历史改动日志](docs/HISTORY.md)
- [数据库历史说明](sql_script/readme.md)
