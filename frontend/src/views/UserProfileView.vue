<template>
  <PageLayout title="我的主页" show-back @back="goBack">
    <div class="user-profile">
      <template v-if="!editing">
        <section class="profile-hero">
          <div class="profile-hero__copy">
            <span>个人主页</span>
            <h1>{{ user?.nickname || '我的主页' }}</h1>
            <p>{{ isLeader ? '你的店铺和开团信息会展示在这里' : '完善头像和昵称，创建店铺后就能开团经营' }}</p>
          </div>
        </section>

        <AppCard class="profile-identity-card">
          <div class="profile-identity-card__main">
            <ImageWithFallback
              :src="user?.avatarUrl"
              :alt="user?.nickname || '用户头像'"
              class="profile-identity-card__avatar"
              demo-kind="avatar"
              width="64px"
              height="64px"
              radius="50%"
            />
            <div class="profile-identity-card__copy">
              <div class="profile-identity-card__name-row">
                <h2>{{ user?.nickname || '未设置昵称' }}</h2>
                <AppStatusPill :variant="isLeader ? 'orange' : 'green'" size="sm">
                  {{ isLeader ? '团长' : '团员' }}
                </AppStatusPill>
              </div>
              <p>{{ formatPhone(user?.phone) }}</p>
            </div>
            <button type="button" class="profile-identity-card__edit" @click="startEdit">
              <van-icon name="edit" size="16" />
              <span>编辑资料</span>
            </button>
          </div>

          <div class="profile-trust-strip">
            <div>
              <b>个人资料</b>
              <span>{{ user?.avatarUrl ? '已设置头像' : '默认头像' }}</span>
            </div>
            <div>
              <b>开团身份</b>
              <span>{{ isLeader ? '店铺已开通' : '暂未开店' }}</span>
            </div>
          </div>
        </AppCard>

        <AppCard class="profile-store-card">
          <div class="profile-store-card__header">
            <ImageWithFallback
              :src="store?.logoUrl"
              :alt="store?.name || '店铺 Logo'"
              class="profile-store-card__logo"
              demo-kind="store"
              width="56px"
              height="56px"
              radius="14px"
            />
            <div>
              <span>开团主页</span>
              <h2>{{ store?.name || '暂未创建店铺' }}</h2>
              <p>{{ storeStatusText }}</p>
            </div>
          </div>

          <div class="profile-store-card__grid">
            <div>
              <b>店铺头像</b>
              <span>{{ store?.logoUrl ? '已设置' : '空' }}</span>
            </div>
            <div>
              <b>团购活动</b>
              <span>{{ isLeader ? '从团购管理查看' : '空' }}</span>
            </div>
            <div>
              <b>履约信息</b>
              <span>{{ isLeader ? '跟随店铺设置' : '空' }}</span>
            </div>
            <div>
              <b>订阅关系</b>
              <span>{{ isLeader ? '可查看订阅用户' : '空' }}</span>
            </div>
          </div>

          <div class="profile-store-card__actions">
            <AppButton
              v-if="isLeader"
              variant="ghost"
              @click="router.push('/leader/store')"
            >
              编辑店铺
            </AppButton>
            <AppButton
              v-if="isLeader && user?.leaderId"
              variant="primary"
              @click="router.push(`/leaders/${user.leaderId}`)"
            >
              查看团长主页
            </AppButton>
            <AppButton
              v-if="!isLeader"
              variant="primary"
              block
              @click="router.push('/store/create')"
            >
              创建店铺并开团
            </AppButton>
          </div>
        </AppCard>
      </template>

      <template v-else>
        <AppFormCard title="编辑个人资料">
          <AppFormRow label="头像" class="profile-edit-avatar-row">
            <ImageUploader
              v-model="form.avatarUrl"
              :disabled="submitting"
              :preview-alt="form.nickname || '用户头像'"
              demo-kind="avatar"
              :show-url-input="false"
              :show-hint="false"
              button-label="更换头像"
            />
          </AppFormRow>

          <AppFormRow label="昵称">
            <van-field
              v-model="form.nickname"
              placeholder="请输入昵称"
              maxlength="50"
              show-word-limit
            />
          </AppFormRow>

          <AppFormRow label="手机号">
            {{ formatPhone(user?.phone) }}
          </AppFormRow>
        </AppFormCard>
      </template>
    </div>

    <template #action>
      <AppFixedActions v-if="editing">
        <AppButton variant="ghost" :disabled="submitting" @click="cancelEdit">取消</AppButton>
        <AppButton variant="primary" :loading="submitting" @click="saveProfile">保存</AppButton>
      </AppFixedActions>
    </template>
  </PageLayout>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { showToast } from 'vant'
import { storeToRefs } from 'pinia'
import { useAuthStore } from '@/stores/auth'
import PageLayout from '@/components/PageLayout.vue'
import AppFormCard from '@/components/AppFormCard.vue'
import AppFormRow from '@/components/AppFormRow.vue'
import AppFixedActions from '@/components/AppFixedActions.vue'
import AppButton from '@/components/AppButton.vue'
import AppCard from '@/components/AppCard.vue'
import AppStatusPill from '@/components/AppStatusPill.vue'
import ImageUploader from '@/components/ImageUploader.vue'
import ImageWithFallback from '@/components/ImageWithFallback.vue'
import { formatPhone } from '@/utils'

