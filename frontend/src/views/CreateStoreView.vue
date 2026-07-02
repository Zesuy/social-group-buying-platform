<template>
  <PageLayout title="创建店铺" show-back @back="goBack">
    <div class="create-store__content app-content app-content--tight">
      <ReminderBanner v-if="leaderError" type="warning" :text="leaderError" />

      <van-form @submit="handleSubmit">
        <!-- 店铺名称 (required) -->
        <van-field
          v-model="form.name"
          label="店铺名称"
          placeholder="请输入店铺名称（必填）"
          :rules="nameRules"
        />

        <!-- Logo URL (optional) -->
        <van-field
          v-model="form.logoUrl"
          label="Logo 链接"
          placeholder="可选，输入图片 URL"
        />

        <!-- 店铺简介 (optional) -->
        <van-field
          v-model="form.description"
          label="店铺简介"
          type="textarea"
          placeholder="可选，介绍你的店铺"
          :maxlength="200"
          show-word-limit
        />

        <!-- 默认物流方式 (required) -->
        <van-field name="defaultDeliveryType" label="默认物流方式">
          <template #input>
            <van-radio-group v-model="form.defaultDeliveryType" direction="horizontal">
              <van-radio name="express">快递配送</van-radio>
              <van-radio name="pickup">到店自提</van-radio>
              <van-radio name="local_delivery">同城配送</van-radio>
            </van-radio-group>
          </template>
        </van-field>

        <!-- Submit -->
        <div class="create-store__submit">
          <van-button
            round
            block
            type="primary"
            native-type="submit"
            :loading="submitting"
            loading-text="创建中..."
          >
            创建店铺
          </van-button>
        </div>
      </van-form>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { showToast, showDialog } from 'vant'
import { useAuthStore } from '@/stores/auth'
import { createStore } from '@/api/stores'
import { resolveDisplayImageUrl } from '@/utils'
import PageLayout from '@/components/PageLayout.vue'
import ReminderBanner from '@/components/ReminderBanner.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

function goBack() {
  router.back()
}

const form = ref({
  name: '',
  logoUrl: '',
  description: '',
  defaultDeliveryType: 'express',
})

const nameRules = [
  { required: true, message: '请输入店铺名称' },
  { validator: (val: string) => val.trim().length >= 1, message: '店铺名称不能为空' },
]

const submitting = ref(false)
const leaderError = ref<string | null>(null)

onMounted(async () => {
  if (authStore.isLeader) {
    leaderError.value = '您已创建过店铺，如需修改请前往管理页面。'
    showDialog({
      title: '提示',
      message: '您已创建过店铺，是否前往管理？',
      showCancelButton: true,
      confirmButtonText: '前往管理',
      cancelButtonText: '留在当前页',
    }).then(() => {
      router.replace('/leader/store')
    }).catch(() => {
      // User cancelled, stay on this page with the warning banner
    })
  }
})

async function handleSubmit() {
  submitting.value = true
  try {
    const data = {
      name: form.value.name.trim(),
      logoUrl: resolveDisplayImageUrl(form.value.logoUrl.trim(), form.value.name, 'store'),
      description: form.value.description.trim() || null,
      defaultDeliveryType: form.value.defaultDeliveryType,
    }
    await createStore(data)
    showToast('店铺创建成功')
    // Refresh user identity
    await authStore.fetchMe()
    // Navigate to redirect or /leader/store
    const redirect = route.query.redirect as string
    router.replace(redirect || '/leader/store')
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    // If STORE_ALREADY_EXISTS, just refresh and redirect
    if (apiErr.code === 'STORE_ALREADY_EXISTS') {
      await authStore.fetchMe()
      router.replace((route.query.redirect as string) || '/leader/store')
      return
    }
    showToast(apiErr.message || '创建失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.create-store__content {
  background: var(--color-bg);
}

.create-store__content :deep(.van-cell-group) {
  border-radius: var(--radius-card);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  border: 1px solid rgba(237, 240, 242, 0.72);
}

.create-store__content :deep(.van-cell) {
  min-height: 52px;
}

.create-store__submit {
  margin-top: var(--spacing-xl);
  padding: 0;
}

.create-store__submit :deep(.van-button) {
  height: var(--button-capsule-height);
  font-weight: 900;
}
</style>
