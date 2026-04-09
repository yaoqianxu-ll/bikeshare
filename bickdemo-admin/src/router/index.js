import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { trackSiteVisit } from '@/api/analytics'

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
        path: '/marketplace',
        name: 'MarketplaceManage',
        component: () => import('@/views/MarketplaceModeration.vue')
      },
      {
        path: '/rentals',
        name: 'RentalsManage',
        component: () => import('@/views/Rentals.vue')
      },
      {
        path: '/tickets',
        name: 'TicketsManage',
        component: () => import('@/views/Tickets.vue')
      },
      {
        path: '/forum',
        name: 'ForumManage',
        component: () => import('@/views/ForumModeration.vue')
      },
      {
        path: '/forum/comments',
        name: 'ForumCommentManage',
        component: () => import('@/views/CommentModeration.vue')
      },
      {
        path: '/backgrounds',
        name: 'BackgroundsManage',
        component: () => import('@/views/Backgrounds.vue')
      },
      {
        path: '/activities',
        name: 'ActivitiesManage',
        component: () => import('@/views/Activities.vue')
      },
      {
        path: '/notices',
        name: 'NoticesManage',
        component: () => import('@/views/Notices.vue')
      },
      {
        path: '/system',
        redirect: '/system/users'
      },
      {
        path: '/system/users',
        name: 'SystemUsers',
        component: () => import('@/views/Users.vue')
      },
      {
        path: '/system/blacklist',
        name: 'SystemBlacklist',
        component: () => import('@/views/Blacklist.vue')
      },
      {
        path: '/system/login-logs',
        name: 'SystemLoginLogs',
        component: () => import('@/views/LoginLogs.vue')
      },
      {
        path: '/system/visitor-logs',
        name: 'SystemVisitorLogs',
        component: () => import('@/views/VisitorLogs.vue')
      },
      {
        path: '/system/operation-logs',
        name: 'SystemOperationLogs',
        component: () => import('@/views/OperationLogs.vue')
      },
      {
        path: '/points',
        name: 'PointsManage',
        component: () => import('@/views/PointsManagement.vue')
      },
      {
        path: '/vip',
        name: 'VipManage',
        component: () => import('@/views/VipManagement.vue')
      },
      {
        path: '/benefits',
        name: 'BenefitsManage',
        component: () => import('@/views/BenefitsManagement.vue')
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    component: () => import('@/views/NotFound.vue')
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

router.afterEach((to) => {
  void trackSiteVisit(to)
})

export default router
