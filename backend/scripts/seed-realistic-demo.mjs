#!/usr/bin/env node
/**
 * API-driven realistic demo data seed for local development.
 *
 * Business data is created through public/local APIs. Use
 * reset-dev-data.sql first when a clean rebuild is needed.
 */

import { mkdir, rm, writeFile } from 'node:fs/promises'
import { tmpdir } from 'node:os'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'
import { execFile } from 'node:child_process'
import { promisify } from 'node:util'

const API_BASE_URL = (process.env.API_BASE_URL || 'http://localhost:8080/api/v1').replace(/\/$/, '')
const ROOT_DIR = join(dirname(fileURLToPath(import.meta.url)), '..', '..')
const TMP_IMAGE_DIR = join(tmpdir(), 'groupshop-realistic-demo-images')
const CLEAR_LOCAL_UPLOADS = process.env.CLEAR_LOCAL_UPLOADS !== 'false'
const execFileAsync = promisify(execFile)

const IMAGE_SOURCES = {
  avatarWoman: 'https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=480&q=80',
  avatarKitchen: 'https://images.unsplash.com/photo-1580489944761-15a19d654956?auto=format&fit=crop&w=480&q=80',
  avatarMan: 'https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=480&q=80',
  avatarStore: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=480&q=80',
  fruitStore: 'https://images.unsplash.com/photo-1542838132-92c53300491e?auto=format&fit=crop&w=900&q=80',
  kitchenStore: 'https://images.unsplash.com/photo-1556911220-bff31c812dba?auto=format&fit=crop&w=900&q=80',
  dairyStore: 'https://images.unsplash.com/photo-1550583724-b2692b85b150?auto=format&fit=crop&w=900&q=80',
  dailyStore: 'https://images.unsplash.com/photo-1583258292688-d0213dc5a3a8?auto=format&fit=crop&w=900&q=80',
  peach: 'https://images.unsplash.com/photo-1567306226416-28f0efdc88ce?auto=format&fit=crop&w=1200&q=80',
  strawberry: 'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&w=1200&q=80',
  orange: 'https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&w=1200&q=80',
  pear: 'https://images.unsplash.com/photo-1514756331096-242fdeb70d4a?auto=format&fit=crop&w=1200&q=80',
  greens: 'https://images.unsplash.com/photo-1566385101042-1a0aa0c1268c?auto=format&fit=crop&w=1200&q=80',
  tomato: 'https://images.unsplash.com/photo-1566385101042-1a0aa0c1268c?auto=format&fit=crop&w=1200&q=80',
  bread: 'https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=1200&q=80',
  cake: 'https://images.unsplash.com/photo-1578985545062-69928b1d9587?auto=format&fit=crop&w=1200&q=80',
  dumpling: 'https://images.unsplash.com/photo-1496116218417-1a781b1c416c?auto=format&fit=crop&w=1200&q=80',
  meal: 'https://images.unsplash.com/photo-1547592180-85f173990554?auto=format&fit=crop&w=1200&q=80',
  egg: 'https://images.unsplash.com/photo-1587486913049-53fc88980cfc?auto=format&fit=crop&w=1200&q=80',
  milk: 'https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=1200&q=80',
  beef: 'https://images.unsplash.com/photo-1603048297172-c92544798d5a?auto=format&fit=crop&w=1200&q=80',
  chicken: 'https://images.unsplash.com/photo-1587593810167-a84920ea0781?auto=format&fit=crop&w=1200&q=80',
  tissue: 'https://images.unsplash.com/photo-1583947215259-38e31be8751f?auto=format&fit=crop&w=1200&q=80',
  detergent: 'https://images.unsplash.com/photo-1585421514284-efb74c2b69ba?auto=format&fit=crop&w=1200&q=80',
  towel: 'https://images.unsplash.com/photo-1600369672770-985fd30004eb?auto=format&fit=crop&w=1200&q=80',
  storage: 'https://images.unsplash.com/photo-1586023492125-27b2c045efd7?auto=format&fit=crop&w=1200&q=80',
  tea: 'https://images.unsplash.com/photo-1544787219-7f47ccb76574?auto=format&fit=crop&w=1200&q=80',
  flower: 'https://images.unsplash.com/photo-1490750967868-88aa4486c946?auto=format&fit=crop&w=1200&q=80',
}

