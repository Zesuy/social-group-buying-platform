# Backend Dev Data

本目录放本地开发 / 联调用的数据脚本，不属于 Flyway migration。

## API 驱动重建真实演示数据

推荐使用这一套脚本重建本地演示数据。清库只负责删除本地业务表数据；账号、店铺、图片、商品、团购、订单和消息等业务数据由真实 API 创建。

前置条件：

- MySQL 容器已启动。
- 后端服务已启动并可访问 `http://localhost:8080/api/v1/health`。
- 本地只针对开发库执行，不要对共享库或生产库执行。

```bash
docker exec -i groupshop-mysql mysql -uroot -proot groupshop < backend/scripts/reset-dev-data.sql
zsh -ic 'cd /home/zesuy/work/Engnerring_train-dev1 && node backend/scripts/seed-realistic-demo.mjs'
```

如果后端 API 地址不是默认值：

```bash
API_BASE_URL=http://localhost:8080/api/v1 zsh -ic 'cd /home/zesuy/work/Engnerring_train-dev1 && node backend/scripts/seed-realistic-demo.mjs'
```

脚本会创建 4 个商家、约 20 个商品、12 个团购、8 个买家和一组不同状态的订单；图片通过 `POST /api/v1/my/uploads/images` 上传，返回的 `/uploads/...` URL 会继续用于店铺、商品和团购创建。

## 重置当前业务数据（旧 SQL 直插）

在后端 schema 已经由 Flyway 创建完成后执行：

```bash
docker exec -i groupshop-mysql mysql -uroot -proot groupshop < backend/scripts/dev-seed.sql
```

脚本会清空本地业务表，保留 `flyway_schema_history` 和平台分类，然后插入一套适合当前产品定位的演示数据：

- 团长：`王姐鲜果团`
- 店铺：`王姐社区鲜果店`
- 主团购：`周末阳山水蜜桃社区团`
- 团购内商品：`阳山水蜜桃 5 斤装`、`阳山水蜜桃 10 斤家庭装`、`海南玫珑瓜 1 个装`
- 商品详情：包含商品描述和 `detailImageUrls`
- 团购正文：包含 `contentBlocks`
- 位置：店铺带经纬度，可用于距离展示
- 买家地址和一笔示例订单

## 从零重建本地 MySQL

如果需要彻底清空 Docker volume：

```bash
cd backend
docker compose down -v
docker compose up -d
```

然后启动后端一次，让 Flyway 自动建表；后端启动成功后回到仓库根目录执行：

```bash
docker exec -i groupshop-mysql mysql -uroot -proot groupshop < backend/scripts/dev-seed.sql
```

不要对共享库或生产库执行本目录脚本。
