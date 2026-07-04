import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount, type VueWrapper } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { createRouter, createWebHashHistory } from 'vue-router'
import UserProfileView from '@/views/UserProfileView.vue'
import { useAuthStore } from '@/stores/auth'

const wrappers: VueWrapper[] = []

vi.mock('vant', async () => {
  const actual = await vi.importActual<typeof import('vant')>('vant')
  return {
    ...actual,
    showToast: vi.fn(),
  }
})

function createTestRouter() {
  return createRouter({
    history: createWebHashHistory(),
    routes: [
      { path: '/', component: { template: '<div>首页</div>' } },
      { path: '/profile/me', component: UserProfileView },
      { path: '/store/create', component: { template: '<div>创建店铺</div>' } },
    ],
  })
}

function mountPage() {
  const pinia = createPinia()
  setActivePinia(pinia)
  const authStore = useAuthStore()
  authStore.accessToken = 'token'
  authStore.user = {
    id: 'u1',
    nickname: '买家用户',
    avatarUrl: null,
    phone: '13800000000',
    hasLeader: false,
    leaderId: null,
    storeId: null,
  }

  const updateProfile = vi.spyOn(authStore, 'updateProfile').mockImplementation(async (params) => {
    authStore.user = {
      ...authStore.user!,
      nickname: params.nickname || authStore.user!.nickname,
      avatarUrl: params.avatarUrl ?? null,
    }
  })

  const router = createTestRouter()
  const wrapper = mount(UserProfileView, {
    global: {
      plugins: [pinia, router],
      stubs: {
        PageLayout: {
          template: '<div><slot /><slot name="action" /></div>',
        },
        AppFixedActions: {
          template: '<div><slot /></div>',
        },
        ImageUploader: {
          props: ['modelValue'],
          emits: ['update:modelValue'],
          template: '<input class="image-uploader-stub" :value="modelValue" @input="$emit(\'update:modelValue\', $event.target.value)" />',
        },
      },
    },
  })
  wrappers.push(wrapper)

  return {
    authStore,
    router,
    updateProfile,
    wrapper,
  }
}

describe('UserProfileView', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  afterEach(() => {
    wrappers.splice(0).forEach((wrapper) => wrapper.unmount())
  })

  it('shows ordinary user profile with empty store area', () => {
    const { wrapper } = mountPage()

    expect(wrapper.text()).toContain('买家用户')
    expect(wrapper.text()).toContain('暂未创建店铺')
    expect(wrapper.text()).toContain('创建店铺并开团')
  })

  it('updates nickname through auth store', async () => {
    const { wrapper, updateProfile } = mountPage()

    await wrapper.findAll('button').find((button) => button.text().includes('编辑资料'))?.trigger('click')
    await wrapper.find('input[placeholder="请输入昵称"]').setValue('新昵称')
    await wrapper.findAll('button').find((button) => button.text().includes('保存'))?.trigger('click')
    await flushPromises()

    expect(updateProfile).toHaveBeenCalledWith({
      nickname: '新昵称',
      avatarUrl: null,
    })
    expect(wrapper.text()).toContain('新昵称')
  })
})
