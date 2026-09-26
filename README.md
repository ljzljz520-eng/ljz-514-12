# 重庆旅游线路规划系统

## 🛠 技术栈
- 前端：React + TypeScript + Tailwind + Ant Design + React-Leaflet（Vite）
- 后端：Java 17 + Maven + 内置 HTTP Server + Hibernate ORM
- 数据库：PostgreSQL 15

## 📦 数据文件
- `backend/src/main/resources/nodes.csv`：景点节点（必需）
- `backend/src/main/resources/edges.csv`：景点之间的真实连接（可选，存在则优先使用；否则按地理距离自动生成边）

`edges.csv` 至少包含 `from,to`，并记录五个权重维度（均可缺省）：
`distance_meters`（距离）、`walk_seconds`（步行/乘车耗时）、`slope_percent`（坡度，正上坡/负下坡）、
`transfer`+`transfer_seconds`（是否换乘与等候）、`crowd_level`（拥挤程度 0-4），
另支持 `mode`（walk/bus/rail/cableway/escalator）与 `note`（路况说明）。

算法默认按**综合耗时**（基础耗时 + 坡度附加 + 拥挤延误 + 换乘等候）找路，
也支持最短距离、最少换乘；页面会逐段展示耗时构成，并对比各走法说明“为什么这条路更合适”。
详见 [docs/设计文档.md](./docs/设计文档.md)。

文档索引：见 [docs/README.md](./docs/README.md)。

## 🚀 启动指南
1. 确保 Docker Desktop 已启动
2. 在根目录执行：`docker compose up -d --build`
3. 等待容器启动完成后访问前端与后端接口

## 🔗 服务地址
- 前端：http://localhost:3514
- 后端健康检查：http://localhost:8514/api/health
- 节点列表：http://localhost:8514/api/nodes
- 数据库：localhost:5514（db: cq_travel / user: cq / pass: cq）

## 🧪 测试账号
- 无（本项目无登录鉴权）

---

## 🐳 Docker 镜像源配置

### 推荐配置（基于实际项目验证）

#### 1. Docker 镜像源
当前 `Dockerfile` 使用 `docker.m.daocloud.io` 作为镜像前缀以加速国内拉取（也可替换为官方 Docker Hub 镜像）。

#### 2. npm 依赖源
在 `frontend/Dockerfile` 中使用：`npm config set registry https://registry.npmmirror.com`

#### 3. Maven 依赖源
在 `backend` 目录下提供 `settings.xml` 并在 `backend/Dockerfile` 中使用该镜像配置。
