<template>
  <div class="profile-container">
    <div class="profile-content">
      <!-- 基本信息卡片 -->
      <a-card title="基本信息" class="profile-card" :bordered="false">
        <a-descriptions :column="1" bordered>
          <a-descriptions-item label="用户名">
            <span class="info-value">{{ userInfo.username }}</span>
          </a-descriptions-item>
          <a-descriptions-item label="密码">
            <span class="password-dots">••••••••</span>
            <a-button type="primary" size="small" @click="showPasswordModal" class="password-btn">
              修改密码
            </a-button>
          </a-descriptions-item>
          <a-descriptions-item label="角色">
            <a-tag :color="getRoleColor(userInfo.role)">
              {{ getRoleText(userInfo.role.toLowerCase()) }}
            </a-tag>
          </a-descriptions-item>
        </a-descriptions>
      </a-card>

      <!-- 用户信息卡片 -->
      <a-card title="用户信息" class="profile-card" :bordered="false" v-if="userInfo.role?.toUpperCase() !== 'ADMIN'">
        <template #extra>
          <a-button type="primary" @click="handleSubmit" :loading="loading" class="submit-btn">
            保存修改
          </a-button>
        </template>
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

          <a-form-item label="生日" name="birthday" v-if="userInfo.role?.toUpperCase() === 'USER'">
            <a-date-picker
              v-model:value="formData.birthday"
              style="width: 100%"
              :disabledDate="disabledDate"
              format="YYYY-MM-DD"
              placeholder="请选择生日"
            />
          </a-form-item>
          
          <!-- 员工特有信息 -->
          <template v-if="userInfo.role?.toUpperCase() === 'EMPLOYEE'">
            <a-form-item label="员工编号">
              <span class="info-value">{{ employeeInfo.code || '无' }}</span>
            </a-form-item>
            
            <a-form-item label="所属门店">
              <span class="info-value">{{ employeeInfo.store?.name || '未分配' }}</span>
            </a-form-item>
            
            <a-form-item label="入职时间">
              <span class="info-value">{{ employeeInfo.hireDate || '无' }}</span>
            </a-form-item>
            
            <a-form-item label="员工状态">
              <a-tag :color="employeeInfo.status === '在职' ? 'green' : 'default'">
                {{ employeeInfo.status || '无' }}
              </a-tag>
            </a-form-item>
          </template>

          <!-- 会员信息部分，仅对会员用户显示 -->
          <template v-if="userInfo.role?.toUpperCase() === 'USER'">
            <a-divider orientation="left">会员信息</a-divider>

            <a-form-item label="会员编号">
              <span class="info-value">{{ memberInfo.memberId || '无' }}</span>
            </a-form-item>

            <a-form-item label="会员等级">
              <a-tag :color="getMemberLevelColor(memberInfo.memberLevel)">
                {{ getMemberLevelText(memberInfo.memberLevel) || '无' }}
              </a-tag>
            </a-form-item>

            <a-form-item label="当前积分">
              <span class="info-value points">{{ memberInfo.points || 0 }}</span>
            </a-form-item>

            <a-form-item label="累计消费">
              <span class="info-value spending"
                >¥{{ memberInfo.totalSpending ? memberInfo.totalSpending.toFixed(2) : '0.00' }}</span
              >
            </a-form-item>

            <a-form-item label="注册时间">
              <span class="info-value">{{
                memberInfo.registrationTime ? formatDate(memberInfo.registrationTime) : '无'
              }}</span>
            </a-form-item>

            <a-form-item label="账号状态">
              <a-tag :color="memberInfo.status === '正常' ? 'success' : 'default'">
                {{ memberInfo.status || '无' }}
              </a-tag>
            </a-form-item>
          </template>
        </a-form>
      </a-card>
    </div>

    <!-- 修改密码弹窗 -->
    <a-modal
      v-model:visible="passwordModalVisible"
      title="修改密码"
      @ok="handlePasswordChange"
      :confirmLoading="passwordLoading"
      okText="确定"
      cancelText="取消"
      :maskClosable="false"
      width="500px"
    >
      <a-form
        ref="passwordFormRef"
        :model="passwordForm"
        :rules="passwordRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="当前密码" name="oldPassword">
          <a-input-password
            v-model:value="passwordForm.oldPassword"
            placeholder="请输入当前密码"
            :maxLength="20"
          />
        </a-form-item>

        <a-form-item label="新密码" name="newPassword">
          <a-input-password
            v-model:value="passwordForm.newPassword"
            placeholder="请输入新密码"
            :maxLength="20"
          />
        </a-form-item>

        <a-form-item label="确认新密码" name="confirmPassword">
          <a-input-password
            v-model:value="passwordForm.confirmPassword"
            placeholder="请再次输入新密码"
            :maxLength="20"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import axios from '../../utils/axios'
