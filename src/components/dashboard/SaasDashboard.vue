<script setup>
import { ref, computed } from 'vue'

// State
const sidebarOpen = ref(false)
const searchQuery = ref('')
const activeNav = ref('dashboard')
const userMenuOpen = ref(false)

// Navigation items
const navItems = [
  { id: 'dashboard', label: 'Dashboard', icon: 'M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6' },
  { id: 'analytics', label: 'Analytics', icon: 'M9 19v-6a2 2 0 00-2-2H5a2 2 0 00-2 2v6a2 2 0 002 2h2a2 2 0 002-2zm0 0V9a2 2 0 012-2h2a2 2 0 012 2v10m-6 0a2 2 0 002 2h2a2 2 0 002-2m0 0V5a2 2 0 012-2h2a2 2 0 012 2v14a2 2 0 01-2 2h-2a2 2 0 01-2-2z' },
  { id: 'projects', label: 'Projects', icon: 'M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z' },
  { id: 'team', label: 'Team', icon: 'M12 4.354a4 4 0 110 5.292M15 21H3v-1a6 6 0 0112 0v1zm0 0h6v-1a6 6 0 00-9-5.197M13 7a4 4 0 11-8 0 4 4 0 018 0z' },
  { id: 'settings', label: 'Settings', icon: 'M10.325 4.317c.426-1.756 2.924-1.756 3.35 0a1.724 1.724 0 002.573 1.066c1.543-.94 3.31.826 2.37 2.37a1.724 1.724 0 001.065 2.572c1.756.426 1.756 2.924 0 3.35a1.724 1.724 0 00-1.066 2.573c.94 1.543-.826 3.31-2.37 2.37a1.724 1.724 0 00-2.572 1.065c-.426 1.756-2.924 1.756-3.35 0a1.724 1.724 0 00-2.573-1.066c-1.543.94-3.31-.826-2.37-2.37a1.724 1.724 0 00-1.065-2.572c-1.756-.426-1.756-2.924 0-3.35a1.724 1.724 0 001.066-2.573c-.94-1.543.826-3.31 2.37-2.37.996.608 2.296.07 2.572-1.065z' },
]

