import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { trackSiteVisit } from '@/api/analytics'

const routes = [
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue')
      },
      {
        path: '/bicycles',
        name: 'Bicycles',
        component: () => import('@/views/BicycleList.vue')
      },
      {
        path: '/marketplace',
        name: 'Marketplace',
        component: () => import('@/views/Marketplace.vue')
      },
      {
        path: '/my-rentals',
        name: 'MyRentals',
        component: () => import('@/views/MyRentals.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/friends',
        name: 'Friends',
        component: () => import('@/views/FriendsChat.vue'),
        meta: { requiresAuth: true, fullScreen: true }
      },
      {
        path: '/statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue')
      },
      {
        path: '/points',
        name: 'Points',
        component: () => import('@/views/Points.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/forum',
        name: 'Forum',
        component: () => import('@/views/Forum.vue')
      },
      {
        path: '/profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/activities',
        name: 'Activities',
        component: () => import('@/views/Activities.vue')
      },
      {
        path: '/activities/:id',
        name: 'ActivityDetail',
        component: () => import('@/views/ActivityDetail.vue')
      },
      {
        path: '/notices',
        name: 'Notices',
        component: () => import('@/views/Notices.vue')
      },
      {
        path: '/notifications',
        name: 'NotificationCenter',
        component: () => import('@/views/NotificationCenter.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/tickets',
        name: 'Tickets',
        component: () => import('@/views/Tickets.vue'),
        meta: { requiresAuth: true }
      },
      {
        path: '/tickets/create',
        name: 'TicketCreate',
        component: () => import('@/views/TicketCreate.vue'),
        meta: { requiresAuth: true }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue')
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue')
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
  const userStore = useUserStore()

  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin && userStore.role !== 'ADMIN') {
    next('/bicycles')
  } else {
    next()
  }
})

router.afterEach((to) => {
  void trackSiteVisit(to)
})

export default router
