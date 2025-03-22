<template>
  <div class="products-container">
    <a-card class="product-card">
      <!-- 搜索区域 -->
      <div class="filter-section">
        <a-space>
          <a-input
            v-model:value="searchForm.keyword"
            placeholder="请输入药品名称/编号"
            style="width: 200px"
            allow-clear
          />
          <a-select
            v-model:value="searchForm.category"
            style="width: 150px"
            placeholder="药品分类"
            allow-clear
          >
            <a-select-option
              v-for="category in categoryOptions"
              :key="category.value"
              :value="category.value"
            >
              {{ category.label }}
            </a-select-option>
          </a-select>
          <a-select
            v-model:value="searchForm.status"
            style="width: 120px"
            placeholder="状态"
            allow-clear
          >
            <a-select-option value="active">上架中</a-select-option>
            <a-select-option value="inactive">已下架</a-select-option>
          </a-select>
          <!-- 管理员可以选择门店，使用计算属性判断 -->
          <a-select
            v-if="isAdminUser"
            v-model:value="searchForm.storeId"
            style="width: 150px"
            placeholder="选择门店"
          >
            <a-select-option
              v-for="store in storeOptions"
              :key="store.id"
              :value="store.id"
            >
              {{ store.name }}
            </a-select-option>
          </a-select>
          <a-button type="primary" @click="handleSearch">
            <template #icon><SearchOutlined /></template>
            查询
          </a-button>
          <a-button @click="handleReset">
            <template #icon><ReloadOutlined /></template>
            重置
          </a-button>
          <a-button type="primary" @click="handleAdd">
            <template #icon><PlusOutlined /></template>
            新增药品
          </a-button>
        </a-space>
      </div>

      <!-- 药品列表 -->
      <a-table
        :columns="columns"
        :data-source="productList"
        :pagination="pagination"
        :loading="loading"
        :row-key="(record) => record.id"
        :scroll="{ x: 1000 }"
        @change="handleTableChange"
        bordered
        class="product-table"
      >
        <!-- 药品图片 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'image'">
            <div class="image-container">
              <img v-if="record.image" :src="record.image" alt="药品图片" class="product-image" />
              <span v-else>无图片</span>
            </div>
          </template>

          <!-- 药品状态 -->
          <template v-if="column.dataIndex === 'status'">
            <a-tag :color="record.status === 'active' ? 'green' : 'red'">
              {{ record.status === 'active' ? '上架中' : '已下架' }}
            </a-tag>
          </template>

          <!-- 操作列 -->
          <template v-if="column.dataIndex === 'action'">
            <a-space>
              <a-button type="link" @click="handleEdit(record)">编辑</a-button>
              <a-button type="link" @click="handleToggleStatus(record)" :disabled="toggleLoading">
                {{ record.status === 'active' ? '下架' : '上架' }}
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑药品弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑药品' : '新增药品'"
      width="600px"
      @ok="handleModalSubmit"
      @cancel="modalVisible = false"
      :confirm-loading="submitLoading"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 5 }"
        :wrapper-col="{ span: 19 }"
      >
        <a-form-item label="药品名称" name="name">
          <a-input v-model:value="formData.name" placeholder="请输入药品名称" />
        </a-form-item>
        <a-form-item label="药品分类" name="category">
          <a-select
            v-model:value="formData.category"
            placeholder="请选择药品分类"
            allow-clear
          >
            <a-select-option
              v-for="category in categoryOptions"
              :key="category.value"
              :value="category.value"
            >
              {{ category.label }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <!-- 管理员可以选择门店，使用计算属性判断 -->
        <a-form-item v-if="isAdminUser" label="所属门店" name="storeId">
          <a-select
            v-model:value="formData.storeId"
            placeholder="请选择所属门店"
            allow-clear
            :disabled="isEdit"
          >
            <a-select-option
              v-for="store in storeOptions"
              :key="store.id"
              :value="store.id"
            >
              {{ store.name }}
            </a-select-option>
          </a-select>
        </a-form-item>
        <a-form-item label="规格" name="specification">
          <a-input v-model:value="formData.specification" placeholder="请输入规格" />
        </a-form-item>

        <a-form-item label="生产厂家" name="manufacturer">
          <a-input v-model:value="formData.manufacturer" placeholder="请输入生产厂家" />
        </a-form-item>

        <a-form-item label="批准文号" name="approvalNumber">
          <a-input v-model:value="formData.approvalNumber" placeholder="请输入批准文号" />
        </a-form-item>

        <a-form-item label="零售价" name="retailPrice" required>
          <a-input-number
            v-model:value="formData.retailPrice"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入零售价"
            addon-after="元"
          />
        </a-form-item>

        <a-form-item label="进货价" name="costPrice">
          <a-input-number
            v-model:value="formData.costPrice"
            :min="0"
            :precision="2"
            style="width: 100%"
            placeholder="请输入进货价"
            addon-after="元"
          />
        </a-form-item>

        <a-form-item label="库存数量" name="stock">
          <a-input-number
            v-model:value="formData.stock"
            :min="0"
            style="width: 100%"
            placeholder="请输入库存数量"
          />
        </a-form-item>

        <a-form-item label="药品图片" name="image">
          <a-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :show-upload-list="false"
            :before-upload="beforeUpload"
            :customRequest="customUpload"
          >
            <img v-if="imageUrl" :src="imageUrl" alt="药品图片" class="upload-img" />
            <div v-else>
              <plus-outlined />
              <div class="ant-upload-text">上传图片</div>
            </div>
          </a-upload>
          <div class="upload-tip" v-if="!imageUrl">支持JPG、PNG格式，大小不超过2MB</div>
        </a-form-item>

        <a-form-item label="药品介绍" name="description">
          <a-textarea v-model:value="formData.description" :rows="4" placeholder="请输入药品介绍" />
        </a-form-item>

        <a-form-item label="使用说明" name="usage">
          <a-textarea v-model:value="formData.usage" :rows="4" placeholder="请输入使用说明" />
        </a-form-item>

        <a-form-item label="状态" name="status">
          <a-radio-group v-model:value="formData.status">
            <a-radio value="active">上架</a-radio>
            <a-radio value="inactive">下架</a-radio>
          </a-radio-group>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import axios from '../../utils/axios'

// 是否管理员
const isAdmin = ref(false)

// 从localStorage获取用户角色判断是否为管理员
const isAdminUser = computed(() => {
  return localStorage.getItem('userRole')?.toUpperCase() === 'ADMIN'
})

// 搜索表单
const searchForm = ref({
  keyword: '',
  category: undefined,
  status: undefined,
  storeId: undefined
})

// 药品列表
const productList = ref([])

// 分页配置
const pagination = ref({
  current: 1,
  pageSize: 10,
  total: 0,
  showSizeChanger: true,
  showQuickJumper: true,
  showTotal: (total) => `共 ${total} 条记录`
})

// 加载状态
const loading = ref(false)
const submitLoading = ref(false)
const toggleLoading = ref(false)

// 弹窗控制
const modalVisible = ref(false)
const isEdit = ref(false)

// 表单引用
const formRef = ref(null)

// 表单数据
const formData = ref({
  id: null,
  name: '',
  code: '',
  category: undefined,
  specification: '',
  manufacturer: '',
  approvalNumber: '',
  retailPrice: null,
  costPrice: null,
  stock: 0,
  image: '',
  description: '',
  usage: '',
  status: 'active',
  storeId: null
})

// 图片上传相关
const fileList = ref([])
const imageUrl = ref('')

// 药品分类选项
const categoryOptions = [
  { label: '处方药', value: 'prescription' },
  { label: '非处方药', value: 'otc' },
  { label: '中药', value: 'chinese' },
  { label: '保健品', value: 'health' },
  { label: '医疗器械', value: 'device' }
]

// 门店列表
const storeOptions = ref([])

// 表格列定义
const columns = [
  {
    title: '药品图片',
    dataIndex: 'image',
    key: 'image',
    width: 100
  },
  {
    title: '药品名称',
    dataIndex: 'name',
    key: 'name',
    width: 150
  },
  {
    title: '药品编号',
    dataIndex: 'code',
    key: 'code',
    width: 120,
  },
  {
    title: '分类',
    dataIndex: 'category',
    key: 'category',
    width: 100,
    customRender: ({text}) => {
      const category = categoryOptions.find(item => item.value === text)
      return category ? category.label : text
    }
  },
  {
    title: '规格',
    dataIndex: 'specification',
    key: 'specification',
    width: 120
  },
  {
    title: '零售价',
    dataIndex: 'retailPrice',
    key: 'retailPrice',
    width: 100,
    customRender: ({text}) => (text ? `¥${text}` : '-')
  },
  {
    title: '库存',
    dataIndex: 'stock',
    key: 'stock',
    width: 80
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 80
  },
  {
    title: '操作',
    dataIndex: 'action',
    key: 'action',
    width: 120,
    fixed: 'right'
  }
]

// 获取用户信息
const fetchUserInfo = async () => {
  try {
    const response = await axios.get('/user')
    if (response && response.code === 200) {
      isAdmin.value = response.data.role?.toUpperCase() === 'ADMIN'
      
      // 如果是管理员，加载门店列表
      if (isAdmin.value) {
        fetchStoreList()
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
  }
}

// 获取门店列表
const fetchStoreList = async () => {
  try {
    const response = await axios.get('/employee/stores')
    if (response && response.code === 200) {
      storeOptions.value = response.data || []
      // 管理员默认选中第一个门店
      if (isAdminUser.value && storeOptions.value.length > 0 && !searchForm.value.storeId) {
        searchForm.value.storeId = storeOptions.value[0].id
      }
    }
  } catch (error) {
    console.error('获取门店列表失败:', error)
  }
}

// 获取药品列表
const fetchProducts = async (params = {}) => {
  try {
    loading.value = true
    const { page = pagination.value.current - 1, size = pagination.value.pageSize } = params
    
    // 构建查询参数
    const queryParams = {
      page,
      size,
      sort: 'id',
      order: 'desc'
    }
    
    // 添加筛选条件
    if (searchForm.value.keyword?.trim()) {
      queryParams.keyword = searchForm.value.keyword.trim()
    }
    if (searchForm.value.category) {
      queryParams.category = searchForm.value.category
    }
    if (searchForm.value.status) {
      queryParams.drugStatus = searchForm.value.status
    }
    
    // 管理员和员工的处理方式
    if (isAdminUser.value) {
      // 管理员必须选择门店查询
      if (!searchForm.value.storeId && storeOptions.value.length > 0) {
        searchForm.value.storeId = storeOptions.value[0].id
      }
      
      // 确保有门店ID
      if (!searchForm.value.storeId) {
        loading.value = false
        return // 如果没有门店ID，不执行查询
      }
      
      const response = await axios.get(`/inventory/store/${searchForm.value.storeId}`, { 
        params: queryParams,
        paramsSerializer: params => {
          return Object.entries(params)
            .filter(([_, value]) => value != null)
            .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
            .join('&')
        }
      })
      
      if (response.code === 200) {
        productList.value = response.data.content.map(item => ({
          id: item.productId,
          name: item.productName,
          code: item.productCode,
          category: item.category,
          specification: item.specification,
          manufacturer: item.manufacturer,
          retailPrice: item.price,
          stock: item.quantity,
          image: item.image,
          description: item.description,
          status: item.drugStatus || 'active',
          storeId: item.storeId
        }))
        pagination.value.total = response.data.totalElements
        pagination.value.current = page + 1
      }
    } else {
      // 员工从库存接口获取自己门店的药品
      const response = await axios.get('/inventory/current', { 
        params: queryParams,
        paramsSerializer: params => {
          return Object.entries(params)
            .filter(([_, value]) => value != null)
            .map(([key, value]) => `${key}=${encodeURIComponent(value)}`)
            .join('&')
        }
      })
      
      if (response.code === 200) {
        productList.value = response.data.content.map(item => ({
          id: item.productId,
          name: item.productName,
          code: item.productCode,
          category: item.category,
          specification: item.specification,
          manufacturer: item.manufacturer,
          retailPrice: item.price,
          stock: item.quantity,
          image: item.image,
          description: item.description,
          status: item.drugStatus || 'active',
          storeId: item.storeId
        }))
        pagination.value.total = response.data.totalElements
        pagination.value.current = page + 1
      }
    }
  } catch (error) {
    console.error('获取药品列表失败:', error)
    message.error('获取药品列表失败')
  } finally {
    loading.value = false
  }
}

// 处理表格变化
const handleTableChange = (pag, filters, sorter) => {
  pagination.value.current = pag.current
  pagination.value.pageSize = pag.pageSize
  fetchProducts({
    page: pag.current - 1, // 转换为后端分页
    size: pag.pageSize
  })
}

// 处理搜索
const handleSearch = () => {
  pagination.value.current = 1 // 重置到第一页
  fetchProducts({
    page: 0, // 后端分页从0开始
    size: pagination.value.pageSize
  })
}

// 处理重置
const handleReset = () => {
  // 保存当前选中的门店ID
  const currentStoreId = searchForm.value.storeId
  
  searchForm.value = {
    keyword: '',
    category: undefined,
    status: undefined,
    storeId: currentStoreId // 保留门店选择不重置
  }
  
  pagination.value.current = 1
  fetchProducts({
    page: 0,
    size: pagination.value.pageSize
  })
}

// 处理添加
const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    id: null,
    name: '',
    code: '',
    category: undefined,
    specification: '',
    manufacturer: '',
    approvalNumber: '',
    retailPrice: null,
    costPrice: null,
    stock: 0,
    image: '',
    description: '',
    usage: '',
    status: 'active',
    storeId: null
  }
  
  // 如果是管理员，可以选择门店并默认选中第一个
  if (isAdminUser.value) {
    fetchStoreList().then(() => {
      if (storeOptions.value.length > 0) {
        formData.value.storeId = storeOptions.value[0].id
      }
    })
  }

  fileList.value = []
  imageUrl.value = ''
  modalVisible.value = true
}

// 处理编辑
const handleEdit = (record) => {
  isEdit.value = true
  formData.value = { 
    ...record,
    // 如果是管理员编辑，需要确保storeId存在
    storeId: record.storeId || (isAdminUser.value && storeOptions.value.length > 0 ? storeOptions.value[0].id : null)
  }

  if (record.image) {
    imageUrl.value = record.image
    fileList.value = [
      {
        uid: '-1',
        name: 'image.png',
        status: 'done',
        url: record.image
      }
    ]
  } else {
    imageUrl.value = ''
    fileList.value = []
  }

  modalVisible.value = true
}

// 处理上下架
const handleToggleStatus = async (record) => {
  try {
    toggleLoading.value = true
    const newStatus = record.status === 'active' ? 'inactive' : 'active'

    // 更新库存商品状态
    const requestData = {
      storeId: record.storeId,
      productId: record.id,
      drugStatus: newStatus
    }

    await axios.put(`/inventory/status`, requestData)

    message.success(`${newStatus === 'active' ? '上架' : '下架'}成功`)
    fetchProducts()
  } catch (error) {
    console.error('更新状态失败:', error)
    message.error('操作失败')
  } finally {
    toggleLoading.value = false
  }
}

// 表单验证规则
const rules = {
  name: [{ required: true, message: '请输入药品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择药品分类', trigger: 'change' }],
  retailPrice: [{ required: true, message: '请输入零售价', trigger: 'blur' }]
}

// 图片上传前检查
const beforeUpload = (file) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('只能上传JPG或PNG格式的图片!')
    return false
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片大小不能超过2MB!')
    return false
  }

  // 创建本地预览URL
  const reader = new FileReader()
  reader.readAsDataURL(file)
  reader.onload = () => {
    imageUrl.value = reader.result

    // 更新文件列表，用于UI显示
    fileList.value = [
      {
        uid: '-1',
        name: file.name,
        status: 'done',
        url: reader.result
      }
    ]
  }

  // 返回false阻止默认上传行为
  return false
}

