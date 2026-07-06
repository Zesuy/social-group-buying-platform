# 前端交接文档：公共浏览体验重构（P1 Batch 04-07）

> 编写时间：2026-07-03
> 交接人：Codex 会话（额度用完，转交 Claude）
> 接手人：下一位开发者
> 状态：历史交接记录。当前能力状态以 `AGENTS.md`、实际代码、`docs/API设计.md` 和 `docs/前后端联调文档.md` 为准。

---

## 一、当前进度

### ✅ 已完成

**规范性文档（3 个新文件）：**

| 文件 | 说明 |
|------|------|
| `docs/前端产品与页面设计准则.md` | 定义"不做传统商城"、"团购是活动"等前端产品原则 |
| `DESIGN.md` | 视觉系统规范：色板、组件规格、UI 验收清单 |
| `docs/dev/frontend-ai-workflow.md` | 前端 AI 工作流规范：需求澄清→技术方案→实现→验收 |

**AGENTS.md 等引用更新：** 已把新文档纳入开发入口索引。

**前端页面重构（核心改动）：**

| 页面 | 改动 |
|------|------|
| `GroupBuyDetailView.vue` | 从传统商品详情页重构为"私域团购活动落地页"。结构：活动信息→团长信任→热销商品→内容介绍→商品选择→商品详情→履约承诺 |
| `IndexView.vue` | 首页团购卡片流改造，对齐活动卡形态 |
| `GroupBuyFeedCard.vue` | 卡片组件改为活动卡，突出团长信任、活动信息 |
| `SkuSheet.vue` | 去掉"加入购物车"，详情页只保留"立即购买" |

**P1 后端功能引入：**

| 功能 | 来源 | 状态 |
|------|------|------|
| 距离计算（`distanceText`） | P1 Batch 06 | ✅ 详情接口透传 `latitude/longitude` 参数，页面展示 `store.distanceText` |
| 结构化内容块（`contentBlocks`） | P1 Batch 07 | ✅ 已渲染 paragraph/section/image/list/deliveryNote 等类型 |
| `featuredItem` 热销商品 | P1 Batch 07 | ✅ 已在前台展示 |

**测试：**
- E2E 选择器与新页面结构对齐，36 passed
- typecheck / lint / unit test / build 全部通过

---

### ❌ 未完成 / 下一阶段建议

#### 1. 详情页视觉打磨（高优先级）

- **无封面图时首屏仍然偏"占位"**：当前 `heroImageUrl` 有降级链（coverImageUrl → galleryImageUrls[0] → leader.avatarUrl → store.logoUrl → featuredItem.coverImageUrl），但是当所有来源都为空时，封面区 220px 高度显示空白占位，质感差。
  - **建议**：做一套默认商品图占位策略（SVG 占位 / 主题色底 + 品类图标），或后续配合真实数据生成商品图。
  - P1 快速修复：至少让无图时只显示极简高度（56px 条状占位），不要留 220px 空白。

- **商品质量不够时整页表现弱**：封面无图 + 商品无描述 + contentBlocks 为空时，详情页看起来很空。需要在这些场景下做引导态/降级展示。

#### 2. 更多 P1 后端功能可引入（中优先级）

以下是 P1 已完成、但前端详情页尚未接入的功能：

| 功能 | 后端是否完成 | 建议 |
|------|------------|------|
| **收藏团购** | ✅ `POST/DELETE /api/v1/group-buys/{id}/favorite` + 接口响应 `viewer.favorited` | 详情页 buybar 可加收藏心形按钮，复用现有收藏 API |
| **浏览历史自动记录** | ✅ 后端访问详情时自动记录 | 前端不需要额外调用，但可以加"最近浏览"入口 |
| **分享卡片 / 分享 token** | ✅ 后端 `POST .../share-card` 返回 `shareToken` + `landingPath` | 当前 `handleShare` 只 toast "后续开放"，可接入真实分享能力 |
| **隐藏团购支持** | ✅ 后端支持 `visibility=hidden`+`shareToken` 访问和下单 | 详情页需要处理 `shareToken` 查询参数并透传到下单流程 |
| **预售团购差异展示** | ✅ 后端 `groupType=presale` | 当前详情页按普通团购渲染，预售的"预计发货时间"等信息应更突出 |
| **订单预览/下单链路** | ✅ 完整 P1 Batch 04-06 | 当前详情→下单链路已通，但可增加：数量选择后的实时价格预览、库存紧张提示等 |

