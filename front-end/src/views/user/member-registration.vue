<template>
  <div class="registration-container">
    <div class="registration-content">
      <!-- 会员注册卡片 -->
      <a-card title="会员注册" class="registration-card" :bordered="false">
        <p v-if="loading" class="loading-message">加载中...</p>
        <div v-else>
          <div v-if="isMember" class="already-member">
            <a-result
              status="success"
              title="您已经是会员"
              sub-title="您已成功注册为会员，可以享受会员价格（9折优惠）和积分特权。"
            >
              <template #extra>
                <a-button type="primary" @click="$router.push('/home/products')">
                  浏览药品
                </a-button>
              </template>
            </a-result>
          </div>
          <div v-else>
            <a-form
              ref="formRef"
              :model="formData"
              :rules="rules"
              :label-col="{ span: 4 }"
              :wrapper-col="{ span: 16 }"
              layout="horizontal"
            >
              <a-form-item label="用户姓名" name="name">
                <a-input
                  v-model:value="formData.name"
                  placeholder="请输入姓名"
                  :maxLength="20"
                  allow-clear
                />
              </a-form-item>

              <a-form-item label="手机号码" name="phoneNumber">
                <a-input
                  v-model:value="formData.phoneNumber"
                  placeholder="请输入手机号码"
                  :maxLength="11"
                  allow-clear
                />
              </a-form-item>

              <a-form-item label="邮箱地址" name="email">
                <a-input v-model:value="formData.email" placeholder="请输入邮箱地址" allow-clear />
              </a-form-item>

              <a-form-item label="性别" name="gender">
                <a-radio-group v-model:value="formData.gender" button-style="solid">
                  <a-radio-button value="male">男</a-radio-button>
                  <a-radio-button value="female">女</a-radio-button>
                </a-radio-group>
              </a-form-item>

              <a-form-item label="生日" name="birthday">
                <a-date-picker
                  v-model:value="formData.birthday"
                  style="width: 100%"
                  :disabledDate="disabledDate"
                  format="YYYY-MM-DD"
                  placeholder="请选择生日"
                />
              </a-form-item>

              <a-divider orientation="left">会员权益</a-divider>

              <div class="member-benefits">
                <div class="benefit-item">
                  <a-tag color="#1890ff">折扣</a-tag>
                  <span>所有药品享受9折优惠</span>
                </div>
              </div>

              <a-form-item name="agreement" :wrapper-col="{ offset: 4, span: 16 }">
                <a-checkbox v-model:checked="formData.agreement">
                  我同意成为药店会员，享受会员价格及相关权益
                </a-checkbox>
              </a-form-item>

              <a-form-item :wrapper-col="{ offset: 4, span: 16 }">
                <a-button
                  type="primary"
                  :disabled="!formData.agreement"
                  @click="handleSubmit"
                  :loading="submitLoading"
                  class="submit-btn"
                >
                  提交注册
                </a-button>
              </a-form-item>
            </a-form>
          </div>
        </div>
      </a-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import axios from '@/utils/axios'
import dayjs from 'dayjs'

// 表单数据
const formData = reactive({
  name: '',
  phoneNumber: '',
  email: '',
  birthday: null,
  gender: '',
  agreement: false
})

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'change' },
    { max: 20, message: '姓名不能超过20个字符', trigger: 'change' }
  ],
  phoneNumber: [
    { required: true, message: '请输入手机号码', trigger: 'change' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'change' }
  ],
  email: [
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'change' }
  ]
}

const formRef = ref(null)
const loading = ref(true)
const submitLoading = ref(false)
const isMember = ref(false)

// 禁用未来日期
const disabledDate = (current) => {
  return current && current > dayjs().endOf('day')
}

// 获取用户会员信息
const fetchMemberInfo = async () => {
  loading.value = true
  try {
    const response = await axios.get('/member/current')
    if (response.code === 200) {
      const memberInfo = response.data
      // 如果已经是会员，设置isMember为true
      if (memberInfo.isRegistered) {
        isMember.value = true
      } else {
        // 预填充表单
        formData.name = memberInfo.name || ''
        formData.phoneNumber = memberInfo.phoneNumber || ''
        formData.email = memberInfo.email || ''
        if (memberInfo.birthday) {
          formData.birthday = dayjs(memberInfo.birthday)
        }
        formData.gender = memberInfo.gender || ''
      }
    }
  } catch (error) {
    if (error.response && error.response.status === 404) {
      // 用户还没有会员信息，不做任何处理
    } else {
      message.error('获取会员信息失败')
    }
  } finally {
    loading.value = false
  }
}

// 提交会员注册
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    
    if (!formData.agreement) {
      message.warning('请同意成为会员')
      return
    }
    
    submitLoading.value = true
    
    // 构建请求数据
    const requestData = {
      name: formData.name,
      phoneNumber: formData.phoneNumber,
      email: formData.email,
      birthday: formData.birthday ? formData.birthday.format('YYYY-MM-DD') : null,
      gender: formData.gender
    }
    
    // 发送请求注册会员
    const response = await axios.post('/member/register', requestData)
    
    if (response.code === 200) {
      message.success('会员注册成功')
      isMember.value = true
    } else {
      message.error(response.message || '会员注册失败')
    }
  } catch (error) {
    console.error('会员注册错误:', error)
    message.error('会员注册失败: ' + (error.errorFields?.[0]?.errors?.[0] || '未知错误'))
  } finally {
    submitLoading.value = false
  }
}

// 页面加载时获取会员信息
onMounted(() => {
  fetchMemberInfo()
})
</script>

<style lang="less" scoped>
.registration-container {
  padding: 24px;
  background-color: #f5f5f5;
  min-height: calc(100vh - 64px);

  .registration-content {
    display: flex;
    flex-direction: column;
    gap: 24px;
    max-width: 800px;
    margin: 0 auto;
  }

  .registration-card {
    background-color: #fff;
    border-radius: 8px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);

    :deep(.ant-card-head) {
      border-bottom: 1px solid #f0f0f0;
      padding: 16px 24px;

      .ant-card-head-title {
        font-size: 18px;
        font-weight: 500;
        color: #333;
      }
    }

    :deep(.ant-card-body) {
      padding: 24px;
    }
  }

  .loading-message {
    text-align: center;
    padding: 40px;
    font-size: 16px;
    color: #999;
  }

  .already-member {
    padding: 20px 0;
  }

  .member-benefits {
    background-color: #f9f9f9;
    padding: 16px;
    border-radius: 8px;
    margin-bottom: 24px;
    margin-left: 16.67%; // 对应form-item的offset

    .benefit-item {
      display: flex;
      align-items: center;

      :deep(.ant-tag) {
        margin-right: 12px;
      }

      span {
        color: #555;
      }
    }
  }

  :deep(.ant-divider) {
    margin: 24px 0;

    .ant-divider-inner-text {
      font-size: 16px;
      font-weight: 500;
      color: #333;
    }
  }

  .submit-btn {
    width: 120px;
  }

  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }
}
</style>