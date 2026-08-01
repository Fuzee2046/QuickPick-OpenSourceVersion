import axios from 'axios';
import { showToast } from 'vant';
import 'vant/es/toast/style';

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 10000,
});

// 请求拦截器
request.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('token');
    if (token) {
      config.headers['Authorization'] = `Bearer ${token}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
request.interceptors.response.use(
  (response) => {
    const res = response.data;
    if (res.code !== 200) {
      showToast(res.msg || 'Error');
      return Promise.reject(new Error(res.msg || 'Error'));
    }
    return res.data;
  },
  (error) => {
    if (error.response && (error.response.status === 401 || error.response.status === 403)) {
      showToast('登录已过期或无权限，请重新登录');
      localStorage.clear();
      // 使用 window.location.hash 确保 hash 模式下跳转正确，或者直接用 reload
      if (window.location.hash !== '#/login') {
         window.location.href = '#/login';
         window.location.reload(); // 强制刷新以重置状态
      }
    } else {
      showToast(error.message || '网络异常');
    }
    return Promise.reject(error);
  }
);

export default request;
