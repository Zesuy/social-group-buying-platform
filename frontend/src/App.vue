<template>
  <router-view />
  <BottomTabBar v-if="showTabBar" />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import BottomTabBar from '@/components/BottomTabBar.vue'

const route = useRoute()

// 仅在有 showTabBar 元信息的路由显示底部 TabBar
const showTabBar = computed(() => route.meta.showTabBar === true)
</script>

<style>
@import './styles/global.css';
@import './styles/vant-theme.css';
</style>
