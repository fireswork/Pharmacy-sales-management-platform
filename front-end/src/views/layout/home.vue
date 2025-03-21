<template>
  <a-layout class="layout">
    <a-spin :spinning="loading" tip="加载中..." size="large" class="global-loading">
      <a-layout-header class="header">
        <div class="header-left">
          <div class="logo">
            <svg class="logo-icon" viewBox="0 0 24 24">
              <path
                fill="currentColor"
                d="M20,6H16V4A2,2 0 0,0 14,2H10A2,2 0 0,0 8,4V6H4C2.89,6 2,6.89 2,8V19A2,2 0 0,0 4,21H20A2,2 0 0,0 22,19V8C22,6.89 21.1,6 20,6M10,4H14V6H10V4M20,19H4V8H20V19M6,10H8V12H6V10M6,14H8V16H6V14M10,10H18V12H10V10M10,14H15V16H10V14Z"
              />
            </svg>
            <span>药品管理系统</span>
          </div>
          <a-select
            v-model:value="currentStoreId"
            style="width: 200px"
            placeholder="请选择门店"
            @change="handleStoreChange"
            v-if="userInfo?.role?.toUpperCase() === 'USER'"
          >
            <a-select-option v-for="store in stores" :key="store.id" :value="store.id">
              {{ store?.name }}
            </a-select-option>
          </a-select>
        </div>

        <div class="header-right">
          <a-dropdown>
            <a class="ant-dropdown-link user-dropdown" @click.prevent>
              <a-avatar class="user-avatar">
                <template #icon><UserOutlined /></template>
              </a-avatar>
              <span class="username">{{ userInfo?.username || '未登录' }}</span>
              <DownOutlined />
            </a>
            <template #overlay>
              <a-menu>
                <a-menu-item key="profile">
                  <router-link to="/home/user/profile">个人信息</router-link>
                </a-menu-item>
                <a-menu-item key="logout" @click="handleLogout">退出登录</a-menu-item>
              </a-menu>
            </template>
          </a-dropdown>
        </div>
      </a-layout-header>

      <a-layout>
        <a-layout-sider width="240" class="sider">
          <a-menu
            v-model:selectedKeys="selectedKeys"
            v-model:openKeys="openKeys"
            mode="inline"
            class="side-menu"
          >
            <a-menu-item key="products">
              <template #icon><MedicineBoxOutlined /></template>
              <router-link to="/home/products">药品列表</router-link>
            </a-menu-item>
            <a-menu-item key="productsManagement">
              <template #icon><DatabaseOutlined /></template>
              <router-link to="/home/productsManagement">药品管理</router-link>
            </a-menu-item>
            <a-menu-item key="purchaseManagement">
              <template #icon><ShoppingCartOutlined /></template>
              <router-link to="/home/purchaseManagement">采购管理</router-link>
            </a-menu-item>
            <a-menu-item key="supplierManagement">
              <template #icon><TeamOutlined /></template>
              <router-link to="/home/supplierManagement">供应商管理</router-link>
            </a-menu-item>
            <a-menu-item key="storeManagement">
              <template #icon><ShopOutlined /></template>
              <router-link to="/home/storeManagement">分店管理</router-link>
            </a-menu-item>
            <a-menu-item key="memberManagement">
              <template #icon><CrownOutlined /></template>
              <router-link to="/home/memberManagement">会员管理</router-link>
            </a-menu-item>
            <a-menu-item key="employeeManagement">
              <template #icon><ShopOutlined /></template>
              <router-link to="/home/employeeManagement">员工管理</router-link>
            </a-menu-item>
            <a-menu-item key="order">
              <template #icon><OrderedListOutlined /></template>
              <router-link to="/home/order">订单管理</router-link>
            </a-menu-item>
            <a-menu-item key="warehouse">
              <template #icon><InboxOutlined /></template>
              <router-link to="/home/warehouse">仓库管理</router-link>
            </a-menu-item>
            <a-menu-item key="finance">
              <template #icon><AccountBookOutlined /></template>
              <router-link to="/home/finance">财务管理</router-link>
            </a-menu-item>
            <a-menu-item key="cart">
              <template #icon><ShoppingOutlined /></template>
              <router-link to="/home/cart">购物车</router-link>
            </a-menu-item>

            <a-sub-menu key="user">
              <template #icon><UserOutlined /></template>
              <template #title>个人中心</template>
              <a-menu-item key="profile">
                <router-link to="/home/user/profile">个人信息</router-link>
              </a-menu-item>
              <a-menu-item key="userAddress">
                <router-link to="/home/user/address">收货地址</router-link>
              </a-menu-item>
              <a-menu-item key="userOrders">
                <router-link to="/home/user/orders">我的订单</router-link>
              </a-menu-item>
              <a-menu-item key="favorites">
                <template #icon><HeartOutlined /></template>
                <router-link to="/home/user/favorites">我的收藏</router-link>
              </a-menu-item>
              <a-menu-item key="reviews">
                <template #icon><CommentOutlined /></template>
                <router-link to="/home/user/reviews">我的评价</router-link>
              </a-menu-item>
            </a-sub-menu>
          </a-menu>
        </a-layout-sider>

        <a-layout-content class="content">
          <router-view></router-view>
        </a-layout-content>
      </a-layout>
    </a-spin>
  </a-layout>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/utils/axios'