const merchants = [
  {
    key: 'fruit',
    phone: '13870010001',
    nickname: '王姐鲜果团',
    avatarImage: 'avatarWoman',
    storeImage: 'fruitStore',
    storeName: '王姐社区鲜果店',
    description: '每周二、五开团，主做当季鲜果和社区自提。同一批次集中收单，减少损耗后给邻居们更稳的价格。',
    defaultDeliveryType: 'local_delivery',
    latitude: 30.27415,
    longitude: 120.12538,
    products: [
      product('阳山水蜜桃 5 斤装', '偏软甜口，中大果混装。适合现吃，轻微压痕不影响食用。', 'fresh_fruit', 3990, 180, ['peach', 'pear']),
      product('临安高山小番茄', '酸甜口，早晨采摘后冷链到店，小朋友加餐很合适。', 'vegetable_food', 2690, 120, ['tomato', 'greens']),
      product('赣南脐橙家庭箱', '果径均匀，水分足，办公室和家庭都适合。', 'fresh_fruit', 4590, 90, ['orange', 'strawberry']),
      product('奶油草莓尝鲜盒', '当日分拣，软果不隔夜，适合当天吃完。', 'fresh_fruit', 3290, 75, ['strawberry', 'peach']),
      product('砀山梨 5 斤装', '清甜多汁，外皮自然果点，不影响口感。', 'fresh_fruit', 2990, 130, ['pear', 'orange']),
    ],
    groupBuys: [
      groupBuy('周末阳山水蜜桃社区团', '王姐本周从阳山果园集中收单，适合家庭囤货、办公室拼团和邻里群分享。', 'local_delivery', ['阳山水蜜桃 5 斤装', '奶油草莓尝鲜盒'], 'peach'),
      groupBuy('早市小番茄鲜蔬团', '早晨采摘后统一配送，适合做沙拉、便当和孩子加餐。', 'pickup', ['临安高山小番茄', '砀山梨 5 斤装'], 'tomato'),
      groupBuy('赣南脐橙预售团', '下周果园直发，到货后统一分拣配送。', 'local_delivery', ['赣南脐橙家庭箱'], 'orange', { groupType: 'presale', timing: 'presale' }),
    ],
  },
  {
    key: 'kitchen',
    phone: '13870010002',
    nickname: '林阿姨社区厨房',
    avatarImage: 'avatarKitchen',
    storeImage: 'kitchenStore',
    storeName: '林阿姨晚餐小灶',
    description: '小区厨房少量现做，主打晚餐加菜、周末早餐和家庭聚餐。每天按预订量备料，不做长期货架。',
    defaultDeliveryType: 'pickup',
    latitude: 30.28288,
    longitude: 120.1332,
    products: [
      product('手作鲜肉小馄饨 30 只', '猪前腿肉现拌馅，薄皮小馄饨，适合早餐和夜宵。', 'prepared_food', 3690, 80, ['dumpling', 'meal']),
      product('红豆吐司 2 条', '低糖红豆馅，早晨现烤，建议两天内吃完。', 'prepared_food', 2990, 60, ['bread', 'cake']),
      product('番茄牛腩家庭份', '慢炖牛腩，汤汁浓，适合 2-3 人晚餐加菜。', 'prepared_food', 6990, 45, ['meal', 'beef']),
      product('古早味蛋糕 1 盒', '软绵蛋香，老人孩子都好入口。', 'prepared_food', 2590, 70, ['cake', 'bread']),
      product('手切净菜包', '青菜、菌菇、胡萝卜搭配，回家直接下锅。', 'vegetable_food', 1990, 100, ['greens', 'tomato']),
    ],
    groupBuys: [
      groupBuy('周五晚餐加菜团', '林阿姨按订单备料，适合下班后不用再买菜的家庭。', 'pickup', ['番茄牛腩家庭份', '手切净菜包'], 'meal'),
      groupBuy('周末早餐小馄饨团', '周六早上 8 点后自提，适合全家早餐。', 'pickup', ['手作鲜肉小馄饨 30 只', '红豆吐司 2 条'], 'dumpling'),
      groupBuy('古早味蛋糕隐藏分享团', '老客群内分享，少量现烤，售完即止。', 'pickup', ['古早味蛋糕 1 盒'], 'cake', { visibility: 'hidden' }),
    ],
  },
  {
    key: 'dairy',
    phone: '13870010003',
    nickname: '小周肉禽蛋奶团',
    avatarImage: 'avatarMan',
    storeImage: 'dairyStore',
    storeName: '小周冷链优选',
    description: '主做肉禽蛋奶冷链团，按小区订单集中配送，适合家庭一周备货。',
    defaultDeliveryType: 'local_delivery',
    latitude: 30.2657,
    longitude: 120.1451,
    products: [
      product('可生食鲜鸡蛋 30 枚', '当天分拣，蛋壳干净，适合早餐和烘焙。', 'meat_egg_dairy', 4290, 120, ['egg', 'milk']),
      product('低温鲜牛奶 950ml*2', '低温配送，到手后请冷藏。', 'meat_egg_dairy', 3590, 90, ['milk', 'egg']),
      product('草饲牛腱子 1kg', '适合卤牛肉和炖汤，提前分切真空包装。', 'meat_egg_dairy', 8990, 50, ['beef', 'chicken']),
      product('黄油鸡整鸡 1 只', '净膛冷鲜鸡，适合炖汤和白切。', 'meat_egg_dairy', 5990, 55, ['chicken', 'meal']),
      product('家庭早餐组合', '鸡蛋、牛奶、吐司组合，适合一周早餐。', 'other', 7990, 40, ['egg', 'bread']),
    ],
    groupBuys: [
      groupBuy('一周早餐蛋奶团', '小周冷链配送，适合家庭一周早餐补给。', 'local_delivery', ['可生食鲜鸡蛋 30 枚', '低温鲜牛奶 950ml*2'], 'egg'),
      groupBuy('周日牛腱子卤味团', '统一分切真空包装，周日傍晚送达。', 'express', ['草饲牛腱子 1kg', '黄油鸡整鸡 1 只'], 'beef'),
      groupBuy('家庭早餐组合即将截止', '鸡蛋牛奶吐司一套配齐，适合工作日早餐。', 'local_delivery', ['家庭早餐组合'], 'milk', { timing: 'endingSoon' }),
    ],
  },
  {
    key: 'daily',
    phone: '13870010004',
    nickname: '许店长日用团',
    avatarImage: 'avatarStore',
    storeImage: 'dailyStore',
    storeName: '许店长家庭补货站',
    description: '小区日用补货，纸品、清洁、收纳和小物集中拼单，减少单件配送成本。',
    defaultDeliveryType: 'pickup',
    latitude: 30.2912,
    longitude: 120.1189,
    products: [
      product('原生木浆抽纸 16 包', '家庭常备规格，柔韧不易破。', 'daily_goods', 4590, 160, ['tissue', 'storage']),
      product('浓缩洗衣液 2kg', '低泡易漂洗，适合日常衣物。', 'daily_goods', 3990, 100, ['detergent', 'towel']),
      product('纯棉毛巾 3 条装', '柔软吸水，颜色随机搭配。', 'daily_goods', 2990, 90, ['towel', 'detergent']),
      product('厨房收纳盒 6 件套', '透明可叠放，适合干货和零食收纳。', 'daily_goods', 5290, 70, ['storage', 'tissue']),
      product('茉莉花茶家庭罐', '清香型口粮茶，饭后解腻。', 'other', 3690, 75, ['tea', 'flower']),
    ],
    groupBuys: [
      groupBuy('家庭纸品补货团', '许店长集中拼单，纸品整箱到店，小区自提更省心。', 'pickup', ['原生木浆抽纸 16 包', '纯棉毛巾 3 条装'], 'tissue'),
      groupBuy('厨房清洁收纳团', '适合周末整理厨房，清洁和收纳一次配齐。', 'pickup', ['浓缩洗衣液 2kg', '厨房收纳盒 6 件套'], 'storage'),
      groupBuy('茉莉花茶老客团已结束', '上一期老客茶团，用于展示历史团购状态。', 'pickup', ['茉莉花茶家庭罐'], 'tea', { endAfterCreate: true }),
    ],
  },
]

