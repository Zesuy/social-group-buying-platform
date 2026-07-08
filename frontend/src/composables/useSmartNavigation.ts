import { computed } from 'vue'
import { useRoute, useRouter, type RouteLocationNormalizedLoaded, type RouteLocationRaw } from 'vue-router'

export function canSmartGoBack(): boolean {
  const state = window.history.state as { back?: unknown; position?: number } | null
  return Boolean(state?.back) || Number(state?.position ?? 0) > 0
}

function normalizeFallback(fallback?: RouteLocationRaw | string | null): RouteLocationRaw {
  return fallback || '/'
}

export function getRouteBackFallback(route: RouteLocationNormalizedLoaded): RouteLocationRaw {
  return normalizeFallback(route.meta.backFallback as RouteLocationRaw | string | undefined)
}

export function useSmartNavigation(defaultFallback?: RouteLocationRaw | string) {
  const router = useRouter()
  const route = useRoute()
  const fallbackTarget = computed(() => normalizeFallback(defaultFallback ?? route.meta.backFallback as RouteLocationRaw | string | undefined))

  function goBack(fallback?: RouteLocationRaw | string) {
    if (canSmartGoBack()) {
      router.back()
      return
    }
    void router.replace(normalizeFallback(fallback ?? fallbackTarget.value))
  }

  function goBackOrReplace(fallback?: RouteLocationRaw | string) {
    goBack(fallback)
  }

  async function goAfterSuccess(target?: RouteLocationRaw | string) {
    await router.replace(normalizeFallback(target ?? fallbackTarget.value))
  }

  return {
    goBack,
    goBackOrReplace,
    goAfterSuccess,
    hasBrowserBackTarget: canSmartGoBack,
  }
}
