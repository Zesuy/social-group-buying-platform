# P1 Batch 7：团购活动内容模型重构

## 1. 目标

彻底修正“团购详情 = 商品详情”的对象建模偏差。用户从首页、团长主页、分享卡片进入的对象是一次团购活动；商品只是团购活动内部的售卖项。

本批为后端补齐团购活动自己的内容表达能力：团购活动可以有主图、多图、标题、正文和结构化内容块；商品保留自己的名称、主图、图文描述。公共团购详情接口必须能支持前端按“团长 / 店铺信任 -> 团购活动内容 -> 热销商品 -> 商品购买与商品说明”的层级渲染，而不是把商品描述硬塞成团购介绍。

## 2. 前置条件

P1 Batch 1 到 Batch 6 已完成；公共团购列表、团购详情、团长团购管理、商品管理、下单、支付、库存、收藏、浏览历史、分享 token 和距离展示链路稳定。

本批必须优先参考 `docs/团购活动内容块设计.md`。如该文档仍写着“当前 P1 不新增后端字段”，本批需要同步更新其阶段策略，将内容块能力从预留状态推进为 P1 Batch 7 实施范围。

## 3. 核心对象边界

### 3.1 团购活动

团购活动是传播和转化容器，负责表达：

- 团长为什么开这个团；
- 本团有什么优惠、承诺、场景和推荐理由；
- 团长 / 店铺信任信息；
- 团购主图、多图、标题、正文和结构化内容块；
- 本团有哪些商品，以及哪个商品最热销或最适合默认购买。

一个团购活动可以包含多个商品。用户进入的是团购活动详情页，页面可以先展示团长主图和活动内容，再展示该团的热销商品和多个可购买商品项。

### 3.2 商品

商品是团购活动内的售卖项来源，负责表达：

- 商品名称；
- 商品主图；
- 商品自己的图文描述；
- 基础价格、参考库存、分类和状态。

商品不负责承载“本次开团优惠”“团长推荐语”“团购活动正文”。同一个商品被复用于不同团购时，不应把某次团购的营销文案写入商品描述。

### 3.3 团购商品关系

`group_buy_items` 负责表达商品在本次团购中的售卖方式：

- 团购展示名；
- 团购价；
- 团购库存；
- 已售数量；
- 排序。

`group_buys` 与 `products` 是多对多关系，通过 `group_buy_items` 关联；一个团购必须能够承载多个商品，一个商品也可以被复用到多个团购。

`group_buy_items` 不负责保存大段团购正文，也不负责保存商品自己的详情图文。

## 4. 涉及接口

增强以下既有接口，不新增平行的“商品详情式团购接口”：

- `GET /api/v1/group-buys/{groupBuyId}`
- `POST /api/v1/my/store/group-buys`
- `POST /api/v1/my/store/group-buys/drafts`
- `PATCH /api/v1/my/store/group-buys/{groupBuyId}`
- `GET /api/v1/my/store/group-buys/{groupBuyId}`
- `GET /api/v1/my/store/products`
- `POST /api/v1/my/store/products`
- `GET /api/v1/my/store/products/{productId}`
- `PATCH /api/v1/my/store/products/{productId}`

公共团购列表 `GET /api/v1/group-buys` 仍只返回摘要，不返回长正文和完整内容块；可返回团购主图和必要摘要字段。

## 5. 数据模型调整

### 5.1 group_buys

新增可空字段：

- `gallery_image_urls json`：团购活动多图，JSON 字符串数组，最多 9 张。
- `content_blocks json`：团购活动结构化正文块，保持原始顺序。

保留既有字段：

- `title`：团购活动标题。
- `introduction`：团购活动简短介绍，兼容旧前端，也可作为无内容块时的正文降级来源。
- `cover_image_url`：团购活动主图，不再被解释为商品图。

`content_blocks` 最小支持类型：

| 类型 | 用途 | 字段 |
|---|---|---|
| `paragraph` | 活动正文段落 | `text` |
| `section` | 小标题 + 正文 | `title`, `text` |
| `image` | 活动图片、产地、实拍、对比图 | `url`, `caption` |
| `list` | 卖点、注意事项、适用场景 | `items` |
| `deliveryNote` | 发货、截单和履约说明 | `text` |

### 5.2 products

新增可空字段：

- `detail_image_urls json`：商品详情图片，JSON 字符串数组，最多 9 张。

保留既有字段：

- `description`：商品自己的文字描述。
- `cover_image_url`：商品主图。

本批不为商品引入完整内容块编辑器；商品只需要“文字描述 + 多张详情图”的轻量图文能力。

### 5.3 group_buy_items

本批不新增字段。公共详情中的热销商品通过团购商品关系计算：

