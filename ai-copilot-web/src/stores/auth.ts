import request from '@/utils/request';
import { create } from 'zustand';

interface User {
    id: number;
    email: string;
    nickname: string;
    phone?: string;
    role: string;
    avatar?: string;
}

interface AuthState {
    token: string | null;
    user: User | null;
    loading: boolean;
    setToken: (token: string) => void;
    setUser: (user: User) => void;
    login: (account: string, password: string) => Promise<void>;
    register: (email: string, password: string, nickname: string, inviteCode?: string) => Promise<void>;
    logout: () => void;
    fetchProfile: () => Promise<void>;
}

export const useAuthStore = create<AuthState>((set) => ({
    token: localStorage.getItem('token'),
    user: null,
    loading: false,

    setToken: (token) => {
        localStorage.setItem('token', token);
        set({ token });
    },

    setUser: (user) => set({ user }),

    login: async (account, password) => {
        set({ loading: true });
        try {
            const res: any = await request.post('/auth/login', { account, password });
            localStorage.setItem('token', res.data.token);
            set({ token: res.data.token, user: res.data.user, loading: false });
        } catch {
            set({ loading: false });
            throw new Error('登录失败');
        }
    },

    register: async (email, password, nickname, inviteCode) => {
        set({ loading: true });
        try {
            const res: any = await request.post('/auth/register', { email, password, nickname, inviteCode });
            localStorage.setItem('token', res.data.token);
            set({ token: res.data.token, user: res.data.user, loading: false });
        } catch {
            set({ loading: false });
            throw new Error('注册失败');
        }
    },

    logout: () => {
        localStorage.removeItem('token');
        set({ token: null, user: null });
        window.location.href = '/login';
    },

    fetchProfile: async () => {
        const res: any = await request.get('/auth/me');
        set({ user: res.data });
    },
}));