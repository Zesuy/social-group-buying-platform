<template>
  <PageLayout title="发布团购" show-back @back="goBack">
    <div class="publish-content">
      <!-- 通告条（demo notice） -->
      <div class="notice">
        类似功能的产品相关的团购。对于任何违禁行为，邻鲜团将处理
        <span class="close">×</span>
      </div>

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
        <div class="form-card">
          <div class="form-title between">
            <span>团购介绍</span>
            <button class="btn" type="button" disabled>复制已有团购</button>
          </div>
          <div style="padding:14px">
            <input v-model="form.title" class="input" style="margin-bottom:10px" placeholder="团购标题，例如：山东蒙阴白玉蜜桃，新鲜清甜" />
            <textarea v-model="form.introduction" class="textarea" style="height:150px" placeholder="活动介绍、产品说明、发货信息等"></textarea>
            <div class="toolbar">
              <div class="tool"><span class="ti">▧</span>大图</div>
              <div class="tool"><span class="ti">▦</span>小图</div>
              <div class="tool"><span class="ti">▶</span>视频</div>
              <div class="tool"><span class="ti">✎</span>文字</div>
              <div class="tool"><span class="ti">🏷</span>标签</div>
              <div class="tool"><span class="ti">👥</span>加粉</div>
            </div>
          </div>
        </div>
        <div class="form-card">
          <div class="form-title between">
            <span>封面图片 URL</span>
          </div>
          <div style="padding:14px">
            <input v-model="form.coverImageUrl" class="input" placeholder="可选，输入封面图片链接" />
          </div>
        </div>
      </div>

      <!-- Tab 1: 商品 -->
      <div v-show="activeTab === 1">
        <div class="form-card">
          <div class="form-title between">
            <span>团购商品</span>
            <button class="btn" type="button" disabled>从商品库导入</button>
          </div>
          <div class="search" style="margin:12px">⌕ 搜索商品名称、规格</div>

          <div v-for="(item, index) in form.items" :key="index">
            <div class="field">
              <label>名称</label>
              <input v-model="item.displayName" class="input" placeholder="商品名称" />
              <span></span>
            </div>
            <div class="field">
              <label>团购价</label>
              <input v-model="item.priceText" class="input" placeholder="0.00" type="digit" @input="onItemPriceInput(index, ($event.target as HTMLInputElement).value)" />
              <span></span>
            </div>
            <div class="field">
              <label>团购库存</label>
              <input v-model.number="item.groupStock" class="input" placeholder="库存数量" type="number" />
              <span></span>
            </div>
            <div class="field" v-if="form.items.length > 1">
              <label></label>
              <button class="btn danger" type="button" @click="removeItem(index)" style="width:auto">删除此商品</button>
              <span></span>
            </div>
          </div>

          <button class="btn" type="button" style="width:calc(100% - 28px);margin:14px" @click="addItem">+ 添加商品</button>
        </div>
      </div>

      <!-- Tab 2: 设置 -->
      <div v-show="activeTab === 2">
        <div class="form-card">
          <div class="form-title">团购设置</div>
          <div class="field">
            <label>物流方式</label>
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
          </div>
          <div class="field">
            <label>开始时间</label>
            <input v-model="form.startTime" type="datetime-local" class="input" />
            <span></span>
          </div>
          <div class="field">
            <label>结束时间</label>
            <input v-model="form.endTime" type="datetime-local" class="input" />
            <span></span>
          </div>
          <!-- 非 MVP 占位项 -->
          <div class="field">
            <label>开团通知</label>
            <span class="value">全部订阅用户</span>
            <b class="muted">›</b>
          </div>
          <div class="field">
            <label>隐私设置</label>
            <span class="value">所有人均可转发</span>
            <b class="muted">›</b>
          </div>
          <div class="field">
            <label>帮卖设置</label>
            <span class="value">未设置</span>
            <b class="muted">›</b>
          </div>
        </div>

        <!-- 协议勾选 -->
        <div class="row" style="gap:8px;color:#8a8f98;margin:12px 0" @click="form.agreed = !form.agreed">
          <span :style="{ width:'18px',height:'18px',border:'1px solid #ccc',borderRadius:'50%',display:'inline-flex',alignItems:'center',justifyContent:'center',background:form.agreed?'var(--color-primary)':'transparent',color:form.agreed?'#fff':'transparent',fontSize:'12px' }">{{ form.agreed ? '✓' : '' }}</span>
          我已阅读并同意 <span style="color:var(--color-primary)">《用户服务协议》《隐私政策》</span>
        </div>
      </div>
    </div>

    <template #action>
      <div class="fixed-actions">
        <button class="btn ghost" type="button" disabled>保存并预览</button>
        <button class="btn primary" type="button" :disabled="submitting" @click="handleSubmit">
          {{ submitting ? '发布中...' : '发布团购' }}
        </button>
      </div>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import { createGroupBuy } from '@/api/leaderGroupBuys'

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
      coverImageUrl: form.coverImageUrl || null,
      deliveryType: form.deliveryType,
      startTime: toISOWithTZ(form.startTime),
      endTime: toISOWithTZ(form.endTime),
      items: form.items.map((item) => ({
        product: {
          name: item.displayName,
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
  padding-bottom: 80px;
}

/* 通告条 */
.notice {
  background: #fff7e6;
  color: #e96c2b;
  padding: 10px 14px;
  margin: 0 -14px 12px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 13px;
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

/* 表单卡片 */
.form-card {
  background: #fff;
  border-radius: 14px;
  margin-bottom: 12px;
  overflow: hidden;
}
.form-title {
  font-weight: 900;
  font-size: 18px;
  padding: 14px;
  border-bottom: 1px solid var(--line, #edf0f2);
}
.form-title.between {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.form-title .btn {
  font-size: 13px;
}

/* 字段行 */
.field {
  display: grid;
  grid-template-columns: 94px 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 13px 14px;
  border-bottom: 1px solid var(--line, #edf0f2);
  min-height: 52px;
}
.field label {
  color: #262b32;
  font-weight: 700;
  font-size: 14px;
}
.field .value {
  color: #9aa0a6;
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

/* 按钮 */
.btn {
  border: 0;
  background: #fff;
  border-radius: 9px;
  padding: 8px 14px;
  font-weight: 800;
  color: var(--color-primary);
  border: 1px solid #aeeccd;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  font-size: 14px;
}
.btn.primary {
  background: var(--color-primary);
  color: #fff;
  border-color: var(--color-primary);
}
.btn.ghost {
  background: #fff;
  color: #666;
  border-color: #e7eaee;
}
.btn.danger {
  background: #fff;
  color: #f25541;
  border-color: #ffd3cc;
}
.btn:disabled {
  opacity: 0.5;
}

/* 固定底栏 */
.fixed-actions {
  background: #fff;
  border-top: 1px solid #eee;
  padding: 10px 14px calc(10px + var(--safe-area-bottom, 0px));
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.fixed-actions .btn {
  height: 50px;
  font-size: 18px;
  border-radius: 8px;
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

.row {
  display: flex;
  align-items: center;
}
.muted { color: #969ca5; }
.between { display: flex; align-items: center; justify-content: space-between; }
</style>
