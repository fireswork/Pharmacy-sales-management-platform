<template>
  <div class="warehouse-container">
    <a-card>
      <!-- 搜索区域 -->
      <div class="filter-section">
        <a-form layout="inline">
          <div class="filter-row">
            <a-form-item label="关键词">
              <a-input
                v-model:value="searchForm.keyword"
                placeholder="请输入药品名称/编号"
                style="width: 200px"
                allow-clear
              />
            </a-form-item>
            
            <a-form-item label="药品分类">
              <a-select
                v-model:value="searchForm.category"
                style="width: 160px"
                placeholder="药品分类"
                allow-clear
              >
                <a-select-option
                  v-for="cat in categories"
                  :key="cat.value"
                  :value="cat.value"
                >
                  {{ cat.label }}
                </a-select-option>
              </a-select>
            </a-form-item>
            
            <a-form-item label="库存状态">
              <a-select
                v-model:value="searchForm.stockStatus"
                style="width: 120px"
                placeholder="库存状态"
                allow-clear
              >
                <a-select-option value="normal">正常</a-select-option>
                <a-select-option value="low">偏低</a-select-option>
                <a-select-option value="warning">警告</a-select-option>
              </a-select>
            </a-form-item>
          </div>
          
          <div class="filter-row">
            <a-form-item label="选择门店">
              <a-select
                v-model:value="searchForm.storeId"
                style="width: 160px"
                placeholder="选择门店"
                allow-clear
              >
                <a-select-option
                  v-for="store in storeOptions"
                  :key="store.id"
                  :value="store.id"
                >
                  {{ store.name }}
                </a-select-option>
              </a-select>
            </a-form-item>
            
            <a-form-item label="日期范围">
              <a-range-picker
                v-model:value="searchForm.dateRange"
                style="width: 240px"
                :placeholder="['开始日期', '结束日期']"
              />
            </a-form-item>
          </div>
          
          <div class="action-row">
            <a-form-item>
              <a-space>
                <a-button type="primary" @click="handleSearch" :loading="loading">
                  <template #icon><SearchOutlined /></template>
                  查询
                </a-button>
                <a-button @click="handleReset">
                  <template #icon><ReloadOutlined /></template>
                  重置
                </a-button>
              </a-space>
            </a-form-item>
            
            <a-form-item class="operation-buttons">
              <a-space>
                <a-button type="primary" @click="handleInbound">
                  <template #icon><ImportOutlined /></template>
                  入库登记
                </a-button>
                <a-button type="primary" danger @click="handleOutbound">
                  <template #icon><ExportOutlined /></template>
                  出库登记
                </a-button>
              </a-space>
            </a-form-item>
          </div>
        </a-form>
      </div>

      <!-- 库存列表 -->
      <a-table
        :columns="columns"
        :data-source="stockList"
        :pagination="pagination"
        :row-key="(record) => record.id"
        :loading="loading"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <!-- 库存状态列 -->
          <template v-if="column.key === 'stockStatus'">
            <a-tag :color="getStockStatusColor(record)">
              {{ getStockStatusText(record) }}
            </a-tag>
          </template>

          <!-- 操作列 -->
          <template v-if="column.key === 'action'">
            <a-space>
              <a-button
                type="link"
                size="small"
                @click="handleStockRecord(record)"
              >
                出入库记录
              </a-button>
              <a-button type="link" size="small" @click="handleAdjust(record)">
                库存调整
              </a-button>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <!-- 入库登记弹窗 -->
    <a-modal
      v-model:visible="inboundVisible"
      title="入库登记"
      @ok="handleInboundSubmit"
      :confirmLoading="submitLoading"
      width="680px"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
    >
      <a-form
        ref="inboundFormRef"
        :model="inboundForm"
        :rules="inboundRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="选择药品" name="productId" required>
          <a-select
            v-model:value="inboundForm.productId"
            placeholder="请选择药品"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option
              v-for="item in productOptions"
              :key="item.id"
              :value="item.id"
            >
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="入库数量" name="quantity" required>
          <a-input-number
            v-model:value="inboundForm.quantity"
            :min="1"
            style="width: 100%"
            placeholder="请输入入库数量"
          />
        </a-form-item>

        <a-form-item label="生产批号" name="batchNumber" required>
          <a-input
            v-model:value="inboundForm.batchNumber"
            placeholder="请输入生产批号"
          />
        </a-form-item>

        <a-form-item label="生产日期" name="productionDate" required>
          <a-date-picker
            v-model:value="inboundForm.productionDate"
            style="width: 100%"
            :disabledDate="disabledProductionDate"
          />
        </a-form-item>

        <a-form-item label="有效期至" name="expirationDate" required>
          <a-date-picker
            v-model:value="inboundForm.expirationDate"
            style="width: 100%"
            :disabledDate="disabledExpirationDate"
          />
        </a-form-item>

        <a-form-item label="所属门店" name="storeId" required>
          <a-select
            v-model:value="inboundForm.storeId"
            placeholder="请选择门店"
            show-search
            :filter-option="filterOption"
          >
            <a-select-option
              v-for="item in storeOptions"
              :key="item.id"
              :value="item.id"
            >
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="inboundForm.remark"
            :rows="2"
            placeholder="请输入备注信息"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 出库登记弹窗 -->
    <a-modal
      v-model:visible="outboundVisible"
      title="出库登记"
      @ok="handleOutboundSubmit"
      :confirmLoading="submitLoading"
      width="680px"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
    >
      <a-form
        ref="outboundFormRef"
        :model="outboundForm"
        :rules="outboundRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="选择门店" name="storeId" required>
          <a-select
            v-model:value="outboundForm.storeId"
            placeholder="请选择门店"
            show-search
            :filter-option="filterOption"
            @change="handleStoreSelect"
          >
            <a-select-option
              v-for="item in storeOptions"
              :key="item.id"
              :value="item.id"
            >
              {{ item.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="选择药品" name="productId" required>
          <a-select
            v-model:value="outboundForm.productId"
            placeholder="请选择药品"
            show-search
            :filter-option="filterOption"
            @change="handleProductSelect"
            :disabled="!outboundForm.storeId"
          >
            <a-select-option
              v-for="item in filteredProductOptions"
              :key="item.id"
              :value="item.id"
            >
              {{ item.name }} (库存: {{ item.stock }})
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="出库数量" name="quantity" required>
          <a-input-number
            v-model:value="outboundForm.quantity"
            :min="1"
            :max="selectedProductStock"
            style="width: 100%"
            placeholder="请输入出库数量"
          />
        </a-form-item>

        <a-form-item label="出库类型" name="type" required>
          <a-select
            v-model:value="outboundForm.type"
            placeholder="请选择出库类型"
          >
            <a-select-option value="sale">销售出库</a-select-option>
            <a-select-option value="transfer">调拨出库</a-select-option>
            <a-select-option value="damage">损坏出库</a-select-option>
            <a-select-option value="expired">过期出库</a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item
          label="目标分店"
          name="targetStoreId"
          v-if="outboundForm.type === 'transfer'"
          required
        >
          <a-select
            v-model:value="outboundForm.targetStoreId"
            placeholder="请选择目标分店"
          >
            <a-select-option
              v-for="store in storeOptions.filter(
                (s) => s.id !== outboundForm.storeId
              )"
              :key="store.id"
              :value="store.id"
            >
              {{ store.name }}
            </a-select-option>
          </a-select>
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="outboundForm.remark"
            :rows="2"
            placeholder="请输入备注信息"
          />
        </a-form-item>
      </a-form>
    </a-modal>

    <!-- 库存记录弹窗 -->
    <a-modal
      v-model:visible="recordVisible"
      title="出入库记录"
      :footer="null"
      width="800px"
      :maskClosable="false"
    >
      <div class="record-filter">
        <a-space>
          <a-select
            v-model:value="recordFilter.type"
            style="width: 120px"
            placeholder="记录类型"
            allow-clear
            @change="fetchStockRecords"
          >
            <a-select-option value="inbound">入库</a-select-option>
            <a-select-option value="outbound">出库</a-select-option>
          </a-select>
          <a-range-picker
            v-model:value="recordFilter.dateRange"
            style="width: 240px"
            :placeholder="['开始日期', '结束日期']"
            @change="fetchStockRecords"
          />
        </a-space>
      </div>

      <a-table
        :columns="recordColumns"
        :data-source="stockRecords"
        :pagination="recordPagination"
        :loading="recordLoading"
        size="small"
        class="record-table"
        bordered
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'recordType'">
            <a-tag
              :color="record.recordType === 'inbound' ? 'success' : 'error'"
            >
              {{ record.recordType === "inbound" ? "入库" : "出库" }}
            </a-tag>
          </template>
          <template v-if="column.key === 'sourceType'">
            <span>
              {{ getSourceTypeText(record.sourceType) }}
              <a-link v-if="record.sourceId" @click="viewSource(record)"
                >查看</a-link
              >
            </span>
          </template>
        </template>
      </a-table>
    </a-modal>

    <!-- 库存调整弹窗 -->
    <a-modal
      v-model:visible="adjustVisible"
      title="库存调整"
      @ok="handleAdjustSubmit"
      :confirmLoading="submitLoading"
      width="680px"
      :maskClosable="false"
      okText="确定"
      cancelText="取消"
    >
      <a-form
        ref="adjustFormRef"
        :model="adjustForm"
        :rules="adjustRules"
        :label-col="{ span: 6 }"
        :wrapper-col="{ span: 16 }"
      >
        <a-form-item label="药品名称">
          <span>{{ currentProduct?.name }}</span>
        </a-form-item>

        <a-form-item label="当前库存">
          <span>{{ currentProduct?.stock }}</span>
        </a-form-item>

        <a-form-item label="调整类型" name="adjustType" required>
          <a-radio-group v-model:value="adjustForm.adjustType">
            <a-radio value="increase">增加</a-radio>
            <a-radio value="decrease">减少</a-radio>
          </a-radio-group>
        </a-form-item>

        <a-form-item label="调整数量" name="quantity" required>
          <a-input-number
            v-model:value="adjustForm.quantity"
            :min="1"
            :max="
              adjustForm.adjustType === 'decrease'
                ? currentProduct?.stock
                : undefined
            "
            style="width: 100%"
            placeholder="请输入调整数量"
          />
        </a-form-item>

        <a-form-item
          label="生产批号"
          name="batchNumber"
          v-if="adjustForm.adjustType === 'increase'"
        >
          <a-input
            v-model:value="adjustForm.batchNumber"
            placeholder="请输入生产批号"
          />
        </a-form-item>

        <a-form-item
          label="生产日期"
          name="productionDate"
          v-if="adjustForm.adjustType === 'increase'"
        >
          <a-date-picker
            v-model:value="adjustForm.productionDate"
            style="width: 100%"
            :disabledDate="disabledProductionDate"
          />
        </a-form-item>

        <a-form-item
          label="有效期至"
          name="expirationDate"
          v-if="adjustForm.adjustType === 'increase'"
        >
          <a-date-picker
            v-model:value="adjustForm.expirationDate"
            style="width: 100%"
            :disabledDate="disabledExpirationDate"
          />
        </a-form-item>

        <a-form-item label="备注" name="remark">
          <a-textarea
            v-model:value="adjustForm.remark"
            :rows="2"
            placeholder="请输入调整原因"
          />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, reactive } from "vue";
