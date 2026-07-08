import { App as CapacitorApp } from '@capacitor/app'
import { Capacitor } from '@capacitor/core'
import { Geolocation } from '@capacitor/geolocation'
import { Share } from '@capacitor/share'
import type { Router } from 'vue-router'
import { canSmartGoBack, getRouteBackFallback } from '@/composables/useSmartNavigation'

export const NATIVE_APP_RESUME_EVENT = 'groupshop:native-app-resume'
type NativeShareResult = 'shared' | 'unsupported' | 'aborted' | 'failed'

export interface NativeLocation {
  latitude: number
  longitude: number
}

const ROOT_TAB_PATHS = new Set(['/', '/orders', '/open-group', '/messages', '/profile'])

export function isNativeApp(): boolean {
  return Capacitor.isNativePlatform()
}

export async function setupNativeAppBridge(router: Router): Promise<void> {
  if (!isNativeApp()) return

  await CapacitorApp.addListener('backButton', async () => {
    if (ROOT_TAB_PATHS.has(router.currentRoute.value.path)) {
      await CapacitorApp.exitApp()
      return
    }
    if (canSmartGoBack()) {
      router.back()
      return
    }
    await router.replace(getRouteBackFallback(router.currentRoute.value))
  })

  await CapacitorApp.addListener('appStateChange', ({ isActive }) => {
    if (isActive) {
      window.dispatchEvent(new Event(NATIVE_APP_RESUME_EVENT))
    }
  })
}

export async function shareByNative(data: ShareData): Promise<NativeShareResult | null> {
  if (!isNativeApp()) return null

  try {
    const canShare = await Share.canShare()
    if (!canShare.value) return 'unsupported'
    await Share.share({
      title: data.title,
      text: data.text,
      url: data.url,
      dialogTitle: data.title,
    })
    return 'shared'
  } catch (err) {
    const name = (err as { name?: string }).name
    const message = (err as { message?: string }).message?.toLowerCase() || ''
    if (name === 'AbortError' || message.includes('cancel')) return 'aborted'
    return 'failed'
  }
}

export async function requestCurrentLocation(options: PositionOptions): Promise<NativeLocation> {
  if (isNativeApp()) {
    const permission = await Geolocation.requestPermissions({ permissions: ['location'] })
    if (permission.location !== 'granted') {
      throw new Error('未获得定位权限，请检查系统设置')
    }
    const position = await Geolocation.getCurrentPosition(options)
    return normalizeLocation(position.coords.latitude, position.coords.longitude)
  }

  return requestBrowserLocation(options)
}

function requestBrowserLocation(options: PositionOptions): Promise<NativeLocation> {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('当前浏览器不支持定位'))
      return
    }
    navigator.geolocation.getCurrentPosition(
      position => {
        try {
          resolve(normalizeLocation(position.coords.latitude, position.coords.longitude))
        } catch (err) {
          reject(err)
        }
      },
      () => reject(new Error('未获得定位权限，请检查浏览器设置')),
      options,
    )
  })
}

function normalizeLocation(latitude: number, longitude: number): NativeLocation {
  const location = {
    latitude: Number(latitude.toFixed(7)),
    longitude: Number(longitude.toFixed(7)),
  }
  if (
    location.latitude < -90
    || location.latitude > 90
    || location.longitude < -180
    || location.longitude > 180
  ) {
    throw new Error('定位坐标异常，请稍后重试')
  }
  return location
}