// Stats data
const stats = ref([
  { label: 'Total Revenue', value: '$45,231', change: '+20.1%', trend: 'up', icon: 'M12 8c-1.657 0-3 .895-3 2s1.343 2 3 2 3 .895 3 2-1.343 2-3 2m0-8c1.11 0 2.08.402 2.599 1M12 8V7m0 1v8m0 0v1m0-1c-1.11 0-2.08-.402-2.599-1M21 12a9 9 0 11-18 0 9 9 0 0118 0z' },
  { label: 'Active Users', value: '2,338', change: '+15.2%', trend: 'up', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z' },
  { label: 'Bounce Rate', value: '42.3%', change: '-4.3%', trend: 'down', icon: 'M13 7h8m0 0v8m0-8l-8 8-4-4-6 6' },
  { label: 'Avg. Session', value: '4m 32s', change: '+12.5%', trend: 'up', icon: 'M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z' },
])

// Recent projects
const projects = ref([
  { id: 1, name: 'Website Redesign', status: 'In Progress', progress: 75, members: 4, dueDate: '2026-03-15' },
  { id: 2, name: 'Mobile App Development', status: 'Review', progress: 90, members: 6, dueDate: '2026-03-20' },
  { id: 3, name: 'API Integration', status: 'Completed', progress: 100, members: 3, dueDate: '2026-03-01' },
  { id: 4, name: 'Database Migration', status: 'In Progress', progress: 45, members: 2, dueDate: '2026-04-01' },
  { id: 5, name: 'Security Audit', status: 'Pending', progress: 0, members: 5, dueDate: '2026-04-15' },
])

// Computed
const filteredProjects = computed(() => {
  if (!searchQuery.value) return projects.value
  return projects.value.filter(p =>
    p.name.toLowerCase().includes(searchQuery.value.toLowerCase())
  )
})

// Methods
const toggleSidebar = () => {
  sidebarOpen.value = !sidebarOpen.value
}

const toggleUserMenu = () => {
  userMenuOpen.value = !userMenuOpen.value
}

const handleNavClick = (itemId) => {
  activeNav.value = itemId
  sidebarOpen.value = false
}
</script>

<template>
  <div class="min-h-screen bg-gradient-to-br from-indigo-50 via-white to-purple-50">
    <!-- Skip Link for Accessibility -->
    <a href="#main-content" class="sr-only focus:not-sr-only focus:absolute focus:top-4 focus:left-4 focus:z-50 focus:px-4 focus:py-2 focus:bg-indigo-600 focus:text-white focus:rounded-lg">
      Skip to main content
    </a>

    <!-- Mobile Sidebar Overlay -->
    <div
      v-if="sidebarOpen"
      @click="toggleSidebar"
      class="fixed inset-0 bg-gray-900/50 backdrop-blur-sm z-40 lg:hidden"
      aria-hidden="true"
    ></div>

    <!-- Sidebar Navigation - Glassmorphism -->
    <aside
      :class="[
        'fixed top-0 left-0 h-full w-64 z-50 transition-transform duration-300 ease-in-out',
        'lg:translate-x-0',
        sidebarOpen ? 'translate-x-0' : '-translate-x-full',
        'bg-white/80 backdrop-blur-xl border-r border-white/20 shadow-2xl'
      ]"
      role="navigation"
      aria-label="Main navigation"
    >
      <!-- Logo -->
      <div class="flex items-center gap-3 px-6 py-5 border-b border-gray-200/50">
        <div class="w-10 h-10 rounded-xl bg-gradient-to-br from-indigo-500 to-purple-600 flex items-center justify-center shadow-lg">
          <svg class="w-6 h-6 text-white" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M13 10V3L4 14h7v7l9-11h-7z"/>
          </svg>
        </div>
        <span class="text-xl font-bold bg-gradient-to-r from-indigo-600 to-purple-600 bg-clip-text text-transparent">
          SaaSify
        </span>
      </div>

      <!-- Nav Items -->
      <nav class="mt-6 px-3 space-y-1">
        <button
          v-for="item in navItems"
          :key="item.id"
          @click="handleNavClick(item.id)"
          :class="[
            'w-full flex items-center gap-3 px-4 py-3 rounded-xl transition-all duration-200 cursor-pointer group',
            'focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2',
            activeNav === item.id
              ? 'bg-gradient-to-r from-indigo-500/10 to-purple-500/10 text-indigo-600 shadow-sm'
              : 'text-gray-600 hover:bg-white/60 hover:shadow-md'
          ]"
          :aria-current="activeNav === item.id ? 'page' : undefined"
        >
          <svg
            class="w-5 h-5 transition-colors"
            :class="activeNav === item.id ? 'text-indigo-500' : 'text-gray-400 group-hover:text-gray-600'"
            fill="none"
            stroke="currentColor"
            viewBox="0 0 24 24"
          >
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="item.icon"/>
          </svg>
          <span class="font-medium">{{ item.label }}</span>
          <span
            v-if="activeNav === item.id"
            class="ml-auto w-1.5 h-1.5 rounded-full bg-indigo-500"
            aria-hidden="true"
          ></span>
        </button>
      </nav>

      <!-- Upgrade Card -->
      <div class="absolute bottom-6 left-3 right-3">
        <div class="p-4 rounded-2xl bg-gradient-to-br from-indigo-500/10 to-purple-500/10 border border-indigo-200/50 backdrop-blur-sm">
          <h4 class="font-semibold text-gray-800 mb-1">Upgrade to Pro</h4>
          <p class="text-sm text-gray-500 mb-3">Get access to all features</p>
          <button class="w-full py-2 px-4 bg-gradient-to-r from-indigo-500 to-purple-600 text-white rounded-lg font-medium cursor-pointer hover:shadow-lg hover:shadow-indigo-500/30 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2">
            Upgrade Now
          </button>
        </div>
      </div>
    </aside>

    <!-- Main Content Area -->
    <div class="lg:ml-64">
      <!-- Top Navbar -->
      <header class="sticky top-0 z-30 bg-white/70 backdrop-blur-xl border-b border-gray-200/50">
        <div class="flex items-center justify-between px-4 py-3 sm:px-6">
          <!-- Left: Menu + Search -->
          <div class="flex items-center gap-3 sm:gap-4">
            <!-- Mobile Menu Button -->
            <button
              @click="toggleSidebar"
              class="lg:hidden p-2 rounded-lg text-gray-500 hover:bg-white/60 cursor-pointer transition-colors duration-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              aria-label="Toggle navigation menu"
            >
              <svg class="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M4 6h16M4 12h16M4 18h16"/>
              </svg>
            </button>

            <!-- Search Input -->
            <div class="relative">
              <svg class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"/>
              </svg>
              <input
                v-model="searchQuery"
                type="text"
                placeholder="Search projects..."
                class="w-48 sm:w-64 pl-10 pr-4 py-2.5 bg-white/60 border border-gray-200 rounded-xl text-sm placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:border-transparent transition-all duration-200"
              />
            </div>
          </div>

          <!-- Right: Actions -->
          <div class="flex items-center gap-2 sm:gap-3">
            <!-- Notification Button -->
            <button
              class="relative p-2.5 rounded-xl text-gray-500 hover:bg-white/60 cursor-pointer transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
              aria-label="View notifications"
            >
              <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 17h5l-1.405-1.405A2.032 2.032 0 0118 14.158V11a6.002 6.002 0 00-4-5.659V5a2 2 0 10-4 0v.341C7.67 6.165 6 8.388 6 11v3.159c0 .538-.214 1.055-.595 1.436L4 17h5m6 0v1a3 3 0 11-6 0v-1m6 0H9"/>
              </svg>
              <span class="absolute top-2 right-2.5 w-2 h-2 bg-red-500 rounded-full border-2 border-white"></span>
            </button>

            <!-- User Menu -->
            <div class="relative">
              <button
                @click="toggleUserMenu"
                class="flex items-center gap-2 p-1.5 pr-3 rounded-xl bg-white/60 border border-gray-200 hover:border-indigo-300 cursor-pointer transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-indigo-500"
                aria-label="Open user menu"
                aria-expanded="userMenuOpen"
              >
                <img
                  src="https://ui-avatars.com/api/?name=John+Doe&background=6366F1&color=fff&size=32"
                  alt="User avatar"
                  class="w-8 h-8 rounded-lg"
                />
                <span class="hidden sm:block text-sm font-medium text-gray-700">John Doe</span>
                <svg class="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"/>
                </svg>
              </button>

              <!-- Dropdown Menu -->
              <div
                v-if="userMenuOpen"
                class="absolute right-0 mt-2 w-48 py-2 bg-white/90 backdrop-blur-xl rounded-xl shadow-2xl border border-gray-200/50"
                role="menu"
              >
                <a href="#" class="block px-4 py-2 text-sm text-gray-600 hover:bg-indigo-50 cursor-pointer transition-colors" role="menuitem">Profile</a>
                <a href="#" class="block px-4 py-2 text-sm text-gray-600 hover:bg-indigo-50 cursor-pointer transition-colors" role="menuitem">Settings</a>
                <hr class="my-2 border-gray-200/50"/>
                <a href="#" class="block px-4 py-2 text-sm text-red-600 hover:bg-red-50 cursor-pointer transition-colors" role="menuitem">Sign out</a>
              </div>
            </div>
          </div>
        </div>
      </header>

      <!-- Main Dashboard Content -->
      <main id="main-content" class="p-4 sm:p-6 space-y-6">
        <!-- Page Header -->
        <div class="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 class="text-2xl sm:text-3xl font-bold text-gray-800">Dashboard Overview</h1>
            <p class="text-gray-500 mt-1">Welcome back! Here's what's happening today.</p>
          </div>
          <button class="inline-flex items-center justify-center gap-2 px-5 py-2.5 bg-gradient-to-r from-indigo-500 to-purple-600 text-white rounded-xl font-medium cursor-pointer hover:shadow-xl hover:shadow-indigo-500/30 transition-all duration-200 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:ring-offset-2 self-start sm:self-auto">
            <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/>
            </svg>
            New Project
          </button>
        </div>

        <!-- Stats Grid -->
        <div class="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div
            v-for="(stat, index) in stats"
            :key="index"
            class="group p-5 bg-white/70 backdrop-blur-xl rounded-2xl border border-gray-200/50 shadow-lg hover:shadow-xl hover:scale-[1.02] transition-all duration-200 cursor-pointer focus-within:ring-2 focus-within:ring-indigo-500"
            tabindex="0"
            role="button"
            :aria-label="`${stat.label}: ${stat.value}, ${stat.change}`"
          >
            <div class="flex items-start justify-between">
              <div>
                <p class="text-sm font-medium text-gray-500">{{ stat.label }}</p>
                <p class="text-2xl font-bold text-gray-800 mt-1">{{ stat.value }}</p>
                <div class="flex items-center gap-1 mt-2">
                  <svg
                    :class="stat.trend === 'up' ? 'text-emerald-500' : 'text-red-500'"
                    class="w-4 h-4"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                  >
                    <path
                      v-if="stat.trend === 'up'"
                      stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M13 7h8m0 0v8m0-8l-8 8-4-4-6 6"
                    />
                    <path
                      v-else
                      stroke-linecap="round" stroke-linejoin="round" stroke-width="2"
                      d="M13 17h8m0 0V9m0 8l-8-8-4 4-6-6"
                    />
                  </svg>
                  <span
                    :class="stat.trend === 'up' ? 'text-emerald-600' : 'text-red-600'"
                    class="text-sm font-medium"
                  >
                    {{ stat.change }}
                  </span>
                  <span class="text-xs text-gray-400">vs last month</span>
                </div>
              </div>
              <div class="p-3 rounded-xl bg-gradient-to-br from-indigo-500/10 to-purple-500/10 group-hover:from-indigo-500/20 group-hover:to-purple-500/20 transition-all duration-200">
                <svg class="w-6 h-6 text-indigo-500" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" :d="stat.icon"/>
                </svg>
              </div>
            </div>
          </div>
        </div>

        <!-- Content Grid: Charts + Projects -->
        <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <!-- Revenue Chart Placeholder -->
          <div class="lg:col-span-2 p-6 bg-white/70 backdrop-blur-xl rounded-2xl border border-gray-200/50 shadow-lg">
            <div class="flex items-center justify-between mb-4">
              <h2 class="text-lg font-semibold text-gray-800">Revenue Overview</h2>
              <select class="px-3 py-1.5 text-sm bg-white/60 border border-gray-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500">
                <option>Last 7 days</option>
                <option>Last 30 days</option>
                <option>Last 90 days</option>
              </select>
            </div>
            <!-- Chart Placeholder - Replace with actual chart component -->
            <div class="h-64 flex items-end justify-between gap-2 px-4">
              <div class="w-full bg-gradient-to-t from-indigo-500/30 to-indigo-500/60 rounded-t-lg" style="height: 40%;"></div>
              <div class="w-full bg-gradient-to-t from-indigo-500/30 to-indigo-500/60 rounded-t-lg" style="height: 65%;"></div>
              <div class="w-full bg-gradient-to-t from-indigo-500/30 to-indigo-500/60 rounded-t-lg" style="height: 50%;"></div>
              <div class="w-full bg-gradient-to-t from-indigo-500/30 to-indigo-500/60 rounded-t-lg" style="height: 80%;"></div>
              <div class="w-full bg-gradient-to-t from-indigo-500/30 to-indigo-500/60 rounded-t-lg" style="height: 60%;"></div>
              <div class="w-full bg-gradient-to-t from-indigo-500/30 to-indigo-500/60 rounded-t-lg" style="height: 90%;"></div>
              <div class="w-full bg-gradient-to-t from-indigo-500/30 to-indigo-500/60 rounded-t-lg" style="height: 75%;"></div>
            </div>
            <div class="flex justify-between mt-3 text-xs text-gray-400 px-4">
              <span>Mon</span>
              <span>Tue</span>
              <span>Wed</span>
              <span>Thu</span>
              <span>Fri</span>
              <span>Sat</span>
              <span>Sun</span>
            </div>
          </div>

          <!-- Traffic Sources -->
          <div class="p-6 bg-white/70 backdrop-blur-xl rounded-2xl border border-gray-200/50 shadow-lg">
            <h2 class="text-lg font-semibold text-gray-800 mb-4">Traffic Sources</h2>
            <!-- Donut Chart Placeholder -->
            <div class="relative w-40 h-40 mx-auto mb-4">
              <svg viewBox="0 0 36 36" class="w-full h-full transform -rotate-90">
                <circle cx="18" cy="18" r="16" fill="none" stroke="#E5E7EB" stroke-width="4"></circle>
                <circle cx="18" cy="18" r="16" fill="none" stroke="#6366F1" stroke-width="4" stroke-dasharray="60 100"></circle>
                <circle cx="18" cy="18" r="16" fill="none" stroke="#8B5CF6" stroke-width="4" stroke-dasharray="25 100" stroke-dashoffset="-60"></circle>
                <circle cx="18" cy="18" r="16" fill="none" stroke="#10B981" stroke-width="4" stroke-dasharray="15 100" stroke-dashoffset="-85"></circle>
              </svg>
              <div class="absolute inset-0 flex items-center justify-center">
                <div class="text-center">
                  <span class="text-2xl font-bold text-gray-800">12.5K</span>
                  <p class="text-xs text-gray-400">visitors</p>
                </div>
              </div>
            </div>
            <!-- Legend -->
            <div class="space-y-2">
              <div class="flex items-center justify-between text-sm">
                <div class="flex items-center gap-2">
                  <span class="w-3 h-3 rounded-full bg-indigo-500"></span>
                  <span class="text-gray-600">Direct</span>
                </div>
                <span class="font-medium text-gray-800">60%</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <div class="flex items-center gap-2">
                  <span class="w-3 h-3 rounded-full bg-purple-500"></span>
                  <span class="text-gray-600">Social</span>
                </div>
                <span class="font-medium text-gray-800">25%</span>
              </div>
              <div class="flex items-center justify-between text-sm">
                <div class="flex items-center gap-2">
                  <span class="w-3 h-3 rounded-full bg-emerald-500"></span>
                  <span class="text-gray-600">Organic</span>
                </div>
                <span class="font-medium text-gray-800">15%</span>
              </div>
            </div>
          </div>
        </div>

        <!-- Projects Table -->
        <div class="bg-white/70 backdrop-blur-xl rounded-2xl border border-gray-200/50 shadow-lg overflow-hidden">
          <div class="flex items-center justify-between p-6 border-b border-gray-200/50">
            <h2 class="text-lg font-semibold text-gray-800">Recent Projects</h2>
            <button class="text-sm text-indigo-600 hover:text-indigo-700 font-medium cursor-pointer focus:outline-none focus:underline">
              View All
            </button>
          </div>
          <div class="overflow-x-auto">
            <table class="w-full">
              <thead class="bg-gray-50/50">
                <tr>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Project</th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Status</th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Progress</th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Team</th>
                  <th scope="col" class="px-6 py-3 text-left text-xs font-medium text-gray-500 uppercase tracking-wider">Due Date</th>
                </tr>
              </thead>
              <tbody class="divide-y divide-gray-200/50">
                <tr
                  v-for="project in filteredProjects"
                  :key="project.id"
                  class="hover:bg-gray-50/50 transition-colors cursor-pointer"
                >
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span class="text-sm font-medium text-gray-800">{{ project.name }}</span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <span
                      :class="[
                        'inline-flex px-3 py-1 text-xs font-medium rounded-full',
                        project.status === 'Completed' ? 'bg-emerald-100 text-emerald-700' :
                        project.status === 'In Progress' ? 'bg-indigo-100 text-indigo-700' :
                        project.status === 'Review' ? 'bg-amber-100 text-amber-700' :
                        'bg-gray-100 text-gray-700'
                      ]"
                    >
                      {{ project.status }}
                    </span>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex items-center gap-3">
                      <div class="w-24 h-2 bg-gray-200 rounded-full overflow-hidden">
                        <div
                          :class="[
                            'h-full rounded-full transition-all duration-300',
                            project.progress === 100 ? 'bg-emerald-500' : 'bg-indigo-500'
                          ]"
                          :style="{ width: project.progress + '%' }"
                        ></div>
                      </div>
                      <span class="text-sm text-gray-600">{{ project.progress }}%</span>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap">
                    <div class="flex -space-x-2">
                      <img
                        v-for="n in Math.min(project.members, 3)"
                        :key="n"
                        :src="`https://ui-avatars.com/api/?name=Member+${n}&background=random&size=32`"
                        :alt="`Team member ${n}`"
                        class="w-8 h-8 rounded-full border-2 border-white"
                      />
                      <div
                        v-if="project.members > 3"
                        class="w-8 h-8 rounded-full bg-gray-200 border-2 border-white flex items-center justify-center text-xs font-medium text-gray-600"
                      >
                        +{{ project.members - 3 }}
                      </div>
                    </div>
                  </td>
                  <td class="px-6 py-4 whitespace-nowrap text-sm text-gray-600">
                    {{ new Date(project.dueDate).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <!-- Empty State -->
          <div
            v-if="filteredProjects.length === 0"
            class="text-center py-12"
          >
            <svg class="mx-auto h-12 w-12 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M20 13V6a2 2 0 00-2-2H6a2 2 0 00-2 2v7m16 0v5a2 2 0 01-2 2H6a2 2 0 01-2-2v-5m16 0h-2.586a1 1 0 00-.707.293l-2.414 2.414a1 1 0 01-.707.293h-3.172a1 1 0 01-.707-.293l-2.414-2.414A1 1 0 006.586 13H4"/>
            </svg>
            <p class="mt-4 text-gray-500">No projects match your search</p>
          </div>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
/* Prefers reduced motion */
@media (prefers-reduced-motion: reduce) {
  * {
    animation-duration: 0.01ms !important;
    animation-iteration-count: 1 !important;
    transition-duration: 0.01ms !important;
  }
}
</style>
