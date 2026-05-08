<template>
  <div class="dashboard">
    <h2 class="page-title">数据概览</h2>
    <el-row :gutter="20">
      <el-col :span="6" v-for="stat in stats" :key="stat.label">
        <el-card shadow="hover">
          <div class="stat-card">
            <div class="stat-icon" :style="{ background: stat.color }">
              <el-icon :size="28"><component :is="stat.icon" /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ stat.value }}</div>
              <div class="stat-label">{{ stat.label }}</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card header="Token 用量趋势">
          <div class="chart-placeholder">
            <el-empty v-if="!usageData.length" description="暂无数据" :image-size="80" />
            <el-table v-else :data="usageData" size="small">
              <el-table-column prop="modelName" label="模型" />
              <el-table-column prop="totalTokens" label="Token 用量" />
              <el-table-column prop="callCount" label="调用次数" />
            </el-table>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="系统状态">
          <el-descriptions :column="1" border>
            <el-descriptions-item label="API 状态">
              <el-tag :type="healthStatus === 'UP' ? 'success' : 'danger'">{{ healthStatus }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="租户数量">{{ tenantCount }}</el-descriptions-item>
            <el-descriptions-item label="知识文档">{{ knowledgeCount }}</el-descriptions-item>
            <el-descriptions-item label="今日会话">{{ todayConversations }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, markRaw } from 'vue'
import { User, Coin, ChatLineRound, Document } from '@element-plus/icons-vue'
import api from '@/api'

const healthStatus = ref('...')
const tenantCount = ref(0)
const knowledgeCount = ref(0)
const todayConversations = ref(0)
const usageData = ref<any[]>([])

const stats = ref([
  { label: '租户总数', value: '...', icon: markRaw(User), color: '#409eff' },
  { label: 'Token 用量(月)', value: '...', icon: markRaw(Coin), color: '#67c23a' },
  { label: '今日会话', value: '...', icon: markRaw(ChatLineRound), color: '#e6a23c' },
  { label: '知识文档', value: '...', icon: markRaw(Document), color: '#f56c6c' },
])

onMounted(async () => {
  try {
    const h = await api.get('/health')
    healthStatus.value = h.data?.status || 'UP'
  } catch { healthStatus.value = 'DOWN' }

  try {
    const t = await api.get('/tenants', { params: { page: 1, size: 1 } })
    tenantCount.value = t.data?.total || 0
    stats.value[0].value = String(tenantCount.value)
  } catch { /* empty */ }

  try {
    const k = await api.get('/knowledge/docs', { params: { page: 1, size: 1 } })
    knowledgeCount.value = k.data?.total || 0
    stats.value[3].value = String(knowledgeCount.value)
  } catch { /* empty */ }

  try {
    const c = await api.get('/conversations', { params: { page: 1, size: 1 } })
    todayConversations.value = c.data?.total || 0
    stats.value[2].value = String(todayConversations.value)
  } catch { /* empty */ }

  // Try to load billing summary
  stats.value[1].value = '—'
})
</script>

<style scoped>
.page-title { font-size: 22px; margin-bottom: 20px; color: #303133; }
.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
}
.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}
.stat-value { font-size: 24px; font-weight: 600; color: #303133; }
.stat-label { font-size: 13px; color: #909399; margin-top: 4px; }
.chart-placeholder { min-height: 200px; }
</style>
