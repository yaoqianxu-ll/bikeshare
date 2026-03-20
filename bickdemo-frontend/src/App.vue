<template>
  <router-view :key="route.fullPath" />
  <ThemeToggle v-if="showFloatingToggle" />
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ThemeToggle from '@/components/ThemeToggle.vue'

const route = useRoute()
const showFloatingToggle = computed(() => route.name === 'Login' || route.name === 'Register')
</script>
