<template>
  <PageLayout title="登录" show-back @back="goBack">
    <div class="login-page__body">
      <!-- 品牌标志 -->
      <div class="login-page__brand">
        <div class="login-page__brand-icon">邻</div>
        <h2 class="login-page__title">欢迎登录</h2>
        <p class="login-page__subtitle">邻鲜团 — 邻里团购，新鲜直达</p>
      </div>

      <!-- 登录表单卡片 -->
      <div class="login-page__form-card">
        <van-form @submit="handleLogin">
          <van-cell-group :border="false">
            <van-field
              v-model="form.phone"
              name="phone"
              label="手机号"
              placeholder="请输入手机号"
              :rules="phoneRules"
              maxlength="11"
              type="tel"
              clearable
            />
            <van-field
              v-model="form.nickname"
              name="nickname"
              label="昵称"
              placeholder="请输入昵称"
              :rules="[{ required: true, message: '请输入昵称' }]"
              clearable
            />
            <van-field
              v-model="form.avatarUrl"
              name="avatarUrl"
              label="头像"
              placeholder="头像 URL（选填）"
              clearable
            />
          </van-cell-group>

          <!-- 错误提示 -->
          <div v-if="errorMessage" class="login-page__error">
            <van-icon name="info-o" />
            <span>{{ errorMessage }}</span>
          </div>

          <div class="login-page__submit">
            <van-button
              round
              block
              type="primary"
              native-type="submit"
              :loading="isLoading"
              loading-text="正在登录..."
            >
              登录
            </van-button>
          </div>
        </van-form>
      </div>

      <!-- 快捷填充 -->
      <div class="login-page__shortcuts">
        <p class="login-page__shortcuts-title">快捷测试</p>
        <van-button
          size="small"
          plain
          type="primary"
          block
          class="shortcut-btn"
          @click="fillBuyer"
        >
          买家测试用户（13800000000）
        </van-button>
        <van-button
          size="small"
          plain
          type="primary"
          block
          class="shortcut-btn"
          @click="fillLeader"
        >
          团长测试用户（13700000000）
        </van-button>
      </div>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import { useAuthStore } from '@/stores'
import { getErrorMessage } from '@/api'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isLoading = ref(false)
const errorMessage = ref('')

const form = reactive({
  phone: '',
  nickname: '',
  avatarUrl: '',
})

const phoneRules = [
  { required: true, message: '请输入手机号' },
  { pattern: /^1\d{10}$/, message: '请输入正确的手机号格式' },
]

function fillBuyer() {
  form.phone = '13800000000'
  form.nickname = '买家用户'
  form.avatarUrl = ''
  errorMessage.value = ''
}

function fillLeader() {
  form.phone = '13700000000'
  form.nickname = '团长用户'
  form.avatarUrl = ''
  errorMessage.value = ''
}

function goBack() {
  router.back()
}

async function handleLogin() {
  isLoading.value = true
  errorMessage.value = ''

  try {
    await authStore.login({
      phone: form.phone,
      nickname: form.nickname,
      avatarUrl: form.avatarUrl || undefined,
    })

    showToast('登录成功')

    const redirect = route.query.redirect as string | undefined
    router.push(redirect || '/profile')
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    errorMessage.value = apiErr.message || getErrorMessage(apiErr.code || 'UNKNOWN_ERROR')
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.login-page__body {
  padding: 28px 14px;
}

/* ── 品牌区 ── */
.login-page__brand {
  text-align: center;
  margin-bottom: 28px;
}

.login-page__brand-icon {
  width: 72px;
  height: 72px;
  border-radius: 18px;
  background: var(--color-primary);
  color: #fff;
  font-size: 36px;
  font-weight: 900;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}

.login-page__title {
  font-size: var(--font-size-xxl);
  font-weight: 900;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.login-page__subtitle {
  font-size: var(--font-size-md);
  color: var(--color-text-hint);
}

/* ── 表单卡片 ── */
.login-page__form-card {
  background: var(--color-bg-card);
  border-radius: var(--radius-card);
  overflow: hidden;
  box-shadow: var(--shadow-card);
  border: 1px solid rgba(237, 240, 242, 0.72);
}

.login-page__error {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  margin: 12px 16px 0;
  background-color: #fff1f0;
  border-radius: var(--radius-sm);
  color: var(--color-price);
  font-size: var(--font-size-sm);
}

.login-page__submit {
  padding: 24px 14px;
}

.login-page__submit :deep(.van-button) {
  height: var(--button-capsule-height);
  font-weight: 900;
}

/* ── 快捷填充 ── */
.login-page__shortcuts {
  margin-top: 24px;
}

.login-page__shortcuts-title {
  font-size: var(--font-size-sm);
  color: var(--color-text-hint);
  text-align: center;
  margin-bottom: 12px;
}

.shortcut-btn {
  font-size: var(--font-size-sm);
  min-height: var(--touch-size-min);
  margin-bottom: 8px;
}
</style>
