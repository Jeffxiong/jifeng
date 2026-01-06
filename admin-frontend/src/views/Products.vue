<template>
  <div class="products-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <h3>产品管理</h3>
          <div>
            <el-button type="success" @click="handleCreate" style="margin-right: 10px">
              <el-icon><Plus /></el-icon>
              <span>新增产品</span>
            </el-button>
            <el-button type="primary" @click="handleRefresh">
              <el-icon><Refresh /></el-icon>
              <span>刷新</span>
            </el-button>
          </div>
        </div>
      </template>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        border
        style="width: 100%"
      >
        <el-table-column prop="name" label="产品名称" width="200" />
        <el-table-column prop="points" label="所需积分" width="120" />
        <el-table-column prop="description" label="描述" min-width="200" show-overflow-tooltip />
        <el-table-column prop="stock" label="库存" width="120">
          <template #default="{ row }">
            <el-input-number
              v-model="row.stock"
              :min="0"
              :max="9999"
              size="small"
              @change="handleStockChange(row)"
              style="width: 100px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="monthlyLimit" label="月度限制" width="120" />
        <el-table-column prop="status" label="状态" width="120">
          <template #default="{ row }">
            <el-switch
              v-model="row.status"
              :active-value="1"
              :inactive-value="0"
              active-text="上架"
              inactive-text="下架"
              @change="handleStatusChange(row)"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              @click="handleEdit(row)"
            >
              编辑
            </el-button>
            <el-button
              type="danger"
              size="small"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog
      v-model="editDialogVisible"
      :title="isEditMode ? '编辑产品' : '新增产品'"
      width="600px"
      @close="handleDialogClose"
    >
      <el-form
        :model="editForm"
        :rules="editRules"
        ref="editFormRef"
        label-width="100px"
      >
        <el-form-item label="产品名称" prop="name">
          <el-input v-model="editForm.name" />
        </el-form-item>
        <el-form-item label="所需积分" prop="points">
          <el-input-number v-model="editForm.points" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="描述" prop="description">
          <el-input
            v-model="editForm.description"
            type="textarea"
            :rows="3"
          />
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="editForm.stock" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="月度限制" prop="monthlyLimit">
          <el-input-number v-model="editForm.monthlyLimit" :min="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="图片URL" prop="image">
          <el-input v-model="editForm.image" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="editForm.status">
            <el-radio :label="1">上架</el-radio>
            <el-radio :label="0">下架</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="isEditMode ? handleSave() : handleCreateSave()" :loading="saving">
          {{ isEditMode ? '保存' : '创建' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh, Plus } from '@element-plus/icons-vue'
import {
  getAllProducts,
  updateProduct,
  updateProductStock,
  updateProductStatus,
  createProduct,
  type Product
} from '@/api/product'

const loading = ref(false)
const saving = ref(false)
const tableData = ref<Product[]>([])
const editDialogVisible = ref(false)
const editFormRef = ref()
const isDataLoading = ref(false) // 标志：是否正在加载数据，用于防止刷新时触发状态变更
const isEditMode = ref(true) // 标志：是否为编辑模式，false表示新增模式

const editForm = reactive<Partial<Product>>({
  id: undefined,
  name: '',
  points: 0,
  description: '',
  stock: 0,
  monthlyLimit: 0,
  image: '',
  status: 1
})

const editRules = {
  name: [{ required: true, message: '请输入产品名称', trigger: 'blur' }],
  points: [{ required: true, message: '请输入所需积分', trigger: 'blur' }],
  description: [{ required: true, message: '请输入描述', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const loadData = async () => {
  loading.value = true
  isDataLoading.value = true // 标记正在加载数据
  try {
    const products = await getAllProducts()
    // 确保每个产品的 status 字段存在且为数字类型（0 或 1）
    tableData.value = products.map(product => ({
      ...product,
      status: product.status !== undefined && product.status !== null ? Number(product.status) : 1
    }))
  } catch (error: any) {
    // 如果是 token 过期错误，不显示错误信息（拦截器已经处理了）
    const errorMessage = error.message || '加载失败'
    if (!errorMessage.includes('Token') && !errorMessage.includes('token') && 
        !errorMessage.includes('登录') && !errorMessage.includes('过期') && 
        !errorMessage.includes('401')) {
      ElMessage.error(errorMessage)
    }
  } finally {
    loading.value = false
    // 延迟重置标志，确保数据绑定完成后再允许状态变更
    setTimeout(() => {
      isDataLoading.value = false
    }, 300)
  }
}

const handleRefresh = () => {
  loadData()
}

const handleStockChange = async (row: Product) => {
  // 如果正在加载数据，忽略库存变更事件（防止刷新时触发）
  if (isDataLoading.value) {
    return
  }
  
  try {
    await updateProductStock(row.id, row.stock)
    ElMessage.success('库存更新成功')
  } catch (error: any) {
    // 如果是 token 过期错误，不显示错误信息（拦截器已经处理了）
    const errorMessage = error.message || '更新失败'
    if (!errorMessage.includes('Token') && !errorMessage.includes('token') && 
        !errorMessage.includes('登录') && !errorMessage.includes('过期') && 
        !errorMessage.includes('401')) {
      ElMessage.error(errorMessage)
      // 重新加载数据以恢复原始状态
      await loadData()
    }
  }
}

const handleStatusChange = async (row: Product) => {
  // 如果正在加载数据，忽略状态变更事件（防止刷新时触发）
  if (isDataLoading.value) {
    return
  }
  
  try {
    await updateProductStatus(row.id, row.status)
    ElMessage.success('状态更新成功')
  } catch (error: any) {
    // 如果是 token 过期错误，不显示错误信息（拦截器已经处理了）
    const errorMessage = error.message || '更新失败'
    if (!errorMessage.includes('Token') && !errorMessage.includes('token') && 
        !errorMessage.includes('登录') && !errorMessage.includes('过期') && 
        !errorMessage.includes('401')) {
      ElMessage.error(errorMessage)
      // 重新加载数据以恢复原始状态
      await loadData()
    }
  }
}

const handleCreate = () => {
  // 重置表单
  Object.assign(editForm, {
    id: undefined,
    name: '',
    points: 1,
    description: '',
    stock: 0,
    monthlyLimit: 0,
    image: '',
    status: 1
  })
  isEditMode.value = false
  editDialogVisible.value = true
}

const handleEdit = (row: Product) => {
  Object.assign(editForm, { ...row })
  isEditMode.value = true
  editDialogVisible.value = true
}

const handleDialogClose = () => {
  // 对话框关闭时重置表单验证状态
  if (editFormRef.value) {
    editFormRef.value.clearValidate()
  }
}

const handleSave = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid: boolean) => {
    if (valid && editForm.id) {
      saving.value = true
      try {
        await updateProduct(editForm.id, editForm)
        ElMessage.success('保存成功')
        editDialogVisible.value = false
        await loadData()
      } catch (error: any) {
        ElMessage.error(error.message || '保存失败')
      } finally {
        saving.value = false
      }
    }
  })
}

const handleCreateSave = async () => {
  if (!editFormRef.value) return
  
  await editFormRef.value.validate(async (valid: boolean) => {
    if (valid) {
      saving.value = true
      try {
        // 构建创建产品的数据（排除id和usedThisMonth）
        const createData = {
          name: editForm.name,
          points: editForm.points,
          description: editForm.description || '',
          stock: editForm.stock || 0,
          monthlyLimit: editForm.monthlyLimit || 0,
          image: editForm.image || '',
          status: editForm.status !== undefined ? editForm.status : 1
        }
        await createProduct(createData)
        ElMessage.success('创建成功')
        editDialogVisible.value = false
        await loadData()
      } catch (error: any) {
        ElMessage.error(error.message || '创建失败')
      } finally {
        saving.value = false
      }
    }
  })
}

const handleDelete = async (row: Product) => {
  try {
    await ElMessageBox.confirm('确定要删除该产品吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    // 这里可以添加删除API调用
    ElMessage.warning('删除功能待实现')
  } catch {
    // 用户取消
  }
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.products-page {
  width: 100%;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h3 {
  margin: 0;
}
</style>

