<template>
  <div class="products-container">
    <!-- 搜索区域 -->
    <div class="search-section">
      <a-card class="search-card">
        <a-form layout="inline">
          <a-form-item>
            <a-input-search
              v-model:value="searchForm.keyword"
              placeholder="搜索药品名称/症状"
              enter-button
              allowClear
              @search="handleSearch"
              style="width: 300px"
            />
          </a-form-item>
          <a-form-item>
            <a-select
              v-model:value="searchForm.category"
              placeholder="药品分类"
              style="width: 200px"
              allowClear
            >
              <a-select-option v-for="cat in categories" :key="cat.value" :value="cat.value">
                {{ cat.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="handleSearch">搜索</a-button>
            <a-button style="margin-left: 8px" @click="resetSearch">重置</a-button>
          </a-form-item>
        </a-form>
      </a-card>
    </div>

    <!-- 药品列表 -->
    <div class="products-list">
      <a-row :gutter="[16, 16]">
        <a-col :xs="24" :sm="12" :md="8" :lg="6" v-for="product in products" :key="product.id">
          <a-card hoverable class="product-card">
            <template #cover>
              <img :src="product.image || 'https://via.placeholder.com/300x200?text=No+Image'" :alt="product.name" />
            </template>
            <a-card-meta :title="product.name">
              <template #description>
                <div class="product-info">
                  <div class="product-name">{{ product.productName }}</div>
                  <div class="price">¥{{ product.price || '暂无价格' }}</div>
                  <div class="stock">
                    库存: 
                    <span :class="getStockClass(product.quantity)">
                      {{ product.quantity || 0 }}
                    </span>
                    <a-tag v-if="getStockStatus(product.quantity)" :color="getStockStatusColor(product.quantity)">
                      {{ getStockStatus(product.quantity) }}
                    </a-tag>
                  </div>
                  <div class="description">{{ product.description }}</div>
                  <a-tag v-if="product.prescription" color="red">处方药</a-tag>
                  <a-tag v-else color="green">非处方药</a-tag>
                </div>
              </template>
            </a-card-meta>
            <div class="card-actions">
              <a-button type="link" @click="toggleFavorite(product)">
                <HeartOutlined v-if="!product.isFavorite" />
                <HeartFilled v-else style="color: #ff4d4f" />
                {{ product.isFavorite ? '已收藏' : '收藏' }}
              </a-button>
              <a-button 
                type="primary" 
                @click="addToCart(product)"
                :disabled="!product.quantity"
              >
                <ShoppingCartOutlined /> 加入购物车
              </a-button>
            </div>
          </a-card>
        </a-col>
      </a-row>
    </div>

    <!-- 分页 -->
    <div class="pagination">
      <a-pagination
        v-model:current="pagination.current"
        :total="pagination.total"
        :pageSize="pagination.pageSize"
        @change="handlePageChange"
      />
    </div>

    <!-- 详情弹窗 -->
    <a-modal
      v-model:visible="detailVisible"
      :title="selectedProduct?.name"
      width="700px"
      @cancel="closeDetails"
    >
      <div class="product-detail" v-if="selectedProduct">
        <div class="detail-image">
          <img 
            :src="selectedProduct.image || 'https://via.placeholder.com/300x200?text=No+Image'" 
            :alt="selectedProduct.name" 
          />
        </div>
        <div class="detail-info">
          <h3>基本信息</h3>
          <p><strong>价格：</strong>¥{{ selectedProduct.price || '暂无价格' }}</p>
          <p>
            <strong>库存：</strong>
            <span :class="getStockClass(selectedProduct.quantity)">
              {{ selectedProduct.quantity || 0 }}
            </span>
            <a-tag v-if="getStockStatus(selectedProduct.quantity)" :color="getStockStatusColor(selectedProduct.quantity)">
              {{ getStockStatus(selectedProduct.quantity) }}
            </a-tag>
          </p>
          <p><strong>类型：</strong>{{ selectedProduct.prescription ? '处方药' : '非处方药' }}</p>
          <h3>适用症状</h3>
          <p>{{ selectedProduct.symptoms || '暂无信息' }}</p>
          <h3>使用方法</h3>
          <p>{{ selectedProduct.usage || '暂无信息' }}</p>
          <h3>注意事项</h3>
          <p>{{ selectedProduct.precautions || '暂无信息' }}</p>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { ShoppingCartOutlined, HeartOutlined, HeartFilled } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import request from '@/utils/axios'

// 搜索表单
const searchForm = reactive({
  keyword: '',
  category: undefined
})

// 分类数据
const categories = [
  { label: '感冒用药', value: 'cold' },
  { label: '消化系统', value: 'digest' },
  { label: '心脑血管', value: 'heart' },
  { label: '维生素', value: 'vitamin' }
]

// 分页数据
const pagination = reactive({
  current: 1,
  pageSize: 12,
  total: 0
})

// 药品数据
const products = ref([])
const loading = ref(false)

// 详情弹窗
const detailVisible = ref(false)
const selectedProduct = ref(null)

// 获取收藏列表
const fetchFavorites = async () => {
  try {
    const res = await request({
      url: '/favorites',
      method: 'get',
      params: {
        storeId: localStorage.getItem('currentStoreId')
      }
    })
    // 更新产品的收藏状态
    const favoriteProductIds = res.data.map(item => item.productId)
    products.value = products.value.map(product => ({
      ...product,
      isFavorite: favoriteProductIds.includes(product.id)
    }))
  } catch (error) {
    message.error('获取收藏列表失败')
  }
}

// 获取药品列表
const fetchProducts = async () => {
  loading.value = true
  try {
    const res = await request({
      url: `/inventory/store/${localStorage.getItem('currentStoreId')}`,
      method: 'get',
      params: {
        keyword: searchForm.keyword,
        page: pagination.current - 1,
        size: pagination.pageSize
      }
    })
    
    products.value = res.data.content || []
    pagination.total = res.data.totalElements || 0
    
    // 获取收藏状态
    await fetchFavorites()
  } catch (error) {
    message.error('获取药品列表失败')
  } finally {
    loading.value = false
  }
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

// 收藏/取消收藏
const toggleFavorite = async (product) => {
  try {
    if (product.isFavorite) {
      // 取消收藏
      await request({
        url: `/favorites/${product.id}`,
        method: 'delete',
        params: {
          storeId: localStorage.getItem('currentStoreId')
        }
      })
      message.success('已取消收藏')
    } else {
      // 添加收藏
      await request({
        url: `/favorites/${product.id}`,
        method: 'post',
        params: {
          storeId: localStorage.getItem('currentStoreId')
        }
      })
      message.success('收藏成功')
    }
    // 更新产品的收藏状态
    product.isFavorite = !product.isFavorite
  } catch (error) {
    message.error(error.response?.data?.message || '操作失败')
  }
}

// 处理搜索
const handleSearch = () => {
  pagination.current = 1
  fetchProducts()
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.category = undefined
  pagination.current = 1
  fetchProducts()
}

// 显示详情
const showDetails = (product) => {
  selectedProduct.value = product
  detailVisible.value = true
}

// 关闭详情
const closeDetails = () => {
  detailVisible.value = false
  selectedProduct.value = null
}

// 加入购物车
const addToCart = async (product) => {
  if (!product.quantity) {
    message.warning(`${product.name} 当前无库存`)
    return
  }
  
  try {
    await request({
      url: '/cart',
      method: 'post',
      data: {
        productId: product.id,
        storeId: localStorage.getItem('currentStoreId'),
        quantity: 1
      }
    })
    message.success(`已将 ${product.productName} 加入购物车`)
  } catch (error) {
    if (error.response?.status === 400) {
      message.warning(error.response.data.message || '添加失败')
    } else {
      message.error('添加失败')
    }
  }
}

// 处理分页
const handlePageChange = (page) => {
  pagination.current = page
  fetchProducts()
}

// 初始化
onMounted(() => {
  fetchProducts()
})
</script>

<style lang="less" scoped>
.products-container {
  padding: 16px;
  
  .search-section {
    margin-bottom: 16px;
    
    .search-card {
      background: rgba(255, 255, 255, 0.8);
      backdrop-filter: blur(10px);
    }
  }

  .ant-tag {
    margin-left: 10px;
  }

  .products-list {
    margin-bottom: 16px;

    .product-card {
      height: 100%;
      transition: all 0.3s;
      
      img {
        height: 200px;
        object-fit: cover;
      }

      .product-info {
        .product-name {
          font-size: 16px;
          font-weight: bold;
          margin-bottom: 8px;
          color: #262626;
        }

        .price {
          color: #f5222d;
          font-size: 18px;
          font-weight: bold;
          margin: 8px 0;
        }

        .stock {
          color: #8c8c8c;
          margin-bottom: 8px;
          
          .stock-empty {
            color: #f5222d;
            font-weight: bold;
          }
          
          .stock-low {
            color: #fa8c16;
            font-weight: bold;
          }
          
          .stock-normal {
            color: #52c41a;
            font-weight: bold;
          }
        }

        .description {
          margin: 8px 0;
          height: 40px;
          overflow: hidden;
          text-overflow: ellipsis;
          display: -webkit-box;
          -webkit-line-clamp: 2;
          -webkit-box-orient: vertical;
        }
      }

      .card-actions {
        margin-top: 16px;
        display: flex;
        justify-content: space-between;
        align-items: center;
        
        .ant-btn {
          padding: 0 8px;
          
          &.ant-btn-link {
            color: #595959;
            
            &:hover {
              color: #1890ff;
            }
          }
        }
      }
    }
  }

  .pagination {
    text-align: center;
    margin-top: 16px;
  }
}

.product-detail {
  display: flex;
  gap: 24px;

  .detail-image {
    flex: 0 0 300px;
    
    img {
      width: 100%;
      border-radius: 8px;
    }
  }

  .detail-info {
    flex: 1;

    h3 {
      margin: 16px 0 8px;
      color: #1890ff;
    }

    p {
      margin: 8px 0;
      line-height: 1.6;
    }
    
    .stock-empty {
      color: #f5222d;
      font-weight: bold;
    }
    
    .stock-low {
      color: #fa8c16;
      font-weight: bold;
    }
    
    .stock-normal {
      color: #52c41a;
      font-weight: bold;
    }
  }
}

// 响应式调整
@media (max-width: 768px) {
  .product-detail {
    flex-direction: column;
    
    .detail-image {
      flex: none;
      width: 100%;
    }
  }
}
</style> 