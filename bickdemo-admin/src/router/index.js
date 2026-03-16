import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'AdminLogin',
    component: () => import('@/views/Login.vue'),
    meta: { guestOnly: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/AdminLayout.vue'),
    meta: { requiresAuth: true, requiresAdmin: true },
    children: [
      {
        path: '',
        redirect: '/dashboard'
      },
      {
        path: '/dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue')
      },
      {
        path: '/bicycles',
        name: 'BicyclesManage',
        component: () => import('@/views/Bicycles.vue')
      },
      {
        path: '/rentals',
        name: 'RentalsManage',
        component: () => import('@/views/Rentals.vue')
      },
      {
        path: '/forum',
        name: 'ForumManage',
        component: () => import('@/views/ForumModeration.vue')
      },
      {
        path: '/backgrounds',
        name: 'BackgroundsManage',
        component: () => import('@/views/Backgrounds.vue')
      },
      {
        path: '/system',
        redirect: '/system/login-logs'
      },
      {
        path: '/system/login-logs',
        name: 'SystemLoginLogs',
        component: () => import('@/views/SystemManage.vue')
      },
      {
        path: '/system/operation-logs',
        name: 'SystemOperationLogs',
        component: () => import('@/views/SystemManage.vue')
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore()

  if (to.meta.requiresAuth && !authStore.isLoggedIn) {
    next('/login')
    return
  }

  if (to.meta.requiresAdmin && authStore.role !== 'ADMIN') {
    authStore.logout()
    next('/login')
    return
  }

  if (to.meta.guestOnly && authStore.isLoggedIn) {
    next('/dashboard')
    return
  }

  next()
})

export default router
