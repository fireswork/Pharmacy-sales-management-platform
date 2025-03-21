<template>
  <div class="favorites-container">
    <a-card title="我的收藏" :bordered="false">
      <!-- 商品列表 -->
      <div class="favorites-list">
        <!-- 商品列表 -->
        <div v-if="favoriteProducts.length" class="product-list">
          <div v-for="item in favoriteProducts" :key="item.id" class="product-item">
            <div class="product-image">
              <img 
                :src="item.image || 'https://via.placeholder.com/120x120?text=No+Image'" 
                :alt="item.productName" 
              />
            </div>
            <div class="product-content">
              <div class="product-header">
                <h3 class="product-name">{{ item.productName }}</h3>
                <div class="product-price">¥{{ item.price || '暂无价格' }}</div>
              </div>
              
              <div class="product-info">
                <div class="info-row">
                  <span class="label">规格：</span>
                  <span>{{ item.specification || '暂无' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">生产商：</span>
                  <span>{{ item.manufacturer || '暂无' }}</span>
                </div>
                <div class="info-row">
                  <span class="label">库存：</span>
                  <span :class="getStockClass(item.quantity)">{{ item.quantity || 0 }}</span>
                  <a-tag 
                    v-if="getStockStatus(item.quantity)" 
                    :color="getStockStatusColor(item.quantity)"
                    class="stock-tag"
                  >
                    {{ getStockStatus(item.quantity) }}
                  </a-tag>
                </div>
                <div class="info-row">
                  <span class="label">类型：</span>
                  <a-tag v-if="item.prescription" color="red">处方药</a-tag>
                  <a-tag v-else color="green">非处方药</a-tag>
                </div>
                <div class="tags-row">
                  <a-tag v-if="!item.available" color="red">已下架</a-tag>
                </div>
              </div>

              <div class="product-actions">
                <a-button 
                  type="primary" 
                  @click="addToCart(item)"
                  :disabled="!item.available || !item.inStock"
                >
                  <ShoppingCartOutlined /> 加入购物车
                </a-button>
                <a-button type="link" danger @click="removeFromFavorites(item)">
                  <DeleteOutlined /> 取消收藏
                </a-button>
              </div>
            </div>
          </div>
        </div>

        <!-- 空状态 -->
        <a-empty v-else description="暂无收藏商品" />
      </div>

      <!-- 分页 -->
      <div class="pagination" v-if="favoriteProducts.length">
        <a-pagination
          v-model:current="pagination.current"
          :total="pagination.total"
          :pageSize="pagination.pageSize"
          show-total
          @change="handlePageChange"
        />
      </div>
    </a-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { DeleteOutlined, ShoppingCartOutlined } from '@ant-design/icons-vue'
import request from '@/utils/axios'

const favoriteProducts = ref([])
const loading = ref(false)

// 分页配置
const pagination = {
  current: 1,
  pageSize: 8,
  total: 0
}

// 获取库存状态
const getStockStatus = (quantity) => {
  if (quantity <= 0) return '缺货'
  if (quantity <= 10) return '低库存'
  return '充足'
}

// 获取库存状态颜色
const getStockStatusColor = (quantity) => {
  if (quantity <= 0) return 'red'
  if (quantity <= 10) return 'orange'
  return 'green'
}

// 获取库存数量的样式类
const getStockClass = (quantity) => {
  if (quantity <= 0) return 'stock-empty'
  if (quantity <= 10) return 'stock-low'
  return 'stock-normal'
}

// 获取收藏列表
const fetchFavorites = async () => {
  loading.value = true
  try {
    const res = await request({
      url: '/favorites',
      method: 'get',
      params: {
        storeId: localStorage.getItem('currentStoreId'),
        page: pagination.current - 1,
        size: pagination.pageSize
      }
    })
    favoriteProducts.value = res.data
    pagination.total = res.data.length
  } catch (error) {
    message.error('获取收藏列表失败')
  } finally {
    loading.value = false
  }
}

// 取消收藏
const removeFromFavorites = async (product) => {
  try {
    await request({
      url: `/favorites/${product.productId}`,
      method: 'delete',
      params: {
        storeId: localStorage.getItem('currentStoreId')
      }
    })
    message.success('已取消收藏')
    // 重新获取收藏列表
    fetchFavorites()
  } catch (error) {
    message.error('取消收藏失败')
  }
}

// 加入购物车
const addToCart = (product) => {
  if (!product.available || !product.inStock) {
    message.warning('商品已下架或缺货')
    return
  }
  // TODO: 调用购物车接口
  message.success('已加入购物车')
}

// 处理分页
const handlePageChange = (page) => {
  pagination.current = page
  fetchFavorites()
}

// 页面加载时获取数据
onMounted(() => {
  fetchFavorites()
})
</script>

<style lang="less" scoped>
.favorites-container {
  padding: 16px;

  .favorites-list {
    .product-list {
      .product-item {
        display: flex;
        padding: 24px;
        margin-bottom: 16px;
        background: #fff;
        border-radius: 8px;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
        transition: all 0.3s;

        &:hover {
          box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
        }

        .product-image {
          flex: 0 0 120px;
          margin-right: 24px;

          img {
            width: 120px;
            height: 120px;
            object-fit: cover;
            border-radius: 4px;
          }
        }

        .product-content {
          flex: 1;
          display: flex;
          flex-direction: column;
          justify-content: space-between;

          .product-header {
            display: flex;
            justify-content: space-between;
            align-items: flex-start;
            margin-bottom: 16px;

            .product-name {
              font-size: 18px;
              font-weight: 500;
              color: #262626;
              margin: 0;
              flex: 1;
              padding-right: 16px;
            }

            .product-price {
              font-size: 20px;
              font-weight: bold;
              color: #ff4d4f;
              white-space: nowrap;
            }
          }

          .product-info {
            flex: 1;

            .info-row {
              margin-bottom: 8px;
              color: #595959;
              display: flex;
              align-items: center;

              .label {
                color: #8c8c8c;
                margin-right: 8px;
                min-width: 70px;
              }

              .stock-empty {
                color: #ff4d4f;
                font-weight: 500;
              }
              
              .stock-low {
                color: #faad14;
                font-weight: 500;
              }
              
              .stock-normal {
                color: #52c41a;
                font-weight: 500;
              }

              .stock-tag {
                margin-left: 8px;
              }

              .ant-tag {
                margin-right: 8px;
                padding: 2px 8px;
              }
            }

            .tags-row {
              margin-top: 12px;

              .ant-tag {
                margin-right: 8px;
                padding: 2px 8px;
              }
            }
          }

          .product-actions {
            display: flex;
            justify-content: flex-end;
            align-items: center;
            margin-top: 16px;
            gap: 12px;

            .ant-btn {
              min-width: 120px;
            }
          }
        }
      }
    }
  }

  .pagination {
    margin-top: 24px;
    text-align: right;
  }
}

// 响应式样式
@media (max-width: 768px) {
  .favorites-container {
    .product-list {
      .product-item {
        flex-direction: column;
        padding: 16px;

        .product-image {
          margin: 0 auto 16px;
          text-align: center;
        }

        .product-content {
          .product-header {
            flex-direction: column;
            
            .product-name {
              margin-bottom: 8px;
            }
          }

          .product-actions {
            flex-direction: column;
            
            .ant-btn {
              width: 100%;
            }
          }
        }
      }
    }
  }
}
</style> 