const router = useRouter()
const authStore = useAuthStore()
const { user, store, isLeader } = storeToRefs(authStore)

const editing = ref(false)
const submitting = ref(false)
const form = reactive({
  nickname: '',
  avatarUrl: '',
})

const trimmedNickname = computed(() => form.nickname.trim())
const storeStatusText = computed(() => {
  if (!store.value) return '创建店铺后，这里会展示店铺资料和开团入口'
  return store.value.status === 'active' ? '营业中，可继续发布和管理团购' : '店铺资料待完善'
})

function syncForm() {
  form.nickname = user.value?.nickname || ''
  form.avatarUrl = user.value?.avatarUrl || ''
}

function startEdit() {
  syncForm()
  editing.value = true
}

function cancelEdit() {
  editing.value = false
  syncForm()
}

async function saveProfile() {
  if (!trimmedNickname.value) {
    showToast('请输入昵称')
    return
  }

  submitting.value = true
  try {
    await authStore.updateProfile({
      nickname: trimmedNickname.value,
      avatarUrl: form.avatarUrl.trim() || null,
    })
    showToast('保存成功')
    editing.value = false
    syncForm()
  } catch (err) {
    showToast((err as { message?: string }).message || '保存失败')
  } finally {
    submitting.value = false
  }
}

function goBack() {
  router.back()
}

syncForm()
</script>

<style scoped>
.user-profile {
  padding: 12px 14px 96px;
}

.profile-hero {
  min-height: 132px;
  display: flex;
  align-items: flex-end;
  margin-bottom: 12px;
  padding: 18px 16px;
  border-radius: var(--radius-card);
  border: 1px solid var(--color-border);
  background: var(--color-bg-card);
  box-shadow: var(--shadow-card);
}

.profile-hero__copy {
  min-width: 0;
}

.profile-hero__copy span {
  display: block;
  margin-bottom: 6px;
  color: var(--color-primary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.profile-hero__copy h1 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 24px;
  font-weight: 800;
  line-height: 1.2;
  word-break: break-word;
}

.profile-hero__copy p {
  margin: 8px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
}

.profile-identity-card,
.profile-store-card {
  margin-bottom: 12px;
}

.profile-identity-card__main,
.profile-store-card__header {
  display: flex;
  gap: 12px;
  align-items: center;
}

.profile-identity-card__avatar,
.profile-store-card__logo {
  flex-shrink: 0;
}

.profile-identity-card__copy,
.profile-store-card__header > div {
  min-width: 0;
  flex: 1;
}

.profile-identity-card__edit {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  min-width: 88px;
  min-height: 44px;
  padding: 0 12px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  background: var(--color-bg-card);
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.profile-identity-card__name-row {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.profile-identity-card__name-row h2,
.profile-store-card__header h2 {
  margin: 0;
  color: var(--color-text-primary);
  font-size: 18px;
  font-weight: 800;
  line-height: 1.3;
  word-break: break-word;
}

.profile-identity-card__copy p,
.profile-store-card__header p,
.profile-store-card__header span {
  margin: 4px 0 0;
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.45;
}

.profile-store-card__header span {
  display: block;
  margin: 0 0 4px;
  color: var(--color-text-hint);
  font-weight: 700;
}

.profile-trust-strip,
.profile-store-card__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 14px;
}

.profile-trust-strip div,
.profile-store-card__grid div {
  min-height: 58px;
  padding: 10px;
  border-radius: var(--radius-md);
  background: var(--color-bg-page);
}

.profile-trust-strip b,
.profile-store-card__grid b {
  display: block;
  margin-bottom: 4px;
  color: var(--color-text-primary);
  font-size: var(--font-size-sm);
  font-weight: 700;
}

.profile-trust-strip span,
.profile-store-card__grid span {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
  line-height: 1.4;
}

.profile-store-card__actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
}

.profile-store-card__actions :deep(.app-button) {
  flex: 1;
  min-height: 44px;
}

.user-profile :deep(.van-field) {
  padding: 0;
}

.user-profile :deep(.van-cell::after) {
  display: none;
}

.profile-edit-avatar-row {
  grid-template-columns: 1fr;
  align-items: start;
  gap: 10px;
}

.profile-edit-avatar-row :deep(.app-form-row__label) {
  width: 100%;
}

.profile-edit-avatar-row :deep(.image-uploader) {
  max-width: 260px;
}

.profile-edit-avatar-row :deep(.image-uploader__actions) {
  align-items: stretch;
}

.profile-edit-avatar-row :deep(.app-button) {
  min-height: 44px;
  white-space: nowrap;
}

@media (max-width: 340px) {
  .profile-hero {
    min-height: 120px;
    padding: 16px 14px;
  }

  .profile-hero__copy h1 {
    font-size: 22px;
  }

  .profile-trust-strip,
  .profile-store-card__grid {
    grid-template-columns: 1fr;
  }

  .profile-identity-card__main {
    flex-wrap: wrap;
    align-items: flex-start;
  }

  .profile-identity-card__edit {
    width: 100%;
  }

  .profile-edit-avatar-row :deep(.image-uploader) {
    max-width: 100%;
  }
}
</style>