const buyers = [
  ['13970020001', '陈小满', 'avatarWoman'],
  ['13970020002', '周晨', 'avatarMan'],
  ['13970020003', '何雨晴', 'avatarKitchen'],
  ['13970020004', '赵一诺', 'avatarStore'],
  ['13970020005', '孙阿姨', 'avatarWoman'],
  ['13970020006', '刘嘉木', 'avatarMan'],
  ['13970020007', '吴晓晓', 'avatarKitchen'],
  ['13970020008', '郑可', 'avatarStore'],
]

const addresses = [
  ['陈小满', '13970020001', '浙江省', '杭州市', '西湖区', '文三路桂花城 8 幢 1 单元 602'],
  ['周晨', '13970020002', '浙江省', '杭州市', '西湖区', '古墩路春天花园 3 幢 1502'],
  ['何雨晴', '13970020003', '浙江省', '杭州市', '拱墅区', '湖墅南路运河湾 2 幢 901'],
  ['赵一诺', '13970020004', '浙江省', '杭州市', '西湖区', '竞舟路星洲花园 12 幢 301'],
  ['孙阿姨', '13970020005', '浙江省', '杭州市', '拱墅区', '莫干山路和睦新村 5 幢 402'],
  ['刘嘉木', '13970020006', '浙江省', '杭州市', '西湖区', '紫荆花路府苑新村 7 幢 1201'],
  ['吴晓晓', '13970020007', '浙江省', '杭州市', '上城区', '钱江路近江家园 4 幢 802'],
  ['郑可', '13970020008', '浙江省', '杭州市', '西湖区', '学院路翠苑二区 22 幢 501'],
]

