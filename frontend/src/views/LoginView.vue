<template>
  <PageLayout :title="pageTitle" show-back @back="goBack">
    <div class="auth-page">
      <section class="auth-hero">
        <div class="auth-hero__mark">邻</div>
        <div>
          <h1>{{ heroTitle }}</h1>
          <p>{{ heroSubtitle }}</p>
        </div>
      </section>

      <section class="auth-panel">
        <div class="auth-panel__tabs" role="tablist" aria-label="认证方式">
          <button
            type="button"
            :class="{ 'is-active': !isRegisterMode }"
            @click="switchMode('login')"
          >
            登录
          </button>
          <button
            type="button"
            :class="{ 'is-active': isRegisterMode }"
            @click="switchMode('register')"
          >
            注册
          </button>
        </div>

        <van-form @submit="handleSubmit">
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
              v-model="form.code"
              name="code"
              label="验证码"
              placeholder="请输入 6 位验证码"
              :rules="codeRules"
              maxlength="6"
              type="tel"
              clearable
            >
              <template #button>
                <button
                  type="button"
                  class="auth-code-button"
                  :disabled="codeButtonDisabled"
                  @click="handleSendCode"
                >
                  {{ codeButtonText }}
                </button>
              </template>
            </van-field>
            <van-field
              v-if="isRegisterMode"
              v-model="form.nickname"
              name="nickname"
              label="昵称"
              placeholder="例如 王姐鲜果团"
              :rules="nicknameRules"
              maxlength="24"
              clearable
            />
          </van-cell-group>

          <div v-if="devCodeHint" class="auth-page__hint">
            演示验证码：<strong>{{ devCodeHint }}</strong>
          </div>

          <div v-if="errorMessage" class="auth-page__error">
            <van-icon name="info-o" />
            <span>{{ errorMessage }}</span>
          </div>

          <label class="auth-agreement">
            <van-checkbox v-model="agreementChecked" icon-size="16px" />
            <span>我已阅读并同意《用户服务协议》《隐私政策》</span>
          </label>

          <div class="auth-page__submit">
            <van-button
              round
              block
              type="primary"
              native-type="submit"
              :loading="isLoading"
              :loading-text="submitLoadingText"
            >
              {{ submitText }}
            </van-button>
          </div>
        </van-form>
      </section>

      <section class="auth-switch">
        <span>{{ switchPrompt }}</span>
        <button type="button" @click="switchMode(isRegisterMode ? 'login' : 'register')">
          {{ switchAction }}
        </button>
      </section>

      <section class="auth-dev">
        <button
          type="button"
          class="auth-dev__toggle"
          :aria-expanded="devPanelOpen"
          @click="devPanelOpen = !devPanelOpen"
        >
          <span>开发测试账号</span>
          <van-icon :name="devPanelOpen ? 'arrow-up' : 'arrow-down'" />
        </button>
        <div v-if="devPanelOpen" class="auth-dev__content">
          <button type="button" @click="loginAsBuyer" :disabled="devLoginLoading">
            买家测试用户（13800000000）
          </button>
          <button type="button" @click="loginAsLeader" :disabled="devLoginLoading">
            团长测试用户（13700000000）
          </button>
        </div>
      </section>
    </div>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, reactive, ref, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { showToast } from 'vant'
import PageLayout from '@/components/PageLayout.vue'
import { useAuthStore } from '@/stores'
import { getErrorMessage } from '@/api'
import { useSmartNavigation } from '@/composables'
import type { AuthCodeScene } from '@/types'

const router = useRouter()
const route = useRoute()
const { goBack } = useSmartNavigation('/profile')
const authStore = useAuthStore()

const isLoading = ref(false)
const sendingCode = ref(false)
const devLoginLoading = ref(false)
const errorMessage = ref('')
const devCodeHint = ref('')
const agreementChecked = ref(false)
const countdown = ref(0)
const devPanelOpen = ref(false)
let countdownTimer: number | undefined

const form = reactive({
  phone: '',
  code: '',
  nickname: '',
})

