import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import naive from 'naive-ui'
import App from './App.vue'
import router from './router'
import './styles/global.css'

// 修复下拉框悬停文字下沉/滚动问题
function fixDropdownHover() {
  const observer = new MutationObserver(() => {
    // 修复下拉选项 span 的样式
    document.querySelectorAll('.el-select-dropdown__item span, .el-dropdown-menu__item span').forEach(span => {
      span.style.transform = 'none'
      span.style.position = 'static'
      span.style.top = 'auto'
      span.style.left = 'auto'
    })
    
    // 移除 el-select input 的 title 属性（防止浏览器默认 tooltip）
    document.querySelectorAll('.el-select .el-input__inner, .el-select__wrapper, .el-select__selection').forEach(el => {
      if (el.getAttribute('title')) {
        el.removeAttribute('title')
      }
    })
    
    // 移除 el-dropdown 触发器的 title 属性
    document.querySelectorAll('.el-dropdown [title], .el-dropdown .el-tooltip__trigger').forEach(el => {
      if (el.getAttribute('title')) {
        el.removeAttribute('title')
      }
    })
  })
  observer.observe(document.body, { childList: true, subtree: true, attributes: true })
}

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(naive)
app.use(router)
app.use(ElementPlus, {
  message: {
    duration: 2000
  }
})

router.isReady().then(() => {
  app.mount('#app')
  fixDropdownHover()
})
