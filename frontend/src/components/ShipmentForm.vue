<template>
  <van-form @submit="handleSubmit">
    <!-- 配送方式 -->
    <van-field name="deliveryType" label="配送方式">
      <template #input>
        <van-radio-group v-model="form.deliveryType">
          <van-radio name="express">快递配送</van-radio>
          <van-radio name="pickup">到店自提</van-radio>
          <van-radio name="local_delivery">同城配送</van-radio>
        </van-radio-group>
      </template>
    </van-field>

    <!-- 物流公司 (当配送方式为快递或同城配送时) -->
    <van-field
      v-if="form.deliveryType !== 'pickup'"
      v-model="form.logisticsCompany"
      name="logisticsCompany"
      label="物流公司"
      placeholder="请输入物流公司名称"
      :rules="[{ required: true, message: '请输入物流公司' }]"
      clearable
    />

    <!-- 运单号 (当配送方式为快递或同城配送时) -->
    <van-field
      v-if="form.deliveryType !== 'pickup'"
      v-model="form.trackingNo"
      name="trackingNo"
      label="运单号"
      placeholder="请输入运单号"
      :rules="[{ required: true, message: '请输入运单号' }]"
      clearable
    />

    <!-- 自提提示 -->
    <div v-if="form.deliveryType === 'pickup'" class="shipment-form__note">
      <van-icon name="info-o" class="shipment-form__note-icon" />
      <span>选择到店自提，无需填写物流信息</span>
    </div>

    <!-- 提交按钮 -->
    <div class="shipment-form__submit">
      <van-button
        round
        block
        type="primary"
        native-type="submit"
        :loading="loading"
        loading-text="发货中..."
      >
        确认发货
      </van-button>
    </div>
  </van-form>
</template>

<script setup lang="ts">
import { reactive } from 'vue'
import { showToast } from 'vant'
import type { ShipRequest } from '@/types'

withDefaults(defineProps<{
  loading?: boolean
}>(), {
  loading: false,
})

const emit = defineEmits<{
  submit: [data: ShipRequest]
}>()

const form = reactive({
  deliveryType: 'express',
  logisticsCompany: '',
  trackingNo: '',
})

/** 验证并提交 */
function handleSubmit() {
  if (form.deliveryType !== 'pickup') {
    if (!form.logisticsCompany.trim()) {
      showToast('请输入物流公司')
      return
    }
    if (!form.trackingNo.trim()) {
      showToast('请输入运单号')
      return
    }
  }

  emit('submit', {
    deliveryType: form.deliveryType,
    logisticsCompany: form.deliveryType !== 'pickup' ? form.logisticsCompany.trim() : '',
    trackingNo: form.deliveryType !== 'pickup' ? form.trackingNo.trim() : '',
  })
}
</script>

<style scoped>
.shipment-form__note {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: var(--spacing-md) var(--spacing-lg);
  padding: 10px 12px;
  background: var(--color-primary-light);
  border-radius: var(--radius-sm);
  font-size: var(--font-size-sm);
  color: var(--color-primary);
}

.shipment-form__note-icon {
  font-size: var(--font-size-lg);
  flex-shrink: 0;
}

.shipment-form__submit {
  margin-top: var(--spacing-xl);
  padding: 0 var(--spacing-lg) calc(var(--spacing-xl) + var(--safe-area-bottom));
}

.shipment-form__submit :deep(.van-button) {
  height: var(--button-capsule-height);
  font-weight: 900;
}
</style>