// 自定义上传方法 - 实际上不会发送请求，只是为了配合组件API
const customUpload = ({ file, onSuccess }) => {
  // 这里不做任何网络请求，只是模拟成功回调
  setTimeout(() => {
    onSuccess('ok', file)
  }, 0)
}

// 处理表单提交
const handleModalSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true

    // 区分新增和编辑
    if (isEdit.value) {
      // 编辑操作 - 更新库存
      const requestData = {
        storeId: isAdminUser.value ? formData.value.storeId : null, // 管理员可以选择门店
        productId: formData.value.id,
        quantity: formData.value.stock,
        price: formData.value.retailPrice,
        adjustType: 'update', // 使用update类型表示直接更新库存
        remark: '手动修改库存'
      }

      await axios.put(`/inventory/adjust`, requestData)
      message.success('库存信息更新成功')
    } else {
      // 新增操作 - 添加产品并同时创建库存
      const requestData = {
        name: formData.value.name,
        category: formData.value.category,
        specification: formData.value.specification,
        manufacturer: formData.value.manufacturer,
        approvalNumber: formData.value.approvalNumber,
        retailPrice: formData.value.retailPrice,
        costPrice: formData.value.costPrice,
        stock: formData.value.stock,
        image: imageUrl.value, // 使用本地预览URL（base64数据）
        description: formData.value.description,
        usage: formData.value.usage,
        status: formData.value.status
      }
      
      // 管理员需要选择门店
      if (isAdminUser.value) {
        if (!formData.value.storeId) {
          message.error('请选择所属门店')
          submitLoading.value = false
          return
        }
        requestData.storeId = formData.value.storeId
      }
      
      await axios.post('/products', requestData)
      message.success('药品添加成功')
    }

    modalVisible.value = false
    fetchProducts()
  } catch (error) {
    console.error('保存信息失败:', error)
    message.error(error.response?.data?.message || '保存信息失败')
  } finally {
    submitLoading.value = false
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchUserInfo()
  // 先获取门店列表，然后再获取产品列表
  if (isAdminUser.value) {
    fetchStoreList().then(() => {
      // 确保门店列表加载完成后再获取产品
      fetchProducts()
    })
  } else {
    fetchProducts()
  }
})
</script>