import {
  DownOutlined,
  UserOutlined,
  MedicineBoxOutlined,
  DatabaseOutlined,
  ShoppingCartOutlined,
  ShoppingOutlined,
  IdcardOutlined,
  EnvironmentOutlined,
  UnorderedListOutlined,
  HeartOutlined,
  CommentOutlined,
  TeamOutlined,
  ShopOutlined,
  CrownOutlined,
  OrderedListOutlined,
  InboxOutlined,
  AccountBookOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

const route = useRoute()
const router = useRouter()
const selectedKeys = ref(['products'])
const openKeys = ref(['user'])

// 添加 loading 状态
const loading = ref(false)

// 用户信息相关
const userInfo = ref(null)

// 获取用户信息的方法
const getUserInfo = async () => {
  const response = await axios.get('/user')
  if (response.code === 200) {
    userInfo.value = response.data
  }
  if (response.data.role.toUpperCase() === 'USER') {
    loading.value = true
    fetchStores()
  }
}

// 添加门店选择相关数据
const currentStoreId = ref(null)
const stores = ref([])

// 获取门店列表
const fetchStores = async () => {
  try {
    // 从 localStorage 获取上次选择的门店 ID
    const savedStoreId = localStorage.getItem('currentStoreId')
    if (savedStoreId) {
      currentStoreId.value = parseInt(savedStoreId)
    }
    
    const res = await axios.get('/store')
    stores.value = res.data.content || []
    
    // 如果有门店数据，且没有从 localStorage 获取到门店 ID，则默认选择第一个
    if (stores.value.length > 0 && !currentStoreId.value) {
      currentStoreId.value = stores.value[0].id
      // 保存到 localStorage
      localStorage.setItem('currentStoreId', currentStoreId.value)
    }
  } catch (error) {
    message.error('获取门店列表失败')
  } finally {
    // 无论成功失败，都关闭 loading
    loading.value = false
  }
}

// 切换门店
const handleStoreChange = (storeId) => {
  currentStoreId.value = storeId
  // 保存到 localStorage
  localStorage.setItem('currentStoreId', storeId)
  message.success('已切换到新门店')
  
  // 如果当前在产品或库存页面，刷新数据
  const currentPath = router.currentRoute.value.path
  if (currentPath.includes('/products') || currentPath.includes('/inventory')) {
    router.go(0)  // 刷新当前页面
  }
}

// 修改路由监听逻辑
watch(
  () => route.path,
  (newPath) => {
    const paths = newPath.split('/')
    // 根据路径判断选中的菜单项
    if (paths[2] === 'user') {
      openKeys.value = ['user']
      selectedKeys.value = [paths[3] || 'profile'] // 选中用户中心子菜单
    } else {
      selectedKeys.value = [paths[2] || 'products'] // 选中主菜单
    }
  },
  { immediate: true }
)

const handleLogout = () => {
  localStorage.removeItem('access_token')  // 更新为正确的 token key
  message.success('退出成功')
  router.push('/login')
}

// 在组件挂载时获取用户信息和门店列表
onMounted(() => {
  getUserInfo()
})
</script>

<style lang="less" scoped>
.layout {
  min-height: 100vh;
  background: #f0f2f5;
}

.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
  
  :deep(.ant-spin-container) {
    width: 100%;
    height: 100%;
  }
}

.ant-layout {
  height: calc(100vh - 64px);
}

.header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);

  .header-left {
    display: flex;
    align-items: center;
    gap: 24px;

    .logo {
      display: flex;
      align-items: center;
      gap: 12px;

      .logo-icon {
        width: 32px;
        height: 32px;
        color: #1890ff;
      }

      img {
        height: 36px;
        width: auto;
      }

      span {
        color: #1890ff;
        font-size: 20px;
        font-weight: bold;
        white-space: nowrap;
      }
    }

    .store-select {
      width: 200px;
    }
  }

  .header-right {
    .user-dropdown {
      display: flex;
      align-items: center;
      gap: 8px;

      .user-avatar {
        background-color: #1890ff;
      }

      .username {
        margin-right: 4px;
      }
    }

    .ant-dropdown-link {
      color: rgba(0, 0, 0, 0.85);
      cursor: pointer;
    }
  }
}

.sider {
  background: #fff;
  box-shadow: 2px 0 8px 0 rgba(29, 35, 41, 0.05);

  :deep(.ant-menu) {
    .ant-menu-item {
      height: 40px;
      line-height: 40px;
      margin: 4px 0;
      width: 100% !important;  // 确保菜单项宽度适应新的侧边栏宽度

      &::after {
        right: 0;
      }
    }

    .ant-menu-submenu {
      .ant-menu-submenu-title {
        height: 40px;
        line-height: 40px;
        margin: 4px 0;
        width: 100% !important;  // 确保子菜单标题宽度适应新的侧边栏宽度
      }
    }
  }
}

.content {
  padding: 16px;
  background: #f0f2f5;
}

// 菜单项样式
.side-menu :deep(.ant-menu-item) {
  margin: 0 !important;
  border-radius: 0;
  padding-left: 24px !important;
  width: 100%;

  &.ant-menu-item-selected {
    background: rgba(24, 144, 255, 0.1);
    color: #1890ff;

    &::after {
      display: none;
    }

    a {
      color: #1890ff;
    }
  }
}

// 子菜单样式
.side-menu :deep(.ant-menu-submenu) {
  .ant-menu-submenu-title {
    margin: 0;
    padding-left: 24px !important;
  }

  .ant-menu-sub {
    background: transparent;

    .ant-menu-item {
      padding-left: 48px !important;
    }
  }
}
</style>
