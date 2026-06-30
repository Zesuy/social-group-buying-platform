<template>
  <div class="address-form-wrap">
    <!-- 表单卡片 -->
    <div class="form-card">
      <!-- 收货人 -->
      <div class="field" @click="focusField('receiverName')">
        <label>收货人</label>
        <span v-if="!editingField || editingField !== 'receiverName'" class="value" :class="{ 'value--empty': !form.receiverName }">
          {{ form.receiverName || '请输入姓名' }}
        </span>
        <input
          v-else
          ref="nameInputRef"
          v-model="form.receiverName"
          class="field-input"
          name="receiverName"
          placeholder="请输入姓名"
          maxlength="32"
          @blur="editingField = ''"
          @keyup.enter="editingField = ''"
        />
        <b class="muted" v-if="editingField !== 'receiverName'"></b>
      </div>

      <!-- 手机号 -->
      <div class="field" @click="focusField('receiverPhone')">
        <label>手机号</label>
        <span v-if="!editingField || editingField !== 'receiverPhone'" class="value" :class="{ 'value--empty': !form.receiverPhone }">
          {{ form.receiverPhone || '请输入手机号码' }}
        </span>
        <input
          v-else
          ref="phoneInputRef"
          v-model="form.receiverPhone"
          class="field-input"
          name="receiverPhone"
          type="tel"
          placeholder="请输入手机号码"
          maxlength="11"
          @blur="editingField = ''"
          @keyup.enter="editingField = ''"
        />
        <b class="muted" v-if="editingField !== 'receiverPhone'"></b>
      </div>

      <!-- 省市区 -->
      <div class="field" @click="showAreaPicker = true">
        <label>省市区</label>
        <span class="value" :class="{ 'value--empty': !regionText }">
          {{ regionText || '请选择省市区' }}
        </span>
        <b class="muted">›</b>
      </div>

      <!-- 详细地址 -->
      <div class="field" @click="focusField('detail')">
        <label>详细地址</label>
        <span v-if="!editingField || editingField !== 'detail'" class="value" :class="{ 'value--empty': !form.detail }">
          {{ form.detail || '街道门牌、楼层房间号' }}
        </span>
        <textarea
          v-else
          ref="detailInputRef"
          v-model="form.detail"
          class="field-textarea"
          name="detail"
          placeholder="街道门牌、楼层房间号"
          maxlength="128"
          @blur="editingField = ''"
        />
        <b class="muted" v-if="editingField !== 'detail'"></b>
      </div>

      <!-- 设为默认 -->
      <div class="field">
        <label>设为默认</label>
        <span class="value">下单时优先使用</span>
        <span :class="['switch', { on: form.isDefault }]" @click="form.isDefault = !form.isDefault" />
      </div>
    </div>

    <!-- 省市区选择器 -->
    <van-popup v-model:show="showAreaPicker" position="bottom" round>
      <van-area
        v-model="areaValue"
        :area-list="areaList"
        :columns-num="3"
        @confirm="onAreaConfirm"
        @cancel="showAreaPicker = false"
        :title="'选择省市区'"
      />
    </van-popup>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, watch, nextTick } from 'vue'
import { areaList } from '@vant/area-data'
import type { AddressData } from '@/types'
import { showToast } from 'vant'

const props = withDefaults(defineProps<{
  address?: AddressData | null
  loading?: boolean
}>(), {
  address: null,
  loading: false,
})

const emit = defineEmits<{
  submit: [data: {
    receiverName: string
    receiverPhone: string
    province: string
    city: string
    district: string
    detail: string
    isDefault: boolean
  }]
}>()

const form = reactive({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: false,
})

// 编辑状态
const editingField = ref('')
const nameInputRef = ref<HTMLInputElement | null>(null)
const phoneInputRef = ref<HTMLInputElement | null>(null)
const detailInputRef = ref<HTMLTextAreaElement | null>(null)

// 省市区选择器
const showAreaPicker = ref(false)
const areaValue = ref('')

