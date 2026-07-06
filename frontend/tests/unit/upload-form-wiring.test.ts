import { beforeEach, describe, expect, it, vi } from 'vitest'
import { flushPromises, mount } from '@vue/test-utils'
import CreateStoreView from '@/views/CreateStoreView.vue'
import LeaderStoreView from '@/views/leader/LeaderStoreView.vue'
import PublishGroupBuyView from '@/views/leader/PublishGroupBuyView.vue'
import { createStore, getMyStore, updateMyStore } from '@/api/stores'
import { createGroupBuy, polishGroupBuyCopy } from '@/api/leaderGroupBuys'
import { uploadImage } from '@/api/uploads'
import { listProducts } from '@/api/products'
import { showToast } from 'vant'

const push = vi.fn()
const back = vi.fn()
const replace = vi.fn()
const fetchMe = vi.fn()

vi.mock('vue-router', () => ({
  useRoute: () => ({ query: {} }),
  useRouter: () => ({ push, back, replace }),
}))

vi.mock('@/stores/auth', () => ({
  useAuthStore: () => ({
    isLeader: false,
    fetchMe,
  }),
}))

vi.mock('@/api/stores', () => ({
  createStore: vi.fn(),
  getMyStore: vi.fn(),
  updateMyStore: vi.fn(),
}))

vi.mock('@/api/leaderGroupBuys', () => ({
  createGroupBuy: vi.fn(),
  polishGroupBuyCopy: vi.fn(),
}))

vi.mock('@/api/uploads', () => ({
  uploadImage: vi.fn(),
}))

vi.mock('@/api/products', () => ({
  listProducts: vi.fn(),
}))

vi.mock('vant', async () => {
  const actual = await vi.importActual<typeof import('vant')>('vant')
  return {
    ...actual,
    showToast: vi.fn(),
  }
})

const uploadedImage = {
  url: '/uploads/images/uploaded.png',
  objectKey: 'images/uploaded.png',
  originalFilename: 'uploaded.png',
  contentType: 'image/png' as const,
  size: 9,
}

const popupStub = {
  props: ['show'],
  template: '<div v-if="show"><slot /></div>',
}

const globalStubs = {
  VanPopup: popupStub,
  'van-popup': popupStub,
}

async function uploadFirstFile(wrapper: ReturnType<typeof mount>) {
  const input = wrapper.find('input[type="file"]')
  Object.defineProperty(input.element, 'files', {
    value: [new File(['image'], 'uploaded.png', { type: 'image/png' })],
    configurable: true,
  })
  await input.trigger('change')
  await flushPromises()
}

