<template>
  <div class="login-container">
    <!-- 动态背景 -->
    <div class="dynamic-background">
      <div class="gradient-bg"></div>
      <div class="stars">
        <div v-for="n in 20" :key="`star-${n}`" class="star"></div>
      </div>
    </div>

    <!-- 粒子动画 -->
    <div class="particles">
      <div v-for="n in 15" :key="`particle-${n}`" class="particle"></div>
    </div>

    <div class="login-content">
      <div class="login-box">
        <!-- 彩色光晕 -->
        <div class="glow"></div>

        <div
          class="system-title animate__animated animate__fadeInDown animate__faster"
        >
          <svg class="logo-icon" viewBox="0 0 24 24">
            <path
              fill="currentColor"
              d="M20,6H16V4A2,2 0 0,0 14,2H10A2,2 0 0,0 8,4V6H4C2.89,6 2,6.89 2,8V19A2,2 0 0,0 4,21H20A2,2 0 0,0 22,19V8C22,6.89 21.1,6 20,6M10,4H14V6H10V4M20,19H4V8H20V19M6,10H8V12H6V10M6,14H8V16H6V14M10,10H18V12H10V10M10,14H15V16H10V14Z"
            />
          </svg>
          <h1 class="text-gradient">药品商城</h1>
        </div>
        <div
          class="welcome-text animate__animated animate__fadeIn animate__faster"
        >
          <span class="text-shine">{{
            isLogin ? "欢迎登录" : "用户注册"
          }}</span>
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
          <a-form-item
            name="username"
            :rules="[{ required: true, message: '请输入用户名!' }]"
          >
            <a-input
              v-model:value="loginForm.username"
              :size="'large'"
              placeholder="请输入用户名"
              class="hover-input"
            >
              <template #prefix>
                <UserOutlined style="color: rgba(0, 0, 0, 0.25)" />
              </template>
            </a-input>
          </a-form-item>

          <a-form-item
            name="password"
            :rules="[{ required: true, message: '请输入密码!' }]"
          >
            <a-input-password
              v-model:value="loginForm.password"
              :size="'large'"
              placeholder="请输入密码"
              class="hover-input"
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
              class="submit-button"
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
              class="hover-input"
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
              { min: 6, message: '密码至少6个字符!' },
              { validator: validatePasswordComplexity },
            ]"
          >
            <a-input-password
              v-model:value="registerForm.password"
              :size="'large'"
              placeholder="请输入密码"
              class="hover-input"
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
              class="hover-input"
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
              class="submit-button"
            >
              注册
            </a-button>
          </a-form-item>
        </a-form>

        <!-- 切换按钮 -->
        <div class="toggle-container">
          <a @click="toggleForm" class="text-link">{{
            isLogin ? "没有账号？立即注册" : "已有账号？立即登录"
          }}</a>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { reactive, ref, nextTick, onMounted } from "vue";
import { UserOutlined, LockOutlined } from "@ant-design/icons-vue";
import { message } from "ant-design-vue";
import axios from "@/utils/axios";
import { useRouter, useRoute } from "vue-router";
import "animate.css";

const router = useRouter();
const route = useRoute();
const loading = ref(false);
const isLogin = ref(true);

const loginForm = reactive({
  username: "",
  password: "",
});

const registerForm = reactive({
  username: "",
  password: "",
  confirmPassword: "",
});

const validateConfirmPassword = async (rule, value) => {
  if (value !== registerForm.password) {
    throw new Error("两次输入的密码不一致!");
  }
};

const validatePasswordComplexity = async (rule, value) => {
  // 检查密码长度
  if (value.length < 6) {
    throw new Error("密码长度至少为6个字符");
  }

  // 检查是否包含数字
  if (!/\d/.test(value)) {
    throw new Error("密码必须包含至少一个数字");
  }
};

