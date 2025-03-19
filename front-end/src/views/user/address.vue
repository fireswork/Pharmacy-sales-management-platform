<template>
  <div class="address-container">
    <a-card title="收货地址管理">
      <template #extra>
        <a-button type="primary" @click="showAddressModal">
          <PlusOutlined /> 新增地址
        </a-button>
      </template>

      <a-list :data-source="addresses" item-layout="horizontal">
        <template #renderItem="{ item }">
          <a-list-item>
            <div class="address-item">
              <div class="address-info">
                <div class="contact">
                  <span class="name">{{ item.name }}</span>
                  <span class="phone">{{ item.phone }}</span>
                  <a-tag v-if="item.isDefault" color="blue">默认</a-tag>
                </div>
                <div class="address">{{ item.address }}</div>
              </div>
              <div class="address-actions">
                <a-button type="link" @click="editAddress(item)">编辑</a-button>
                <a-button 
                  type="link" 
                  danger 
                  @click="deleteAddress(item)"
                  v-if="!item.isDefault"
                >
                  删除
                </a-button>
                <a-button 
                  type="link" 
                  @click="setDefaultAddress(item)"
                  v-if="!item.isDefault"
                >
                  设为默认
                </a-button>
              </div>
            </div>
          </a-list-item>
        </template>
      </a-list>
    </a-card>

    <!-- 地址编辑弹窗 -->
    <a-modal
      v-model:visible="addressModalVisible"
      :title="addressForm.id ? '编辑地址' : '新增地址'"
      @ok="handleAddressSubmit"
    >
      <a-form :model="addressForm" :label-col="{ span: 4 }">
        <a-form-item label="收货人" required>
          <a-input v-model:value="addressForm.name" />
        </a-form-item>
        <a-form-item label="手机号" required>
          <a-input v-model:value="addressForm.phone" />
        </a-form-item>
        <a-form-item label="地址" required>
          <a-textarea v-model:value="addressForm.address" :rows="3" />
        </a-form-item>
        <a-form-item label="默认地址">
          <a-switch v-model:checked="addressForm.isDefault" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { PlusOutlined } from '@ant-design/icons-vue'

// 地址列表
const addresses = ref([
  {
    id: 1,
    name: '张三',
    phone: '13800138000',
    address: '北京市朝阳区某某街道某某小区1号楼1单元101',
    isDefault: true
  },
  {
    id: 2,
    name: '张三',
    phone: '13800138000',
    address: '上海市浦东新区某某路某某大厦B座2201',
    isDefault: false
  }
])

// 地址弹窗相关
const addressModalVisible = ref(false)
const addressForm = reactive({
  id: null,
  name: '',
  phone: '',
  address: '',
  isDefault: false
})

// 显示地址弹窗
const showAddressModal = () => {
  Object.assign(addressForm, {
    id: null,
    name: '',
    phone: '',
    address: '',
    isDefault: false
  })
  addressModalVisible.value = true
}

// 编辑地址
const editAddress = (address) => {
  Object.assign(addressForm, address)
  addressModalVisible.value = true
}

// 删除地址
const deleteAddress = (address) => {
  addresses.value = addresses.value.filter(item => item.id !== address.id)
  message.success('删除成功')
}

// 设置默认地址
const setDefaultAddress = (address) => {
  addresses.value.forEach(item => item.isDefault = false)
  address.isDefault = true
  message.success('设置成功')
}

// 提交地址表单
const handleAddressSubmit = () => {
  if (addressForm.id) {
    // 编辑现有地址
    const index = addresses.value.findIndex(item => item.id === addressForm.id)
    addresses.value[index] = { ...addressForm }
  } else {
    // 添加新地址
    addresses.value.push({
      ...addressForm,
      id: Date.now()
    })
  }
  addressModalVisible.value = false
  message.success('保存成功')
}
</script>

<style lang="less" scoped>
.address-container {
  padding: 16px;

  .address-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    width: 100%;

    .address-info {
      flex: 1;

      .contact {
        margin-bottom: 8px;

        .name {
          font-weight: 500;
          margin-right: 16px;
        }

        .phone {
          color: #8c8c8c;
        }

        :deep(.ant-tag) {
          margin-left: 8px;
        }
      }

      .address {
        color: #595959;
      }
    }

    .address-actions {
      display: flex;
      gap: 8px;
    }
  }
}
</style> 