import { message } from "ant-design-vue";
import {
  SearchOutlined,
  ReloadOutlined,
  ImportOutlined,
  ExportOutlined,
} from "@ant-design/icons-vue";
import dayjs from "dayjs";
import request from "@/utils/axios";

// 搜索表单
const searchForm = ref({
  keyword: "",
  category: undefined,
  stockStatus: undefined,
  dateRange: [],
  storeId: undefined,
});

// 药品分类 - 从药品管理页面获取的分类
const categories = [
  { label: '处方药', value: 'prescription' },
  { label: '非处方药', value: 'otc' },
  { label: '中药', value: 'chinese' },
  { label: '保健品', value: 'health' },
  { label: '医疗器械', value: 'device' }
]

// 表格列定义
const columns = [
  {
    title: "药品编号",
    dataIndex: "productCode",
    width: 120,
  },
  {
    title: "药品名称",
    dataIndex: "productName",
    width: 200,
  },
  {
    title: "分类",
    dataIndex: "category",
    width: 100,
    customRender: ({ text }) => {
      const category = categories.find((c) => c.value === text);
      return category ? category.label : text;
    },
  },
  {
    title: "所属门店",
    dataIndex: "storeName",
    width: 120,
  },
  {
    title: "当前库存",
    dataIndex: "quantity",
    width: 100,
  },
  {
    title: "库存状态",
    key: "stockStatus",
    width: 100,
  },
  {
    title: "最近入库",
    dataIndex: "lastUpdateTime",
    width: 180,
  },
  {
    title: "操作",
    key: "action",
    width: 180,
    fixed: "right",
  },
];