const handleLogin = async (values) => {
  try {
    loading.value = true;
    const response = await axios.post("/auth/login", {
      username: values.username,
      password: values.password,
    });

    if (response.code === 200) {
      // 检查用户角色
      const userRole = response.data.role;
      if (userRole === "USER") {
        // 保存 token 和角色信息
        localStorage.setItem("access_token", response.data.access_token);
        localStorage.setItem("userRole", userRole);
        message.success("登录成功！");
        // 跳转到首页
        nextTick(() => {
          router.push("/home");
        });
      } else {
        message.error("此入口仅供普通用户登录，请使用员工/管理员登录入口！");
        localStorage.removeItem("access_token");
      }
    }
  } catch (error) {
    console.error("登录失败：", error);
    message.error(error.response?.data?.message || "登录失败，请重试！");
  } finally {
    loading.value = false;
  }
};

const handleRegister = async (values) => {
  try {
    // 先验证两次密码是否一致
    if (values.password !== registerForm.confirmPassword) {
      message.error("两次输入的密码不一致！");
      return;
    }

    loading.value = true;
    const response = await axios.post("/auth/register", {
      username: values.username,
      password: values.password,
    });

    if (response.code === 200) {
      message.success("注册成功！");

      // 使用 nextTick 确保状态更新完成
      await nextTick(() => {
        // 清空注册表单
        registerForm.username = "";
        registerForm.password = "";
        registerForm.confirmPassword = "";

        // 自动填充登录表单
        loginForm.username = values.username;

        // 最后切换到登录界面
        isLogin.value = true;
      });
    }
  } catch (error) {
    console.error("注册失败：", error);
    if (error.response?.data?.message) {
      message.error(error.response.data.message);
    } else {
      message.error("注册失败，请重试！");
    }
  } finally {
    loading.value = false;
  }
};

const toggleForm = () => {
  // 清空登录表单
  if (!isLogin.value) {
    loginForm.username = "";
    loginForm.password = "";
  }

  // 清空注册表单
  if (isLogin.value) {
    registerForm.username = "";
    registerForm.password = "";
    registerForm.confirmPassword = "";
  }

  // 切换表单状态
  isLogin.value = !isLogin.value;
};

onMounted(() => {
  // 检查是否是从密码修改页面跳转过来的
  if (route.query.passwordChanged === "true") {
    message.success("密码已修改，请使用新密码登录");
  }
});
</script>

<style lang="less" scoped>
@keyframes floating {
  0% {
    transform: translateY(0px);
  }
  50% {
    transform: translateY(-15px);
  }
  100% {
    transform: translateY(0px);
  }
}

@keyframes pulse {
  0% {
    transform: scale(1);
  }
  50% {
    transform: scale(1.05);
  }
  100% {
    transform: scale(1);
  }
}

@keyframes gradientAnimation {
  0% {
    background-position: 0% 50%;
  }
  50% {
    background-position: 100% 50%;
  }
  100% {
    background-position: 0% 50%;
  }
}

@keyframes star-animation {
  0% {
    opacity: 0;
    transform: scale(0);
  }
  50% {
    opacity: 1;
    transform: scale(1.2);
  }
  100% {
    opacity: 0;
    transform: scale(0);
  }
}

@keyframes particle-animation {
  0% {
    transform: translate(0, 0) rotate(0deg);
    opacity: 0;
  }
  10% {
    opacity: 1;
  }
  90% {
    opacity: 1;
  }
  100% {
    transform: translate(var(--x, 100px), var(--y, 100px)) rotate(var(--r, 360deg));
    opacity: 0;
  }
}

@keyframes text-shine {
  0% {
    background-position: 0% 50%;
  }
  100% {
    background-position: 100% 50%;
  }
}

.login-container {
  height: 100vh;
  width: 100vw;
  overflow: hidden;
  position: relative;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #121212;
}

