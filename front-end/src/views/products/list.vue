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
            style="width: 160px"
            placeholder="药品分类"
            allow-clear
          >
            <a-select-option v-for="cat in categories" :key="cat.value" :value="cat.value">
              {{ cat.label }}
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
        :data-source="products"
        :pagination="pagination"
        :row-key="record => record.id"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <!-- 图片列 -->
          <template v-if="column.key === 'image'">
            <img :src="record.image" :alt="record.name" class="product-image" />
          </template>

          <!-- 状态列 -->
          <template v-if="column.key === 'status'">
            <a-tag :color="record.status === 'on' ? 'success' : 'default'">
              {{ record.status === 'on' ? '在售' : '下架' }}
            </a-tag>
          </template>

          <!-- 库存列 -->
          <template v-if="column.key === 'stock'">
            <span :class="{ 'low-stock': record.stock < 100 }">{{ record.stock }}</span>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button type="link" size="small" @click="handleEdit(record)">编辑</a-button>
              <a-button 
                type="link" 
                size="small" 
                :danger="record.status === 'on'"
                @click="handleStatusChange(record)"
              >
                {{ record.status === 'on' ? '下架' : '上架' }}
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 新增/编辑弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑药品' : '新增药品'"
      @ok="handleModalSubmit"
      :confirmLoading="submitLoading"
      width="680px"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
    >
      <a-form
        ref="formRef"
        :model="formData"
        :rules="rules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="药品编号" name="code" required>
          <a-input v-model:value="formData.code" placeholder="请输入药品编号" />
        </a-form-item>

        <a-form-item label="药品名称" name="name" required>
          <a-input v-model:value="formData.name" placeholder="请输入药品名称" />
        </a-form-item>

        <a-row>
          <a-col :span="12">
            <a-form-item label="药品分类" name="category" required :label-col="{ span: 12 }" :wrapper-col="{ span: 12 }">
              <a-select v-model:value="formData.category" placeholder="请选择">
                <a-select-option v-for="cat in categories" :key="cat.value" :value="cat.value">
                  {{ cat.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status" required :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
              <a-select v-model:value="formData.status" placeholder="请选择" style="width: 100%">
                <a-select-option value="on">在售</a-select-option>
                <a-select-option value="off">下架</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
        </a-row>

        <a-row>
          <a-col :span="12">
            <a-form-item label="售价" name="price" required :label-col="{ span: 12 }" :wrapper-col="{ span: 12 }">
              <a-input-number
                v-model:value="formData.price"
                :min="0"
                :precision="2"
                :step="0.1"
                style="width: 100%"
                addon-after="元"
              />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="库存" name="stock" required :label-col="{ span: 6 }" :wrapper-col="{ span: 14 }">
              <a-input-number
                v-model:value="formData.stock"
                :min="0"
                :precision="0"
                style="width: 100%"
                addon-after="件"
              />
            </a-form-item>
          </a-col>
        </a-row>

        <a-form-item label="规格" name="specification" required>
          <a-input v-model:value="formData.specification" placeholder="请输入药品规格，如：10片/盒" />
        </a-form-item>

        <a-form-item label="生产厂家" name="manufacturer" required>
          <a-input v-model:value="formData.manufacturer" placeholder="请输入生产厂家" />
        </a-form-item>

        <a-form-item label="药品图片" name="image">
          <div class="upload-wrapper">
            <a-upload
              v-model:file-list="fileList"
              list-type="picture-card"
              :max-count="1"
              @preview="handlePreview"
              @change="handleChange"
            >
              <div v-if="fileList.length < 1">
                <plus-outlined />
                <div style="margin-top: 8px">上传图片</div>
              </div>
            </a-upload>
            <div class="upload-tip">建议尺寸：800x800px，支持jpg、png格式</div>
          </div>
        </a-form-item>

        <a-form-item label="药品描述" name="description">
          <a-textarea
            v-model:value="formData.description"
            :rows="4"
            placeholder="请输入药品描述，包括主要功效、使用方法等"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 图片预览弹窗 -->
    <a-modal
      v-model:visible="previewVisible"
      title="图片预览"
      :footer="null"
      okText="确定"
      cancelText="取消"
    >
      <img :src="previewImage" style="width: 100%" />
    </a-modal>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { SearchOutlined, ReloadOutlined, PlusOutlined } from '@ant-design/icons-vue'
import { useRouter } from 'vue-router'

const router = useRouter()

// 搜索表单
const searchForm = ref({
  keyword: '',
  category: undefined
})

// 药品分类
const categories = [
  { value: 'OTC', label: 'OTC药品' },
  { value: 'RX', label: '处方药' },
  { value: 'TCM', label: '中药' },
  { value: 'HEALTH', label: '保健品' }
]

// 表格列定义
const columns = [
  {
    title: '药品编号',
    dataIndex: 'code',
    width: 120,
  },
  {
    title: '图片',
    key: 'image',
    width: 100,
    align: 'center',
  },
  {
    title: '药品名称',
    dataIndex: 'name',
    width: 200,
  },
  {
    title: '分类',
    dataIndex: 'categoryName',
    width: 120,
  },
  {
    title: '售价',
    dataIndex: 'price',
    width: 100,
    align: 'right',
  },
  {
    title: '库存',
    key: 'stock',
    width: 100,
    align: 'right',
  },
  {
    title: '状态',
    key: 'status',
    width: 100,
    align: 'center',
  },
  {
    title: '操作',
    key: 'action',
    width: 150,
    align: 'center',
    fixed: 'right'
  }
]

// 模拟数据
const products = ref([
  {
    id: 1,
    code: 'MED001',
    name: '布洛芬缓释胶囊',
    category: 'OTC',
    categoryName: 'OTC药品',
    image: '/images/product1.jpg',
    price: 25.80,
    stock: 1000,
    status: 'on'
  },
  {
    id: 2,
    code: 'MED002',
    name: '感冒灵颗粒',
    category: 'OTC',
    categoryName: 'OTC药品',
    image: '/images/product2.jpg',
    price: 32.50,
    stock: 80,
    status: 'on'
  },
  {
    id: 3,
    code: 'MED003',
    name: '板蓝根颗粒',
    category: 'TCM',
    categoryName: '中药',
    image: '/images/product3.jpg',
    price: 28.50,
    stock: 500,
    status: 'on'
  }
])

// 分页配置
const pagination = {
  total: products.value.length,
  pageSize: 10,
  showTotal: (total) => `共 ${total} 条记录`,
}

// 搜索
const handleSearch = () => {
  message.success('搜索成功')
}

// 重置
const handleReset = () => {
  searchForm.value = {
    keyword: '',
    category: undefined
  }
  handleSearch()
}

// 弹窗相关
const modalVisible = ref(false)
const submitLoading = ref(false)
const formRef = ref()
const fileList = ref([])
const previewVisible = ref(false)
const previewImage = ref('')
const isEdit = ref(false)

// 表单数据
const formData = ref({
  code: '',
  name: '',
  category: undefined,
  status: 'on',
  image: '',
  price: 0,
  stock: 0,
  specification: '',
  manufacturer: '',
  description: ''
})

// 表单验证规则
const rules = {
  code: [{ required: true, message: '请输入药品编号' }],
  name: [{ required: true, message: '请输入药品名称' }],
  category: [{ required: true, message: '请选择药品分类' }],
  status: [{ required: true, message: '请选择状态' }],
  price: [{ required: true, message: '请输入售价' }],
  stock: [{ required: true, message: '请输入库存' }],
  specification: [{ required: true, message: '请输入药品规格' }],
  manufacturer: [{ required: true, message: '请输入生产厂家' }]
}

// 新增药品
const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    code: '',
    name: '',
    category: undefined,
    status: 'on',
    image: '',
    price: 0,
    stock: 0,
    specification: '',
    manufacturer: '',
    description: ''
  }
  fileList.value = []
  modalVisible.value = true
}

