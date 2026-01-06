<template>
  <div class="exchanges-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>用户积分兑换查询</h3>
        </div>
      </template>
      
      <!-- 搜索表单 -->
      <el-form :model="searchForm" :inline="true" class="search-form">
        <el-form-item label="用户ID">
          <el-input
            v-model.number="searchForm.userId"
            placeholder="请输入用户ID"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="产品ID">
          <el-input
            v-model.number="searchForm.productId"
            placeholder="请输入产品ID"
            clearable
            style="width: 200px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select
            v-model="searchForm.status"
            placeholder="请选择状态"
            clearable
            style="width: 200px"
          >
            <el-option label="全部" value="" />
            <el-option label="待处理" value="pending" />
            <el-option label="已完成" value="completed" />
            <el-option label="已取消" value="cancelled" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch" :loading="loading">
            <el-icon><Search /></el-icon>
            <span>查询</span>
          </el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="productName" label="产品名称" width="150" />
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column prop="points" label="消耗积分" width="120" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag
              :type="
                row.status === 'completed'
                  ? 'success'
                  : row.status === 'pending'
                  ? 'warning'
                  : 'info'
              "
            >
              {{
                row.status === 'completed'
                  ? '已完成'
                  : row.status === 'pending'
                  ? '待处理'
                  : '已取消'
              }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="couponCode" label="优惠券码" width="150" />
        <el-table-column prop="createdAt" label="创建时间" width="180">
          <template #default="{ row }">
            {{ formatDate(row.createdAt) }}
          </template>
        </el-table-column>
      </el-table>

      <div class="table-footer">
        <div class="total">共 {{ tableData.length }} 条记录</div>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import { getExchangeRecords, type ExchangeRecord } from '@/api/exchange'

const loading = ref(false)
const tableData = ref<ExchangeRecord[]>([])

const searchForm = reactive({
  userId: undefined as number | undefined,
  productId: undefined as number | undefined,
  status: ''
})

const handleSearch = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (searchForm.userId) params.userId = searchForm.userId
    if (searchForm.productId) params.productId = searchForm.productId
    if (searchForm.status) params.status = searchForm.status
    
    tableData.value = await getExchangeRecords(params)
    ElMessage.success('查询成功')
  } catch (error: any) {
    ElMessage.error(error.message || '查询失败')
  } finally {
    loading.value = false
  }
}

const handleReset = () => {
  searchForm.userId = undefined
  searchForm.productId = undefined
  searchForm.status = ''
  handleSearch()
}

const formatDate = (date: string) => {
  if (!date) return '-'
  return new Date(date).toLocaleString('zh-CN')
}

onMounted(() => {
  handleSearch()
})
</script>

<style scoped>
.exchanges-page {
  width: 100%;
}

.card-header h3 {
  margin: 0;
}

.search-form {
  margin-bottom: 20px;
}

.table-footer {
  margin-top: 20px;
  text-align: right;
}

.total {
  color: #666;
  font-size: 14px;
}
</style>

