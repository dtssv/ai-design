import axios from 'axios';

const request = axios.create({
    baseURL: '/api/admin/v1',
    timeout: 30000,
});

request.interceptors.request.use((config) => {
    const token = localStorage.getItem('admin_token');
    if (token) {
        config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
});

request.interceptors.response.use(
    (response) => {
        const data = response.data;
        if (data.code !== 0) {
            const msg = data.message || '请求失败';
            console.error('[API Error]', msg);
            return Promise.reject(new Error(msg));
        }
        return data;
    },
    (error) => {
        if (error.response?.status === 401) {
            localStorage.removeItem('admin_token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default request;