import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/theme-chalk/dark/css-vars.css'
import './styles/global.css'

import App from './App.vue'
import router from './router'
import { useThemeStore } from './stores/theme'

// 修复下拉框悬停文字飘移问题
function fixDropdownHover() {
  // 已在 element.css 中处理
}

const app = createApp(App)
const pinia = createPinia()
const themeStore = useThemeStore(pinia)

themeStore.initTheme()

app.use(pinia)
app.use(router)

router.isReady().then(() => {
  app.mount('#app')
  fixDropdownHover()
})
