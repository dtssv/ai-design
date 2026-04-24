import request from '@/utils/request';
import { create } from 'zustand';

interface AdminUser {
    id: number;
    email: string;
    nickname: string;
    role: string;
    avatarUrl?: string;
}

interface AuthState {
    token: string | null;
    user: AdminUser | null;
    loading: boolean;
    login: (account: string, password: string) => Promise<void>;
    logout: () => void;
    fetchProfile: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
    token: localStorage.getItem('admin_token'),
    user: null,
    loading: false,

    login: async (account, password) => {
        set({ loading: true });
        try {
            const res: any = await request.post('/auth/login', { account, password });
            localStorage.setItem('admin_token', res.data.token);
            set({ token: res.data.token, user: res.data.user, loading: false });
        } catch {
            set({ loading: false });
            throw new Error('登录失败');
        }
    },

    logout: () => {
        localStorage.removeItem('admin_token');
        set({ token: null, user: null });
        window.location.href = '/login';
    },

    fetchProfile: async () => {
        try {
            const res: any = await request.get('/auth/me');
            set({ user: res.data });
        } catch {
            localStorage.removeItem('admin_token');
            set({ token: null, user: null });
        }
    },
}));