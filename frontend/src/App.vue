<template>
  <router-view />
  <NewOrderNotificationDialog />
  <BottomTabBar v-if="showTabBar" :h5-constrained="h5Constrained" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import BottomTabBar from '@/components/BottomTabBar.vue'
import NewOrderNotificationDialog from '@/components/NewOrderNotificationDialog.vue'

const route = useRoute()

// 仅在有 showTabBar 元信息的路由显示底部 TabBar
const showTabBar = computed(() => route.meta.showTabBar === true)
const h5Constrained = computed(() => (
  route.meta?.h5Constrained === true || !(route.path ?? '').startsWith('/merchant')
))
</script>

<style>
@import './styles/global.css';
@import './styles/vant-theme.css';
</style>
