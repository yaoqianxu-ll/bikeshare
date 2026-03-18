import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/global.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'

// 修复下拉框悬停文字下沉问题
function fixDropdownSpans() {
  const observer = new MutationObserver(() => {
    document.querySelectorAll('.el-select-dropdown__item span, .el-dropdown-menu__item span').forEach(span => {
      span.style.transform = 'none'
      span.style.position = 'static'
      span.style.top = 'auto'
      span.style.left = 'auto'
    })
  })
  observer.observe(document.body, { childList: true, subtree: true })
}

const app = createApp(App)
const pinia = createPinia()
const themeStore = useThemeStore(pinia)

themeStore.initTheme()

// 注册所有图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(pinia)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
  message: {
    duration: 2000
  }
})

router.isReady().then(() => {
  app.mount('#app')
  fixDropdownSpans()
})
