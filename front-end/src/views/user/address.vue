<template>
  <div class="address-container">
    <a-card title="收货地址管理">
      <template #extra>
        <a-button type="primary" @click="handleAdd">
          <template #icon><PlusOutlined /></template>
          新增地址
        </a-button>
      </template>

      <div class="address-list">
        <a-empty v-if="addressList.length === 0" description="暂无收货地址" />

        <div v-else class="address-grid">
          <div v-for="address in addressList" :key="address.id" class="address-card">
            <div class="address-card-content">
              <div class="address-header">
                <div class="address-info">
                  <span class="receiver">{{ address.receiver }}</span>
                  <span class="phone">{{ address.phoneNumber }}</span>
                </div>
                <a-tag v-if="address.default" color="blue" class="default-tag">默认</a-tag>
              </div>
              
              <div class="address-detail">{{ address.address }}</div>
              
              <div class="address-actions">
                <a-button type="link" size="small" @click="handleEdit(address)">
                  <template #icon><EditOutlined /></template>
                  编辑
                </a-button>
                <a-button type="link" size="small" @click="handleDelete(address)">
                  <template #icon><DeleteOutlined /></template>
                  删除
                </a-button>
                <a-button 
                  v-if="!address.default" 
                  type="link" 
                  size="small" 
                  @click="handleSetDefault(address)"
                >
                  <template #icon><CheckCircleOutlined /></template>
                  设为默认
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </a-card>

    <!-- 新增/编辑地址弹窗 -->
    <a-modal
      v-model:visible="modalVisible"
      :title="isEdit ? '编辑收货地址' : '新增收货地址'"
      @ok="handleModalSubmit"
      :confirmLoading="submitLoading"
      width="500px"
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
        <a-form-item label="收货人" name="receiver" required>
          <a-input v-model:value="formData.receiver" placeholder="请输入收货人姓名" />
        </a-form-item>

        <a-form-item label="手机号码" name="phoneNumber" required>
          <a-input v-model:value="formData.phoneNumber" placeholder="请输入手机号码" />
        </a-form-item>

        <a-form-item label="详细地址" name="address" required>
          <a-textarea v-model:value="formData.address" :rows="3" placeholder="请输入详细地址" />
        </a-form-item>

        <a-form-item label="设为默认" name="default">
          <a-switch v-model:checked="formData.default" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { 
  PlusOutlined, 
  EditOutlined, 
  DeleteOutlined, 
  CheckCircleOutlined 
} from '@ant-design/icons-vue'
import axios from '../../utils/axios'

// 地址列表
const addressList = ref([])

// 加载状态
const loading = ref(false)
const submitLoading = ref(false)

// 弹窗控制
const modalVisible = ref(false)
const isEdit = ref(false)

// 表单引用
const formRef = ref(null)

// 表单数据
const formData = ref({
  id: null,
  receiver: '',
  phoneNumber: '',
  address: '',
  default: false
})

// 表单验证规则
const rules = {
  receiver: [{ required: true, message: '请输入收货人姓名', trigger: 'blur' }],
  phoneNumber: [
    { required: true, message: '请输入手机号码', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }
  ],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }]
}

// 获取地址列表
const fetchAddresses = async () => {
  try {
    loading.value = true
    const response = await axios.get('/address')

    if (response.data) {
      addressList.value = response.data
    }
  } catch (error) {
    console.error('获取收货地址失败:', error)
    message.error('获取收货地址失败')
  } finally {
    loading.value = false
  }
}

// 处理新增
const handleAdd = () => {
  isEdit.value = false
  formData.value = {
    id: null,
    receiver: '',
    phoneNumber: '',
    address: '',
    default: false
  }
  modalVisible.value = true
}

// 处理编辑
const handleEdit = (address) => {
  isEdit.value = true
  formData.value = {
    id: address.id,
    receiver: address.receiver,
    phoneNumber: address.phoneNumber,
    address: address.address,
    default: address.default
  }
  modalVisible.value = true
}

// 处理删除
const handleDelete = async (address) => {
  try {
    await axios.delete(`/address/${address.id}`)
    message.success('删除地址成功')
    fetchAddresses()
  } catch (error) {
    console.error('删除地址失败:', error)
    message.error('删除地址失败')
  }
}

// 设为默认地址
const handleSetDefault = async (address) => {
  try {
    await axios.put(`/address/${address.id}/default`)
    message.success('设置默认地址成功')
    fetchAddresses()
  } catch (error) {
    console.error('设置默认地址失败:', error)
    message.error('设置默认地址失败')
  }
}

// 处理表单提交
const handleModalSubmit = async () => {
  try {
    await formRef.value.validate()
    submitLoading.value = true

    const requestData = {
      receiver: formData.value.receiver,
      phoneNumber: formData.value.phoneNumber,
      address: formData.value.address,
      default: formData.value.default
    }

    if (isEdit.value) {
      await axios.put(`/address/${formData.value.id}`, requestData)
      message.success('更新地址成功')
    } else {
      await axios.post('/address', requestData)
      message.success('添加地址成功')
    }

    modalVisible.value = false
    fetchAddresses()
  } catch (error) {
    console.error('保存地址失败:', error)
    message.error(error.response?.data?.message || '保存地址失败')
  } finally {
    submitLoading.value = false
  }
}

// 组件挂载时获取数据
onMounted(() => {
  fetchAddresses()
})
</script>

<style lang="less" scoped>
.address-container {
  padding: 16px;

  .address-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
    gap: 16px;
    margin-top: 16px;
  }

  .address-card {
    border: 1px solid #e8e8e8;
    border-radius: 8px;
    transition: all 0.3s;
    position: relative;
    overflow: hidden;
    
    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
      border-color: #d9d9d9;
    }
    
    &-content {
      padding: 16px;
    }
  }

  .address-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    
    .address-info {
      display: flex;
      align-items: center;
      flex-wrap: wrap;
      gap: 8px;
    }
    
    .receiver {
      font-weight: 600;
      font-size: 16px;
      color: #333;
    }
    
    .phone {
      color: #666;
    }
    
    .default-tag {
      margin-left: auto;
    }
  }

  .address-detail {
    color: #333;
    margin-bottom: 16px;
    line-height: 1.5;
    word-break: break-all;
    min-height: 40px;
  }

  .address-actions {
    display: flex;
    border-top: 1px dashed #f0f0f0;
    padding-top: 12px;
    justify-content: flex-start;
    flex-wrap: wrap;
    gap: 8px;
    
    .ant-btn {
      padding: 0 8px;
    }
  }
}
</style>
