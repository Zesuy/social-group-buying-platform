import { onBeforeUnmount, type Ref } from 'vue'
import { onBeforeRouteLeave } from 'vue-router'
import { showConfirmDialog } from 'vant'

interface UnsavedChangesGuardOptions {
  isDirty: Ref<boolean> | (() => boolean)
  message?: string
}

function readDirty(source: UnsavedChangesGuardOptions['isDirty']): boolean {
  return typeof source === 'function' ? source() : source.value
}

export function useUnsavedChangesGuard(options: UnsavedChangesGuardOptions) {
  const message = options.message || '当前页面有未保存的内容，确定离开吗？'
  let bypassOnce = false

  function allowNextNavigation() {
    bypassOnce = true
  }

  async function confirmLeave(): Promise<boolean> {
    if (bypassOnce) {
      bypassOnce = false
      return true
    }
    if (!readDirty(options.isDirty)) return true
    try {
      await showConfirmDialog({
        title: '未保存内容',
        message,
        confirmButtonText: '离开',
        cancelButtonText: '继续编辑',
      })
      return true
    } catch {
      return false
    }
  }

  function beforeUnload(event: BeforeUnloadEvent) {
    if (!readDirty(options.isDirty)) return
    event.preventDefault()
    event.returnValue = ''
  }

  window.addEventListener('beforeunload', beforeUnload)
  onBeforeUnmount(() => window.removeEventListener('beforeunload', beforeUnload))
  onBeforeRouteLeave(async () => confirmLeave())

  return {
    allowNextNavigation,
    confirmLeave,
  }
}