// 编辑药品
const handleEdit = (record) => {
  isEdit.value = true
  formData.value = { ...record }
  fileList.value = record.image ? [{
    uid: '-1',
    name: 'image.png',
    status: 'done',
    url: record.image,
  }] : []
  modalVisible.value = true
}

// 提交表单
const handleModalSubmit = () => {
  formRef.value.validate().then(() => {
    submitLoading.value = true
    // 模拟提交
    setTimeout(() => {
      message.success('保存成功')
      modalVisible.value = false
      submitLoading.value = false
      handleSearch()
    }, 1000)
  })
}

// 图片相关处理
const handlePreview = (file) => {
  previewImage.value = file.url || file.preview
  previewVisible.value = true
}

const handleChange = ({ fileList: newFileList }) => {
  fileList.value = newFileList
}

// 修改状态
const handleStatusChange = (record) => {
  const newStatus = record.status === 'on' ? 'off' : 'on'
  const action = newStatus === 'on' ? '上架' : '下架'
  record.status = newStatus
  message.success(`${action}成功`)
}
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

  .low-stock {
    color: #ff4d4f;
  }

  .upload-wrapper {
    .upload-tip {
      margin-top: 8px;
      color: #999;
      font-size: 13px;
    }
  }

  :deep(.ant-upload-list-picture-card-container) {
    width: 100px;
    height: 100px;
  }

  :deep(.ant-upload.ant-upload-select-picture-card) {
    width: 100px;
    height: 100px;
  }

  :deep(.ant-form-item) {
    margin-bottom: 20px;
  }

  :deep(.ant-modal-body) {
    padding: 24px 24px 0;
  }

  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }

  :deep(.ant-form-item-label) {
    text-align: right;
    padding-right: 12px;
    
    label {
      color: #262626;
      
      &::before {
        color: #ff4d4f;
      }
    }
  }

  :deep(.ant-input-number-group-addon) {
    padding: 0 8px;
  }
}
</style> 