// 记录表格列定义
const recordColumns = [
  {
    title: "时间",
    dataIndex: "recordTime",
    width: 180,
    customRender: ({ text }) => dayjs(text).format("YYYY-MM-DD HH:mm"),
  },
  {
    title: "类型",
    key: "recordType",
    width: 100,
  },
  {
    title: "数量",
    dataIndex: "quantity",
    width: 100,
  },
  {
    title: "来源类型",
    key: "sourceType",
    width: 120,
  },
  {
    title: "操作人",
    dataIndex: "operatorName",
    width: 120,
  },
  {
    title: "备注",
    dataIndex: "remark",
  },
];

// 状态管理
const loading = ref(false);
const recordLoading = ref(false);
const submitLoading = ref(false);
const stockList = ref([]);
const total = ref(0);
const currentPage = ref(1);
const pageSize = ref(10);
const storeOptions = ref([]);
const productOptions = ref([]);
const stockRecords = ref([]);
const currentProduct = ref(null);
const selectedProductStock = ref(0);

// 弹窗状态
const inboundVisible = ref(false);
const outboundVisible = ref(false);
const recordVisible = ref(false);
const adjustVisible = ref(false);

// 表单引用
const inboundFormRef = ref();
const outboundFormRef = ref();
const adjustFormRef = ref();

