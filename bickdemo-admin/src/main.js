import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import naive from 'naive-ui'
import App from './App.vue'
import router from './router'
import './styles/global.css'

// 修复下拉框悬停文字下沉/滚动问题 - 暂时禁用，因为会导致抖动
function fixDropdownHover() {
  // 已禁用
}

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(naive)
app.use(router)
app.use(ElementPlus, {
  locale: zhCn,
  message: {
    duration: 2000
  }
})

router.isReady().then(() => {
  app.mount('#app')
  fixDropdownHover()
})