const state = {
  tokens: new Map(),
  merchants: [],
  buyers: [],
  groupBuys: [],
  orders: [],
}

let lastRequest = null

function product(name, description, categoryCode, basePriceAmount, stock, imageKeys) {
  return { name, description, categoryCode, basePriceAmount, stock, imageKeys }
}

function groupBuy(title, introduction, deliveryType, productNames, coverImageKey, options = {}) {
  return { title, introduction, deliveryType, productNames, coverImageKey, ...options }
}

function localTime(offsetHours) {
  const date = new Date(Date.now() + offsetHours * 60 * 60 * 1000)
  const pad = (value) => String(value).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate()),
  ].join('-') + 'T' + [
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds()),
  ].join(':') + '+08:00'
}

function localDateTime(offsetHours) {
  return localTime(offsetHours).replace('+08:00', '')
}

async function main() {
  console.log(`API base: ${API_BASE_URL}`)
  await prepareWorkspace()
  await apiGet('health')

  const categories = await apiGet('categories')
  const categoryIds = Object.fromEntries(categories.map((item) => [item.code, item.id]))

  for (const merchantSpec of merchants) {
    state.merchants.push(await createMerchant(merchantSpec, categoryIds))
  }

  await createDraftAndHiddenShareSamples()
  await createBuyers()
  await createBuyerActivity()
  await verifySeed()
  printSummary()
}

async function prepareWorkspace() {
  await rm(TMP_IMAGE_DIR, { recursive: true, force: true })
  await mkdir(TMP_IMAGE_DIR, { recursive: true })
  if (CLEAR_LOCAL_UPLOADS) {
    await rm(join(ROOT_DIR, 'backend', 'uploads', 'images'), { recursive: true, force: true })
  }
}