1. 优先选择 `sold_count` 最大的 item；
2. `sold_count` 相同按 `sort_order` 升序；
3. 仍相同按 `id` 升序；
4. 没有 item 时返回 `featuredItem = null`。

## 6. API 契约调整

### 6.1 团购创建 / 草稿 / 更新请求

团购请求新增字段：

```json
{
  "title": "爆品开团：山东蜜桃 5 斤装",
  "introduction": "本团产地直发，适合家庭囤货。",
  "coverImageUrl": "https://example.com/group-cover.png",
  "galleryImageUrls": [
    "https://example.com/group-1.png",
    "https://example.com/group-2.png"
  ],
  "contentBlocks": [
    {
      "type": "section",
      "title": "为什么开这个团",
      "text": "这批蜜桃来自固定合作果园，成熟度更稳定。"
    },
    {
      "type": "image",
      "url": "https://example.com/orchard.png",
      "caption": "果园实拍"
    }
  ],
  "deliveryType": "express",
  "shippingTime": "2026-06-30T18:00:00+08:00",
  "startTime": "2026-06-24T12:00:00+08:00",
  "endTime": "2026-07-01T12:00:00+08:00",
  "items": [
    {
      "productId": 501,
      "displayName": "白玉蜜桃 5 斤装",
      "groupPriceAmount": 2990,
      "groupStock": 100,
      "sortOrder": 1
    },
    {
      "productId": 502,
      "displayName": "白玉蜜桃 10 斤家庭装",
      "groupPriceAmount": 5590,
      "groupStock": 50,
      "sortOrder": 2
    }
  ]
}
```

内联创建商品请求新增字段：

```json
{
  "product": {
    "name": "白玉蜜桃",
    "description": "商品自己的口感、规格和储存说明。",
    "coverImageUrl": "https://example.com/product-cover.png",
    "detailImageUrls": [
      "https://example.com/product-detail-1.png"
    ],
    "basePriceAmount": 3990,
    "stock": 100
  },
  "displayName": "白玉蜜桃 5 斤装",
  "groupPriceAmount": 2990,
  "groupStock": 100,
  "sortOrder": 1
}
```

PATCH 语义：

- `galleryImageUrls` 传入非空数组时整体替换；传 `[]` 表示清空；不传表示不改。
- `contentBlocks` 传入数组时整体替换；传 `[]` 表示清空；不传表示不改。
- `introduction` 仍可单独更新，用于摘要和旧端降级。

### 6.2 公共团购详情响应

`GET /api/v1/group-buys/{groupBuyId}` 的 `groupBuy` 增加：

- `galleryImageUrls`
- `contentBlocks`

响应根对象增加：

- `featuredItem`：本团热销商品，结构与 `items[]` 中单项一致；没有商品时为 `null`。

`items[]` 增加嵌套商品信息：

```json
{
  "id": 1001,
  "productId": 501,
  "displayName": "白玉蜜桃 5 斤装",
  "groupPriceAmount": 2990,
  "groupStock": 100,
  "soldCount": 12,
  "sortOrder": 1,
  "coverImageUrl": "https://example.com/product-cover.png",
  "product": {
    "id": 501,
    "name": "白玉蜜桃",
    "description": "商品自己的口感、规格和储存说明。",
    "coverImageUrl": "https://example.com/product-cover.png",
    "detailImageUrls": [
      "https://example.com/product-detail-1.png"
    ],
    "basePriceAmount": 3990,
    "status": "active"
  }
}
```

兼容要求：

- 保留 `items[].coverImageUrl`，继续取商品主图，避免旧前端立即断裂。
- 新前端优先读取 `items[].product.coverImageUrl` 和 `items[].product.detailImageUrls`。
- 团购详情页首屏主图必须来自 `groupBuy.coverImageUrl` 或 `groupBuy.galleryImageUrls`，不是 `items[].coverImageUrl`。

### 6.3 商品管理响应

商品创建、详情、列表和更新响应增加：

- `detailImageUrls`

商品列表可返回该字段，但前端列表页不应渲染长图；商品详情 / 编辑页使用。

## 7. 校验和安全要求

后端必须对 JSON 内容做结构化校验，不能只把前端 JSON 字符串原样入库。

通用限制：

- `galleryImageUrls` 最多 9 个 URL；
- `detailImageUrls` 最多 9 个 URL；
- 单个 URL 最大 512 字符；
- 仅允许 `http://` 或 `https://` URL；
- `contentBlocks` 最多 20 个块；
- `type` 必须来自白名单；
- `paragraph.text` 最多 1000 字；
- `section.title` 最多 40 字；
- `section.text` 最多 1000 字；
- `image.url` 必填，最大 512 字符；
- `image.caption` 最多 80 字；
- `list.items` 最少 1 项、最多 10 项，单项最多 80 字；
- `deliveryNote.text` 最多 1000 字。

