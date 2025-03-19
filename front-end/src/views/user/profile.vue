<template>
  <div class="profile-container">
    <a-row :gutter="[16, 16]">
      <!-- 左侧个人信息卡片 -->
      <a-col :span="8">
        <a-card class="user-card">
          <div class="user-info">
            <a-avatar :size="80" class="avatar">
              <template #icon><UserOutlined /></template>
            </a-avatar>
            <h2 class="username">{{ userInfo.username }}</h2>
            <div class="user-stats">
              <div class="stat-item">
                <span class="number">{{ userInfo.points }}</span>
                <span class="label">积分</span>
              </div>
              <div class="stat-item">
                <span class="number">{{ userInfo.favorites }}</span>
                <span class="label">收藏</span>
              </div>
              <div class="stat-item">
                <span class="number">{{ userInfo.orders }}</span>
                <span class="label">订单</span>
              </div>
            </div>
          </div>
        </a-card>
      </a-col>

      <!-- 右侧信息编辑区域 -->
      <a-col :span="16">
        <a-card title="基本信息">
          <a-form 
            :model="formState" 
            :rules="rules"
            ref="formRef"
            :label-col="{ span: 4 }"
            :wrapper-col="{ span: 16 }"
          >
            <a-form-item label="用户名" name="username">
              <a-input v-model:value="formState.username" />
            </a-form-item>

            <a-form-item label="手机号" name="phone">
              <a-input v-model:value="formState.phone" />
            </a-form-item>

            <a-form-item label="邮箱" name="email">
              <a-input v-model:value="formState.email" />
            </a-form-item>

            <a-form-item label="性别" name="gender">
              <a-radio-group v-model:value="formState.gender">
                <a-radio value="male">男</a-radio>
                <a-radio value="female">女</a-radio>
              </a-radio-group>
            </a-form-item>

            <a-form-item label="生日" name="birthday">
              <a-date-picker 
                v-model:value="formState.birthday"
                style="width: 100%"
              />
            </a-form-item>

            <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
              <a-button type="primary" @click="handleSubmit">保存修改</a-button>
            </a-form-item>
          </a-form>
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { message } from 'ant-design-vue'
import { 
  UserOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'

// 用户基本信息
const userInfo = reactive({
  username: '张三',
  points: 1280,
  favorites: 12,
  orders: 25
})

// 表单数据
const formState = reactive({
  username: '张三',
  phone: '13800138000',
  email: 'zhangsan@example.com',
  gender: 'male',
  birthday: null
})

// 表单规则
const rules = {
  username: [{ required: true, message: '请输入用户名' }],
  phone: [{ required: true, message: '请输入手机号' }],
  email: [{ required: true, type: 'email', message: '请输入正确的邮箱' }]
}

// 表单提交
const handleSubmit = () => {
  message.success('保存成功')
}
</script>

<style lang="less" scoped>
.profile-container {
  padding: 16px;

  .user-card {
    .user-info {
      text-align: center;

      .avatar {
        margin-bottom: 16px;
        background: #1890ff;
      }

      .username {
        margin-bottom: 24px;
      }

      .user-stats {
        display: flex;
        justify-content: space-around;
        padding-top: 24px;
        border-top: 1px solid #f0f0f0;

        .stat-item {
          display: flex;
          flex-direction: column;
          align-items: center;

          .number {
            font-size: 20px;
            font-weight: bold;
            color: #1890ff;
          }

          .label {
            color: #8c8c8c;
            margin-top: 4px;
          }
        }
      }
    }
  }
}
</style> 