async function createMerchant(spec, categoryIds) {
  console.log(`\n== ${spec.nickname} ==`)
  const login = await apiPost('auth/mock-login', {
    phone: spec.phone,
    nickname: spec.nickname,
  })
  const token = login.accessToken
  state.tokens.set(spec.phone, token)

  const avatarUrl = await uploadImageAs(spec.avatarImage, `${spec.key}-avatar.jpg`, token)
  await apiRequest('PATCH', 'me', { nickname: spec.nickname, avatarUrl }, token)

  const logoUrl = await uploadImageAs(spec.storeImage, `${spec.key}-store.jpg`, token)
  const storeResponse = await apiPost('stores', {
    name: spec.storeName,
    logoUrl,
    description: spec.description,
    defaultDeliveryType: spec.defaultDeliveryType,
    latitude: spec.latitude,
    longitude: spec.longitude,
  }, token)

  const merchant = {
    ...spec,
    token,
    userId: login.user.id,
    leaderId: storeResponse.leader.id,
    storeId: storeResponse.store.id,
    products: [],
    groupBuys: [],
  }

  for (const productSpec of spec.products) {
    const coverImageUrl = await uploadImageAs(productSpec.imageKeys[0], slugFilename(productSpec.name, 'cover.jpg'), token)
    const detailImageUrls = []
    for (const imageKey of productSpec.imageKeys) {
      detailImageUrls.push(await uploadImageAs(imageKey, slugFilename(`${productSpec.name}-${imageKey}.jpg`), token))
    }
    const created = await apiPost('my/store/products', {
      name: productSpec.name,
      description: productSpec.description,
      coverImageUrl,
      detailImageUrls,
      categoryId: categoryIds[productSpec.categoryCode],
      basePriceAmount: productSpec.basePriceAmount,
      stock: productSpec.stock,
    }, token)
    merchant.products.push({ ...productSpec, id: created.id, coverImageUrl, detailImageUrls })
  }

  for (const groupBuySpec of spec.groupBuys) {
    const created = await createGroupBuyForMerchant(merchant, groupBuySpec)
    merchant.groupBuys.push(created)
    state.groupBuys.push(created)
  }

  await createCouponsForMerchant(merchant)
  console.log(`created store=${merchant.storeId}, products=${merchant.products.length}, groupBuys=${merchant.groupBuys.length}`)
  return merchant
}

async function createGroupBuyForMerchant(merchant, spec) {
  const coverImageUrl = await uploadImageAs(spec.coverImageKey, slugFilename(`${spec.title}-cover.jpg`), merchant.token)
  const galleryImageUrls = [
    coverImageUrl,
    await uploadImageAs(merchant.storeImage, slugFilename(`${spec.title}-store.jpg`), merchant.token),
  ]
  const items = spec.productNames.map((name, index) => {
    const p = merchant.products.find((item) => item.name === name)
    if (!p) throw new Error(`Product not found for group buy "${spec.title}": ${name}`)
    return {
      productId: p.id,
      displayName: displayNameForGroupItem(name, index),
      groupPriceAmount: Math.max(990, p.basePriceAmount - (index + 1) * 400),
      groupStock: Math.max(20, Math.floor(p.stock * 0.55)),
      sortOrder: index + 1,
    }
  })

  const timing = resolveTiming(spec)
  const body = {
    title: spec.title,
    introduction: spec.introduction,
    coverImageUrl,
    deliveryType: spec.deliveryType,
    groupType: spec.groupType || 'normal',
    visibility: spec.visibility || 'public',
    galleryImageUrls,
    contentBlocks: contentBlocksFor(merchant, spec),
    items,
    ...timing,
  }
  const response = await apiPost('my/store/group-buys', body, merchant.token)
  let shareCard = null
  if (spec.visibility === 'hidden') {
    shareCard = await apiPost(`my/store/group-buys/${response.groupBuy.id}/share-card`, {}, merchant.token)
  }
  if (spec.endAfterCreate) {
    await apiPost(`my/store/group-buys/${response.groupBuy.id}/end`, {}, merchant.token)
  }
  return {
    merchantKey: merchant.key,
    token: merchant.token,
    leaderId: merchant.leaderId,
    storeId: merchant.storeId,
    groupBuy: response.groupBuy,
    items: response.items,
    shareCard,
    spec,
  }
}

async function createDraftAndHiddenShareSamples() {
  const merchant = state.merchants[0]
  const p = merchant.products[0]
  const coverImageUrl = await uploadImageAs('peach', 'fruit-draft-cover.jpg', merchant.token)
  const draft = await apiPost('my/store/group-buys/drafts', {
    title: '下周果园直采草稿团',
    introduction: '团长正在确认果园采摘时间和社区配送批次，暂不对外展示。',
    coverImageUrl,
    deliveryType: 'local_delivery',
    groupType: 'normal',
    visibility: 'public',
    galleryImageUrls: [coverImageUrl],
    contentBlocks: contentBlocksFor(merchant, { title: '下周果园直采草稿团', introduction: '草稿团购' }),
    items: [{
      productId: p.id,
      displayName: '阳山水蜜桃草稿规格',
      groupPriceAmount: 3190,
      groupStock: 40,
      sortOrder: 1,
    }],
  }, merchant.token)
  merchant.groupBuys.push({ merchantKey: merchant.key, token: merchant.token, leaderId: merchant.leaderId, storeId: merchant.storeId, groupBuy: draft.groupBuy, items: draft.items, spec: { title: draft.groupBuy.title, draft: true } })
}

