<template>
  <div class="history">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>分析历史</span>
          <span class="total">共 {{ total }} 条记录</span>
        </div>
      </template>

      <el-table :data="records" style="width: 100%" v-loading="loading">
        <el-table-column prop="fileName" label="文件名" min-width="200" />
        <el-table-column prop="fileSize" label="大小" width="100">
          <template #default="{ row }">
            {{ formatSize(row.fileSize) }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="分析时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row.id)">查看</el-button>
            <el-button type="danger" link @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination" v-if="total > 0">
        <el-pagination
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          @current-change="loadRecords"
          layout="prev, pager, next"
        />
      </div>

      <el-empty v-if="!loading && records.length === 0" description="暂无分析记录" />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '@/api/client'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const loading = ref(false)
const records = ref([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = 20

const loadRecords = async (page = 1) => {
  loading.value = true
  try {
    const res = await api.get('/records/list', {
      headers: { Authorization: Bearer  },
      params: { page: page - 1, size: pageSize }
    })
    if (res.data.success) {
      records.value = res.data.data
      total.value = res.data.total
    }
  } catch (error: any) {
    ElMessage.error(error.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const viewDetail = (id: number) => {
  router.push(/history/)
}

const handleDelete = async (id: number) => {
  await ElMessageBox.confirm('确定删除该记录？', '提示', { type: 'warning' })
  try {
    await api.delete(/records/, {
      headers: { Authorization: Bearer  }
    })
    ElMessage.success('删除成功')
    loadRecords(currentPage.value)
  } catch (error: any) {
    ElMessage.error(error.message || '删除失败')
  }
}

const formatSize = (bytes: number) => {
  if (!bytes) return '-'
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1048576) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / 1048576).toFixed(2) + ' MB'
}

const formatDate = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  loadRecords()
})
</script>

<style scoped>
.history { max-width: 1000px; }
.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.total { color: #909399; font-size: 13px; }
.pagination { margin-top: 20px; display: flex; justify-content: center; }
</style>