// 入库表单
const inboundForm = ref({
  storeId: undefined,
  productId: undefined,
  quantity: 1,
  batchNumber: "",
  productionDate: null,
  expirationDate: null,
  remark: "",
});

// 出库表单
const outboundForm = ref({
  storeId: undefined,
  productId: undefined,
  quantity: 1,
  type: "sale",
  targetStoreId: undefined,
  remark: "",
});

// 库存调整表单
const adjustForm = ref({
  productId: undefined,
  storeId: undefined,
  adjustType: "increase",
  quantity: 1,
  batchNumber: "",
  productionDate: null,
  expirationDate: null,
  remark: "",
});

// 记录过滤
const recordFilter = ref({
  type: undefined,
  dateRange: [],
  productId: undefined,
  storeId: undefined,
});

// 表单验证规则
const inboundRules = {
  storeId: [{ required: true, message: "请选择门店", trigger: "change" }],
  productId: [{ required: true, message: "请选择药品", trigger: "change" }],
  quantity: [{ required: true, message: "请输入入库数量", trigger: "blur" }],
  batchNumber: [{ required: true, message: "请输入生产批号", trigger: "blur" }],
  productionDate: [
    { required: true, message: "请选择生产日期", trigger: "change" },
  ],
  expirationDate: [
    { required: true, message: "请选择有效期", trigger: "change" },
  ],
};