#### 3. 首页剩余工作（中优先级）

- 首页搜索目前是占位，如后续需要接入 `keyword` 参数搜索
- 首页分类横滑 MVP 可不做真实筛选

#### 4. 后续 batch 的页面

前端完整页面清单见 `docs/dev/frontend-mvp-plan-revised.md`：

| Batch | 页面 | 当前状态 |
|-------|------|---------|
| 05 | `/checkout` 下单确认页 | ✅ 已实现 |
| 05 | 地址管理 `/addresses` | ✅ 已实现 |
| 06 | 订单列表/详情 | ✅ 已实现 |
| 07 | 模拟支付 | ✅ 已实现 |
| 08 | 团长订单/发货 | ✅ 已实现 |
| 09 | 订阅列表/会员卡 | ✅ 已实现 |
| 03B | 发布团购三段式页面 | ❌ 待实现 |
| 03C | 团购管理 | ❌ 待实现 |
| 03A | 商品管理 | ❌ 待实现 |

---

## 二、核心原则（接手人必读）

1. **项目定位不是商城**：团购是"一次短周期销售活动"，不是单个商品。详情页表达：谁在组织 → 卖什么 → 为什么现在买 → 什么时候发货。
2. **所有 UI 改动必须遵守** `docs/前端产品与页面设计准则.md` + `DESIGN.md`，不许自己发明设计语言。
3. **金额统一用整数分**，展示用 `formatAmount()`，**严禁浮点运算**。
4. **未实现功能入口**：真实微信支付、帮卖、积分商城、公众号推送、平台后台和完整客服中心等只能灰态或 toast，不接入真实逻辑。购物车、预售团购、优惠券、站内通知、订单上下文轻量聊天和售后后端闭环已不再按占位处理。见 `isFeatureDisabled(feature)` 工具函数。
5. **代码风格**：已有代码的注释密度、命名方式、组件拆分粒度就是标准，新代码对齐即可。
6. **每改完一批页面跑验收命令**：
   ```bash
   npm run typecheck && npm run lint && npm run test:unit && npm run build && npm run test:e2e
   ```
7. **联调文档同步**：改完前端页面后同步更新 `docs/前后端联调文档.md`。

---

## 三、当前待提交的改动

当前工作树上所有的改动都是未提交的（unstaged）。

```text
修改文件（11 个）：
  AGENTS.md
  docs/dev/README.md
  docs/团购活动内容块设计.md
  frontend/src/api/groupBuys.ts
  frontend/src/components/GroupBuyFeedCard.vue
  frontend/src/components/SkuSheet.vue
  frontend/src/types/api.ts
  frontend/src/types/index.ts
  frontend/src/views/GroupBuyDetailView.vue
  frontend/src/views/IndexView.vue
  frontend/tests/e2e/public-browsing-checkout.spec.ts

新文件（3 个，未跟踪）：
  DESIGN.md
  docs/dev/frontend-ai-workflow.md
  docs/前端产品与页面设计准则.md
```

建议用下面这个 commit message 一次性提交：

```
feat(frontend): P1 Batch 04-07 public browsing refactor & design system docs

- Restructure GroupBuyDetailView as activity landing page (not product detail)
- Restructure IndexView/GroupBuyFeedCard for activity-first layout
- Remove cart entry from detail page (non-MVP); keep buy-only in SkuSheet
- Integrate P1 distance display (latitude/longitude → store.distanceText)
- Integrate P1 content blocks rendering (paragraph/section/image/list/deliveryNote)
- Add DESIGN.md visual system spec (colors, components, UI checklist)
- Add 前端产品与页面设计准则.md (activity-first, not mall)
- Add frontend-ai-workflow.md (requirement → design → implementation flow)
- Update E2E selectors for new page semantics
- Update AGENTS.md index with new docs references
```

---

## 四、快速上手

```bash
# 启动前端
cd frontend && npm run dev

# 验收
npm run typecheck && npm run lint && npm run test:unit && npm run test:e2e

# 后端联调默认代理到 localhost:8080
```

后端 API 已完成到 P1 Batch 07。联调文档在 `docs/前后端联调文档.md`（3252 行，覆盖了完整前端 batch）。

接手后的下一步建议：先提交当前改动 → 阅读 `docs/前端产品与页面设计准则.md` + `DESIGN.md` → 对照上文"未完成"列表优先处理详情页无图占位问题 → 按 `frontend-mvp-plan-revised.md` 继续后续页面。