async function createCouponsForMerchant(merchant) {
  await apiPost('my/store/coupons', {
    name: `${merchant.storeName} 新客见面礼`,
    couponType: 'amount',
    claimCondition: 'new_subscriber',
    amount: 500,
    thresholdAmount: 2990,
    totalQuantity: 80,
    perUserLimit: 1,
    startTime: localDateTime(-24),
    endTime: localDateTime(24 * 14),
  }, merchant.token)
  await apiPost('my/store/coupons', {
    name: `${merchant.storeName} 满减红包`,
    couponType: 'red_packet',
    claimCondition: 'general',
    amount: 300,
    thresholdAmount: 3990,
    totalQuantity: 120,
    perUserLimit: 1,
    startTime: localDateTime(-24),
    endTime: localDateTime(24 * 10),
  }, merchant.token)
}

async function createBuyers() {
  for (let i = 0; i < buyers.length; i += 1) {
    const [phone, nickname, avatarImage] = buyers[i]
    const login = await apiPost('auth/mock-login', { phone, nickname })
    const avatarUrl = await uploadImageAs(avatarImage, `buyer-${i + 1}.jpg`, login.accessToken)
    await apiRequest('PATCH', 'me', { nickname, avatarUrl }, login.accessToken)
    const address = addresses[i]
    const addressResponse = await apiPost('my/addresses', {
      receiverName: address[0],
      receiverPhone: address[1],
      province: address[2],
      city: address[3],
      district: address[4],
      detail: address[5],
      isDefault: true,
    }, login.accessToken)
    state.buyers.push({
      phone,
      nickname,
      token: login.accessToken,
      userId: login.user.id,
      addressId: addressResponse.id,
    })
  }
}

async function createBuyerActivity() {
  const orderTargets = state.groupBuys.filter((entry) =>
    entry.groupBuy.status === 'published'
    && entry.groupBuy.visibility === 'public'
    && entry.groupBuy.groupType === 'normal'
    && !entry.spec?.endAfterCreate
    && entry.groupBuy.startTime <= localTime(0)
    && entry.groupBuy.endTime > localTime(0))
  for (let i = 0; i < state.buyers.length; i += 1) {
    const buyer = state.buyers[i]
    const merchant = state.merchants[i % state.merchants.length]
    const altMerchant = state.merchants[(i + 1) % state.merchants.length]
    await apiPost(`leaders/${merchant.leaderId}/subscription`, { source: 'homepage' }, buyer.token)
    await apiPost(`leaders/${altMerchant.leaderId}/subscription`, { source: 'group_buy_detail' }, buyer.token)

    const viewed = orderTargets[i % orderTargets.length]
    await apiGet(`group-buys/${viewed.groupBuy.id}?latitude=30.2741&longitude=120.1551`, buyer.token)
    await apiPost(`group-buys/${viewed.groupBuy.id}/favorite`, {}, buyer.token)
  }

  const statusFlow = ['pending', 'paid', 'shipped', 'completed', 'paid', 'shipped', 'completed', 'pending']
  for (let i = 0; i < state.buyers.length; i += 1) {
    const buyer = state.buyers[i]
    const target = orderTargets[i % orderTargets.length]
    const item = target.items[0]
    const order = await apiPost('orders', {
      groupBuyId: target.groupBuy.id,
      addressId: buyer.addressId,
      remark: `${buyer.nickname} 的演示订单 ${i + 1}`,
      items: [{ groupBuyItemId: item.id, quantity: (i % 2) + 1 }],
    }, buyer.token, { 'Idempotency-Key': `demo-order-${buyer.phone}-${target.groupBuy.id}` })

    const record = { buyer, target, orderId: order.id, intendedStatus: statusFlow[i] }
    if (['paid', 'shipped', 'completed'].includes(statusFlow[i])) {
      await apiPost(`orders/${order.id}/simulate-pay`, {}, buyer.token, { 'Idempotency-Key': `demo-pay-${order.id}` })
    }
    if (['shipped', 'completed'].includes(statusFlow[i])) {
      await apiPost(`my/store/orders/${order.id}/ship`, {
        deliveryType: target.groupBuy.deliveryType,
        logisticsCompany: target.groupBuy.deliveryType === 'pickup' ? '' : '社区团购专线',
        trackingNo: target.groupBuy.deliveryType === 'pickup' ? '' : `GS${Date.now()}${i}`,
      }, target.token, { 'Idempotency-Key': `demo-ship-${order.id}` })
    }
    if (statusFlow[i] === 'completed') {
      await apiPost(`orders/${order.id}/complete`, {}, buyer.token, { 'Idempotency-Key': `demo-complete-${order.id}` })
    }
    state.orders.push(record)
  }
}