const outboundRules = {
  storeId: [{ required: true, message: "请选择门店", trigger: "change" }],
  productId: [{ required: true, message: "请选择药品", trigger: "change" }],
  quantity: [{ required: true, message: "请输入出库数量", trigger: "blur" }],
  type: [{ required: true, message: "请选择出库类型", trigger: "change" }],
  targetStoreId: [
    {
      required: true,
      message: "请选择目标分店",
      trigger: "change",
      validator: (rule, value) => {
        if (outboundForm.value.type === "transfer" && !value) {
          return Promise.reject("请选择目标分店");
        }
        return Promise.resolve();
      },
    },
  ],
};

const adjustRules = {
  adjustType: [
    { required: true, message: "请选择调整类型", trigger: "change" },
  ],
  quantity: [{ required: true, message: "请输入调整数量", trigger: "blur" }],
  batchNumber: [
    {
      required: true,
      message: "请输入生产批号",
      trigger: "blur",
      validator: (rule, value) => {
        if (adjustForm.value.adjustType === "increase" && !value) {
          return Promise.reject("请输入生产批号");
        }
        return Promise.resolve();
      },
    },
  ],
  productionDate: [
    {
      required: true,
      message: "请选择生产日期",
      trigger: "change",
      validator: (rule, value) => {
        if (adjustForm.value.adjustType === "increase" && !value) {
          return Promise.reject("请选择生产日期");
        }
        return Promise.resolve();
      },
    },
  ],
  expirationDate: [
    {
      required: true,
      message: "请选择有效期",
      trigger: "change",
      validator: (rule, value) => {
        if (adjustForm.value.adjustType === "increase" && !value) {
          return Promise.reject("请选择有效期");
        }
        return Promise.resolve();
      },
    },
  ],
};

// 分页配置
const pagination = computed(() => ({
  total: total.value,
  current: currentPage.value,
  pageSize: pageSize.value,
  showSizeChanger: true,
  pageSizeOptions: ["10", "20", "50"],
  showTotal: (total) => `共 ${total} 条记录`,
  onChange: (page, size) => {
    currentPage.value = page;
    pageSize.value = size;
    fetchStockList();
  },
  onShowSizeChange: (current, size) => {
    currentPage.value = 1;
    pageSize.value = size;
    fetchStockList();
  },
}));

// 记录分页配置
const recordPagination = computed(() => ({
  pageSize: 5,
  showSizeChanger: true,
  pageSizeOptions: ["5", "10", "20"],
  showTotal: (total) => `共 ${total} 条记录`,
}));

// 根据选择的门店过滤产品列表
const filteredProductOptions = computed(() => {
  if (!outboundForm.value.storeId) return [];
  return productOptions.value.filter((p) => p.stock > 0);
});

// 获取库存状态颜色
const getStockStatusColor = (record) => {
  if (record.quantity === 0) return "error";
  if (record.quantity <= 30) return "warning";
  return "success";
};

// 获取库存状态文本
const getStockStatusText = (record) => {
  if (record.quantity === 0) return "无库存";
  if (record.quantity <= 30) return "偏低";
  return "正常";
};

// 获取来源类型文本
const getSourceTypeText = (sourceType) => {
  const types = {
    purchase: "采购入库",
    order: "订单出库",
    manual: "手动调整",
    transfer: "调拨出库",
  };
  return types[sourceType] || sourceType;
};

// 日期禁用
const disabledProductionDate = (current) => {
  return current && current > dayjs().endOf("day");
};

const disabledExpirationDate = (current) => {
  return current && current < dayjs().endOf("day");
};

