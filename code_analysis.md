# 产品定价管理系统 — 全链路代码分析

## 一、逻辑说明

### 1.1 系统架构概览

| 层级 | 技术栈 | 说明 |
|------|--------|------|
| 前端 | Vue 3.4 + Vite 5 + Vue Router 4 + Axios + WindiCSS | SPA 单页应用，Nginx 托管生产构建产物并反向代理 `/api` |
| 后端 | Spring Boot 3.2.2 + Java 17 + MyBatis-Plus 3.5.5 + Lombok | RESTful API，端口 8080 |
| 数据库 | MySQL 8.0 | `pricing_db` 库，`product` 表 |
| 部署 | Docker Compose | 三个服务：`db`、`backend`、`frontend` |

### 1.2 登录链路：`/api/auth/login`

#### 前端调用流程

1. 用户在 [LoginView.vue](frontend/src/views/LoginView.vue) 输入用户名和密码，点击「立即登录」
2. `handleLogin()` 方法先做空值校验，然后调用 `request.post('/api/auth/login', { username, password })`
3. [request.js](frontend/src/utils/request.js) 中 Axios 请求拦截器从 `localStorage` 读取 token 并注入 `Authorization: Bearer {token}` 请求头
4. 响应拦截器判断 `res.code !== 200` 时触发 `api-error` 自定义事件，否则直接返回 `res.data`（即解包 `CommonResult` 的 `data` 字段）
5. 登录成功后，将 `res.token`（值为 `"admin-token"`）和 `res.username`（值为 `"管理员"`）存入 `localStorage`，然后 `router.push('/')` 跳转至产品页
6. [router/index.js](frontend/src/router/index.js) 的全局前置守卫 `beforeEach` 检查 `localStorage.getItem('token')`，无 token 时重定向至 `/login`

#### 后端处理流程

```
HTTP POST /api/auth/login
  → AuthController.login(@RequestBody LoginRequest)
    → 硬编码校验: "admin".equals(username) && "123456".equals(password)
      → 成功: CommonResult.success(Map{"token":"admin-token", "username":"管理员"})
      → 失败: CommonResult.failed("用户名或密码不正确")
```

- **Controller 层**：[AuthController.java](backend/src/main/java/com/pricing/controller/AuthController.java)，`@RestController` + `@RequestMapping("/api/auth")`，`@CrossOrigin(origins = "*")`
- **Service 层**：无，认证逻辑直接写在 Controller 中
- **Mapper/Entity 层**：无，不涉及数据库操作
- **DTO**：[LoginRequest.java](backend/src/main/java/com/pricing/dto/LoginRequest.java)，仅含 `username` 和 `password` 两个字段，无校验注解
- **统一响应**：[CommonResult.java](backend/src/main/java/com/pricing/common/CommonResult.java)，泛型封装 `{ code, message, data }`，成功 code=200，失败 code=500

#### 数据流图

```
[浏览器 LoginView]
  → POST /api/auth/login { username, password }
  → [Nginx 反向代理] → [Spring Boot AuthController]
  → 硬编码比对 admin/123456
  → 返回 CommonResult { code:200, data: { token:"admin-token", username:"管理员" } }
  → [Axios 响应拦截器] 解包为 { token:"admin-token", username:"管理员" }
  → 存入 localStorage → 跳转 /
```

### 1.3 产品增删改查链路：`/api/products`

#### 前端调用流程

1. [ProductView.vue](frontend/src/views/ProductView.vue) 在 `onMounted` 时调用 `fetchProducts()` → `request.get('/api/products')` 获取全量产品列表
2. 前端搜索 `searchKey` 通过 `computed` 属性 `filteredProducts` 在客户端做 `name`/`category` 模糊过滤，无后端分页/搜索接口
3. **新增**：`openAddModal()` → 弹窗填写 → `submitForm()` → `request.post('/api/products', currentProduct)`
4. **编辑**：`openEditModal(product)` → 弹窗回填 → `submitForm()` → `request.put('/api/products', currentProduct)`
5. **删除**：`confirmDelete(id)` → 确认弹窗 → `handleDelete()` → `request.delete('/api/products/{id}')`
6. 每次增/改/删成功后重新调用 `fetchProducts()` 刷新列表

#### 后端处理流程（Controller → Service → Mapper → Entity）