async function verifySeed() {
  const publicList = await apiGet('group-buys?page=1&pageSize=20')
  const publicCount = publicList.total ?? publicList.items?.length ?? 0
  if (publicCount < 8) {
    throw new Error(`Expected at least 8 public group buys, got ${publicCount}`)
  }
  for (const merchant of state.merchants) {
    const homepage = await apiGet(`leaders/${merchant.leaderId}/homepage?page=1&pageSize=20&latitude=30.2741&longitude=120.1551`)
    if (!homepage.store || !homepage.leader) {
      throw new Error(`Leader homepage verification failed for ${merchant.nickname}`)
    }
  }
  const completedOrder = state.orders.find((order) => order.intendedStatus === 'completed')
  const buyer = completedOrder?.buyer || state.buyers[0]
  const orders = await apiGet('my/orders?page=1&pageSize=20', buyer.token)
  const memberCards = await apiGet('my/member-cards', buyer.token)
  const notifications = await apiGet('my/notifications?page=1&pageSize=20', buyer.token)
  if ((orders.total ?? 0) < 1) {
    throw new Error(`Expected seeded buyer ${buyer.phone} to have orders`)
  }
  if ((memberCards.items?.length ?? memberCards.length ?? 0) < 1) {
    throw new Error(`Expected seeded buyer ${buyer.phone} to have member cards`)
  }
  if ((notifications.total ?? 0) < 1) {
    throw new Error(`Expected seeded buyer ${buyer.phone} to have notifications`)
  }
}

function resolveTiming(spec) {
  if (spec.timing === 'presale') {
    return { startTime: localTime(12), endTime: localTime(24 * 4), shippingTime: localTime(24 * 6) }
  }
  if (spec.timing === 'endingSoon') {
    return { startTime: localTime(-24), endTime: localTime(5), shippingTime: localTime(24) }
  }
  if (spec.endAfterCreate) {
    return { startTime: localTime(-48), endTime: localTime(24), shippingTime: localTime(48) }
  }
  return { startTime: localTime(-6), endTime: localTime(24 * 3), shippingTime: localTime(24 * 4) }
}

function contentBlocksFor(merchant, spec) {
  return [
    { type: 'section', title: '团长推荐', text: `${merchant.nickname}：${spec.introduction}` },
    { type: 'paragraph', text: '本团按微信群和小区订单集中收单，不是长期货架。请按页面时间下单，成团后统一履约。' },
    { type: 'list', title: '适合谁买', items: ['家庭日常补货', '办公室同事拼单', '邻里群一起凑单'] },
    { type: 'deliveryNote', title: '履约说明', text: `${deliveryText(spec.deliveryType)}，具体到货时间以团长通知为准。` },
  ]
}

function deliveryText(deliveryType) {
  return {
    express: '快递配送',
    pickup: '到店自提',
    local_delivery: '同城配送',
  }[deliveryType] || '团长履约'
}

function displayNameForGroupItem(name, index) {
  const suffixes = ['社区团购装', '家庭分享装', '邻里拼单装']
  return `${name} ${suffixes[index] || '活动装'}`
}

async function uploadImageAs(imageKey, filename, token) {
  const filePath = await downloadImage(imageKey, filename)
  const bytes = await fetchFileBytes(filePath)
  const formData = new FormData()
  formData.append('file', new Blob([bytes.data], { type: bytes.contentType }), filename)
  const response = await apiMultipart('my/uploads/images', formData, token)
  return response.url
}

