import axios from 'axios';
import router from '@/router';
import { message } from 'ant-design-vue';

// 创建 axios 实例
const instance = axios.create({
    baseURL: 'http://localhost:8081/api',  // 确认这里的 baseURL 是否正确
    timeout: 5000
});

// 请求拦截器
instance.interceptors.request.use(
    config => {
        // 从 localStorage 获取 token
        
        const token = localStorage.getItem('access_token');
        
        // 如果 token 存在，将其添加到请求头中
        if (!config.url.includes('/auth')) {
            config.headers.Authorization = `Bearer ${token}`;
        }        
        return config;
    },
    error => {
        return Promise.reject(error);
    }
);

// 响应拦截器
instance.interceptors.response.use(
    response => {
        // 直接返回响应数据
        return response.data;
    },
    error => {
        if (error.response) {
            // 如果响应状态码是 401，说明 token 已过期或无效
            if (error.response.code === 401) {
                // 清除本地存储的 token 和用户信息
                localStorage.removeItem('access_token');
                localStorage.removeItem('user_info');
                
                // 跳转到登录页面
                router.push('/login');
                
                message.error('登录已过期，请重新登录');
            }
            
            switch (error.response.code) {
                case 403: // 禁止访问
                    // 处理禁止访问的情况
                    console.error('Access forbidden');
                    break;
                    
                case 500: // 服务器错误
                    console.error('Server error');
                    break;
                    
                default:
                    console.error(error.response.data.message || 'Error');
            }
        }
        return Promise.reject(error);
    }
);

export default instance; 