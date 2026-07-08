import { nextTick, onBeforeUnmount, onMounted, type Ref, watch } from 'vue'
import { useRoute } from 'vue-router'

interface ScrollPosition {
  top: number
  left: number
}

const scrollPositions = new Map<string, ScrollPosition>()

export function useRouteScrollRestoration(containerRef: Ref<HTMLElement | null>) {
  const route = useRoute()

  function save(path = route.fullPath) {
    const el = containerRef.value
    if (!el) return
    scrollPositions.set(path, {
      top: el.scrollTop,
      left: el.scrollLeft,
    })
  }

  async function restore(path = route.fullPath) {
    await nextTick()
    const position = route.meta?.resetScrollOnEnter ? undefined : scrollPositions.get(path)
    applyPosition(position)
    requestAnimationFrame(() => applyPosition(position))
    window.setTimeout(() => applyPosition(position), 80)
    window.setTimeout(() => applyPosition(position), 180)
  }

  function applyPosition(position?: ScrollPosition) {
    const el = containerRef.value
    if (!el) return
    el.scrollTop = position?.top ?? 0
    el.scrollLeft = position?.left ?? 0
  }

  onBeforeUnmount(() => {
    save()
  })

  onMounted(() => {
    void restore()
  })

  watch(() => route.fullPath, (_next, previous) => {
    save(previous)
    void restore()
  })

  return {
    save,
    restore,
  }
}