<style lang="less" scoped>
.products-container {
  padding: 16px;

  .product-card {
    border-radius: 8px;
    box-shadow: 0 1px 2px -2px rgba(0, 0, 0, 0.16), 0 3px 6px 0 rgba(0, 0, 0, 0.12), 0 5px 12px 4px rgba(0, 0, 0, 0.09);
  }

  .filter-section {
    margin-bottom: 24px;
    padding: 24px;
    background: #fafafa;
    border-radius: 4px;
    border: 1px solid #f0f0f0;
  }

  .product-table {
    margin-top: 16px;
    
    :deep(.ant-table-thead > tr > th) {
      background-color: #f5f7fa;
      font-weight: 600;
    }
    
    :deep(.ant-table-tbody > tr:hover > td) {
      background-color: #e6f7ff;
    }
  }

  .image-container {
    width: 80px;
    height: 80px;
    display: flex;
    align-items: center;
    justify-content: center;
    border: 1px solid #f0f0f0;
    border-radius: 4px;
    overflow: hidden;
    background-color: #f9f9f9;
  }

  .product-image {
    max-width: 100%;
    max-height: 100%;
    object-fit: contain;
  }

  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }
  
  .form-item-tip {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}

:deep(.upload-img) {
  max-width: 100%;
  max-height: 100%;
  object-fit: contain;
}
</style>