```
HTTP GET    /api/products       → ProductController.list()        → productService.list()        → ProductMapper.selectList(null)   → SELECT * FROM product
HTTP GET    /api/products/{id}  → ProductController.getById(id)   → productService.getById(id)   → ProductMapper.selectById(id)     → SELECT * FROM product WHERE id=?
HTTP POST   /api/products       → ProductController.save(product) → productService.save(product)  → ProductMapper.insert(product)    → INSERT INTO product (name,price,category,description) VALUES (?,?,?,?)
HTTP PUT    /api/products       → ProductController.update(product)→ productService.updateById(product)→ ProductMapper.updateById(product)→ UPDATE product SET ... WHERE id=?
HTTP DELETE /api/products/{id}  → ProductController.delete(id)    → productService.removeById(id) → ProductMapper.deleteById(id)     → DELETE FROM product WHERE id=?
```

##### 各层详解

**Controller 层** — [ProductController.java](backend/src/main/java/com/pricing/controller/ProductController.java)

- `@RestController` + `@RequestMapping("/api/products")` + `@CrossOrigin(origins = "*")`
- 通过 `@Autowired` 注入 `ProductService`
- 五个端点分别对应 CRUD + 列表查询，所有返回值统一用 `CommonResult<T>` 包装
- 无参数校验注解（`@Valid`/`@Validated`），无异常处理（`@ExceptionHandler`）

**Service 层** — [ProductService.java](backend/src/main/java/com/pricing/service/ProductService.java) / [ProductServiceImpl.java](backend/src/main/java/com/pricing/service/impl/ProductServiceImpl.java)

- `ProductService` 接口继承 MyBatis-Plus 的 `IService<Product>`，自动获得 `list()`、`getById()`、`save()`、`updateById()`、`removeById()` 等方法签名
- `ProductServiceImpl` 继承 `ServiceImpl<ProductMapper, Product>` 并实现 `ProductService`，空实现体——所有 CRUD 逻辑完全由 MyBatis-Plus 框架提供
- `IService` 的 `save()` 方法内部调用 `BaseMapper.insert()`
- `IService` 的 `updateById()` 方法内部调用 `BaseMapper.updateById()`
- `IService` 的 `removeById()` 方法内部调用 `BaseMapper.deleteById()`
- `IService` 的 `list()` 方法内部调用 `BaseMapper.selectList(null)`

**Mapper 层** — [ProductMapper.java](backend/src/main/java/com/pricing/mapper/ProductMapper.java)

- 继承 `BaseMapper<Product>`，无自定义 SQL 方法
- 通过 [BackendApplication.java](backend/src/main/java/com/pricing/BackendApplication.java) 上的 `@MapperScan("com.pricing.mapper")` 自动扫描注册
- MyBatis-Plus 在运行时自动生成 CRUD SQL，无需 XML 映射文件

**Entity 层** — [Product.java](backend/src/main/java/com/pricing/entity/Product.java)

```java
@Data
@TableName("product")
public class Product {
    @TableId(type = IdType.AUTO)   // 主键自增
    private Long id;
    private String name;            // 产品名称
    private BigDecimal price;       // 产品价格
    private String category;        // 产品分类
    private String description;     // 产品描述
    private LocalDateTime createTime;   // 创建时间（DB 默认 CURRENT_TIMESTAMP）
    private LocalDateTime updateTime;   // 更新时间（DB 默认 CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP）
}
```

- `@TableName("product")` 映射到数据库 `product` 表
- `@TableId(type = IdType.AUTO)` 对应 MySQL `BIGINT AUTO_INCREMENT PRIMARY KEY`
- MyBatis-Plus 配置 `map-underscore-to-camel-case: true`，`create_time` ↔ `createTime` 自动映射
- `createTime` 和 `updateTime` 由数据库默认值管理，Entity 层未配置 `@TableField(fill = ...)`

#### 数据库 Schema — [schema.sql](backend/src/main/resources/db/schema.sql)

```sql
CREATE TABLE IF NOT EXISTS product (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);
```

初始数据由 [data.sql](backend/src/main/resources/db/data.sql) 插入 5 条产品记录。

### 1.4 前端全局机制

| 机制 | 实现位置 | 说明 |
|------|----------|------|
| 路由守卫 | [router/index.js](frontend/src/router/index.js) | `beforeEach` 检查 `localStorage.token`，无 token 重定向 `/login` |
| 请求拦截 | [request.js](frontend/src/utils/request.js) | 请求拦截器注入 `Authorization: Bearer {token}`；响应拦截器解包 `CommonResult`，失败时派发 `api-error` 事件 |
| Toast 通知 | [App.vue](frontend/src/App.vue) + [UIToast.vue](frontend/src/components/UIToast.vue) | 监听 `api-error` 和 `show-toast` 自定义事件，3.5 秒自动消失 |
| 确认弹窗 | [UIConfirm.vue](frontend/src/components/UIConfirm.vue) | 删除操作二次确认，emit `confirm`/`cancel` 事件 |
| API 代理 | [vite.config.js](frontend/vite.config.js) / [nginx.conf](frontend/nginx.conf) | 开发环境 Vite 代理 `/api` → `http://backend:8080`；生产环境 Nginx 代理 `/api` → `http://backend:8080` |

