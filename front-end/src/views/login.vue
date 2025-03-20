<template>
  <div class="login-container">
    <div class="area">
      <ul class="circles">
        <li v-for="n in 10" :key="n"></li>
      </ul>
    </div>

    <div class="login-content">
      <div class="login-box">
        <div class="system-title animate__animated animate__fadeInDown animate__faster">
          <svg class="logo-icon" viewBox="0 0 24 24">
            <path
              fill="currentColor"
              d="M20,6H16V4A2,2 0 0,0 14,2H10A2,2 0 0,0 8,4V6H4C2.89,6 2,6.89 2,8V19A2,2 0 0,0 4,21H20A2,2 0 0,0 22,19V8C22,6.89 21.1,6 20,6M10,4H14V6H10V4M20,19H4V8H20V19M6,10H8V12H6V10M6,14H8V16H6V14M10,10H18V12H10V10M10,14H15V16H10V14Z"
            />
          </svg>
          <h1>药品管理系统</h1>
        </div>
        <div class="welcome-text animate__animated animate__fadeIn animate__faster">
          {{ isLogin ? '欢迎登录' : '用户注册' }}
        </div>

        <!-- 登录表单 -->
        <a-form
          v-if="isLogin"
          :model="loginForm"
          name="loginForm"
          @finish="handleLogin"
          autocomplete="off"
          class="animate__animated animate__fadeIn animate__faster"
        >
          <a-form-item name="username" :rules="[{ required: true, message: '请输入用户名!' }]">
            <a-input v-model:value="loginForm.username" :size="'large'" placeholder="请输入用户名">
              <template #prefix>
                <UserOutlined style="color: rgba(0, 0, 0, 0.25)" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item name="password" :rules="[{ required: true, message: '请输入密码!' }]">
            <a-input-password
              v-model:value="loginForm.password"
              :size="'large'"
              placeholder="请输入密码"
            >
              <template #prefix>
                <LockOutlined style="color: rgba(0, 0, 0, 0.25)" />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              :loading="loading"
              class="submit-button hover-effect"
            >
              登录
            </a-button>
          </a-form-item>
        </a-form>

        <!-- 注册表单 -->
        <a-form
          v-else
          :model="registerForm"
          name="registerForm"
          @finish="handleRegister"
          autocomplete="off"
          class="animate__animated animate__fadeIn animate__faster"
        >
          <a-form-item
            name="username"
            :rules="[
              { required: true, message: '请输入用户名!' },
              { min: 3, message: '用户名至少3个字符!' },
            ]"
          >
            <a-input
              v-model:value="registerForm.username"
              :size="'large'"
              placeholder="请输入用户名"
            >
              <template #prefix>
                <UserOutlined style="color: rgba(0, 0, 0, 0.25)" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item
            name="password"
            :rules="[
              { required: true, message: '请输入密码!' },
              { min: 8, message: '密码至少8个字符!' },
              { validator: validatePasswordComplexity }
            ]"
          >
            <a-input-password
              v-model:value="registerForm.password"
              :size="'large'"
              placeholder="请输入密码"
            >
              <template #prefix>
                <LockOutlined style="color: rgba(0, 0, 0, 0.25)" />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item
            name="confirmPassword"
            :rules="[
              { required: true, message: '请确认密码!' },
              { validator: validateConfirmPassword },
            ]"
          >
            <a-input-password
              v-model:value="registerForm.confirmPassword"
              :size="'large'"
              placeholder="请确认密码"
            >
              <template #prefix>
                <LockOutlined style="color: rgba(0, 0, 0, 0.25)" />
              </template>
            </a-input-password>
          </a-form-item>

          <a-form-item>
            <a-button
              type="primary"
              html-type="submit"
              :loading="loading"
              class="submit-button hover-effect"
            >
              注册
            </a-button>
          </a-form-item>
        </a-form>

        <!-- 切换按钮 -->
        <div class="toggle-container">
          <a @click="toggleForm">{{ isLogin ? '没有账号？立即注册' : '已有账号？立即登录' }}</a>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, nextTick, onMounted } from 'vue'
