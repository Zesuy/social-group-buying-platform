# Backend Dev Data

本目录放本地开发 / 联调用的数据脚本，不属于 Flyway migration。

## 重置当前业务数据

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