安全限制：

- 不接受任意 HTML；
- 不接受 `<script>`、`iframe`、事件属性、内联样式；
- 不接受 `javascript:`、`data:` 等非 HTTP 图片地址；
- 返回时保持原始内容块顺序，不做富文本拼接。

## 8. 实现任务

新增 Flyway 迁移，为 `group_buys` 和 `products` 增加 JSON 字段；更新实体、DTO、Service 转换逻辑和 MyBatis-Plus 映射；新增内容块请求 DTO 或值对象，集中实现校验和序列化 / 反序列化，避免 Service 中散落字符串拼接。

团购创建、草稿创建、发布、更新、详情、公开详情和分享 token 访问详情都必须使用同一套内容字段转换逻辑。商品创建、内联商品创建、商品更新和商品详情必须使用同一套详情图字段转换逻辑。

公共团购详情必须返回 `featuredItem`，并在 `items[]` 内返回商品自己的描述和详情图片。团购活动正文只来自 `group_buys.introduction` / `group_buys.content_blocks`，不得从第一个商品描述中推断或拼接。

同步更新 `docs/API设计.md`、`docs/数据模型设计.md`、`docs/团购活动内容块设计.md` 和 `docs/前后端联调文档.md`。

## 9. 测试要求

MockMvc 覆盖：

- 创建团购时提交 `galleryImageUrls` 和 `contentBlocks` 成功；
- 创建草稿时提交内容块成功；
- 更新团购内容块整体替换、清空和不传不改；
- 公开团购详情返回 `groupBuy.galleryImageUrls`、`groupBuy.contentBlocks`、`featuredItem` 和 `items[].product`；
- 分享 token 访问隐藏团购详情时返回同样内容字段；
- 商品创建 / 更新 / 详情返回 `detailImageUrls`；
- 内联创建商品时保存并返回 `detailImageUrls`；
- 内容块类型非法返回 `VALIDATION_ERROR`；
- 图片 URL 非法返回 `VALIDATION_ERROR`；
- 内容块数量、文本长度、列表项数量超限返回 `VALIDATION_ERROR`；
- 未登录、非团长、跨店铺更新仍按原权限规则拒绝。

Service 覆盖：

- 内容块白名单和字段级校验；
- JSON 序列化 / 反序列化；
- 旧数据字段为空时返回空数组或 `null` 的统一口径；
- `featuredItem` 选择规则；
- 团购正文不从商品描述兜底；
- 商品详情图不影响订单价格、库存扣减和订单快照金额；
- 商品复用到多个团购时，商品描述变更不改变团购活动正文。

迁移测试覆盖新增列存在。

回归测试至少运行公共浏览、团购管理、商品管理、订单创建、支付扣库存、分享 token 和距离展示相关测试。

## 10. 验收标准

团购活动详情接口能清楚区分活动内容和商品内容；团购主图、多图、标题、正文、内容块都属于 `groupBuy`；商品图文说明只出现在 `items[].product` 或商品管理接口中；详情接口能返回热销商品 `featuredItem`；旧字段保持兼容；所有新增 JSON 字段都有结构校验、安全限制和测试覆盖。

前端无需伪造商品长文，也无需把商品详情当作团购详情首屏内容。后续页面可以直接按“团购活动容器 + 多商品售卖项”的结构实现。

## 11. 联调文档更新项

新增“团购活动内容模型和商品图文说明”链路；更新团购发布、草稿创建、团购更新、公开团购详情、分享 token 详情、商品创建、商品详情和商品更新的请求 / 响应样例。

联调文档必须明确：

- 进入 `/group-buys/:id` 是进入团购活动，不是进入商品；
- 团购首屏主图使用 `groupBuy.coverImageUrl` / `groupBuy.galleryImageUrls`；
- 热销商品使用 `featuredItem`；
- 商品购买弹层和商品说明使用 `items[].product.description` / `items[].product.detailImageUrls`；
- 公共列表不返回完整内容块；
- 旧数据没有内容块时前端使用 `groupBuy.introduction` 按段落降级渲染。

## 12. 禁止事项

不引入完整 HTML 富文本编辑器；不允许任意 HTML 入库；不接视频、直播、外链脚本、iframe、自定义 CSS；不做复杂 SKU；不把团购活动营销文案写入商品描述；不把商品详情图当作团购主图；不改变订单金额、支付、库存扣减、售后、优惠券、会员成长和距离计算口径。

## 13. 本批完成后必须输出的结果

- 已实现接口清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改 Flyway 迁移清单，如无则写“无”。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。