const isRegisterMode = computed(() => route.name === 'register' || route.path === '/register')
const codeScene = computed<AuthCodeScene>(() => (isRegisterMode.value ? 'register' : 'login'))
const pageTitle = computed(() => (isRegisterMode.value ? '注册' : '登录'))
const heroTitle = computed(() => (isRegisterMode.value ? '注册邻鲜团' : '登录邻鲜团'))
const heroSubtitle = computed(() => (
  isRegisterMode.value
    ? '创建账号后即可跟团下单、订阅团长，也能继续开店做团长'
    : '登录后查看订单、订阅团长，继续完成跟团购买'
))
const submitText = computed(() => (isRegisterMode.value ? '注册并进入' : '登录'))
const submitLoadingText = computed(() => (isRegisterMode.value ? '正在注册...' : '正在登录...'))
const switchPrompt = computed(() => (isRegisterMode.value ? '已有账号？' : '还没有账号？'))
const switchAction = computed(() => (isRegisterMode.value ? '去登录' : '去注册'))
const codeButtonDisabled = computed(() => sendingCode.value || countdown.value > 0)
const codeButtonText = computed(() => {
  if (sendingCode.value) return '发送中'
  if (countdown.value > 0) return `${countdown.value}s`
  return '获取验证码'
})

const phoneRules = [
  { required: true, message: '请输入手机号' },
  { pattern: /^1\d{10}$/, message: '请输入正确的手机号格式' },
]
const codeRules = [
  { required: true, message: '请输入验证码' },
  { pattern: /^\d{6}$/, message: '请输入 6 位验证码' },
]
const nicknameRules = [
  { required: true, message: '请输入昵称' },
  { validator: (value: string) => value.trim().length > 0, message: '请输入昵称' },
]

watch(isRegisterMode, () => {
  form.code = ''
  devCodeHint.value = ''
  errorMessage.value = ''
  resetCountdown()
})

function getRedirectPath() {
  return (route.query.redirect as string | undefined) || '/profile'
}

function switchMode(mode: AuthCodeScene) {
  const path = mode === 'register' ? '/register' : '/login'
  router.push({ path, query: route.query })
}

function ensurePhoneReady() {
  if (!/^1\d{10}$/.test(form.phone)) {
    errorMessage.value = '请输入正确的手机号'
    return false
  }
  return true
}

function startCountdown(seconds: number) {
  resetCountdown()
  countdown.value = seconds
  countdownTimer = window.setInterval(() => {
    countdown.value -= 1
    if (countdown.value <= 0) {
      resetCountdown()
    }
  }, 1000)
}

function resetCountdown() {
  countdown.value = 0
  if (countdownTimer != null) {
    window.clearInterval(countdownTimer)
    countdownTimer = undefined
  }
}

async function handleSendCode() {
  if (!ensurePhoneReady()) return
  sendingCode.value = true
  errorMessage.value = ''
  devCodeHint.value = ''
  try {
    const data = await authStore.sendAuthCode({
      phone: form.phone,
      scene: codeScene.value,
    })
    if (data.devCode) {
      devCodeHint.value = data.devCode
    }
    startCountdown(data.expiresInSeconds || 60)
    showToast('验证码已发送')
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    errorMessage.value = apiErr.message || getErrorMessage(apiErr.code || 'UNKNOWN_ERROR')
  } finally {
    sendingCode.value = false
  }
}

async function handleSubmit() {
  if (!agreementChecked.value) {
    errorMessage.value = '请先同意服务协议和隐私政策'
    return
  }

  isLoading.value = true
  errorMessage.value = ''
  try {
    if (isRegisterMode.value) {
      await authStore.registerWithCode({
        phone: form.phone,
        code: form.code,
        nickname: form.nickname.trim(),
      })
      showToast('注册成功')
    } else {
      await authStore.loginWithCode({
        phone: form.phone,
        code: form.code,
      })
      showToast('登录成功')
    }

    router.push(getRedirectPath())
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    errorMessage.value = apiErr.message || getErrorMessage(apiErr.code || 'UNKNOWN_ERROR')
  } finally {
    isLoading.value = false
  }
}

