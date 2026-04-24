import axios from 'axios';

const request = axios.create({
    baseURL: '/api/v1',
    timeout: 30000,
});

request.interceptors.request.use((config) => {
    const token = localStorage.getItem('token');
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
            localStorage.removeItem('token');
            window.location.href = '/login';
        }
        return Promise.reject(error);
    }
);

export default request;

export interface SseEventData {
    type: 'text_delta' | 'done' | 'error';
    content?: string;
    messageId?: number;
    snapshotId?: number;
    snapshotVersion?: number;
    model?: string;
    tokenUsage?: number;
    changedFiles?: string[];
    summary?: string;
    error?: string;
}

export function fetchSSE(
    url: string,
    body: object,
    onEvent: (event: SseEventData) => void,
    onDone?: () => void,
    onError?: (err: string) => void
) {
    const token = localStorage.getItem('token');
    fetch(`/api/v1${url}`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify(body),
    }).then(async (res) => {
        const reader = res.body?.getReader();
        const decoder = new TextDecoder();
        if (!reader) return;
        let buffer = '';
        while (true) {
            const { done, value } = await reader.read();
            if (done) break;
            buffer += decoder.decode(value, { stream: true });
            const lines = buffer.split('\n');
            buffer = lines.pop() || '';
            for (const line of lines) {
                if (!line.startsWith('data:')) continue;
                const jsonStr = line.slice(5).trim();
                if (!jsonStr || jsonStr === '[DONE]') continue;
                try {
                    const event: SseEventData = JSON.parse(jsonStr);
                    onEvent(event);
                    if (event.type === 'error') {
                        onError?.(event.error || '生成失败');
                    }
                } catch {
                    // 忽略非JSON行
                }
            }
        }
        onDone?.();
    }).catch((err) => {
        onError?.(err.message || '网络异常');
    });
}

export async function downloadFile(url: string, defaultName: string) {
    const token = localStorage.getItem('token');
    const res = await fetch(`/api/v1${url}`, {
        headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    if (!res.ok) throw new Error(`下载失败: ${res.status}`);
    const disposition = res.headers.get('Content-Disposition');
    const match = disposition?.match(/filename="?([^"]+)"?/);
    const fileName = match?.[1] || defaultName;
    const blob = await res.blob();
    const a = document.createElement('a');
    a.href = URL.createObjectURL(blob);
    a.download = fileName;
    document.body.appendChild(a);
    a.click();
    URL.revokeObjectURL(a.href);
    document.body.removeChild(a);
}