// 搜索过滤
const filterOption = (input, option) => {
  return option.children.toLowerCase().indexOf(input.toLowerCase()) >= 0;
};

// API调用
const fetchStockList = async () => {
  loading.value = true;
  try {
    const params = {
      page: currentPage.value - 1,
      size: pageSize.value,
    };

    if (searchForm.value.keyword) {
      params.keyword = searchForm.value.keyword;
    }

    if (searchForm.value.category) {
      params.category = searchForm.value.category;
    }

    if (searchForm.value.stockStatus) {
      params.stockStatus = searchForm.value.stockStatus;
    }

    // 从下拉框中选择的门店ID
    if (searchForm.value.storeId) {
      params.storeId = searchForm.value.storeId;
    }

    // 添加日期范围参数
    if (searchForm.value.dateRange && searchForm.value.dateRange.length === 2) {
      params.startDate = dayjs(searchForm.value.dateRange[0]).format("YYYY-MM-DD");
      params.endDate = dayjs(searchForm.value.dateRange[1]).format("YYYY-MM-DD");
    }

    const storeId = searchForm.value.storeId || (storeOptions.value.length > 0 ? storeOptions.value[0].id : 1);

    const res = await request({
      url: `/inventory/store/${storeId}`,
      method: "get",
      params,
    });

    if (res.code === 200) {
      stockList.value = res.data.content || [];
      total.value = res.data.totalElements || 0;
    } else {
      message.error(res.message || "获取库存列表失败");
    }
  } catch (error) {
    console.error("获取库存列表失败:", error);
    message.error("获取库存列表失败");
  } finally {
    loading.value = false;
  }
};

const fetchStores = async () => {
  try {
    const res = await request({
      url: "/store/all",
      method: "get",
    });

    if (res.code === 200) {
      storeOptions.value = res.data || [];
    } else {
      message.error(res.message || "获取门店列表失败");
    }
  } catch (error) {
    console.error("获取门店列表失败:", error);
    message.error("获取门店列表失败");
  }
};

const fetchProducts = async () => {
  try {
    const res = await request({
      url: "/products",
      method: "get",
      params: {
        size: 1000, // 获取足够多的产品
        status: "active",
      },
    });

    if (res.code === 200) {
      productOptions.value =
        res.data.content.map((product) => ({
          id: product.id,
          name: product.name,
          code: product.code,
          stock: product.stock || 0,
        })) || [];
    } else {
      message.error(res.message || "获取产品列表失败");
    }
  } catch (error) {
    console.error("获取产品列表失败:", error);
    message.error("获取产品列表失败");
  }
};

const fetchStockRecords = async () => {
  recordLoading.value = true;
  try {
    const params = {
      page: 0,
      size: 100,
    };

    if (recordFilter.value.type) {
      params.recordType = recordFilter.value.type;
    }

    if (recordFilter.value.productId) {
      params.productId = recordFilter.value.productId;
    }

    if (recordFilter.value.storeId) {
      params.storeId = recordFilter.value.storeId;
    }

    if (
      recordFilter.value.dateRange &&
      recordFilter.value.dateRange.length === 2
    ) {
      params.startDate = dayjs(recordFilter.value.dateRange[0]).format(
        "YYYY-MM-DD"
      );
      params.endDate = dayjs(recordFilter.value.dateRange[1]).format(
        "YYYY-MM-DD"
      );
    }

    const res = await request({
      url: "/warehouse/records",
      method: "get",
      params,
    });

    if (res.code === 200) {
      stockRecords.value = res.data.content || [];
    } else {
      message.error(res.message || "获取库存记录失败");
    }
  } catch (error) {
    console.error("获取库存记录失败:", error);
    message.error("获取库存记录失败");
  } finally {
    recordLoading.value = false;
  }
};