async function devLogin(params: { phone: string; nickname: string }) {
  devLoginLoading.value = true
  errorMessage.value = ''
  try {
    await authStore.login(params)
    showToast('登录成功')
    router.push(getRedirectPath())
  } catch (err) {
    const apiErr = err as { code?: string; message?: string }
    errorMessage.value = apiErr.message || getErrorMessage(apiErr.code || 'UNKNOWN_ERROR')
  } finally {
    devLoginLoading.value = false
  }
}

function loginAsBuyer() {
  form.phone = '13800000000'
  form.nickname = '买家用户'
  devLogin({ phone: '13800000000', nickname: '买家用户' })
}

function loginAsLeader() {
  form.phone = '13700000000'
  form.nickname = '团长用户'
  devLogin({ phone: '13700000000', nickname: '团长用户' })
}

onBeforeUnmount(() => {
  resetCountdown()
})
</script>

<style scoped>
.auth-page {
  min-height: 100%;
  padding: 20px 14px 28px;
  background:
    linear-gradient(180deg, var(--color-primary-light) 0, rgba(232, 248, 239, 0) 180px),
    var(--color-bg);
}

.auth-hero {
  display: flex;
  gap: 14px;
  align-items: center;
  padding: 10px 2px 22px;
}

.auth-hero__mark {
  display: flex;
  width: 58px;
  height: 58px;
  flex: 0 0 58px;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  background: var(--color-primary);
  color: #fff;
  font-size: 28px;
  font-weight: 800;
  box-shadow: 0 8px 18px rgba(7, 193, 96, 0.18);
}

.auth-hero h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: var(--font-size-xxl);
  line-height: 32px;
}

.auth-hero p {
  margin: 5px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
  line-height: 21px;
}

.auth-panel {
  overflow: hidden;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.auth-panel__tabs {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 6px;
  padding: 8px;
  background: var(--color-bg-surface);
}

.auth-panel__tabs button {
  min-height: 40px;
  border: 0;
  border-radius: 999px;
  background: transparent;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
}

.auth-panel__tabs button.is-active {
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  font-weight: 700;
  box-shadow: 0 2px 8px rgba(31, 35, 41, 0.06);
}

.auth-code-button {
  min-width: 88px;
  min-height: 36px;
  border: 1px solid var(--color-primary);
  border-radius: 999px;
  background: #fff;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
}

.auth-code-button:disabled {
  border-color: var(--color-border);
  color: var(--color-text-hint);
}

.auth-page__hint {
  margin: 12px 14px 0;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: var(--color-primary-light);
  color: var(--color-primary-dark);
  font-size: var(--font-size-sm);
  line-height: 20px;
}

.auth-page__error {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 12px 14px 0;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: var(--color-price-light);
  color: var(--color-price);
  font-size: var(--font-size-sm);
  line-height: 20px;
}

.auth-agreement {
  display: flex;
  gap: 8px;
  align-items: flex-start;
  padding: 14px 14px 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 20px;
}

.auth-page__submit {
  padding: 18px 14px 16px;
}

.auth-page__submit :deep(.van-button) {
  height: var(--button-capsule-height);
  font-weight: 700;
}

.auth-switch {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  min-height: 48px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
}

.auth-switch button {
  min-height: 40px;
  border: 0;
  background: transparent;
  color: var(--color-primary);
  font-size: var(--font-size-md);
  font-weight: 700;
}

.auth-dev {
  margin-top: 8px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-card);
  background: var(--color-bg-card);
}

.auth-dev__toggle {
  display: flex;
  width: 100%;
  min-height: 46px;
  align-items: center;
  justify-content: space-between;
  border: 0;
  background: transparent;
  padding: 0 14px;
  color: var(--color-text-secondary);
  font-size: var(--font-size-md);
}

.auth-dev__content {
  display: grid;
  gap: 8px;
  padding: 0 12px 12px;
}

.auth-dev__content button {
  min-height: 44px;
  border: 1px solid var(--color-border);
  border-radius: 999px;
  background: var(--color-bg-surface);
  color: var(--color-text-primary);
  font-size: var(--font-size-md);
}

@media (max-width: 340px) {
  .auth-page {
    padding-right: 10px;
    padding-left: 10px;
  }

  .auth-hero {
    gap: 10px;
  }

  .auth-hero__mark {
    width: 50px;
    height: 50px;
    flex-basis: 50px;
    font-size: 24px;
  }
}
</style>
