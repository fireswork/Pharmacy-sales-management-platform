<template>
  <div class="login-container">
    <!-- 替换原有的动画背景 -->
    <div class="animated-background">
      <div class="cube"></div>
      <div class="cube"></div>
      <div class="cube"></div>
      <div class="cube"></div>
      <div class="cube"></div>
      <div class="cube"></div>
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
          员工登录
        </div>

        <!-- 登录表单 -->
        <a-form
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
              员工登录
            </a-button>
          </a-form-item>
        </a-form>
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

const loginForm = reactive({
  username: '',
  password: '',
})

const handleLogin = async (values) => {
  try {
    loading.value = true
    const response = await axios.post('/auth/login', {
      username: values.username,
      password: values.password
    })

    if (response.code === 200) {
      // 检查用户角色
      const userRole = response.data.role
      if (userRole === 'EMPLOYEE' || userRole === 'ADMIN') {
        // 保存 token 和角色信息
        localStorage.setItem('access_token', response.data.access_token)
        localStorage.setItem('userRole', userRole)
        message.success('登录成功！')
        // 跳转到首页
        nextTick(() => {
          router.push('/home')
        })
      } else {
        message.error('您不是员工或管理员，无法登录！')
        localStorage.removeItem('access_token')
      }
    }
  } catch (error) {
    console.error('登录失败：', error)
    message.error(error.response?.data?.message || '登录失败，请重试！')
  } finally {
    loading.value = false
  }
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
  overflow: hidden;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  background: linear-gradient(135deg, #1a2980, #26d0ce);
}

// 新的酷炫动画背景
.animated-background {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;
}

.cube {
  position: absolute;
  top: 80vh;
  left: 45vw;
  width: 10px;
  height: 10px;
  border: solid 1px rgba(255, 255, 255, 0.5);
  transform-origin: top left;
  transform: scale(0) rotate(0deg) translate(-50%, -50%);
  animation: cube 12s ease-in forwards infinite;
  
  &:nth-child(2) {
    animation-delay: 2s;
    left: 25vw;
    top: 40vh;
  }
  
  &:nth-child(3) {
    animation-delay: 4s;
    left: 75vw;
    top: 50vh;
  }
  
  &:nth-child(4) {
    animation-delay: 6s;
    left: 90vw;
    top: 10vh;
  }
  
  &:nth-child(5) {
    animation-delay: 8s;
    left: 10vw;
    top: 85vh;
  }
  
  &:nth-child(6) {
    animation-delay: 10s;
    left: 50vw;
    top: 10vh;
  }
}

@keyframes cube {
  from {
    transform: scale(0) rotate(0deg) translate(-50%, -50%);
    opacity: 1;
    border-radius: 0;
  }
  to {
    transform: scale(20) rotate(960deg) translate(-50%, -50%);
    opacity: 0;
    border-radius: 50%;
  }
}

.login-content {
  position: relative;
  z-index: 1;
  
  .login-box {
    background: rgba(255, 255, 255, 0.95);
    padding: 40px;
    border-radius: 16px;
    box-shadow: 0 15px 30px rgba(0, 0, 0, 0.2);
    backdrop-filter: blur(10px);
    width: 400px;
    position: relative;
    z-index: 1;
    border: 1px solid rgba(255, 255, 255, 0.1);
    overflow: hidden;
    
    &:before {
      content: '';
      position: absolute;
      top: -50%;
      left: -50%;
      width: 200%;
      height: 200%;
      background: linear-gradient(
        to bottom right,
        rgba(255, 255, 255, 0),
        rgba(255, 255, 255, 0.1),
        rgba(255, 255, 255, 0)
      );
      transform: rotate(30deg);
      pointer-events: none;
      z-index: -1;
    }

    .system-title {
      text-align: center;
      margin-bottom: 48px;

      .logo-icon {
        width: 64px;
        height: 64px;
        color: #1890ff;
        margin-bottom: 16px;
        filter: drop-shadow(0 0 5px rgba(24, 144, 255, 0.5));
      }

      h1 {
        font-size: 28px;
        color: #1f1f1f;
        margin: 16px 0 0;
        font-weight: 600;
        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
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
      background: linear-gradient(135deg, #1890ff, #096dd9);
      border: none;
      transition: all 0.3s;
      
      &:hover {
        transform: translateY(-2px);
        box-shadow: 0 8px 16px rgba(24, 144, 255, 0.3);
      }
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
          text-decoration: underline;
        }
      }
    }
  }
}

:deep(.ant-input-affix-wrapper) {
  height: 44px;
  border-radius: 4px;
  transition: all 0.3s;
  
  &:hover {
    border-color: #40a9ff;
  }
  
  &:focus {
    box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
  }
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

// 响应式布局
@media (max-width: 576px) {
  .login-content {
    width: 90%;
    
    .login-box {
      width: 100%;
      padding: 25px;
    }
  }
}
</style> 