// 动态背景
.dynamic-background {
  position: absolute;
  width: 100%;
  height: 100%;
  overflow: hidden;
  z-index: 0;

  .gradient-bg {
    position: absolute;
    top: -50%;
    left: -50%;
    width: 200%;
    height: 200%;
    background: linear-gradient(-45deg, #ee7752, #e73c7e, #23a6d5, #23d5ab);
    background-size: 400% 400%;
    animation: gradientAnimation 15s ease infinite;
    opacity: 0.7;
    transform: rotate(30deg);
  }
}

// 星星效果
.stars {
  position: absolute;
  width: 100%;
  height: 100%;

  .star {
    position: absolute;
    width: 3px;
    height: 3px;
    border-radius: 50%;
    background: white;
    opacity: 0;

    &:nth-child(1) { top: 5%; left: 10%; width: 3px; height: 3px; animation: star-animation 3s ease-in-out 0s infinite; }
    &:nth-child(2) { top: 15%; left: 20%; width: 2px; height: 2px; animation: star-animation 3.5s ease-in-out 0.2s infinite; }
    &:nth-child(3) { top: 25%; left: 15%; width: 4px; height: 4px; animation: star-animation 4s ease-in-out 0.5s infinite; }
    &:nth-child(4) { top: 10%; left: 30%; width: 2px; height: 2px; animation: star-animation 3s ease-in-out 0.7s infinite; }
    &:nth-child(5) { top: 20%; left: 40%; width: 3px; height: 3px; animation: star-animation 4.5s ease-in-out 1s infinite; }
    &:nth-child(6) { top: 5%; left: 50%; width: 2px; height: 2px; animation: star-animation 3.8s ease-in-out 1.2s infinite; }
    &:nth-child(7) { top: 15%; left: 60%; width: 4px; height: 4px; animation: star-animation 4.2s ease-in-out 0.4s infinite; }
    &:nth-child(8) { top: 25%; left: 70%; width: 3px; height: 3px; animation: star-animation 3.5s ease-in-out 0.6s infinite; }
    &:nth-child(9) { top: 10%; left: 80%; width: 2px; height: 2px; animation: star-animation 4s ease-in-out 0.9s infinite; }
    &:nth-child(10) { top: 20%; left: 90%; width: 3px; height: 3px; animation: star-animation 3.2s ease-in-out 1.1s infinite; }
    &:nth-child(11) { top: 30%; left: 5%; width: 2px; height: 2px; animation: star-animation 3.7s ease-in-out 0.3s infinite; }
    &:nth-child(12) { top: 40%; left: 15%; width: 4px; height: 4px; animation: star-animation 4.4s ease-in-out 0.8s infinite; }
    &:nth-child(13) { top: 50%; left: 25%; width: 3px; height: 3px; animation: star-animation 3.9s ease-in-out 1.3s infinite; }
    &:nth-child(14) { top: 60%; left: 35%; width: 2px; height: 2px; animation: star-animation 4.1s ease-in-out 0.1s infinite; }
    &:nth-child(15) { top: 70%; left: 45%; width: 3px; height: 3px; animation: star-animation 3.6s ease-in-out 1.4s infinite; }
    &:nth-child(16) { top: 80%; left: 55%; width: 4px; height: 4px; animation: star-animation 4.3s ease-in-out 0.6s infinite; }
    &:nth-child(17) { top: 90%; left: 65%; width: 2px; height: 2px; animation: star-animation 3.8s ease-in-out 1.5s infinite; }
    &:nth-child(18) { top: 85%; left: 75%; width: 3px; height: 3px; animation: star-animation 4.2s ease-in-out 0.4s infinite; }
    &:nth-child(19) { top: 75%; left: 85%; width: 2px; height: 2px; animation: star-animation 3.4s ease-in-out 0.9s infinite; }
    &:nth-child(20) { top: 65%; left: 95%; width: 4px; height: 4px; animation: star-animation 4.5s ease-in-out 0.2s infinite; }
  }
}

// 粒子效果
.particles {
  position: absolute;
  width: 100%;
  height: 100%;

  .particle {
    position: absolute;
    width: 10px;
    height: 10px;
    border-radius: 50%;
    background: radial-gradient(
      circle,
      rgba(255, 255, 255, 1) 0%,
      rgba(255, 255, 255, 0) 70%
    );

    &:nth-child(1) { top: 10%; left: 20%; --x: 100px; --y: -150px; --r: 180deg; animation: particle-animation 20s ease-in-out 0s infinite; }
    &:nth-child(2) { top: 20%; left: 80%; --x: -150px; --y: 120px; --r: 270deg; animation: particle-animation 22s ease-in-out 1s infinite; }
    &:nth-child(3) { top: 30%; left: 45%; --x: 120px; --y: 100px; --r: 360deg; animation: particle-animation 18s ease-in-out 0.5s infinite; }
    &:nth-child(4) { top: 70%; left: 10%; --x: 200px; --y: -100px; --r: 90deg; animation: particle-animation 24s ease-in-out 0.2s infinite; }
    &:nth-child(5) { top: 65%; left: 70%; --x: -100px; --y: -120px; --r: 225deg; animation: particle-animation 21s ease-in-out 1.5s infinite; }
    &:nth-child(6) { top: 15%; left: 60%; --x: -120px; --y: 150px; --r: 315deg; animation: particle-animation 19s ease-in-out 2s infinite; }
    &:nth-child(7) { top: 55%; left: 30%; --x: 150px; --y: 200px; --r: 45deg; animation: particle-animation 23s ease-in-out 0.8s infinite; }
    &:nth-child(8) { top: 40%; left: 15%; --x: 180px; --y: -200px; --r: 135deg; animation: particle-animation 20s ease-in-out 1.2s infinite; }
    &:nth-child(9) { top: 85%; left: 55%; --x: -200px; --y: -180px; --r: 270deg; animation: particle-animation 22s ease-in-out 0s infinite; }
    &:nth-child(10) { top: 25%; left: 40%; --x: 150px; --y: 180px; --r: 180deg; animation: particle-animation 25s ease-in-out 1.8s infinite; }
    &:nth-child(11) { top: 60%; left: 85%; --x: -180px; --y: -150px; --r: 90deg; animation: particle-animation 19s ease-in-out 0.3s infinite; }
    &:nth-child(12) { top: 75%; left: 25%; --x: 200px; --y: -130px; --r: 225deg; animation: particle-animation 21s ease-in-out 1.7s infinite; }
    &:nth-child(13) { top: 35%; left: 65%; --x: -130px; --y: 200px; --r: 315deg; animation: particle-animation 24s ease-in-out 0.6s infinite; }
    &:nth-child(14) { top: 45%; left: 50%; --x: 170px; --y: 190px; --r: 45deg; animation: particle-animation 23s ease-in-out 1.3s infinite; }
    &:nth-child(15) { top: 80%; left: 35%; --x: -190px; --y: -170px; --r: 135deg; animation: particle-animation 20s ease-in-out 0.9s infinite; }
  }
}

.login-content {
  position: relative;
  z-index: 1;
  // animation: floating 6s ease-in-out infinite;

  .login-box {
    background: rgba(255, 255, 255, 0.92);
    padding: 40px;
    border-radius: 24px;
    box-shadow:
      0 20px 50px rgba(0, 0, 0, 0.3),
      0 10px 20px rgba(0, 0, 0, 0.2);
    backdrop-filter: blur(20px);
    width: 400px;
    position: relative;
    z-index: 1;
    border: 1px solid rgba(255, 255, 255, 0.2);
    overflow: hidden;
    transform-style: preserve-3d;
    perspective: 1000px;

    // 彩色光晕
    .glow {
      position: absolute;
      width: 100%;
      height: 100%;
      top: 0;
      left: 0;
      background: radial-gradient(
        circle at 50% 0%,
        rgba(255, 255, 255, 0.5),
        transparent 70%
      );
      filter: blur(20px);
      z-index: -1;
    }

    &:before {
      content: "";
      position: absolute;
      top: -100%;
      left: -100%;
      width: 300%;
      height: 300%;
      background: linear-gradient(
        to bottom right,
        rgba(255, 255, 255, 0),
        rgba(255, 255, 255, 0.1),
        rgba(255, 255, 255, 0)
      );
      transform: rotate(30deg);
      pointer-events: none;
      z-index: -1;
      animation: gradientAnimation 8s linear infinite;
    }

    .system-title {
      text-align: center;
      margin-bottom: 48px;

      .logo-icon {
        width: 72px;
        height: 72px;
        color: #1890ff;
        margin-bottom: 16px;
        filter: drop-shadow(0 0 10px rgba(24, 144, 255, 0.8));
        animation: pulse 3s ease-in-out infinite;
      }

      h1 {
        font-size: 32px;
        margin: 16px 0 0;
        font-weight: 600;
        text-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }

      .text-gradient {
        background: linear-gradient(90deg, #ff00cc, #3333dd, #00dbde);
        background-size: 300% 100%;
        -webkit-background-clip: text;
        -webkit-text-fill-color: transparent;
        animation: gradientAnimation 8s linear infinite;
      }
    }

    .welcome-text {
      font-size: 18px;
      color: #666;
      text-align: center;
      margin-bottom: 32px;

      .text-shine {
        background: linear-gradient(90deg, #333, #666, #333);
        background-size: 200% 100%;
        -webkit-background-clip: text;
        color: transparent;
        animation: text-shine 3s ease-in-out infinite;
      }
    }

    .submit-button {
      width: 100%;
      height: 46px;
      font-size: 16px;
      margin-top: 24px;
      border-radius: 8px;
      background: linear-gradient(135deg, #5b2de4, #2e99e4);
      border: none;
      transition: all 0.3s;
      position: relative;
      overflow: hidden;

      &:before {
        content: "";
        position: absolute;
        top: -50%;
        left: -50%;
        width: 200%;
        height: 200%;
        background: linear-gradient(
          to bottom right,
          rgba(255, 255, 255, 0),
          rgba(255, 255, 255, 0.3),
          rgba(255, 255, 255, 0)
        );
        transform: rotate(30deg);
        pointer-events: none;
        z-index: 1;
        transition: transform 0.6s;
      }

      &:hover {
        transform: translateY(-3px);
        box-shadow: 0 12px 20px rgba(91, 45, 228, 0.4);
        background: linear-gradient(135deg, #6a3ff5, #37a6f5);

        &:before {
          transform: rotate(0deg) translateX(100%);
        }
      }
    }

    .toggle-container {
      text-align: center;
      margin-top: 20px;

      .text-link {
        color: #5b2de4;
        cursor: pointer;
        transition: all 0.3s;
        position: relative;
        font-weight: 500;

        &:after {
          content: "";
          position: absolute;
          width: 0;
          height: 2px;
          bottom: -2px;
          left: 0;
          background: linear-gradient(90deg, #5b2de4, #2e99e4);
          transition: width 0.3s;
        }

        &:hover {
          color: #6a3ff5;
          text-decoration: none;

          &:after {
            width: 100%;
          }
        }
      }
    }
  }
}

.hover-input {
  transition: all 0.3s ease;
}

:deep(.ant-input-affix-wrapper) {
  height: 44px;
  border-radius: 8px;
  transition: all 0.3s;

  &:hover {
    border-color: #5b2de4;
  }

  &:focus-within {
    border-color: #5b2de4;
    box-shadow: 0 0 0 2px rgba(91, 45, 228, 0.2);
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
      padding: 30px;
    }
  }
}
</style>
