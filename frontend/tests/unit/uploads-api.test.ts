import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/api/request'
import { uploadImage } from '@/api/uploads'

vi.mock('@/api/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('uploads api', () => {
  beforeEach(() => {
    vi.mocked(request.post).mockReset()
  })

  it('posts multipart form data and unwraps response data', async () => {
    vi.mocked(request.post).mockResolvedValue({
      success: true,
      data: {
        url: 'http://localhost:8080/uploads/images/cover.png',
        objectKey: 'images/cover.png',
        originalFilename: 'cover.png',
        contentType: 'image/png',
        size: 9,
      },
      traceId: 'req_1',
    })
    const file = new File(['image'], 'cover.png', { type: 'image/png' })

    const result = await uploadImage(file)

    expect(request.post).toHaveBeenCalledWith('/my/uploads/images', expect.any(FormData))
    const formData = vi.mocked(request.post).mock.calls[0][1] as FormData
    expect(formData.get('file')).toBe(file)
    expect(result.url).toBe('/uploads/images/cover.png')
  })
})
