<template>
  <div class="login-page">
    <NavBar title="登录" show-back @back="goBack" />

    <div class="login-page__body">
      <div class="login-page__header">
        <h2 class="login-page__title">欢迎登录</h2>
        <p class="login-page__subtitle">使用模拟登录快速体验</p>
      </div>

      <!-- 快捷填充按钮 -->
      <div class="login-page__shortcuts">
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

      <!-- 登录表单 -->
      <van-form @submit="handleLogin" class="login-page__form">
        <van-cell-group inset>
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
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import NavBar from '@/components/NavBar.vue'
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

    // 登录成功后跳转到 redirect 参数或首页
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
.login-page {
  min-height: 100vh;
  background-color: var(--color-bg);
}

.login-page__body {
  padding: 24px 16px;
}

.login-page__header {
  text-align: center;
  margin-bottom: 24px;
}

.login-page__title {
  font-size: var(--font-size-xxl);
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.login-page__subtitle {
  font-size: var(--font-size-md);
  color: var(--color-text-hint);
}

.login-page__shortcuts {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-bottom: 24px;
}

.shortcut-btn {
  font-size: var(--font-size-sm);
  min-height: var(--touch-size-min);
}

.login-page__form {
  margin-top: 8px;
}

.login-page__error {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  margin: 12px 16px 0;
  background-color: #fff1f0;
  border-radius: 8px;
  color: var(--color-price);
  font-size: var(--font-size-sm);
}

.login-page__submit {
  padding: 24px 16px 0;
}
</style>
