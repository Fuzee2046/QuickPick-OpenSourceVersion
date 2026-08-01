# 食刻快取 QuickPick

食刻快取是一套面向校园食堂的微信小程序点餐系统，包含用户端、商户管理端和 Spring Boot 后端。项目已在校园场景中持续运行，并围绕高峰点餐、商户接单和订单履约做了完整闭环。

## 项目结构

```text
quickpick-backend/       Spring Boot + MyBatis-Plus 后端
quickpick-miniprogram/   uni-app 用户端小程序
quickpick-merchant-pwa/  Vue 3 + Element Plus 商户端
```

## 技术栈

- Java 17、Spring Boot、Spring Security、JWT、MyBatis-Plus
- MySQL、Redis、Lua 脚本、定时任务分布式锁
- Vue 3、TypeScript、Vite、Element Plus、Pinia
- uni-app / 微信小程序

## Redis 工程化实践

- 店铺与菜品目录缓存、首页列表缓存，降低高频读请求的数据库压力
- 热点 Key 互斥重建，避免缓存失效时并发回源
- 空值缓存防止缓存穿透
- Lua 原子状态机实现订单提交幂等
- 基于 `SET NX EX` 的定时任务分布式锁
- Spring Boot Actuator 健康检查与指标观测，并提供 Redis 故障降级开关

## 本地运行

1. 安装 JDK 17、Maven、Node.js 20+、MySQL 8 和 Redis 7。
2. 复制 `quickpick-backend/quickpick/src/main/resources/application-example.yaml` 为本地 `application.yaml`，填写自己的数据库、JWT、微信、对象存储和支付配置。该文件已被 `.gitignore` 忽略。
3. 在两个前端目录复制 `.env.example` 为对应的 `.env.development`，按本地后端地址调整 `VITE_API_BASE_URL`。
4. 启动后端：

   ```bash
   cd quickpick-backend/quickpick
   mvn spring-boot:run
   ```

5. 启动商户端：

   ```bash
   cd quickpick-merchant-pwa
   npm install
   npm run dev
   ```

数据库脚本和生产数据不在公开仓库中，请使用自己的测试数据。支付、微信登录和 COS 上传默认关闭或使用占位配置。

## 安全说明

公开仓库不包含真实密钥、Token、数据库账号、生产域名、生产数据或部署文档。请通过环境变量、服务器密钥管理或 CI/CD Secret 注入敏感配置。

## License

本项目以 MIT License 发布，详见 [LICENSE](LICENSE)。
