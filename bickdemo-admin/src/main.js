import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import App from './App.vue'
import router from './router'
import './styles/global.css'
import { trackSiteVisit } from './api/analytics'

const app = createApp(App)
const pinia = createPinia()

app.use(pinia)
app.use(router)
app.use(ElementPlus)

router.isReady().then(() => {
  void trackSiteVisit(router.currentRoute.value)
  app.mount('#app')
})
