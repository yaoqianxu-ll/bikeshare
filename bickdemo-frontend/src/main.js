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

// 修复下拉框悬停文字下沉/滚动问题
function fixDropdownHover() {
  // 监听 DOM 变化，移除 el-select 和 el-dropdown 的 title 属性
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
  
  // 同时添加 CSS 样式覆盖
  const style = document.createElement('style')
  style.textContent = `
    /* 禁用 el-select 和 el-dropdown 的 title tooltip */
    .el-select .el-input__inner,
    .el-select__wrapper,
    .el-select__selection,
    .el-dropdown [title] {
      title: none !important;
    }
    
    /* 防止下拉选项文字出现滚动动画 */
    .el-select-dropdown__item,
    .el-select-dropdown__item span,
    .el-dropdown-menu__item,
    .el-dropdown-menu__item span {
      transform: none !important;
      transition: none !important;
      animation: none !important;
    }
  `
  document.head.appendChild(style)
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
  fixDropdownHover()
})
