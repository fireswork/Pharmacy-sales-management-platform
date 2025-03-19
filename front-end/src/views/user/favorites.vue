<template>
  <div class="favorites-container">
    <a-card>
      <!-- 商品列表 -->
      <a-list
        :grid="{ gutter: 16, column: 4 }"
        :data-source="favoriteProducts"
        :pagination="pagination"
      >
        <template #renderItem="{ item }">
          <a-list-item>
            <a-card hoverable class="product-card">
              <template #cover>
                <img :src="item.image" :alt="item.name" class="product-image"/>
              </template>
              <a-card-meta :title="item.name">
                <template #description>
                  <div class="product-info">
                    <span class="price">¥{{ item.price }}</span>
                    <div class="actions">
                      <a-button type="link" @click="removeFromFavorites(item)">
                        <template #icon><DeleteOutlined /></template>
                        取消收藏
                      </a-button>
                      <a-button type="primary" size="small" @click="addToCart(item)">
                        加入购物车
                      </a-button>
                    </div>
                  </div>
                </template>
              </a-card-meta>
            </a-card>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined } from '@ant-design/icons-vue'

// 模拟收藏数据
const favoriteProducts = ref([
  {
    id: 1,
    name: '布洛芬缓释胶囊',
    price: 25.8,
    image: '/images/product1.jpg',
  },
  {
    id: 2,
    name: '感冒灵颗粒',
    price: 32.5,
    image: '/images/product2.jpg',
  },
  {
    id: 3,
    name: '维生素C片',
    price: 45.0,
    image: '/images/product3.jpg',
  },
  {
    id: 4,
    name: '复方板蓝根颗粒',
    price: 28.5,
    image: '/images/product4.jpg',
  },
  {
    id: 5,
    name: '阿莫西林胶囊',
    price: 35.6,
    image: '/images/product5.jpg',
  },
])

// 分页配置
const pagination = {
  pageSize: 8,
  total: favoriteProducts.value.length,
  showTotal: (total) => `共 ${total} 个收藏`,
}

// 取消收藏
const removeFromFavorites = (product) => {
  favoriteProducts.value = favoriteProducts.value.filter(item => item.id !== product.id)
  message.success('已取消收藏')
}

// 加入购物车
const addToCart = (product) => {
  message.success('已加入购物车')
}
</script>

<style lang="less" scoped>
.favorites-container {
  padding: 16px;

  .product-card {
    .product-image {
      height: 200px;
      object-fit: cover;
    }

    :deep(.ant-card-meta-title) {
      margin-bottom: 8px;
      white-space: normal;
      line-height: 1.5;
      height: 44px;
      overflow: hidden;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
    }

    .product-info {
      .price {
        font-size: 18px;
        font-weight: bold;
        color: #ff4d4f;
      }

      .actions {
        margin-top: 12px;
        display: flex;
        justify-content: space-between;
        align-items: center;

        .ant-btn-link {
          padding: 0;
        }
      }
    }
  }
}
</style> 