describe('upload form wiring', () => {
  beforeEach(() => {
    push.mockClear()
    back.mockClear()
    replace.mockClear()
    fetchMe.mockClear()
    vi.mocked(createStore).mockReset()
    vi.mocked(getMyStore).mockReset()
    vi.mocked(updateMyStore).mockReset()
    vi.mocked(createGroupBuy).mockReset()
    vi.mocked(polishGroupBuyCopy).mockReset()
    vi.mocked(uploadImage).mockReset()
    vi.mocked(listProducts).mockReset()
    vi.mocked(showToast).mockClear()
    vi.mocked(uploadImage).mockResolvedValue(uploadedImage)
    vi.mocked(listProducts).mockResolvedValue({
      items: [],
      page: 1,
      pageSize: 50,
      total: 0,
      hasMore: false,
    })
  })

  it('writes uploaded logo url into create store payload', async () => {
    vi.mocked(createStore).mockResolvedValue({
      leader: {
        id: '10',
        displayName: '王姐鲜果团',
        avatarUrl: uploadedImage.url,
      },
      store: {
        id: '20',
        leaderId: '10',
        name: '王姐鲜果店',
        logoUrl: uploadedImage.url,
        description: '社区水果团',
        defaultDeliveryType: 'express',
        distributionEnabled: false,
        status: 'active',
        latitude: null,
        longitude: null,
      },
    })

    const wrapper = mount(CreateStoreView, {
      global: { stubs: globalStubs },
    })
    await wrapper.find('input[placeholder="请输入店铺名称（必填）"]').setValue('王姐鲜果店')
    await uploadFirstFile(wrapper)
    await wrapper.find('textarea[placeholder="可选，介绍你的店铺"]').setValue('社区水果团')
    await wrapper.find('form').trigger('submit')
    await flushPromises()

    expect(createStore).toHaveBeenCalledWith(expect.objectContaining({
      name: '王姐鲜果店',
      logoUrl: uploadedImage.url,
      description: '社区水果团',
      defaultDeliveryType: 'express',
    }))
    expect(fetchMe).toHaveBeenCalledOnce()
    expect(replace).toHaveBeenCalledWith('/leader/store')
  })

  it('writes uploaded logo url into store update payload', async () => {
    vi.mocked(getMyStore).mockResolvedValue({
      leader: {
        id: '10',
        displayName: '王姐鲜果团',
        avatarUrl: null,
      },
      store: {
        id: '20',
        leaderId: '10',
        name: '王姐鲜果店',
        logoUrl: '',
        description: '社区水果团',
        defaultDeliveryType: 'express',
        distributionEnabled: false,
        status: 'active',
        latitude: null,
        longitude: null,
      },
    })
    vi.mocked(updateMyStore).mockResolvedValue({
      leader: {
        id: '10',
        displayName: '王姐鲜果团',
        avatarUrl: uploadedImage.url,
      },
      store: {
        id: '20',
        leaderId: '10',
        name: '王姐鲜果店',
        logoUrl: uploadedImage.url,
        description: '社区水果团',
        defaultDeliveryType: 'express',
        distributionEnabled: false,
        status: 'active',
        latitude: 30.2741,
        longitude: 120.1551,
      },
    })

    const wrapper = mount(LeaderStoreView, {
      global: { stubs: globalStubs },
    })
    await flushPromises()
    await wrapper.findAll('button').find((button) => button.text().includes('编辑资料'))?.trigger('click')
    await uploadFirstFile(wrapper)
    await wrapper.findAll('button').find((button) => button.text().includes('保存'))?.trigger('click')
    await flushPromises()

    expect(updateMyStore).toHaveBeenCalledWith(expect.objectContaining({
      logoUrl: uploadedImage.url,
    }))
    expect(fetchMe).toHaveBeenCalledOnce()
  })

  it('writes uploaded group buy cover url into create payload', async () => {
    vi.mocked(createGroupBuy).mockResolvedValue({
      groupBuy: {
        id: '100',
        storeId: '20',
        leaderId: '10',
        title: '周末鲜果团',
        introduction: null,
        coverImageUrl: uploadedImage.url,
        groupType: 'normal',
        deliveryType: 'express',
        shippingTime: null,
        startTime: null,
        endTime: null,
        visibility: 'public',
        status: 'published',
      },
      items: [],
    })
    const wrapper = mount(PublishGroupBuyView, {
      global: { stubs: globalStubs },
    })

    await wrapper.find('input[placeholder="团购标题，例如：周末阳山水蜜桃社区团"]').setValue('周末鲜果团')
    await uploadFirstFile(wrapper)
    await wrapper.findAll('.seg button')[1].trigger('click')
    await wrapper.findAll('button').find((button) => button.text().includes('新增商品'))?.trigger('click')
    await wrapper.find('input[placeholder="商品名称"]').setValue('白玉蜜桃')
    await wrapper.find('input[placeholder="0.00"]').setValue('29.90')
    await wrapper.find('input[placeholder="库存数量"]').setValue('20')
    await wrapper.findAll('.seg button')[2].trigger('click')
    await wrapper.find('.checkbox-circle').trigger('click')
    await wrapper.findAll('button').find((button) => button.text().includes('发布团购'))?.trigger('click')
    await flushPromises()

    expect(createGroupBuy).toHaveBeenCalledWith(expect.objectContaining({
      title: '周末鲜果团',
      coverImageUrl: uploadedImage.url,
      items: [
        expect.objectContaining({
          product: expect.objectContaining({
            name: '白玉蜜桃',
            basePriceAmount: 2990,
            stock: 20,
          }),
          displayName: '白玉蜜桃',
          groupPriceAmount: 2990,
          groupStock: 20,
        }),
      ],
    }))
    expect(push).toHaveBeenCalledWith('/leader/group-buys')
  })

  it('previews and applies AI polished group buy copy before submit', async () => {
    vi.mocked(polishGroupBuyCopy).mockResolvedValue({
      title: '周末鲜果团购',
      introduction: '这次给大家整理了适合家庭囤货的鲜果团。',
      source: 'local',
      contentBlocks: [
        {
          type: 'paragraph',
          text: '这次给大家整理了适合家庭囤货的鲜果团。',
        },
        {
          type: 'list',
          items: ['白玉蜜桃 5 斤装，团购价 ¥29.90，限量 20 份'],
        },
      ],
    })
    vi.mocked(createGroupBuy).mockResolvedValue({
      groupBuy: {
        id: '101',
        storeId: '20',
        leaderId: '10',
        title: '周末鲜果团购',
        introduction: '这次给大家整理了适合家庭囤货的鲜果团。',
        coverImageUrl: uploadedImage.url,
        groupType: 'normal',
        deliveryType: 'express',
        shippingTime: null,
        startTime: null,
        endTime: null,
        visibility: 'public',
        status: 'published',
      },
      items: [],
    })

    const wrapper = mount(PublishGroupBuyView, {
      global: { stubs: globalStubs },
    })

    await wrapper.find('input[placeholder="团购标题，例如：周末阳山水蜜桃社区团"]').setValue('周末鲜果')
    await wrapper.find('textarea[placeholder="说明规格、口感、截单时间、发货方式和售后口径"]').setValue('香甜多汁')
    await wrapper.findAll('.seg button')[1].trigger('click')
    await wrapper.findAll('button').find((button) => button.text().includes('新增商品'))?.trigger('click')
    await wrapper.find('input[placeholder="商品名称"]').setValue('白玉蜜桃')
    await wrapper.find('input[placeholder="0.00"]').setValue('29.90')
    await wrapper.find('input[placeholder="库存数量"]').setValue('20')

    await wrapper.findAll('.seg button')[0].trigger('click')
    await wrapper.findAll('button').find((button) => button.text().includes('AI 润色'))?.trigger('click')
    await flushPromises()

    expect(polishGroupBuyCopy).toHaveBeenCalledWith(expect.objectContaining({
      title: '周末鲜果',
      introduction: '香甜多汁',
      items: [
        expect.objectContaining({
          displayName: '白玉蜜桃',
          groupPriceAmount: 2990,
          groupStock: 20,
        }),
      ],
    }))
    expect(wrapper.text()).toContain('AI 润色建议')
    expect(wrapper.text()).toContain('周末鲜果团购')

    await wrapper.findAll('button').find((button) => button.text().includes('采用建议'))?.trigger('click')
    await wrapper.findAll('.seg button')[2].trigger('click')
    await wrapper.find('.checkbox-circle').trigger('click')
    await wrapper.findAll('button').find((button) => button.text().includes('发布团购'))?.trigger('click')
    await flushPromises()

    expect(createGroupBuy).toHaveBeenCalledWith(expect.objectContaining({
      title: '周末鲜果团购',
      introduction: '这次给大家整理了适合家庭囤货的鲜果团。',
      contentBlocks: [
        expect.objectContaining({
          type: 'paragraph',
          text: '这次给大家整理了适合家庭囤货的鲜果团。',
        }),
        expect.objectContaining({
          type: 'list',
          items: ['白玉蜜桃 5 斤装，团购价 ¥29.90，限量 20 份'],
        }),
      ],
    }))
  })

  it('shows toast when AI polish fails', async () => {
    vi.mocked(polishGroupBuyCopy).mockRejectedValue(new Error('服务暂不可用'))

    const wrapper = mount(PublishGroupBuyView, {
      global: { stubs: globalStubs },
    })

    await wrapper.findAll('button').find((button) => button.text().includes('AI 润色'))?.trigger('click')
    await flushPromises()

    expect(showToast).toHaveBeenCalledWith('服务暂不可用')
  })
})
