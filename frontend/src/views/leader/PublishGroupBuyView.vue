<template>
  <PageLayout title="发布团购" show-back @back="goBack">
    <div class="publish-content">
      <!-- 通告条（demo notice） -->
      <AppPageNote variant="warning" class="notice">
        类似功能的产品相关的团购。对于任何违禁行为，邻鲜团将处理
        <span class="close">×</span>
      </AppPageNote>

      <!-- Segmented control（demo .seg） -->
      <div class="seg">
        <button
          v-for="(tab, idx) in segTabs"
          :key="idx"
          :class="{ active: activeTab === idx }"
          @click="activeTab = idx"
        >
          {{ tab }}
        </button>
      </div>

      <!-- Tab 0: 介绍 -->
      <div v-show="activeTab === 0">
        <AppFormCard>
          <template #title>
            <div class="flex items-center justify-between">
              <span>团购介绍</span>
              <AppButton variant="plain" disabled>复制已有团购</AppButton>
            </div>
          </template>
          <div class="p14">
            <input v-model="form.title" class="input mb10" placeholder="团购标题，例如：山东蒙阴白玉蜜桃，新鲜清甜" />
            <textarea v-model="form.introduction" class="textarea h150" placeholder="活动介绍、产品说明、发货信息等"></textarea>
            <div class="toolbar">
              <div class="tool"><span class="ti">▧</span>大图</div>
              <div class="tool"><span class="ti">▦</span>小图</div>
              <div class="tool"><span class="ti">▶</span>视频</div>
              <div class="tool"><span class="ti">✎</span>文字</div>
              <div class="tool"><span class="ti">🏷</span>标签</div>
              <div class="tool"><span class="ti">👥</span>加粉</div>
            </div>
          </div>
        </AppFormCard>

        <AppFormCard title="封面图片 URL">
          <div class="p14">
            <ImageUploader
              v-model="form.coverImageUrl"
              :disabled="submitting"
              :preview-alt="form.title || '团购封面'"
              demo-kind="cover"
              placeholder="可选，输入或上传团购封面"
            />
          </div>
        </AppFormCard>
      </div>

      <!-- Tab 1: 商品 -->
      <div v-show="activeTab === 1">
        <AppFormCard>
          <template #title>
            <div class="flex items-center justify-between">
              <span>团购商品</span>
              <AppButton variant="plain" disabled>从商品库导入</AppButton>
            </div>
          </template>
          <div class="search" style="margin:12px">⌕ 搜索商品名称、规格</div>

          <div v-for="(item, index) in form.items" :key="index">
            <AppFormRow label="名称">
              <input v-model="item.displayName" class="input" placeholder="商品名称" />
            </AppFormRow>
            <AppFormRow label="团购价">
              <input v-model="item.priceText" class="input" placeholder="0.00" type="digit" @input="onItemPriceInput(index, ($event.target as HTMLInputElement).value)" />
            </AppFormRow>
            <AppFormRow label="团购库存">
              <input v-model.number="item.groupStock" class="input" placeholder="库存数量" type="number" />
            </AppFormRow>
            <AppFormRow v-if="form.items.length > 1">
              <AppButton variant="danger" @click="removeItem(index)">删除此商品</AppButton>
            </AppFormRow>
          </div>

          <div class="p14">
            <AppButton block @click="addItem">+ 添加商品</AppButton>
          </div>
        </AppFormCard>
      </div>

      <!-- Tab 2: 设置 -->
      <div v-show="activeTab === 2">
        <AppFormCard title="团购设置">
          <AppFormRow label="物流方式">
            <div class="radio-row">
              <label class="radio-label" :class="{ active: form.deliveryType === 'express' }">
                <input type="radio" v-model="form.deliveryType" value="express" /> 快递配送
              </label>
              <label class="radio-label" :class="{ active: form.deliveryType === 'pickup' }">
                <input type="radio" v-model="form.deliveryType" value="pickup" /> 到店自提
              </label>
              <label class="radio-label" :class="{ active: form.deliveryType === 'local_delivery' }">
                <input type="radio" v-model="form.deliveryType" value="local_delivery" /> 同城配送
              </label>
            </div>
          </AppFormRow>
          <AppFormRow label="开始时间">
            <input v-model="form.startTime" type="datetime-local" class="input" />
          </AppFormRow>
          <AppFormRow label="结束时间">
            <input v-model="form.endTime" type="datetime-local" class="input" />
          </AppFormRow>
          <AppFormRow label="开团通知" value="全部订阅用户" arrow />
          <AppFormRow label="隐私设置" value="所有人均可转发" arrow />
          <AppFormRow label="帮卖设置" value="未设置" arrow />
        </AppFormCard>

        <!-- 协议勾选 -->
        <div class="flex items-center gap8 c-hint" style="margin:12px 0" @click="form.agreed = !form.agreed">
          <span class="checkbox-circle" :class="{ checked: form.agreed }">{{ form.agreed ? '✓' : '' }}</span>
          我已阅读并同意 <span class="c-primary">《用户服务协议》《隐私政策》</span>
        </div>
      </div>
    </div>

    <template #action>
      <AppFixedActions>
        <AppButton variant="ghost" disabled>保存并预览</AppButton>
        <AppButton variant="primary" :loading="submitting" @click="handleSubmit">发布团购</AppButton>
      </AppFixedActions>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import AppFormCard from '@/components/AppFormCard.vue'
import AppFormRow from '@/components/AppFormRow.vue'
import AppButton from '@/components/AppButton.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppPageNote from '@/components/AppPageNote.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import { createGroupBuy } from '@/api/leaderGroupBuys'
import { getDemoProductImage } from '@/utils'

const router = useRouter()

