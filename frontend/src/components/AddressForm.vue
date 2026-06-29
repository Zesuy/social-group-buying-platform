<template>
  <van-form @submit="handleSubmit" @failed="handleFailed">
    <van-cell-group inset>
      <van-field
        v-model="form.receiverName"
        name="receiverName"
        label="收货人"
        placeholder="请输入收货人姓名"
        :rules="nameRules"
        maxlength="32"
      />
      <van-field
        v-model="form.receiverPhone"
        name="receiverPhone"
        label="手机号"
        type="tel"
        placeholder="请输入收货人手机号"
        :rules="phoneRules"
        maxlength="11"
      />
      <van-field
        v-model="form.province"
        name="province"
        label="省"
        placeholder="请输入省份"
        :rules="requiredRules"
        maxlength="16"
      />
      <van-field
        v-model="form.city"
        name="city"
        label="市"
        placeholder="请输入城市"
        :rules="requiredRules"
        maxlength="16"
      />
      <van-field
        v-model="form.district"
        name="district"
        label="区/县"
        placeholder="请输入区/县"
        :rules="requiredRules"
        maxlength="16"
      />
      <van-field
        v-model="form.detail"
        name="detail"
        label="详细地址"
        type="textarea"
        placeholder="请输入街道、门牌号等详细信息"
        :rules="detailRules"
        maxlength="128"
        autosize
      />
      <van-field name="isDefault" label="设为默认">
        <template #input>
          <van-switch v-model="form.isDefault" size="22" />
        </template>
      </van-field>
    </van-cell-group>

    <div class="address-form__actions">
      <van-button
        round
        block
        type="primary"
        native-type="submit"
        :loading="loading"
        loading-text="保存中..."
      >
        保存地址
      </van-button>
    </div>
  </van-form>
</template>

<script setup lang="ts">
import { reactive, watch } from 'vue'
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
    }
  },
  { immediate: true },
)

const requiredRules = [{ required: true, message: '请填写此项' }]
const nameRules = [
  { required: true, message: '请输入收货人姓名' },
]
const phoneRules = [
  { required: true, message: '请输入手机号' },
  {
    pattern: /^1\d{10}$/,
    message: '手机号格式不正确',
  },
]
const detailRules = [
  { required: true, message: '请输入详细地址' },
]

function handleSubmit() {
  emit('submit', { ...form })
}

function handleFailed(errorInfo: { errors: Array<{ message: string }> }) {
  if (errorInfo.errors.length > 0) {
    showToast(errorInfo.errors[0].message)
  }
}
</script>

<style scoped>
.address-form__actions {
  padding: var(--spacing-xl) var(--spacing-lg);
  padding-bottom: calc(var(--spacing-xl) + env(safe-area-inset-bottom, 0px));
}

.address-form__actions .van-button {
  min-height: 44px;
}
</style>
