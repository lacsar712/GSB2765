# 全链路代码分析报告

## 逻辑说明

### 一、项目整体架构

本项目为前后端分离的产品定价管理系统，技术栈如下：

- **后端**：Spring Boot 3.2.2 + MyBatis-Plus 3.5.5 + MySQL + Lombok，运行于 8080 端口
- **前端**：Vue 3 (Composition API) + Vue Router + Axios + Windi CSS，通过 Nginx 反向代理后端
- **部署**：Docker Compose 编排，包含 `backend`、`frontend`、`db`（MySQL）三个服务

后端采用经典的 **Controller → Service → Mapper → Entity** 四层分层架构；前端采用 Vue 单文件组件结构，通过 Axios 封装统一请求/响应拦截。

---

### 二、登录链路：`POST /api/auth/login`

登录链路未经过完整四层分层（无 Service/Mapper/Entity），仅在 Controller 层直接处理。

#### 2.1 前端发起

1. 用户在 [LoginView.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/LoginView.vue#L11-L38) 填写用户名和密码，点击「立即登录」触发 `handleLogin()`。
2. 基础校验：用户名和密码均不能为空，空值通过 `window.dispatchEvent(new CustomEvent('show-toast', ...))` 触发全局 Toast 提示。
3. 调用 [request.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/utils/request.js#L3-L6) 创建的 Axios 实例，发送 `POST /api/auth/login`，请求体为 `{ username, password }`。
4. **请求拦截器**（[request.js:8-19](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/utils/request.js#L8-L19)）尝试从 `localStorage` 读取 token 注入 `Authorization` 头（首次登录时 token 为空，此步不附加头）。
5. **响应拦截器**（[request.js:21-36](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/utils/request.js#L21-L36)）解包后端统一响应 `CommonResult`：若 `code !== 200` 则派发 `api-error` 事件并 reject；若 `code === 200` 则直接返回 `res.data`（剥离外层包装）。
6. 登录成功后，前端将 `res.token` 和 `res.username` 写入 `localStorage`，然后通过 `router.push('/')` 跳转到产品列表页。
7. 路由守卫（[router/index.js:22-29](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/router/index.js#L22-L29)）在每次路由切换前检查目标路由 `meta.requiresAuth` 和 `localStorage.token`，无 token 时强制跳转 `/login`。

#### 2.2 后端处理（Controller 层）

1. [AuthController.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/AuthController.java#L10-L25) 接收请求，类级注解 `@RequestMapping("/api/auth")`、方法级注解 `@PostMapping("/login")`。
2. Spring MVC 将 JSON 请求体反序列化为 [LoginRequest.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/dto/LoginRequest.java) DTO（仅含 `username`、`password` 两个字段，使用 Lombok `@Data`）。
3. **硬编码凭据校验**：判断 `"admin".equals(username) && "123456".equals(password)`。
   - 成功：构造 `Map<String, String>`，放入 `token = "admin-token"` 和 `username = "管理员"`，返回 `CommonResult.success(data)`（code=200）。
   - 失败：返回 `CommonResult.failed("用户名或密码不正确")`（code=500）。
4. 返回值由 Spring 自动序列化为 JSON，通过 HTTP 响应返回给前端。

> 注意：登录接口 **完全跳过** Service/Mapper/Entity 层，不查询数据库，不验证密码哈希，不生成 JWT。

---

### 三、产品 CRUD 链路：`/api/products`

产品 CRUD 严格遵循 **Controller → Service → Mapper → Entity** 四层分层，Service/Mapper 均继承自 MyBatis-Plus 通用基类，未添加任何自定义方法。

#### 3.1 Entity 层（数据模型）

[Product.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/entity/Product.java)

- 使用 `@Data`（Lombok）自动生成 getter/setter/toString/equals/hashCode。
- `@TableName("product")` 映射数据库表 `product`。
- `@TableId(type = IdType.AUTO)` 主键 `id` 自增，与 [application.yml:21](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/application.yml#L21) 中 `mybatis-plus.global-config.db-config.id-type: auto` 一致。
- 字段列表：

  | Java 字段        | 数据库列       | 类型              | 说明                     |
  |-----------------|---------------|-------------------|--------------------------|
  | `id`            | `id`          | `Long`            | 自增主键                 |
  | `name`          | `name`        | `String`          | 产品名称（NOT NULL）     |
  | `price`         | `price`       | `BigDecimal`      | 产品价格 DECIMAL(10,2)   |
  | `category`      | `category`    | `String`          | 产品分类                 |
  | `description`   | `description` | `String`          | 产品描述 TEXT            |
  | `createTime`    | `create_time` | `LocalDateTime`   | 创建时间，默认当前时间   |
  | `updateTime`    | `update_time` | `LocalDateTime`   | 更新时间，ON UPDATE 自动 |

- MyBatis-Plus 配置 `map-underscore-to-camel-case: true`（[application.yml:18](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/application.yml#L18)），自动完成下划线到驼峰的映射。

数据库初始化脚本：
- [schema.sql](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/db/schema.sql)：建表语句，InnoDB 引擎，utf8mb4 字符集，`update_time` 使用 `ON UPDATE CURRENT_TIMESTAMP` 自动维护。
- [data.sql](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/db/data.sql)：初始化 5 条产品数据（笔记本、耳机、键盘、手表、椅子）。
- [application.yml:10-13](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/application.yml#L10-L13) 配置 `spring.sql.init.mode: always`，**每次应用启动都会重新执行 schema.sql 和 data.sql**。

#### 3.2 Mapper 层（数据访问）

[ProductMapper.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/mapper/ProductMapper.java)

- 继承 `com.baomidou.mybatisplus.core.mapper.BaseMapper<Product>`。
- 接口体内为空，**无任何自定义 SQL 方法**。
- MyBatis-Plus 在启动时自动为 `BaseMapper` 生成以下常用 CRUD SQL：
  - `selectList(Wrapper)` → `SELECT * FROM product`
  - `selectById(Serializable)` → `SELECT * FROM product WHERE id = ?`
  - `insert(T)` → `INSERT INTO product (...) VALUES (...)`
  - `updateById(T)` → `UPDATE product SET ... WHERE id = ?`
  - `deleteById(Serializable)` → `DELETE FROM product WHERE id = ?`
- 配合 MyBatis-Plus Spring Boot Starter 自动注册为 Mapper Bean，无需 `@Mapper` 注解或 XML 映射文件。

#### 3.3 Service 层（业务逻辑）

- 接口定义：[ProductService.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/service/ProductService.java) 继承 `IService<Product>`（MyBatis-Plus 通用 Service 接口）。
- 实现类：[ProductServiceImpl.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/service/impl/ProductServiceImpl.java)
  - 继承 `ServiceImpl<ProductMapper, Product>`
  - 实现 `ProductService` 接口
  - 添加 `@Service` 注解交由 Spring 容器管理
  - **类体为空，无任何自定义业务方法**，完全依赖 `ServiceImpl` 提供的默认 CRUD 实现：
    - `list()` → 查询全部
    - `getById(id)` → 按 ID 查询
    - `save(entity)` → 新增
    - `updateById(entity)` → 按 ID 更新
    - `removeById(id)` → 按 ID 删除

#### 3.4 Controller 层（接口暴露）

[ProductController.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/ProductController.java)

- `@RestController` + `@RequestMapping("/api/products")`，`@CrossOrigin(origins = "*")` 允许跨域。
- 通过 `@Autowired` 注入 `ProductService`。

5 个端点的调用链如下：

| HTTP 方法 | 路径             | Controller 方法     | Service 方法              | Mapper 方法（由 MP 路由）       | 数据库操作                  |
|----------|------------------|--------------------|---------------------------|--------------------------------|-----------------------------|
| GET      | `/api/products`  | `list()`           | `productService.list()`   | `BaseMapper.selectList(null)`  | `SELECT * FROM product`     |
| GET      | `/api/products/{id}` | `getById(id)`  | `productService.getById()`| `BaseMapper.selectById(id)`    | `SELECT * FROM product WHERE id=?` |
| POST     | `/api/products`  | `save(product)`    | `productService.save()`   | `BaseMapper.insert(product)`   | `INSERT INTO product ...`   |
| PUT      | `/api/products`  | `update(product)`  | `productService.updateById()` | `BaseMapper.updateById()`  | `UPDATE product SET ... WHERE id=?` |
| DELETE   | `/api/products/{id}` | `delete(id)`   | `productService.removeById()` | `BaseMapper.deleteById()`  | `DELETE FROM product WHERE id=?` |

所有返回值均通过 [CommonResult.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/common/CommonResult.java) 统一包装：
- `CommonResult.success(data)` → `{ code: 200, message: "操作成功", data: ... }`
- `CommonResult.failed(msg)` → `{ code: 500, message: msg, data: null }`

#### 3.5 前端消费（数据流完整闭环）

[ProductView.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/ProductView.vue) 在组件挂载时（`onMounted(fetchProducts)`）：

1. `fetchProducts()` → `request.get('/api/products')`：
   - 请求拦截器读取 localStorage 中的 token，附加 `Authorization: Bearer admin-token`。
   - 响应拦截器解包 CommonResult，直接返回产品数组（`List<Product>` 的 JSON）。
   - 赋值给响应式变量 `products`。

2. 新增产品：`openAddModal()` → 表单填写 → `submitForm()` → `request.post('/api/products', currentProduct)`。

3. 编辑产品：`openEditModal(product)` → 表单回显 → `submitForm()` → `request.put('/api/products', currentProduct)`。

4. 删除产品：`confirmDelete(id)` → 弹出 [UIConfirm.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/components/UIConfirm.vue) 二次确认 → `handleDelete()` → `request.delete('/api/products/{id}')`。

5. 前端搜索通过**本地过滤**实现（`filteredProducts` computed 属性），不发送到后端。

6. 退出登录：`handleLogout()` 清除 localStorage token 和 username，路由跳转 `/login`（不调用后端 `/api/auth/logout` 接口）。

7. 全局 Toast 通知由 [App.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/App.vue#L7-L26) 监听 `show-toast` 和 `api-error` 自定义事件驱动，使用 [UIToast.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/components/UIToast.vue) 组件渲染，3.5 秒后自动消失。

---

## 潜在风险

### 1. 认证与安全风险（严重）

- **硬编码凭据**：用户名 `admin`/密码 `123456` 直接写死在 [AuthController.java:17](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/AuthController.java#L17)，无数据库用户表，无密码哈希（BCrypt/Argon2），任何人查看源码即可获取管理员权限。
- **Token 完全伪造且无校验**：后端返回固定字符串 `"admin-token"`，**没有任何接口对 token 进行校验**。产品 CRUD 接口无 `@PreAuthorize`、无 Spring Security 过滤器、无拦截器验证 token 合法性。前端请求拦截器虽然带上了 `Authorization` 头，但后端完全忽略它。攻击者可直接调用 `/api/products` 任意增删改查，无需登录。
- **CORS 全开**：`@CrossOrigin(origins = "*")` 在两个 Controller 上均设置，允许任意域跨域访问，生产环境下会导致 CSRF 风险。
- **SQL 初始化配置危险**：`spring.sql.init.mode=always` 导致每次重启应用都会重新执行 `data.sql`，**新增/修改的产品数据会被重置**为初始 5 条。
- **无 HTTPS**：jdbc URL 中 `useSSL=false`，应用层也未强制 HTTPS，数据明文传输。

### 2. 代码健壮性风险

- **前端 Bug**：[LoginView.vue:19](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/LoginView.vue#L19) 中 `loading.ref = true` 应为 `loading.value = true`（`.ref` 是错误写法，正确为 `.value`），会导致按钮 loading 状态在登录开始时无法正确设置。
- **空指针/异常处理缺失**：`ProductController.getById()` 若传入不存在的 ID，MyBatis-Plus 返回 `null`，Controller 直接包装为 `CommonResult.success(null)` 返回，无 404 处理；`save()` 和 `update()` 均无参数校验（虽引入了 `spring-boot-starter-validation` 依赖但未使用 `@Valid` 和 `@NotBlank` 等注解），传入空 name 或负 price 会直接写入数据库（name 在数据库层面 NOT NULL 会抛异常）。
- **Service 层形同虚设**：`ProductServiceImpl` 完全空实现，未封装任何业务逻辑，未来若增加多表关联、事务、业务校验，需要重构。
- **退出登录前后端不一致**：前端 `handleLogout()` 仅清除本地存储，未调用 `/api/auth/logout`；即便调用了该接口，后端也无实际注销逻辑（只返回字符串）。
- **时间字段处理**：新增产品时前端未传入 `createTime`/`updateTime`，依赖数据库默认值，这是正确做法；但更新时 `update_time` 由 MySQL `ON UPDATE CURRENT_TIMESTAMP` 维护，而 Product 实体中传入的 `updateTime`（若前端传递了）可能会干扰该行为。
- **价格精度**：前端使用 `type="number" step="0.01"`，JavaScript 浮点数运算存在精度问题，应使用字符串或分单位整数传递。

### 3. 工程规范风险

- **统一错误码缺失**：业务失败统一返回 HTTP 200 + code=500，无细分错误码（如 400 参数错误、401 未认证、403 无权限、404 资源不存在），前端无法根据错误码做精细化处理。
- **HTTP 方法语义不准确**：`update` 使用 `@PutMapping` 但路径与 `save` 的 `@PostMapping` 相同，均为 `/api/products`，RESTful 风格下 PUT 更新应使用 `/api/products/{id}` 路径。
- **无分页**：`list()` 返回全量产品数据，数据量增大时会有性能问题，应分页查询。
- **日志配置**：MyBatis-Plus 开启了 `StdOutImpl` SQL 日志打印到标准输出，生产环境应关闭。

---

## 技术细节

### 1. 后端分层架构细节

```
HTTP Request
    │
    ▼
┌─────────────────────────────────────────┐
│  Controller 层                          │
│  (AuthController / ProductController)   │
│  - @RestController / @RequestMapping    │
│  - 参数绑定 (@RequestBody / @PathVariable)│
│  - 调用 Service                         │
│  - 包装 CommonResult 返回               │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  Service 层                             │
│  (ProductService / ProductServiceImpl)  │
│  - 接口继承 IService<Product>           │
│  - 实现继承 ServiceImpl<Mapper, Entity> │
│  - @Service 注解                        │
│  - 默认 CRUD 由 MyBatis-Plus 提供       │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  Mapper 层                              │
│  (ProductMapper)                        │
│  - 继承 BaseMapper<Product>             │
│  - MyBatis-Plus 自动实现 SQL            │
│  - 无 XML 映射文件                      │
└──────────────────┬──────────────────────┘
                   │
                   ▼
┌─────────────────────────────────────────┐
│  Entity 层                              │
│  (Product)                              │
│  - @TableName 映射表名                  │
│  - @TableId 主键策略                    │
│  - @Data (Lombok) 生成访问器            │
│  - 下划线 ↔ 驼峰自动转换                │
└──────────────────┬──────────────────────┘
                   │
                   ▼
              MySQL Database
              (product 表)
```

### 2. MyBatis-Plus 关键配置

| 配置项                                        | 值                           | 作用                                   |
|-----------------------------------------------|------------------------------|----------------------------------------|
| `mybatis-plus.configuration.log-impl`         | `StdOutImpl`                 | 打印 SQL 到控制台                      |
| `mybatis-plus.configuration.map-underscore-to-camel-case` | `true`         | 数据库下划线自动转为 Java 驼峰         |
| `mybatis-plus.global-config.db-config.id-type`| `auto`                       | 全局主键自增，与 `@TableId(type=AUTO)` 协同 |
| `spring.sql.init.mode`                        | `always`                     | 每次启动执行建表和数据初始化脚本       |

### 3. 前端请求封装机制

[request.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/utils/request.js) 的设计模式：

- **Axios 实例创建**：`baseURL` 取环境变量 `VITE_API_BASE_URL`，超时 10 秒。
- **请求拦截器**：统一注入 `Authorization: Bearer <token>` 头，实现类 JWT 认证的前端支持（但后端不校验）。
- **响应拦截器**：实现了响应体自动解包——后端所有接口返回 `CommonResult`，前端业务代码只需拿到 `data` 部分，无需每次判断 `code`。若 `code !== 200`，拦截器自动派发 `api-error` 全局事件，由 [App.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/App.vue#L15-L17) 统一监听并弹出错误 Toast，避免每个组件重复处理错误提示。
- **前端业务调用方式**：由于响应拦截器直接返回 `res.data`，组件中 `const res = await request.get('/api/products')` 拿到的 `res` 就是产品数组本身，而不是 `{ code: 200, data: [...] }`。登录接口同理，`res.token` 直接对应后端 Map 中的 `token` 字段。

### 4. 路由守卫与认证状态管理

[router/index.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/router/index.js#L22-L29) 的 `beforeEach` 全局前置守卫：

- 访问 `/`（产品页，`meta.requiresAuth: true`）时检查 `localStorage.getItem('token')`。
- 无 token → `next('/login')` 强制跳转登录页。
- 有 token 或访问 `/login`（无 `requiresAuth`）→ `next()` 放行。
- 认证状态完全依赖 `localStorage`，无 Vuex/Pinia 等全局状态管理，刷新页面从 localStorage 恢复。

### 5. 全链路数据流图（查询列表）

```
[浏览器]
   │  用户访问 / → router.beforeEach 检查 token → 放行
   ▼
[ProductView.vue onMounted]
   │  fetchProducts()
   ▼
[request.js 请求拦截器]
   │  注入 Authorization: Bearer admin-token
   ▼
[HTTP GET /api/products]
   ▼
[ProductController.list()]
   │  return CommonResult.success(productService.list())
   ▼
[ProductServiceImpl.list()]
   │  (继承自 ServiceImpl) → this.baseMapper.selectList(null)
   ▼
[ProductMapper.selectList(null)]
   │  (MyBatis-Plus BaseMapper 实现)
   │  SQL: SELECT id,name,price,category,description,create_time,update_time FROM product
   ▼
[MySQL product 表]
   │  返回 5 行初始数据
   ▼
[MyBatis ResultSetHandler]
   │  下划线→驼峰映射 → List<Product>
   ▼
[CommonResult<List<Product>>{code:200, data:[...]}]
   │  Spring HttpMessageConverter (Jackson) 序列化为 JSON
   ▼
[HTTP Response]
   ▼
[request.js 响应拦截器]
   │  code === 200 → return res.data
   ▼
[ProductView.vue]
   │  products.value = res  (List<Product>)
   ▼
[Vue 响应式渲染 → 表格展示产品列表]
```

### 6. 关键文件索引

| 层级       | 文件                                                                                              |
|-----------|---------------------------------------------------------------------------------------------------|
| 通用响应   | [CommonResult.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/common/CommonResult.java) |
| Controller | [AuthController.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/AuthController.java)、[ProductController.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/controller/ProductController.java) |
| DTO       | [LoginRequest.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/dto/LoginRequest.java) |
| Service   | [ProductService.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/service/ProductService.java)、[ProductServiceImpl.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/service/impl/ProductServiceImpl.java) |
| Mapper    | [ProductMapper.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/mapper/ProductMapper.java) |
| Entity    | [Product.java](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/java/com/pricing/entity/Product.java) |
| 前端请求  | [request.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/utils/request.js) |
| 前端路由  | [index.js](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/router/index.js) |
| 登录页    | [LoginView.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/LoginView.vue) |
| 产品页    | [ProductView.vue](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/frontend/src/views/ProductView.vue) |
| 数据库    | [schema.sql](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/db/schema.sql)、[data.sql](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/db/data.sql) |
| 配置      | [application.yml](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/src/main/resources/application.yml)、[pom.xml](file:///e:/lzg/code/GSB0608/label-2765/GSB2765/backend/pom.xml) |
