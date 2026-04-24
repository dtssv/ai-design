import { useAuthStore } from '@/stores/auth';
import { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import styles from './Auth.module.css';

export default function Login() {
    const [account, setAccount] = useState('');
    const [password, setPassword] = useState('');
    const [error, setError] = useState('');
    const { login, loading } = useAuthStore();
    const navigate = useNavigate();

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setError('');
        try {
            await login(account, password);
            navigate('/dashboard');
        } catch (err: any) {
            setError(err.message || '登录失败');
        }
    };

    return (
        <div className={styles.container}>
            <div className={styles.card}>
                <div className={styles.header}>
                    <div className={styles.logoIcon}>AI</div>
                    <h1 className={styles.title}>欢迎回来</h1>
                    <p className={styles.subtitle}>登录你的 AI Copilot 账号</p>
                </div>
                <form onSubmit={handleSubmit} className={styles.form}>
                    {error && <div className={styles.error}>{error}</div>}
                    <div className={styles.field}>
                        <label>账号</label>
                        <input
                            type="text"
                            placeholder="邮箱 / 手机号 / 用户名"
                            value={account}
                            onChange={(e) => setAccount(e.target.value)}
                            required
                        />
                    </div>
                    <div className={styles.field}>
                        <label>密码</label>
                        <input
                            type="password"
                            placeholder="输入密码"
                            value={password}
                            onChange={(e) => setPassword(e.target.value)}
                            required
                        />
                    </div>
                    <button type="submit" className={`btn-primary ${styles.submitBtn}`} disabled={loading}>
                        {loading ? '登录中...' : '登录'}
                    </button>
                </form>
                <div className={styles.footer}>
                    还没有账号？<Link to="/register">立即注册</Link>
                </div>
            </div>
        </div>
    );
}