---

## 二、潜在风险

### 2.1 认证与授权（高危）

| 风险 | 位置 | 说明 |
|------|------|------|
| 硬编码凭据 | [AuthController.java:17](backend/src/main/java/com/pricing/controller/AuthController.java#L17) | 用户名密码 `admin/123456` 直接写死在代码中，无法修改、无法多用户支持 |
| 伪 Token | [AuthController.java:19](backend/src/main/java/com/pricing/controller/AuthController.java#L19) | 返回的 token 是固定字符串 `"admin-token"`，非 JWT/签名，任何人可伪造 |
| 无后端鉴权 | 全部 Controller | 后端无任何 Filter/Interceptor 校验请求携带的 token，`/api/products` 接口完全裸露，可被未认证用户直接访问 |
| 密码明文传输 | [LoginRequest.java](backend/src/main/java/com/pricing/dto/LoginRequest.java) | 密码以明文 JSON 传输，生产环境应使用 HTTPS + 前端加密或 OAuth2 |
| CORS 全开 | `@CrossOrigin(origins = "*")` | 所有 Controller 均配置 `origins = "*"`，允许任何域跨域请求，存在 CSRF 风险 |

### 2.2 数据安全（高危）

| 风险 | 位置 | 说明 |
|------|------|------|
| 无参数校验 | [ProductController.java](backend/src/main/java/com/pricing/controller/ProductController.java) | `save()`/`update()` 的 `@RequestBody Product` 未使用 `@Valid` 注解，`name` 可为空、`price` 可为负数 |
| 无删除权限控制 | [ProductController.java:39-42](backend/src/main/java/com/pricing/controller/ProductController.java#L39) | 任何人可删除任意产品，无软删除、无操作日志 |
| SQL 初始化每次执行 | [application.yml:10](backend/src/main/resources/application.yml#L10) | `spring.sql.init.mode=always` 导致每次应用启动都执行 `schema.sql` 和 `data.sql`，`data.sql` 会重复插入初始数据（虽然 `schema.sql` 用了 `IF NOT EXISTS`，但 `data.sql` 无去重逻辑） |

### 2.3 前端缺陷（中危）

| 风险 | 位置 | 说明 |
|------|------|------|
| loading 赋值 Bug | [LoginView.vue:19](frontend/src/views/LoginView.vue#L19) | `loading.ref = true` 应为 `loading.value = true`，导致登录按钮的 loading 状态永远无法正确设置 |
| Token 存储于 localStorage | [LoginView.vue:25-26](frontend/src/views/LoginView.vue#L25) | `localStorage` 易受 XSS 攻击窃取，建议使用 HttpOnly Cookie |
| 前端鉴权可绕过 | [router/index.js:23](frontend/src/router/index.js#L23) | 仅检查 `localStorage` 中是否存在 token，手动设置任意值即可绕过；且后端无校验，前端鉴权形同虚设 |
| 无分页 | [ProductView.vue:32](frontend/src/views/ProductView.vue#L32) | `GET /api/products` 一次拉取全量数据，数据量大时性能劣化 |
| 客户端搜索 | [ProductView.vue:47-53](frontend/src/views/ProductView.vue#L47) | 搜索在前端 `computed` 中完成，无法利用数据库索引，大数据量下体验差 |

### 2.4 运维与健壮性（低危）

| 风险 | 位置 | 说明 |
|------|------|------|
| 数据库密码硬编码 | [application.yml:7](backend/src/main/resources/application.yml#L7) / [docker-compose.yml:8](docker-compose.yml#L8) | `root/root` 写在配置文件和编排文件中，应使用环境变量或密钥管理 |
| 全局异常处理缺失 | 后端 | 无 `@ControllerAdvice`/`@ExceptionHandler`，数据库异常等直接抛出 500 错误页 |
| MyBatis-Plus 日志全量输出 | [application.yml:17](backend/src/main/resources/application.yml#L17) | `log-impl: StdOutImpl` 在生产环境会暴露完整 SQL 和参数，应按 profile 控制 |
| 无事务管理 | Service 层 | `ProductServiceImpl` 无 `@Transactional` 注解，虽然单表 CRUD 影响不大，但不符合最佳实践 |

---

## 三、技术细节

### 3.1 后端技术栈版本与依赖

| 依赖 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.2.2 | Web 框架 + 自动配置 |
| MyBatis-Plus | 3.5.5 | ORM 增强，提供 `BaseMapper` / `IService` / `ServiceImpl` |
| mybatis-plus-spring-boot3-starter | 3.5.5 | Spring Boot 3 兼容的 MyBatis-Plus Starter |
| mysql-connector-j | Runtime | MySQL JDBC 驱动 |
| Lombok | Compile | `@Data`/`@NoArgsConstructor`/`@AllArgsConstructor` 减少样板代码 |
| spring-boot-starter-validation | — | 已引入但未实际使用（缺少 `@Valid` 等注解） |

### 3.2 MyBatis-Plus CRUD 方法映射

`ProductServiceImpl` 继承 `ServiceImpl<ProductMapper, Product>` 后，以下方法自动可用：

| Service 方法 | 底层 Mapper 方法 | 生成的 SQL |
|-------------|-----------------|-----------|
| `list()` | `selectList(null)` | `SELECT id,name,price,category,description,create_time,update_time FROM product` |
| `getById(id)` | `selectById(id)` | `SELECT ... FROM product WHERE id = ?` |
| `save(entity)` | `insert(entity)` | `INSERT INTO product (name,price,category,description) VALUES (?,?,?,?)` |
| `updateById(entity)` | `updateById(entity)` | `UPDATE product SET name=?,price=?,category=?,description=? WHERE id=?` |
| `removeById(id)` | `deleteById(id)` | `DELETE FROM product WHERE id = ?` |

> **注意**：`updateById()` 默认只更新非 null 字段（MyBatis-Plus 的 `FieldStrategy.NOT_NULL` 策略），若前端传入 `null` 值字段则该列不会被更新。

### 3.3 前端请求封装机制

[request.js](frontend/src/utils/request.js) 基于 Axios 封装：

- **baseURL**：`import.meta.env.VITE_API_BASE_URL || ''`，开发环境为空（走 Vite proxy），生产环境通过 Nginx 反向代理
- **请求拦截器**：从 `localStorage.getItem('token')` 读取 token，附加 `Authorization: Bearer {token}` 头
- **响应拦截器**：
  - 成功（`res.code === 200`）：返回 `res.data`，即 `CommonResult` 的 `data` 字段
  - 失败（`res.code !== 200`）：派发 `api-error` 自定义事件，`Promise.reject`
  - 网络错误：派发 `api-error` 事件，`Promise.reject`

### 3.4 路由与鉴权流程

```
用户访问 /
  → router.beforeEach 检查 localStorage.token
    → 有 token: 放行 → ProductView
    → 无 token: 重定向 /login → LoginView
      → 登录成功 → localStorage.setItem('token', 'admin-token') → router.push('/')
```

### 3.5 Docker Compose 服务编排

| 服务 | 镜像 | 端口映射 | 依赖 |
|------|------|----------|------|
| `db` | `mysql:8.0` | `33306:3306` | — |
| `backend` | 自建（`./backend/Dockerfile`） | `12765:8080` | `db` |
| `frontend` | 自建（`./frontend/Dockerfile`） | `32765:80` | `backend` |

- 数据库字符集：`utf8mb4` / `utf8mb4_unicode_ci`
- 后端通过环境变量 `SPRING_DATASOURCE_URL` 覆盖 `application.yml` 中的数据库连接，指向 Docker 内部 `db:3306`
- 前端 Nginx 配置 `try_files $uri $uri/ /index.html` 支持 Vue Router 的 HTML5 History 模式

### 3.6 CommonResult 统一响应结构

```json
{
  "code": 200,
  "message": "操作成功",
  "data": <T>
}
```

- 成功：`CommonResult.success(data)` → `{ code: 200, message: "操作成功", data: <T> }`
- 失败：`CommonResult.failed(msg)` → `{ code: 500, message: <msg>, data: null }`
- 前端响应拦截器自动解包：`code === 200` 时返回 `data`，否则触发错误事件

### 3.7 数据库初始化策略

- `spring.sql.init.mode=always`：每次启动执行
- [schema.sql](backend/src/main/resources/db/schema.sql)：`CREATE TABLE IF NOT EXISTS product`，幂等
- [data.sql](backend/src/main/resources/db/data.sql)：5 条 `INSERT INTO product`，**非幂等**——每次重启都会重复插入相同数据

### 3.8 前端组件通信模式

本项目未使用全局状态管理（如 Pinia/Vuex），而是通过以下方式通信：

- **localStorage**：持久化 token 和 username
- **CustomEvent**：`show-toast`（主动通知）、`api-error`（API 错误通知），在 `App.vue` 中统一监听
- **Props/Emit**：`UIConfirm` 组件通过 `props.visible` 控制显隐，`emit('confirm')`/`emit('cancel')` 回传用户操作
