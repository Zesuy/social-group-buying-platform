# P1 Batch 2：团购草稿、预售、权限、复制、分享

## 1. 目标

提升团长发布效率和私域传播控制能力。支持团购草稿、保存预览、发布动作、最小预售团购、隐藏团购、复制团购和分享海报数据。

## 2. 前置条件

P1 Batch 1 已完成；商品库和分类字段可用于发布团购；MVP 团购发布、管理、结束接口可用。

## 3. 涉及接口

`POST /api/v1/my/store/group-buys/drafts`；`PATCH /api/v1/my/store/group-buys/{groupBuyId}`；`POST /api/v1/my/store/group-buys/{groupBuyId}/publish`；`GET /api/v1/my/store/group-buys/{groupBuyId}/preview`；`POST /api/v1/my/store/group-buys/{groupBuyId}/copy`；`PATCH /api/v1/my/store/group-buys/{groupBuyId}/permission`；`POST /api/v1/my/store/group-buys/{groupBuyId}/share-card`；`GET /api/v1/share/group-buys/{shareToken}`。

## 4. 涉及数据表

`group_buys`、`group_buy_items`、`products`、可新增 `group_buy_share_tokens`；如需独立保存预览草稿内容，可新增 `group_buy_draft_snapshots`。

## 5. 实现任务

将 P1 团购创建拆为草稿保存和发布动作，兼容 MVP 直接发布接口；草稿状态不进入公共首页和普通可购详情；实现 `groupType=presale` 的最小预售语义，包括预售展示、发货承诺和可售时间校验，但不实现定金尾款；支持 `visibility=public/hidden`，隐藏团购不进入公共列表，只能通过团长自有管理端或有效分享 token 访问；实现复制团购，复制标题、介绍、图片、商品配置、权限配置并生成草稿；实现分享海报数据接口，返回团购、店铺、团长、封面、价格区间、二维码落地 token 等结构化数据。

## 6. 测试要求

MockMvc 覆盖创建草稿、更新草稿、发布草稿、预览、复制、隐藏团购公共列表不可见、分享 token 可访问、未登录、非团长、跨店铺拒绝、非法状态流转；Service 覆盖草稿事务、发布校验、预售时间校验、复制数据隔离、分享 token 过期或失效策略。

## 7. 验收标准

团长可反复保存并预览草稿，发布后进入现有购买链路；预售团购可浏览、下单和支付但不产生定金尾款；隐藏团购只能通过授权路径访问；复制团购不会复用原团购 ID 或污染原团购库存。

## 8. 联调文档更新项

新增“团购草稿与预览”“预售团购发布”“隐藏团购与分享入口”“复制团购”链路；更新公共浏览对 `visibility`、`status=draft`、`groupType=presale` 的展示和错误码说明。

## 9. 禁止事项

不实现卡券团购、报名团购、预售定金尾款、真实海报图片生成、帮卖分销；不允许草稿团购被下单；不允许隐藏团购泄漏到首页公共列表；不跳过权限和状态流转测试。

## 10. 本批完成后必须输出的结果

- 已实现接口清单。
- 已新增 / 修改文件清单。
- 已新增 / 修改 Flyway 迁移清单，如无则写“无”。
- 已新增 / 修改测试清单。
- 已更新联调文档的位置。
- 测试运行命令和结果。
- 本批未解决问题，如无则写“无”。