const segTabs = ['介绍', '商品', '设置']
const activeTab = ref(0)
const submitting = ref(false)

interface ItemForm {
  displayName: string
  priceText: string
  groupPriceAmount: number
  groupStock: number
}

const form = reactive({
  title: '',
  introduction: '',
  coverImageUrl: '',
  deliveryType: 'express',
  startTime: '',
  endTime: '',
  agreed: false,
  items: [{
    displayName: '',
    priceText: '',
    groupPriceAmount: 0,
    groupStock: 0,
  }] as ItemForm[],
})

function onItemPriceInput(index: number, val: string) {
  const num = parseFloat(val) || 0
  form.items[index].groupPriceAmount = Math.round(num * 100)
  form.items[index].priceText = val
}

function addItem() {
  form.items.push({
    displayName: '',
    priceText: '',
    groupPriceAmount: 0,
    groupStock: 0,
  })
}

function removeItem(index: number) {
  if (form.items.length > 1) {
    form.items.splice(index, 1)
  }
}

function toISOWithTZ(dt: string): string | null {
  if (!dt) return null
  let normalized = dt
  if (normalized.length === 16) normalized += ':00'
  return `${normalized}+08:00`
}

function validate(): string | null {
  if (!form.title.trim()) return '请输入团购标题'
  if (form.items.length === 0) return '至少需要添加一个商品'
  for (const item of form.items) {
    if (!item.displayName.trim()) return '请填写所有商品的名称'
    if (item.groupPriceAmount < 0) return '团购价格不能为负数'
    if (item.groupStock < 0) return '团购库存不能为负数'
  }
  if (form.startTime && form.endTime) {
    if (new Date(form.endTime) <= new Date(form.startTime)) {
      return '结束时间必须晚于开始时间'
    }
  }
  if (!form.agreed) return '请阅读并同意协议'
  return null
}

function goBack() {
  router.back()
}

async function handleSubmit() {
  const errMsg = validate()
  if (errMsg) {
    showToast(errMsg)
    return
  }

  submitting.value = true
  try {
    await createGroupBuy({
      title: form.title,
      introduction: form.introduction || null,
      coverImageUrl: form.coverImageUrl || getDemoProductImage(form.title),
      deliveryType: form.deliveryType,
      startTime: toISOWithTZ(form.startTime),
      endTime: toISOWithTZ(form.endTime),
      items: form.items.map((item) => ({
        product: {
          name: item.displayName,
          coverImageUrl: getDemoProductImage(item.displayName),
          basePriceAmount: item.groupPriceAmount,
          stock: item.groupStock,
        },
        displayName: item.displayName,
        groupPriceAmount: item.groupPriceAmount,
        groupStock: item.groupStock,
        sortOrder: 1,
      })),
    })
    showToast('发布成功')
    router.push('/leader/group-buys')
  } catch (err) {
    const apiErr = err as { message?: string; code?: string }
    showToast(apiErr.message || '发布失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
/* ===== demo 视觉复刻 ===== */
.publish-content {
  padding: 0 14px 80px;
}

/* 通告条（使用 AppPageNote warning 变体，保留边缘溢出与关闭） */
.notice {
  margin: 0 -14px 12px;
  border-radius: 0;
  border: none;
  justify-content: space-between;
  align-items: center;
}
.close { color: #bbb; font-size: 22px; }

/* seg 控件 */
.seg {
  display: flex;
  background: #fff;
  border-bottom: 1px solid #eee;
  margin: 0 -14px 12px;
}
.seg button {
  flex: 1;
  border: 0;
  background: #fff;
  padding: 14px 0;
  font-weight: 800;
  font-size: 16px;
  color: #676d76;
}
.seg button.active {
  color: var(--color-primary);
  position: relative;
}
.seg button.active::after {
  content: "";
  position: absolute;
  bottom: 0;
  left: 32%;
  right: 32%;
  height: 3px;
  background: var(--color-primary);
  border-radius: 4px;
}

/* 输入框 */
.input {
  height: 42px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #eef0f3;
  padding: 0 10px;
  color: #555;
  width: 100%;
  font-size: 14px;
  outline: none;
}
.input:focus {
  border-color: var(--color-primary);
}

.textarea {
  width: 100%;
  min-height: 92px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #eef0f3;
  padding: 10px;
  color: #555;
  line-height: 1.5;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
  outline: none;
}
.textarea:focus {
  border-color: var(--color-primary);
}

/* 工具栏 */
.toolbar {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 10px;
  text-align: center;
  color: #555;
  padding: 18px 0 0;
}
.tool {
  font-size: 12px;
}
.tool .ti {
  font-size: 22px;
  display: block;
  margin-bottom: 4px;
}

/* 搜索占位 */
.search {
  height: 46px;
  background: #f2f3f5;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #aaa;
  font-size: 18px;
}

/* 单选行 */
.radio-row {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.radio-label {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 14px;
  cursor: pointer;
}
.radio-label input { accent-color: var(--color-primary); }

/* 底部固定按钮 */
.app-fixed-actions .app-button {
  height: 50px;
  font-size: 18px;
}

/* 协议勾选框 */
.checkbox-circle {
  width: 18px;
  height: 18px;
  border: 1px solid #ccc;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  background: transparent;
  color: transparent;
  flex-shrink: 0;
}
.checkbox-circle.checked {
  background: var(--color-primary);
  color: #fff;
}

/* 布局与间距工具类 */
.p14 { padding: 14px; }
.mb10 { margin-bottom: 10px; }
.h150 { height: 150px; }
.flex { display: flex; }
.items-center { align-items: center; }
.justify-between { justify-content: space-between; }
.gap8 { gap: 8px; }
.c-primary { color: var(--color-primary); }
.c-hint { color: #8a8f98; }
</style>
