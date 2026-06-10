# 全链路代码分析：从 `/api/auth/login` 到产品增删改查

> 本文严格按 **Controller → Service → Mapper → Entity** 的分层顺序，针对仓库 `backend/`（Spring Boot + MyBatis-Plus）与 `frontend/`（Vue 3 + Vite + WindiCSS + Axios），梳理登录与产品 CRUD 的全链路调用与数据流，并固定输出「逻辑说明」「潜在风险」「技术细节」三节。

---

## 一、逻辑说明

### 1. 总体架构与启动入口

- 后端入口：[BackendApplication.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/BackendApplication.java)，使用 `@SpringBootApplication` 启动，并通过 `@MapperScan("com.pricing.mapper")` 扫描 Mapper 包；服务端口由 [application.yml](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/application.yml) 指定为 `8080`，并在启动时自动执行 `db/schema.sql` 与 `db/data.sql` 初始化 MySQL 表与种子数据。
- 前端入口：[main.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/main.js) 创建 Vue 应用并挂载 [router/index.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/router/index.js) 中的两个路由：`/login` 与 `/`（带 `requiresAuth`）。前端通过 `localStorage` 中的 `token` 做路由守卫。
- 网关与代理：开发期通过 [vite.config.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/vite.config.js) 将 `/api` 代理至 `http://backend:8080`；生产期由 [nginx.conf](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/nginx.conf) 将 `/api` `proxy_pass` 至 `http://backend:8080`，前端静态资源由 Nginx 提供。

### 2. 登录链路（`/api/auth/login`）

**前端 → 后端 → 前端** 的完整数据流：