import dayjs from 'dayjs'
import { useRouter } from 'vue-router'

// 获取路由实例
const router = useRouter()

// 用户基本信息
const userInfo = ref({
  username: '',
  role: ''
})

// 会员信息
const memberInfo = ref({
  memberId: '',
  name: '',
  phoneNumber: '',
  email: '',
  birthday: '',
  gender: '',
  memberLevel: '',
  points: 0,
  totalSpending: 0,
  registrationTime: '',
  status: ''
})

// 员工信息
const employeeInfo = ref({
  code: '',
  name: '',
  phoneNumber: '',
  email: '',
  store: {
    id: '',
    name: ''
  },
  hireDate: '',
  status: ''
})

// 表单数据
const formData = reactive({
  name: '',
  phoneNumber: '',
  email: '',
  gender: '',
  birthday: null
})

// 表单引用
const formRef = ref(null)

// 加载状态
const loading = ref(false)

// 表单验证规则
const rules = {
  name: [
    { required: true, message: '请输入姓名', trigger: 'blur' },
    { max: 20, message: '姓名不能超过20个字符', trigger: 'blur' }
  ],
  phoneNumber: [{ pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号码', trigger: 'blur' }],
  email: [{ type: 'email', message: '请输入正确的邮箱地址', trigger: 'blur' }]
}

// 密码相关
const passwordModalVisible = ref(false)
const passwordLoading = ref(false)
const passwordFormRef = ref(null)
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

// 密码验证规则
const passwordRules = {
  oldPassword: [{ required: true, message: '请输入当前密码', trigger: 'change' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'change' },
    { min: 6, message: '密码长度至少6位', trigger: 'change' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'change' },
    {
      validator: (rule, value) => {
        if (value !== passwordForm.newPassword) {
          return Promise.reject('两次输入的密码不一致')
        }
        return Promise.resolve()
      },
      trigger: 'change'
    }
  ]
}

// 获取会员等级文本
const getMemberLevelText = (level) => {
  const levelMap = {
    bronze: '青铜会员',
    silver: '白银会员',
    gold: '黄金会员',
    platinum: '铂金会员'
  }
  return levelMap[level] || '普通会员'
}

// 获取会员等级颜色
const getMemberLevelColor = (level) => {
  const colorMap = {
    bronze: '#cd7f32',
    silver: '#c0c0c0',
    gold: '#ffd700',
    platinum: '#e5e4e2'
  }
  return colorMap[level] || 'default'
}

// 获取角色文本
const getRoleText = (role) => {
  const roleMap = {
    admin: '管理员',
    user: '普通用户',
    employee: '员工'
  }
  return roleMap[role] || '未知角色'
}

// 获取角色颜色
const getRoleColor = (role) => {
  const colorMap = {
    admin: 'red',
    user: 'blue',
    staff: 'green'
  }
  return colorMap[role] || 'default'
}

// 显示修改密码弹窗
const showPasswordModal = () => {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordModalVisible.value = true
}

// 日期格式化
const formatDate = (dateStr) => {
  return dayjs(dateStr).format('YYYY-MM-DD')
}

// 禁用未来日期
const disabledDate = (current) => {
  return current && current > dayjs().endOf('day')
}

// 获取用户基本信息
const fetchUserInfo = async () => {
  try {
    const response = await axios.get('/user')

    if (response.code === 200) {
      userInfo.value = response.data
      
      // 根据用户角色获取不同的信息
      if (userInfo.value.role?.toUpperCase() === 'EMPLOYEE') {
        fetchEmployeeInfo()
      } else if (userInfo.value.role?.toUpperCase() === 'USER') {
        fetchMemberInfo()
      } else {
        // 管理员或其他角色不需要额外获取信息
      }
    }
  } catch (error) {
    console.error('获取用户信息失败:', error)
    message.error('获取用户信息失败')
  }
}

// 获取会员信息
const fetchMemberInfo = async () => {
  try {
    const response = await axios.get('/member')

    if (response.code === 200) {
      memberInfo.value = response.data.content?.[0]

      // 填充表单数据
      formData.name = memberInfo.value.name || ''
      formData.phoneNumber = memberInfo.value.phoneNumber || ''
      formData.email = memberInfo.value.email || ''
      formData.gender = memberInfo.value.gender || ''

      if (memberInfo.value.birthday) {
        formData.birthday = dayjs(memberInfo.value.birthday)
      }
    }
  } catch (error) {
    console.error('获取会员信息失败:', error)
    // 如果是404错误，说明用户没有会员信息，这是正常的
    if (error.response && error.response.code !== 404) {
      message.error('获取会员信息失败')
    }
  }
}

// 获取员工信息
const fetchEmployeeInfo = async () => {
  try {
    const response = await axios.get('/employee/current')

    if (response.code === 200) {
      employeeInfo.value = response.data

      // 填充表单数据
      formData.name = employeeInfo.value.name || ''
      formData.phoneNumber = employeeInfo.value.phoneNumber || ''
      formData.email = employeeInfo.value.email || ''
      formData.gender = employeeInfo.value.gender || ''
    }
  } catch (error) {
    console.error('获取员工信息失败:', error)
    message.error('获取员工信息失败')
  }
}

// 提交表单
const handleSubmit = async () => {
  try {
    await formRef.value.validate()
    loading.value = true

    const updateData = {
      name: formData.name,
      phoneNumber: formData.phoneNumber,
      email: formData.email,
      gender: formData.gender,
      birthday: formData.birthday ? formData.birthday.format('YYYY-MM-DD') : null
    }

    let response
    if (userInfo.value.role?.toUpperCase() === 'EMPLOYEE') {
      response = await axios.put('/employee/current', updateData)
    } else if (userInfo.value.role?.toUpperCase() === 'USER') {
      response = await axios.put('/member/current', updateData)
    } else {
      throw new Error('不支持的用户角色')
    }

    if (response.code === 200) {
      message.success(userInfo.value.role?.toUpperCase() === 'EMPLOYEE' ? '员工信息更新成功' : '会员信息更新成功')
      
      // 根据角色重新获取信息
      if (userInfo.value.role?.toUpperCase() === 'EMPLOYEE') {
        fetchEmployeeInfo()
      } else if (userInfo.value.role?.toUpperCase() === 'USER') {
        fetchMemberInfo()
      }
    }
  } catch (error) {
    console.error('更新信息失败:', error)
    message.error(error.response?.data?.message || '更新信息失败')
  } finally {
    loading.value = false
  }
}

// 修改密码
const handlePasswordChange = async () => {
  try {
    await passwordFormRef.value.validate()
    passwordLoading.value = true

    const response = await axios.put('/user/password', {
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })

    if (response.code === 200) {
      message.success('密码修改成功，请使用新密码重新登录')
      passwordModalVisible.value = false

      // 清空表单
      passwordForm.oldPassword = ''
      passwordForm.newPassword = ''
      passwordForm.confirmPassword = ''

      // 延迟一秒后执行退出登录操作
      setTimeout(() => {
        // 清除本地存储的 token 和用户信息
        localStorage.removeItem('access_token')
        localStorage.removeItem('user_info')

        // 跳转到登录页面
        router.push('/login')
      }, 1000)
    }
  } catch (error) {
    console.error('修改密码失败:', error)
    message.error(error.response?.data?.message || '修改密码失败')
  } finally {
    passwordLoading.value = false
  }
}

// 组件挂载时获取用户信息
onMounted(() => {
  fetchUserInfo()
})
</script>

<style lang="less" scoped>
.profile-container {
  padding: 24px;
  background-color: #f5f5f5;
  min-height: calc(100vh - 64px);

  .profile-header {
    margin-bottom: 24px;

    h1 {
      font-size: 24px;
      font-weight: 500;
      margin-bottom: 8px;
      color: #333;
    }

    p {
      color: #666;
      font-size: 14px;
    }
  }

  .profile-content {
    display: flex;
    flex-direction: column;
    gap: 24px;
  }

  .profile-card {
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

  .info-value {
    font-size: 14px;
    color: #333;

    &.points {
      color: #fa8c16;
      font-weight: 500;
    }

    &.spending {
      color: #52c41a;
      font-weight: 500;
    }
  }

  .password-dots {
    color: #333;
    letter-spacing: 2px;
    margin-right: 16px;
  }

  .password-btn {
    margin-left: 16px;
  }

  .submit-btn {
    width: 120px;
  }

  :deep(.ant-descriptions-item-label) {
    width: 120px;
    background-color: #fafafa;
  }

  :deep(.ant-form-item) {
    margin-bottom: 24px;
  }

  :deep(.ant-divider) {
    margin: 24px 0;

    .ant-divider-inner-text {
      font-size: 16px;
      font-weight: 500;
      color: #333;
    }
  }

  .password-tip {
    font-size: 12px;
    color: #999;
    margin-top: 4px;
  }
}
</style>
