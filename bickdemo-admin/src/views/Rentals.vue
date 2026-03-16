<template>
  <div class="page-stack">
    <section class="page-hero">
      <div class="hero-head">
        <div class="hero-copy">
          <span class="hero-tag">Orders</span>
          <h2>租赁订单</h2>
          <p>统一查看租赁状态、数量、金额和时间信息，便于快速掌握运营节奏。</p>
        </div>
      </div>
      <div class="hero-chips">
        <div class="hero-chip">
          <span>当前页订单</span>
          <strong>{{ records.length }}</strong>
        </div>
        <div class="hero-chip">
          <span>订单总量</span>
          <strong>{{ total }}</strong>
        </div>
      </div>
    </section>

    <el-card class="page-card" shadow="never">
      <div class="content-header">
        <div>
          <h3>订单列表</h3>
          <p>支持分页查看订单流转详情。</p>
        </div>
      </div>
      <el-table v-loading="loading" :data="records">
        <el-table-column prop="username" label="用户" min-width="120" />
        <el-table-column prop="bicycleName" label="车辆" min-width="150" />
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="rentalStatusType(row.status)" effect="light">{{ rentalStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="数量" width="80" align="center">
          <template #default="{ row }">{{ row.quantity || 1 }}</template>
        </el-table-column>
        <el-table-column label="总价" width="110" align="right">
          <template #default="{ row }">{{ money(row.totalPrice) }}</template>
        </el-table-column>
        <el-table-column label="开始时间" min-width="180">
          <template #default="{ row }">{{ formatDate(row.startTime || row.createdAt) }}</template>
        </el-table-column>
        <el-table-column label="结束时间" min-width="180">
          <template #default="{ row }">{{ formatDate(row.endTime) }}</template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          background
          layout="total, prev, pager, next"
          :total="total"
          @current-change="load"
        />
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { getAllRentals } from '@/api/rental'
import { formatDate, money, rentalStatusText, rentalStatusType } from '@/utils/format'

const loading = ref(false)
const records = ref([])
const total = ref(0)
const query = reactive({ page: 1, size: 10 })

const load = async () => {
  loading.value = true
  try {
    const res = await getAllRentals(query)
    records.value = res.data?.records || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>