const fetchProductStockRecords = async (productId, storeId) => {
  recordLoading.value = true;
  try {
    const params = {
      page: 0,
      size: 100,
      storeId,
    };

    const res = await request({
      url: `/warehouse/records/product/${productId}`,
      method: "get",
      params,
    });

    if (res.code === 200) {
      stockRecords.value = res.data.content || [];
    } else {
      message.error(res.message || "获取产品库存记录失败");
    }
  } catch (error) {
    console.error("获取产品库存记录失败:", error);
    message.error("获取产品库存记录失败");
  } finally {
    recordLoading.value = false;
  }
};

// 处理函数
const handleSearch = () => {
  currentPage.value = 1; // 重置为第一页
  fetchStockList();
};

const handleReset = () => {
  searchForm.value = {
    keyword: "",
    category: undefined,
    stockStatus: undefined,
    dateRange: [],
    storeId: undefined,
  };
  currentPage.value = 1; // 重置为第一页
  fetchStockList();
};

const handleInbound = () => {
  inboundForm.value = {
    storeId: undefined,
    productId: undefined,
    quantity: 1,
    batchNumber: "",
    productionDate: null,
    expirationDate: null,
    remark: "",
  };
  inboundVisible.value = true;
};

const handleOutbound = () => {
  outboundForm.value = {
    storeId: undefined,
    productId: undefined,
    quantity: 1,
    type: "sale",
    targetStoreId: undefined,
    remark: "",
  };
  outboundVisible.value = true;
};

const handleStoreSelect = async (value) => {
  // 当选择门店时，获取该门店的药品库存
  try {
    const res = await request({
      url: `/inventory/store/${value}`,
      method: "get",
      params: {
        size: 1000,
      },
    });

    if (res.code === 200) {
      productOptions.value =
        res.data.content.map((item) => ({
          id: item.productId,
          name: item.productName,
          code: item.productCode,
          stock: item.quantity || 0,
        })) || [];
    } else {
      message.error(res.message || "获取门店药品列表失败");
    }
  } catch (error) {
    console.error("获取门店药品列表失败:", error);
    message.error("获取门店药品列表失败");
  }
};

const handleProductSelect = (value) => {
  const product = productOptions.value.find((item) => item.id === value);
  if (outboundForm.value.storeId) {
    selectedProductStock.value = product ? product.stock : 0;
  }
};

const handleInboundSubmit = async () => {
  try {
    await inboundFormRef.value.validate();
    submitLoading.value = true;

    // 确保日期格式正确
    const requestData = {
      ...inboundForm.value,
      productionDate: inboundForm.value.productionDate
        ? dayjs(inboundForm.value.productionDate).format("YYYY-MM-DD")
        : null,
      expirationDate: inboundForm.value.expirationDate
        ? dayjs(inboundForm.value.expirationDate).format("YYYY-MM-DD")
        : null,
    };

    const res = await request({
      url: "/warehouse/inbound",
      method: "post",
      data: requestData,
    });

    if (res.code === 201 || res.code === 200) {
      message.success("入库成功");
      inboundVisible.value = false;
      fetchStockList(); // 刷新库存列表
    } else {
      message.error(res.message || "入库失败");
    }
  } catch (error) {
    console.error("入库失败:", error);
    message.error("入库失败: " + (error.message || "未知错误"));
  } finally {
    submitLoading.value = false;
  }
};

const handleOutboundSubmit = async () => {
  try {
    await outboundFormRef.value.validate();
    submitLoading.value = true;

    // 准备请求数据
    const requestData = { ...outboundForm.value };

    // 调用出库API
    const res = await request({
      url: "/warehouse/outbound",
      method: "post",
      data: requestData,
    });

    if (res.code === 201) {
      message.success("出库成功");
      outboundVisible.value = false;
      fetchStockList(); // 刷新库存列表
    } else {
      message.error(res.message || "出库失败");
    }
  } catch (error) {
    console.error("出库失败:", error);
    message.error("出库失败: " + (error.message || "未知错误"));
  } finally {
    submitLoading.value = false;
  }
};