const regionText = computed(() => {
  const parts = [form.province, form.city, form.district].filter(Boolean)
  return parts.join(' ')
})

/** 聚焦编辑字段 */
function focusField(field: string) {
  editingField.value = field
  nextTick(() => {
    if (field === 'receiverName') nameInputRef.value?.focus()
    else if (field === 'receiverPhone') phoneInputRef.value?.focus()
    else if (field === 'detail') detailInputRef.value?.focus()
  })
}

interface AreaOption {
  code?: string
  value?: string
  name?: string
  text?: string
}

interface AreaConfirmPayload {
  selectedOptions?: AreaOption[]
  selectedValues?: string[]
}

function getAreaOptionName(option?: AreaOption) {
  return option?.name || option?.text || ''
}

function findAreaCode(province: string, city: string, district: string) {
  const provinceCode = Object.entries(areaList.province_list).find(([, name]) => name === province)?.[0] || ''
  const cityCode = Object.entries(areaList.city_list).find(([code, name]) => {
    return name === city && (!provinceCode || code.startsWith(provinceCode.slice(0, 2)))
  })?.[0] || ''
  const countyCode = Object.entries(areaList.county_list).find(([code, name]) => {
    return name === district && (!cityCode || code.startsWith(cityCode.slice(0, 4)))
  })?.[0] || ''

  return countyCode || cityCode || provinceCode
}

/** 省市区确认 */
function onAreaConfirm(payload: AreaConfirmPayload | AreaOption[]) {
  const opts = Array.isArray(payload) ? payload : (payload.selectedOptions || [])
  form.province = getAreaOptionName(opts[0])
  form.city = getAreaOptionName(opts[1])
  form.district = getAreaOptionName(opts[2])
  areaValue.value = opts[2]?.code || opts[2]?.value || opts[1]?.code || opts[1]?.value || opts[0]?.code || opts[0]?.value || ''
  showAreaPicker.value = false
}

/** 填充初始数据 */
watch(
  () => props.address,
  (addr) => {
    if (addr) {
      form.receiverName = addr.receiverName
      form.receiverPhone = addr.receiverPhone
      form.province = addr.province
      form.city = addr.city
      form.district = addr.district
      form.detail = addr.detail
      form.isDefault = addr.isDefault
      areaValue.value = findAreaCode(addr.province, addr.city, addr.district)
    }
  },
  { immediate: true },
)

/** 验证并提交 */
function doSubmit() {
  if (!form.receiverName) { showToast('请输入收货人姓名'); return }
  if (!form.receiverPhone) { showToast('请输入手机号'); return }
  if (!/^1\d{10}$/.test(form.receiverPhone)) { showToast('手机号格式不正确'); return }
  if (!form.province) { showToast('请选择省市区'); return }
  if (!form.detail) { showToast('请输入详细地址'); return }

  emit('submit', {
    receiverName: form.receiverName,
    receiverPhone: form.receiverPhone,
    province: form.province,
    city: form.city,
    district: form.district,
    detail: form.detail,
    isDefault: form.isDefault,
  })
}

defineExpose({ submit: doSubmit })
</script>

<style scoped>
.address-form-wrap {
  padding: 12px 14px 0;
}

/* ── 字段行（复用全局 .field，补充内联编辑样式） ── */
.field-input {
  height: 36px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #eef0f3;
  padding: 0 10px;
  color: #555;
  width: 100%;
  font-size: 14px;
  font-family: inherit;
  outline: none;
}

.field-input:focus {
  border-color: var(--color-primary);
}

.field-textarea {
  min-height: 80px;
  background: #f7f8fa;
  border-radius: 8px;
  border: 1px solid #eef0f3;
  padding: 10px;
  color: #555;
  width: 100%;
  font-size: 14px;
  font-family: inherit;
  line-height: 1.5;
  outline: none;
  resize: none;
}

.field-textarea:focus {
  border-color: var(--color-primary);
}

.value--empty {
  color: var(--color-text-placeholder);
}
</style>
