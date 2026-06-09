import { createApp } from 'vue'
import { createPinia } from 'pinia'
import 'element-plus/theme-chalk/dark/css-vars.css'

// 手动导入 ElMessage / ElMessageBox 等服务组件的样式
// unplugin-vue-components 只能自动导入模板组件的 CSS，无法覆盖 JS 调用的服务组件
import 'element-plus/theme-chalk/el-message.css'
import 'element-plus/theme-chalk/el-message-box.css'
import 'element-plus/theme-chalk/el-overlay.css'

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