const handleStockRecord = (record) => {
  recordFilter.value = {
    type: undefined,
    dateRange: [],
    productId: record.productId,
    storeId: record.storeId,
  };

  fetchProductStockRecords(record.productId, record.storeId);
  recordVisible.value = true;
};

const handleAdjust = (record) => {
  currentProduct.value = {
    id: record.productId,
    name: record.productName,
    stock: record.quantity,
  };

  adjustForm.value = {
    productId: record.productId,
    storeId: record.storeId,
    adjustType: "increase",
    quantity: 1,
    batchNumber: "",
    productionDate: null,
    expirationDate: null,
    remark: "",
  };

  adjustVisible.value = true;
};

const handleAdjustSubmit = async () => {
  try {
    await adjustFormRef.value.validate();
    submitLoading.value = true;

    const requestData = {
      ...adjustForm.value,
      productionDate: adjustForm.value.productionDate
        ? dayjs(adjustForm.value.productionDate).format("YYYY-MM-DD")
        : null,
      expirationDate: adjustForm.value.expirationDate
        ? dayjs(adjustForm.value.expirationDate).format("YYYY-MM-DD")
        : null,
    };

    const res = await request({
      url: "/warehouse/adjust",
      method: "post",
      data: requestData,
    });

    if (res.code === 201) {
      message.success("库存调整成功");
      adjustVisible.value = false;
      fetchStockList(); // 刷新库存列表
    } else {
      message.error(res.message || "库存调整失败");
    }
  } catch (error) {
    console.error("库存调整失败:", error);
    message.error("库存调整失败: " + (error.message || "未知错误"));
  } finally {
    submitLoading.value = false;
  }
};

const viewSource = (record) => {
  // 实现查看来源逻辑
  if (!record.sourceId) {
    message.info("没有关联的来源信息");
    return;
  }

  if (record.sourceType === "purchase") {
    // 跳转到采购单详情
    window.open(`/#/purchase/detail/${record.sourceId}`);
  } else if (record.sourceType === "order") {
    // 跳转到订单详情
    window.open(`/#/order/detail/${record.sourceId}`);
  } else {
    message.info("无法查看该类型的来源信息");
  }
};

// 生命周期钩子
onMounted(() => {
  fetchStores();
  fetchProducts();
  fetchStockList();
});
</script>

<style lang="less" scoped>
.warehouse-container {
  padding: 16px;

  .filter-section {
    margin-bottom: 24px;
    padding: 24px;
    background: #fafafa;
    border-radius: 4px;
    
    .filter-row {
      display: flex;
      flex-wrap: wrap;
      margin-bottom: 16px;
      
      .ant-form-item {
        margin-right: 16px;
        margin-bottom: 8px;
      }
    }
    
    .action-row {
      display: flex;
      justify-content: space-between;
      flex-wrap: wrap;
      
      @media (max-width: 768px) {
        flex-direction: column;
        
        .operation-buttons {
          margin-top: 8px;
        }
      }
    }
  }

  :deep(.ant-form-item) {
    margin-bottom: 16px;
  }
}

.record-table {
  margin-top: 10px;
}

/* 响应式调整 */
@media (max-width: 576px) {
  .filter-section {
    .ant-form-item {
      width: 100%;
      margin-right: 0;
    }
    
    .ant-input, 
    .ant-select,
    .ant-picker {
      width: 100% !important;
    }
    
    .action-row {
      .ant-space {
        display: flex;
        flex-direction: column;
        width: 100%;
        
        .ant-btn {
          width: 100%;
          margin-bottom: 8px;
        }
      }
    }
  }
}

@media (min-width: 577px) and (max-width: 992px) {
  .filter-section {
    .filter-row {
      .ant-form-item {
        flex: 0 0 calc(50% - 16px);
      }
    }
  }
}
</style>
