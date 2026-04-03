<template>
  <el-dropdown trigger="click" @command="handleCommand">
    <span class="page-size-trigger">
      {{ modelValue }}条/页<el-icon class="el-icon--right"><ArrowDown /></el-icon>
    </span>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="size in pageSizes"
          :key="size"
          :command="size"
          :class="{ 'is-active': modelValue === size }"
        >
          {{ size }}条/页
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup>
import { ArrowDown } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: {
    type: Number,
    default: 10
  },
  pageSizes: {
    type: Array,
    default: () => [10, 20, 50, 100]
  }
})

const emit = defineEmits(['update:modelValue', 'change'])

const handleCommand = (size) => {
  emit('update:modelValue', size)
  emit('change', size)
}
</script>

<style scoped>
.page-size-trigger {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.72);
  border: 1px solid rgba(15, 23, 42, 0.12);
  color: var(--bs-ink);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.page-size-trigger:hover {
  background: rgba(15, 23, 42, 0.04);
  border-color: rgba(var(--brand-primary-rgb), 0.45);
}

:deep(.el-dropdown-menu__item.is-active) {
  color: var(--brand-primary);
  font-weight: 600;
}

/* Dark mode */
html.dark .page-size-trigger {
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(148, 163, 184, 0.20);
  color: #ffffff;
}

html.dark .page-size-trigger:hover {
  background: rgba(255, 255, 255, 0.10);
}
</style>
