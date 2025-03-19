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
              <img :src="product.image" :alt="product.name" />
            </template>
            <a-card-meta :title="product.name">
              <template #description>
                <div class="product-info">
                  <div class="price">¥{{ product.price }}</div>
                  <div class="stock">库存: {{ product.stock }}</div>
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
              <a-button type="primary" @click="addToCart(product)">
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
          <img :src="selectedProduct.image" :alt="selectedProduct.name" />
        </div>
        <div class="detail-info">
          <h3>基本信息</h3>
          <p><strong>价格：</strong>¥{{ selectedProduct.price }}</p>
          <p><strong>库存：</strong>{{ selectedProduct.stock }}</p>
          <p><strong>类型：</strong>{{ selectedProduct.prescription ? '处方药' : '非处方药' }}</p>
          <h3>适用症状</h3>
          <p>{{ selectedProduct.symptoms }}</p>
          <h3>使用方法</h3>
          <p>{{ selectedProduct.usage }}</p>
          <h3>注意事项</h3>
          <p>{{ selectedProduct.precautions }}</p>
        </div>
      </div>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ShoppingCartOutlined, HeartOutlined, HeartFilled } from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'

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
  total: 100
})

// 模拟药品数据
const products = ref([
  {
    id: 1,
    name: '布洛芬缓释胶囊',
    price: 35.8,
    stock: 100,
    image: 'https://example.com/medicine1.jpg',
    description: '用于缓解轻至中度疼痛',
    prescription: false,
    symptoms: '头痛、发热、关节痛',
    usage: '口服，一次1粒，每日2次',
    precautions: '空腹服用可能会出现胃部不适',
    isFavorite: false
  },
  // ... 更多药品数据
])

// 详情弹窗
const detailVisible = ref(false)
const selectedProduct = ref(null)

// 收藏/取消收藏
const toggleFavorite = (product) => {
  product.isFavorite = !product.isFavorite
  message.success(product.isFavorite ? '收藏成功' : '已取消收藏')
}

// 处理搜索
const handleSearch = () => {
  // 实现搜索逻辑
  console.log('搜索条件：', searchForm)
}

// 重置搜索
const resetSearch = () => {
  searchForm.keyword = ''
  searchForm.category = undefined
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
const addToCart = (product) => {
  message.success(`已将 ${product.name} 加入购物车`)
}

// 处理分页
const handlePageChange = (page) => {
  pagination.current = page
  // 加载对应页数据
}
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
        .price {
          color: #f5222d;
          font-size: 18px;
          font-weight: bold;
          margin: 8px 0;
        }

        .stock {
          color: #8c8c8c;
          margin-bottom: 8px;
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