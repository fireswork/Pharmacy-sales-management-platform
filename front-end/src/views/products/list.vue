<template>
  <div class="products-container">
    <a-card>
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
            <a-select-option v-for="category in categoryOptions" :key="category.value" :value="category.value">
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
        :row-key="record => record.id"
        @change="handleTableChange"
      >
        <!-- 药品图片 -->
        <template #bodyCell="{ column, record }">
          <template v-if="column.dataIndex === 'image'">
            <img
              v-if="record.image"
              :src="record.image"
              alt="药品图片"
              class="product-image"
            />
            <span v-else>无图片</span>
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
              <a-button
                type="link"
                @click="handleToggleStatus(record)"
                :disabled="toggleLoading"
              >
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
      @ok="handleModalSubmit"
      :confirmLoading="submitLoading"
      width="700px"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 4 }"
        :wrapper-col="{ span: 18 }"
      >
        <a-form-item label="药品名称" name="name" required>
          <a-input v-model:value="formData.name" placeholder="请输入药品名称" />
        </a-form-item>

        <a-form-item label="药品分类" name="category" required>
          <a-select v-model:value="formData.category" placeholder="请选择药品分类">
            <a-select-option v-for="category in categoryOptions" :key="category.value" :value="category.value">
              {{ category.label }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="规格" name="specification">
          <a-input v-model:value="formData.specification" placeholder="请输入药品规格" />
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

        <a-form-item label="药品图片" name="image">
          <a-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :show-upload-list="false"
            :before-upload="beforeUpload"
            @change="handleImageChange"
          >
            <img v-if="imageUrl" :src="imageUrl" alt="药品图片" class="upload-img" />
            <div v-else>
              <plus-outlined />
              <div class="ant-upload-text">上传图片</div>
            </div>
          </a-upload>
        </a-form-item>

        <a-form-item label="药品介绍" name="description">
          <a-textarea
            v-model:value="formData.description"
            :rows="4"
            placeholder="请输入药品介绍"
          />
        </a-form-item>

        <a-form-item label="使用说明" name="usage">
          <a-textarea
            v-model:value="formData.usage"
            :rows="4"
            placeholder="请输入使用说明"
          />
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
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import axios from '../../utils/axios'

// 搜索表单
const searchForm = ref({
  keyword: '',
  category: undefined,
  status: undefined
})

// 药品列表
const productList = ref([])

// 分页配置
const pagination = reactive({
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
  image: '',
  description: '',
  usage: '',
  status: 'active'
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
    width: 120
  },
  {
    title: '分类',
    dataIndex: 'category',
    key: 'category',
    width: 120
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
    align: 'right',
    render: (text) => `¥${text ? Number(text).toFixed(2) : '0.00'}`
  },
  {
    title: '状态',
    dataIndex: 'status',
    key: 'status',
    width: 100
  },
  {
    title: '操作',
    dataIndex: 'action',
    key: 'action',
    width: 120,
    fixed: 'right'
  }
]

// 获取药品列表
const fetchProducts = async () => {
  try {
    loading.value = true
    
    const params = {
      page: pagination.current - 1,  // Spring Data JPA 分页从0开始
      size: pagination.pageSize,
      sort: 'id',
      order: 'desc'
    }
    
    if (searchForm.keyword) {
      params.keyword = searchForm.keyword
    }
    
    if (searchForm.category) {
      params.category = searchForm.category
    }
    
    if (searchForm.status) {
      params.status = searchForm.status
    }

    const response = await axios.get('/products', { params })

    if (response.data && response.data) {
      productList.value = response.data.content
      pagination.total = response.data.totalElements
    }
  } catch (error) {
    console.error('获取药品列表失败:', error)
    message.error('获取药品列表失败')
  } finally {
    loading.value = false
  }
}

// 处理表格变化
const handleTableChange = (pag) => {
  pagination.current = pag.current
  pagination.pageSize = pag.pageSize
  fetchProducts()
}

// 处理搜索
const handleSearch = () => {
  pagination.current = 1
  fetchProducts()
}

// 处理重置
const handleReset = () => {
  searchForm.value = {
    keyword: '',
    category: undefined,
    status: undefined
  }
  pagination.current = 1
  fetchProducts()
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
    image: '',
    description: '',
    usage: '',
    status: 'active'
  }
  fileList.value = []
  imageUrl.value = ''
  modalVisible.value = true
}

// 处理编辑
const handleEdit = (record) => {
  isEdit.value = true
  formData.value = { ...record }
  
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
    
    await axios.put(`/products/${record.id}/status`, { status: newStatus })
    
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
  name: [
    { required: true, message: '请输入药品名称', trigger: 'blur' }
  ],
  category: [
    { required: true, message: '请选择药品分类', trigger: 'change' }
  ],
  retailPrice: [
    { required: true, message: '请输入零售价', trigger: 'blur' }
  ]
}

// 图片上传前检查
const beforeUpload = (file) => {
  const isJpgOrPng = file.type === 'image/jpeg' || file.type === 'image/png'
  if (!isJpgOrPng) {
    message.error('只能上传JPG或PNG格式的图片!')
  }
  const isLt2M = file.size / 1024 / 1024 < 2
  if (!isLt2M) {
    message.error('图片大小不能超过2MB!')
  }
  return isJpgOrPng && isLt2M
}

// 处理图片变更
const handleImageChange = (info) => {
  if (info.file.status === 'uploading') {
    return
  }
  if (info.file.status === 'done') {
    // 实际项目中应该从响应中获取图片URL
    // 这里模拟一个URL
    imageUrl.value = URL.createObjectURL(info.file.originFileObj)
    formData.value.image = imageUrl.value
  } else if (info.file.status === 'error') {
    message.error('图片上传失败')
  }
}

// 处理表单提交
const handleModalSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true
    
    const requestData = {
      name: formData.value.name,
      code: formData.value.code,
      category: formData.value.category,
      specification: formData.value.specification,
      manufacturer: formData.value.manufacturer,
      approvalNumber: formData.value.approvalNumber,
      retailPrice: formData.value.retailPrice,
      costPrice: formData.value.costPrice,
      image: formData.value.image,
      description: formData.value.description,
      usage: formData.value.usage,
      status: formData.value.status
    }
    
    if (isEdit.value) {
      await axios.put(`/products/${formData.value.id}`, requestData)
      message.success('药品信息更新成功')
    } else {
      await axios.post('/products', requestData)
      message.success('药品添加成功')
    }
    
    modalVisible.value = false
    fetchProducts()
  } catch (error) {
    console.error('保存药品信息失败:', error)
    message.error(error.response?.data?.message || '保存药品信息失败')
  } finally {
    submitLoading.value = false
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchProducts()
})
</script>

<style lang="less" scoped>
.products-container {
  padding: 16px;

  .filter-section {
    margin-bottom: 24px;
    padding: 24px;
    background: #fafafa;
    border-radius: 4px;
  }

  .product-image {
    width: 60px;
    height: 60px;
    object-fit: cover;
    border-radius: 4px;
  }

  .upload-img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }

  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }
}
</style> 