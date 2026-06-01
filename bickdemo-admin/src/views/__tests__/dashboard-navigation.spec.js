import { mount, flushPromises } from '@vue/test-utils'
import { defineComponent, nextTick } from 'vue'
import { createMemoryHistory, createRouter } from 'vue-router'
import { describe, expect, it, vi } from 'vitest'
import Dashboard from '@/views/Dashboard.vue'

const destroySpy = vi.fn()
const reflowSpy = vi.fn()

vi.mock('highcharts', () => ({
  default: {
    chart: vi.fn(() => ({
      destroy: destroySpy,
      reflow: reflowSpy
    }))
  }
}))

vi.mock('@/api/forum', () => ({
  getPendingForumPosts: vi.fn().mockResolvedValue({ data: [] })
}))

vi.mock('@/api/rental', () => ({
  getAllRentals: vi.fn().mockResolvedValue({ data: { records: [] } }),
  getStatistics: vi.fn().mockResolvedValue({ data: {} })
}))

vi.mock('@/api/system', () => ({
  getSystemOverview: vi.fn().mockResolvedValue({ data: {} })
}))

const HostView = defineComponent({
  template: '<router-view />'
})

const TargetView = defineComponent({
  template: '<div>Target View</div>'
})

const globalStubs = {
  'el-button': { template: '<button><slot /></button>' },
  'el-card': { template: '<section><slot name="header" /><slot /></section>' },
  'el-icon': { template: '<span />' },
  'el-table': { template: '<div />' },
  'el-table-column': { template: '<div />' },
  'el-tag': { template: '<span><slot /></span>' }
}

async function mountWithRouter() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [
      { path: '/dashboard', component: Dashboard },
      { path: '/target', component: TargetView }
    ]
  })

  router.push('/dashboard')
  await router.isReady()

  const wrapper = mount(HostView, {
    global: {
      plugins: [router],
      stubs: globalStubs
    }
  })

  await flushPromises()
  await nextTick()

  return { router, wrapper }
}

describe('Dashboard navigation cleanup', () => {
  it('switches away from dashboard without blocking the next route view', async () => {
    destroySpy.mockClear()
    reflowSpy.mockClear()

    const { router, wrapper } = await mountWithRouter()

    expect(wrapper.text()).toContain('控制台')

    await router.push('/target')
    await flushPromises()
    await nextTick()

    expect(destroySpy).toHaveBeenCalledTimes(1)
    expect(wrapper.text()).toContain('Target View')
  })
})