import { UserOutlined, LockOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import axios from '@/utils/axios'
import { useRouter, useRoute } from 'vue-router'
import 'animate.css'

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const isLogin = ref(true)

const loginForm = reactive({
  username: '',
  password: '',
})

const registerForm = reactive({
  username: '',
  password: '',
  confirmPassword: '',
})

const validateConfirmPassword = async (rule, value) => {
  if (value !== registerForm.password) {
    throw new Error('两次输入的密码不一致!')
  }
}

const validatePasswordComplexity = async (rule, value) => {
  // 检查密码长度
  if (value.length < 8) {
    throw new Error('密码长度至少为8个字符')
  }
  
  // 检查是否包含数字
  if (!/\d/.test(value)) {
    throw new Error('密码必须包含至少一个数字')
  }
  
}

const handleLogin = async (values) => {
  try {
    loading.value = true
    const response = await axios.post('/auth/login', {
      username: values.username,
      password: values.password
    })
    console.log(response)

    if (response.code === 200) {
      // 保存 token
      localStorage.setItem('access_token', response.data.access_token)
      message.success('登录成功！')
      // 跳转到首页
      nextTick(() => {
        router.push('/home/products')
      })
    }
  } catch (error) {
    console.error('登录失败：', error)
    message.error(error.response?.data?.message || '登录失败，请重试！')
  } finally {
    loading.value = false
  }
}

const handleRegister = async (values) => {
  try {
    // 先验证两次密码是否一致
    if (values.password !== registerForm.confirmPassword) {
      message.error('两次输入的密码不一致！')
      return
    }

    loading.value = true
    const response = await axios.post('/auth/register', {
      username: values.username,
      password: values.password
    })

    if (response.code === 200) {
      message.success('注册成功！')
      
      // 使用 nextTick 确保状态更新完成
      await nextTick(() => {
        // 清空注册表单
        registerForm.username = ''
        registerForm.password = ''
        registerForm.confirmPassword = ''
        
        // 自动填充登录表单
        loginForm.username = values.username
        
        // 最后切换到登录界面
        isLogin.value = true
      })
    }
  } catch (error) {
    console.error('注册失败：', error)
    if (error.response?.data?.message) {
      message.error(error.response.data.message)
    } else {
      message.error('注册失败，请重试！')
    }
  } finally {
    loading.value = false
  }
}

const toggleForm = () => {
  // 清空登录表单
  if (!isLogin.value) {
    loginForm.username = ''
    loginForm.password = ''
  }
  
  // 清空注册表单
  if (isLogin.value) {
    registerForm.username = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
  }
  
  // 切换表单状态
  isLogin.value = !isLogin.value
}

onMounted(() => {
  // 检查是否是从密码修改页面跳转过来的
  if (route.query.passwordChanged === 'true') {
    message.success('密码已修改，请使用新密码登录')
  }
})
</script>

<style lang="less" scoped>
.login-container {
  height: 100vh;
  width: 100vw;
  background: linear-gradient(135deg, #1a2a6c 0%, #b21f1f 50%, #fdbb2d 100%);
  display: flex;
  justify-content: center;
  align-items: center;
  position: relative;
  overflow: hidden;

  .login-content {
    .login-box {
      background: rgba(255, 255, 255, 0.95);
      padding: 40px;
      border-radius: 16px;
      box-shadow: 0 8px 32px rgba(0, 0, 0, 0.1);
      backdrop-filter: blur(10px);
      width: 400px;
      position: relative;
      z-index: 1;

      .system-title {
        text-align: center;
        margin-bottom: 48px;

        .logo-icon {
          width: 64px;
          height: 64px;
          color: #1890ff;
          margin-bottom: 16px;
        }

        h1 {
          font-size: 28px;
          color: #1f1f1f;
          margin: 16px 0 0;
          font-weight: 600;
        }
      }

      .welcome-text {
        font-size: 16px;
        color: #666;
        text-align: center;
        margin-bottom: 32px;
      }

      .submit-button {
        width: 100%;
        height: 44px;
        font-size: 16px;
        margin-top: 24px;
        border-radius: 4px;
      }

      .toggle-container {
        text-align: center;
        margin-top: 16px;

        a {
          color: #1890ff;
          cursor: pointer;
          transition: color 0.3s;

          &:hover {
            color: #40a9ff;
          }
        }
      }

      .password-rules {
        margin-top: 16px;
        padding: 12px;
        background-color: #f5f5f5;
        border-radius: 4px;
        font-size: 12px;
        color: #666;

        .rules-title {
          margin-bottom: 8px;
          font-weight: 500;
        }

        ul {
          margin: 0;
          padding-left: 20px;

          li {
            margin-bottom: 4px;
            
            &:last-child {
              margin-bottom: 0;
            }
          }
        }
      }
    }
  }
}

.hover-effect {
  transition: all 0.3s ease;
  &:hover {
    transform: translateY(-2px);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  }
}

// 保持原有的动画背景样式
.area {
  position: absolute;
  width: 100%;
  height: 100%;
}

.circles {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  overflow: hidden;
  margin: 0;
  padding: 0;
}

.circles li {
  position: absolute;
  display: block;
  list-style: none;
  width: 20px;
  height: 20px;
  background: rgba(255, 255, 255, 0.2);
  animation: animate 25s linear infinite;
  bottom: -150px;
  border-radius: 50%;
}

.circles li:nth-child(1) {
  left: 25%;
  width: 80px;
  height: 80px;
  animation-delay: 0s;
}

.circles li:nth-child(2) {
  left: 10%;
  width: 20px;
  height: 20px;
  animation-delay: 2s;
  animation-duration: 12s;
}

.circles li:nth-child(3) {
  left: 70%;
  width: 20px;
  height: 20px;
  animation-delay: 4s;
}

.circles li:nth-child(4) {
  left: 40%;
  width: 60px;
  height: 60px;
  animation-delay: 0s;
  animation-duration: 18s;
}

.circles li:nth-child(5) {
  left: 65%;
  width: 20px;
  height: 20px;
  animation-delay: 0s;
}

@keyframes animate {
  0% {
    transform: translateY(0) rotate(0deg);
    opacity: 1;
    border-radius: 0;
  }
  100% {
    transform: translateY(-1000px) rotate(720deg);
    opacity: 0;
    border-radius: 50%;
  }
}

:deep(.ant-input-affix-wrapper) {
  height: 44px;
  border-radius: 4px;
}

:deep(.ant-input) {
  font-size: 14px;
}

:deep(.ant-form-item) {
  margin-bottom: 24px;
}

:deep(.ant-input-prefix) {
  margin-right: 8px;
}
</style>