async function downloadImage(imageKey, filename) {
  const sourceUrl = IMAGE_SOURCES[imageKey]
  if (!sourceUrl) throw new Error(`Unknown image key: ${imageKey}`)
  const filePath = join(TMP_IMAGE_DIR, filename)
  try {
    const response = await fetch(sourceUrl)
    if (!response.ok) {
      throw new Error(`HTTP ${response.status}`)
    }
    const contentType = response.headers.get('content-type') || ''
    if (!contentType.startsWith('image/')) {
      throw new Error(`non-image content-type ${contentType}`)
    }
    await writeFile(filePath, Buffer.from(await response.arrayBuffer()))
  } catch (fetchError) {
    try {
      await execFileAsync('curl', ['-L', '--fail', '--max-time', '30', '-o', filePath, sourceUrl])
    } catch (curlError) {
      throw new Error(`Image download failed: ${imageKey} ${sourceUrl} (fetch: ${fetchError.message}; curl: ${curlError.message})`)
    }
  }
  return filePath
}

async function fetchFileBytes(filePath) {
  const response = await fetch(`file://${filePath}`).catch(() => null)
  if (response?.ok) {
    return { data: Buffer.from(await response.arrayBuffer()), contentType: 'image/jpeg' }
  }
  const { readFile } = await import('node:fs/promises')
  return { data: await readFile(filePath), contentType: 'image/jpeg' }
}

async function apiGet(path, token) {
  return apiRequest('GET', path, undefined, token)
}

async function apiPost(path, body, token, headers = {}) {
  return apiRequest('POST', path, body, token, headers)
}

async function apiMultipart(path, formData, token) {
  return apiRequest('POST', path, formData, token)
}

async function apiRequest(method, path, body, token, extraHeaders = {}) {
  const url = `${API_BASE_URL}/${path.replace(/^\//, '')}`
  const headers = { ...extraHeaders }
  const init = { method, headers }
  if (token) headers.Authorization = `Bearer ${token}`
  if (body instanceof FormData) {
    init.body = body
  } else if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    init.body = JSON.stringify(body)
  }
  lastRequest = { method, url, body: body instanceof FormData ? '[multipart]' : body }
  const response = await fetch(url, init)
  const text = await response.text()
  let json = null
  try {
    json = text ? JSON.parse(text) : null
  } catch {
    throw new Error(`Non-JSON response from ${method} ${url}: ${text.slice(0, 300)}`)
  }
  if (!response.ok || json?.success === false) {
    const code = json?.error?.code || response.status
    const message = json?.error?.message || response.statusText
    throw new Error(`API failed ${method} ${url}: ${code} ${message}\n${text}`)
  }
  return json?.data
}

function slugFilename(input, fallback) {
  const ascii = input
    .normalize('NFKD')
    .replace(/[^\w.-]+/g, '-')
    .replace(/^-+|-+$/g, '')
    .toLowerCase()
  return ascii || fallback
}

function printSummary() {
  console.log('\nSeed completed.')
  console.log('\nLeader accounts:')
  for (const merchant of state.merchants) {
    console.log(`- ${merchant.nickname} phone=${merchant.phone} leaderId=${merchant.leaderId} storeId=${merchant.storeId}`)
  }
  console.log('\nBuyer accounts:')
  for (const buyer of state.buyers) {
    console.log(`- ${buyer.nickname} phone=${buyer.phone} userId=${buyer.userId} addressId=${buyer.addressId}`)
  }
  console.log('\nRecommended pages:')
  const recommended = state.groupBuys.filter((entry) => entry.groupBuy.status === 'published').slice(0, 5)
  for (const entry of recommended) {
    console.log(`- /group-buys/${entry.groupBuy.id} ${entry.groupBuy.title}`)
  }
  console.log('\nOrder samples:')
  for (const order of state.orders) {
    console.log(`- orderId=${order.orderId} buyer=${order.buyer.nickname} intendedStatus=${order.intendedStatus}`)
  }
}

main().catch((error) => {
  console.error('\nSeed failed.')
  if (lastRequest) {
    console.error('Last request:', JSON.stringify(lastRequest, null, 2))
  }
  console.error(error.stack || error.message)
  process.exit(1)
})
