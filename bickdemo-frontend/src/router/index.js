import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      {
        path: '',
        redirect: '/bicycles'
      },
      {
        path: '/bicycles',
        name: 'Bicycles',
        component: () => import('@/views/BicycleList.vue')
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
        meta: { requiresAuth: true }
      },
      {
        path: '/admin',
        name: 'Admin',
        component: () => import('@/views/Admin.vue'),
        meta: { requiresAuth: true, requiresAdmin: true }
      },
      {
        path: '/statistics',
        name: 'Statistics',
        component: () => import('@/views/Statistics.vue')
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

export default router
