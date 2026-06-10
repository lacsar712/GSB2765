# 产品定价后台管理系统

本系统是一个现代化、全栈式的产品定价管理后台，基于 Spring Boot 3 和 Vue 3 开发，完全容器化，支持一键启动。

## 🛠 技术栈

- **前端**: Vue 3 + Vite + Windi CSS (Tailwind CSS) + Axios
- **后端**: Spring Boot 3 + MyBatis-Plus + MySQL 8.0
- **部署**: Docker + Docker Compose

## 🚀 启动指南 (How to Run)

1. 确保您的机器上已安装 **Docker** 和 **Docker Compose**。
2. 在项目根目录下执行以下命令：
   ```bash
   docker compose up --build
   ```
3. 等待容器构建并启动完成。

## 🔗 服务地址 (Services)

- **前端地址**: [http://localhost:32765](http://localhost:32765)
- **后端地址**: [http://localhost:12765](http://localhost:12765)
- **数据库地址**: `localhost:33306` (用户名: `root`, 密码: `root`)

## 🧪 测试账号

- **用户名**: `admin`
- **密码**: `123456`

## ✨ 功能特性

- **现代化 UI**: 登录页采用左右布局，主界面简洁美观。
- **全栈 CRUD**: 支持产品的增、删、改、查。
- **UI 交互**: 使用自研 Toast 和 Modal 替代原生 alert/confirm，提供更好的交互体验。
- **容器化**: 环境完全隔离，零配置运行。
- **字符规范**: 全链路使用 `utf8mb4`，彻底避免中文乱码。
- **种子数据**: 启动即包含演示数据，无需手动录入。

---
*Powered by Antigravity*