1. 用户在 [LoginView.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/LoginView.vue#L11-L38) 输入 `username/password`，`handleLogin` 通过 [request.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/utils/request.js) 这个 axios 实例发送 `POST /api/auth/login`。请求拦截器会从 `localStorage` 取 `token`（首次登录无）注入 `Authorization: Bearer xxx`。
2. 后端 **Controller**：[AuthController.login()](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/AuthController.java#L15-L25) 接收 `@RequestBody LoginRequest`（[LoginRequest.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/dto/LoginRequest.java)），仅做硬编码校验：当 `username == "admin"` 且 `password == "123456"` 时返回 `CommonResult.success({token: "admin-token", username: "管理员"})`，否则返回 `CommonResult.failed("用户名或密码不正确")`。
3. 该接口 **不经过 Service / Mapper / Entity**，因为本仓库没有 `User` 实体，登录是纯内存校验。
4. 响应回到前端后，由 [request.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/utils/request.js#L21-L31) 响应拦截器统一处理：`code === 200` 时返回 `res.data`；否则派发自定义事件 `api-error` 并 `Promise.reject`。
5. 登录成功后将 `token`、`username` 写入 `localStorage`，弹出成功 Toast 并跳转到 `/`。
6. `/api/auth/logout`：[AuthController.logout()](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/AuthController.java#L27-L30) 直接返回成功；前端 [ProductView.vue.handleLogout](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/ProductView.vue#L41-L45) 实际未调用该接口，仅清空 `localStorage` 并跳回 `/login`。

### 3. 产品 CRUD 链路（`/api/products`）

进入主页后 `onMounted` 触发 `fetchProducts`，按 **Controller → Service → Mapper → Entity** 分层链路如下：

#### 3.1 列表查询 `GET /api/products`

- **Controller**：[ProductController.list()](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/ProductController.java#L19-L22) 调用 `productService.list()`，包装为 `CommonResult<List<Product>>`。
- **Service**：[ProductService](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/service/ProductService.java) 继承 `IService<Product>`；实现类 [ProductServiceImpl](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/service/impl/ProductServiceImpl.java) 继承 `ServiceImpl<ProductMapper, Product>`，自身无任何额外逻辑，`list()` 方法由 MyBatis-Plus 提供（等价 `SELECT * FROM product`）。
- **Mapper**：[ProductMapper](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/mapper/ProductMapper.java) 仅继承 `BaseMapper<Product>`，无自定义方法。
- **Entity**：[Product](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/entity/Product.java) 通过 `@TableName("product")` 与 `product` 表映射，主键 `id` 为 `IdType.AUTO`，包含 `name/price/category/description/createTime/updateTime`。`map-underscore-to-camel-case: true` 完成 `create_time → createTime` 等驼峰映射。
- **数据回传**：经 `request.js` 解包后，前端 `products.value = res`，再通过 `filteredProducts` 计算属性按名称/分类做客户端模糊过滤。

#### 3.2 新增 `POST /api/products`

- 前端 [ProductView.openAddModal](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/ProductView.vue#L55-L59) 弹出表单，`submitForm` 校验后调用 `request.post('/api/products', currentProduct.value)`。
- **Controller**：[ProductController.save()](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/ProductController.java#L29-L32) 接收 `@RequestBody Product`，转交 `productService.save(product)`，返回布尔结果。
- **Service/Mapper/Entity**：`save()` 由 MyBatis-Plus `IService` 默认实现，调用 `BaseMapper.insert(product)`，由于 `@TableId(type = IdType.AUTO)`，主键回填到 `Product.id`；未传 `createTime/updateTime`，依赖数据库列默认值 `CURRENT_TIMESTAMP` 写入。
- 成功后前端关闭模态框并 `fetchProducts()` 重新拉列表。

#### 3.3 修改 `PUT /api/products`

- 前端 [openEditModal](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/ProductView.vue#L61-L65) 拷贝当前行到 `currentProduct`，提交时走 `request.put('/api/products', currentProduct.value)`。
- **Controller**：[ProductController.update()](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/ProductController.java#L34-L37) 调用 `productService.updateById(product)`，最终路由到 `BaseMapper.updateById(entity)`：根据主键 `id` 动态更新非空字段；`update_time` 由 SQL `ON UPDATE CURRENT_TIMESTAMP` 自动刷新。

#### 3.4 删除 `DELETE /api/products/{id}`

- 前端 [confirmDelete](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/ProductView.vue#L97-L100) 弹出 `UIConfirm`，确认后调用 `request.delete(\`/api/products/${id}\`)`。
- **Controller**：[ProductController.delete()](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/ProductController.java#L39-L42) 路径变量 `id` 转交 `productService.removeById(id)`；底层为 `BaseMapper.deleteById(id)`，物理删除一条 `product` 记录。

#### 3.5 详情 `GET /api/products/{id}`

- 后端 [getById()](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/ProductController.java#L24-L27) 已实现，前端当前未使用（编辑直接复用列表数据）。

### 4. 通用响应包装

[CommonResult](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/common/CommonResult.java) 是统一返回体：`{code, message, data}`，成功 `200`，失败 `500`。前端 [request.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/utils/request.js#L21-L31) 据此判断业务成功，否则派发 `api-error` 事件供 [UIToast.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/components/UIToast.vue) 等组件订阅展示。

---

## 二、潜在风险

1. **认证形同虚设**：[AuthController.login()](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/AuthController.java#L17) 用户名/密码硬编码为 `admin/123456`，且 `token` 固定为字符串 `"admin-token"`，没有签名/过期/刷新机制。任何人直接 `Bearer admin-token` 即可冒充。
2. **后端无鉴权过滤**：`/api/products/**` 没有 `Spring Security`、拦截器或过滤器对 `Authorization` 做校验，**未登录可直接调用**所有产品 CRUD 接口，前端的 `requiresAuth` 仅是“前端门禁”，绕过路由守卫即可访问。
3. **CORS 全开**：两个 Controller 上都使用 `@CrossOrigin(origins = "*")`，生产环境存在跨站请求伪造与凭据泄露风险，应限制为可信源。
4. **入参未校验**：`Product` 与 `LoginRequest` 上未使用 `@Valid`/`@NotBlank` 等约束；前端 [submitForm](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/ProductView.vue#L67-L95) 仅做了名称非空与价格 > 0 的弱校验，且校验可被绕过；`price` 用前端 `type=number`，但提交时是字符串赋给 `BigDecimal`，依赖 Jackson 自动转换，输入异常字符会抛 400。
5. **缺乏全局异常处理**：未见 `@RestControllerAdvice`，一旦 Service 抛出 `DataAccessException`、`NumberFormatException` 等，将以默认 Whitelabel 错误返回，前端响应拦截器读 `res.code` 时会因 `undefined !== 200` 走错分支并丢失原始错误。
6. **明文密码**：登录密码以明文形式在 HTTP 报文与日志中传输（`mybatis-plus.configuration.log-impl: StdOutImpl` 也会标准输出 SQL，但登录此处无 SQL）；同时 Spring Boot SQL 明文打印在生产环境会泄露业务数据。
7. **登出未失效**：`/api/auth/logout` 不做任何处理，前端 `handleLogout` 也未调用该接口，只清 `localStorage`；旧 token 仍可使用（虽然本身就是固定串）。
8. **localStorage 存 token**：易受 XSS 攻击窃取，应考虑 `HttpOnly Cookie`+ CSRF Token。
9. **登录页 Bug**：[LoginView.vue#L19](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/LoginView.vue#L19) 写成 `loading.ref = true`，应为 `loading.value = true`，导致按钮 loading 状态从未被开启，但会在 `finally` 中 `loading.value = false` 触发响应式（无副作用，仅功能缺失）。
10. **CRUD 无分页**：`productService.list()` 无条件全表扫描，数据量大时存在性能与流量问题；未来应迁移到 `IPage<Product>` 分页方案。
11. **删除为物理删除**：`removeById` 直接 `DELETE`，无逻辑删除字段，操作不可恢复，且无审计日志。
12. **数据库连接信息明文**：[application.yml](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/application.yml#L4-L8) 中 `root/root` 明文配置，建议使用环境变量与 Docker secrets。
13. **`spring.sql.init.mode: always`**：每次启动都会重跑 `data.sql`，[data.sql](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/db/data.sql) 中的 `INSERT` 不带 `ON DUPLICATE KEY` 约束，会持续累积重复数据。
14. **前端 `price.toLocaleString` 风险**：[ProductView.vue#L225](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/ProductView.vue#L225) 假设后端返回 `number`，但 Jackson 默认会把 `BigDecimal` 序列化为数字字符串/数字（JS 端是数字），仍可能因精度损失出现展示偏差；建议统一字符串处理或使用 `decimal.js`。
15. **错误冒泡**：响应拦截器在 `code !== 200` 时 `Promise.reject`，调用方多用 `catch` 仅 `console.error`，用户层面无法感知具体错误，唯一感知途径是 `api-error` 全局事件 → Toast。

---

## 三、技术细节

### 3.1 后端栈

- **Spring Boot + MyBatis-Plus**：通过 `ServiceImpl<ProductMapper, Product>` 与 `BaseMapper<Product>` 自动获得 `list/getById/save/updateById/removeById/page` 等方法，[ProductServiceImpl](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/service/impl/ProductServiceImpl.java) 与 [ProductMapper](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/mapper/ProductMapper.java) 都为空壳（零业务代码）。
- **Lombok**：[Product](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/entity/Product.java)、[CommonResult](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/common/CommonResult.java)、[LoginRequest](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/dto/LoginRequest.java) 通过 `@Data/@AllArgsConstructor/@NoArgsConstructor` 自动生成 getter/setter/构造器。
- **ORM 映射**：实体字段 `createTime/updateTime` 借助 `mybatis-plus.configuration.map-underscore-to-camel-case: true` 与数据库 `create_time/update_time` 互通；时间默认值由 SQL 语义提供（`DEFAULT CURRENT_TIMESTAMP / ON UPDATE CURRENT_TIMESTAMP`），后端无 `@TableField(fill = ...)` 自动填充处理器。
- **DB 初始化**：`spring.sql.init.schema-locations` & `data-locations` 指定 `classpath:db/schema.sql` 与 `db/data.sql`，启动时执行；`schema.sql` 使用 `CREATE TABLE IF NOT EXISTS`，但 `data.sql` 是无判断 `INSERT`。
- **路由前缀**：`@RequestMapping("/api/auth")` 与 `@RequestMapping("/api/products")`；REST 动词使用规范（GET/POST/PUT/DELETE）；`@PathVariable Long id` 用于 `/{id}` 风格。
- **统一响应**：`CommonResult<T>` 静态工厂 `success(data)` / `failed(message)`，code=200/500。
- **MyBatis-Plus SQL 日志**：`log-impl: StdOutImpl` 启用控制台输出，便于本地调试。
- **应用打包**：通过 [Dockerfile](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/Dockerfile) 构建镜像，端口 8080。

### 3.2 前端栈

- **Vue 3 + Composition API**：`<script setup>` 风格；状态使用 `ref/computed`；路由使用 `vue-router 4` 的 `createWebHistory`。
- **路由守卫**：[router/index.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/router/index.js#L22-L29) `beforeEach` 检查 `localStorage.token`，仅前端层面拦截。
- **Axios 实例**：[request.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/utils/request.js) 设置 `baseURL = import.meta.env.VITE_API_BASE_URL || ''`、`timeout=10000`；请求拦截器注入 `Bearer Token`；响应拦截器解 `CommonResult` 包装并通过 `window.dispatchEvent(new CustomEvent('api-error', ...))` 传递错误。
- **UI 框架**：WindiCSS（按 `virtual:windi.css`），通过 [vite.config.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/vite.config.js) 启用 `WindiCSS()` 插件；样式以 utility-class 为主。
- **跨域与代理**：开发期 Vite proxy `/api` → `http://backend:8080`；生产期 Nginx `location /api` 反代到同一后端服务；二者都依赖 docker-compose 中的服务名 `backend`。
- **登录交互**：[LoginView.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/LoginView.vue) 表单两字段 + 提交按钮，成功后 `localStorage.setItem('token', res.token)` 并跳转 `/`。
- **产品页交互**：[ProductView.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/ProductView.vue) 包含搜索框（`computed filteredProducts`）、新增/编辑模态框（共用 `currentProduct` + `isEdit` 切换）、删除确认（[UIConfirm.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/components/UIConfirm.vue)）、骨架屏 (`v-if="loading" v-for="i in 5"`) 与空状态。
- **全局通知**：通过 `window.dispatchEvent(new CustomEvent('show-toast', { detail }))` 让 [UIToast.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/components/UIToast.vue) 监听并展示。

### 3.3 关键时序（登录 + 列表）

```text
浏览器 ── POST /api/auth/login {username,password}
        │  axios(baseURL='') → vite proxy → http://backend:8080
        ▼
AuthController.login(LoginRequest)
        │  硬编码校验 admin/123456
        ▼
CommonResult.success({token:'admin-token', username:'管理员'})
        │
浏览器 ── localStorage.token = 'admin-token'  →  router.push('/')

浏览器 ── GET /api/products    (Authorization: Bearer admin-token)
        ▼
ProductController.list()
        ▼
ProductServiceImpl.list()  (= ServiceImpl.list())
        ▼
ProductMapper.selectList(null) (= BaseMapper.selectList)
        ▼  SQL: SELECT id,name,price,category,description,create_time,update_time FROM product
MySQL → ResultSet → Product[]
        ▼
CommonResult.success(List<Product>)
        ▼
request.js 拦截器解包 → ProductView.products = [...]
```

### 3.4 端到端编排

- 顶层 [docker-compose.yml](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/docker-compose.yml) 串联 `frontend`（Nginx 静态托管）+ `backend`（Spring Boot）+ `db`（MySQL，host 名 `db`）三个服务；`application.yml` 中 `jdbc:mysql://db:3306/pricing_db` 与 `nginx.conf`/`vite.config.js` 中 `http://backend:8080` 均依赖此命名网络。

---

> 总结：本仓库以最小化样板演示了一个 Spring Boot + MyBatis-Plus 的产品 CRUD 与一个“假认证”登录链路，前端用 Vue 3 + WindiCSS 完成 UI 闭环。代码结构清晰、分层标准，但 **完全缺乏真正的鉴权、参数校验、异常处理与生产安全考量**，仅适合作为学习示例或脚手架，**不可直接用于生产**。
