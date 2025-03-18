<template>
  <a-layout class="layout">
    <a-layout-header class="header">
      <div class="logo">药品管理系统</div>
      <a-menu
        v-model:selectedKeys="selectedKeys"
        theme="dark"
        mode="horizontal"
        :style="{ lineHeight: '64px' }"
      >
        <a-menu-item key="home">
          <router-link to="/home">首页</router-link>
        </a-menu-item>
        <a-menu-item key="products">
          <router-link to="/products">药品列表</router-link>
        </a-menu-item>
        <a-menu-item key="cart">
          <router-link to="/cart">购物车</router-link>
        </a-menu-item>
      </a-menu>
      <div class="user-menu">
        <a-dropdown>
          <a class="ant-dropdown-link" @click.prevent>
            {{ userName }}
            <DownOutlined />
          </a>
          <template #overlay>
            <a-menu>
              <a-menu-item key="profile">
                <router-link to="/user/profile">个人中心</router-link>
              </a-menu-item>
              <a-menu-item key="orders">
                <router-link to="/user/orders">我的订单</router-link>
              </a-menu-item>
              <a-menu-item key="favorites">
                <router-link to="/user/favorites">我的收藏</router-link>
              </a-menu-item>
              <a-menu-item key="logout" @click="handleLogout"> 退出登录 </a-menu-item>
            </a-menu>
          </template>
        </a-dropdown>
      </div>
    </a-layout-header>

    <a-layout>
      <a-layout-sider v-if="showSider" width="200" style="background: #fff">
        <a-menu
          v-model:selectedKeys="siderSelectedKeys"
          v-model:openKeys="openKeys"
          mode="inline"
          :style="{ height: '100%', borderRight: 0 }"
        >
          <a-menu-item key="profile">
            <router-link to="/user/profile">个人信息</router-link>
          </a-menu-item>
          <a-menu-item key="address">
            <router-link to="/user/address">收货地址</router-link>
          </a-menu-item>
          <a-menu-item key="orders">
            <router-link to="/user/orders">我的订单</router-link>
          </a-menu-item>
          <a-menu-item key="favorites">
            <router-link to="/user/favorites">我的收藏</router-link>
          </a-menu-item>
          <a-menu-item key="reviews">
            <router-link to="/user/reviews">我的评价</router-link>
          </a-menu-item>
        </a-menu>
      </a-layout-sider>

      <a-layout-content :style="{ padding: '24px', minHeight: 'calc(100vh - 64px)' }">
        <router-view></router-view>
      </a-layout-content>
    </a-layout>
  </a-layout>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { DownOutlined } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const selectedKeys = ref(['home'])
const siderSelectedKeys = ref(['profile'])
const openKeys = ref(['user'])

const userName = ref('用户名') // 这里应该从用户状态获取

const showSider = computed(() => {
  return route.path.startsWith('/user')
})

const handleLogout = () => {
  localStorage.removeItem('token')
  message.success('退出成功')
  router.push('/login')
}
</script>

<style lang="less" scoped>
.layout {
  min-height: 100vh;
}

.header {
  display: flex;
  align-items: center;
  padding: 0 50px;

  .logo {
    color: #fff;
    font-size: 20px;
    margin-right: 50px;
  }

  .user-menu {
    margin-left: auto;
    color: #fff;
  }
}

:deep(.ant-dropdown-link) {
  color: #fff;
  cursor: pointer;
}
</style>
